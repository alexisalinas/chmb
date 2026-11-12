package com.banamex.cops.update_offer.service.impl;

import com.banamex.cops.update_offer.audit.AuditLogService;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationStartDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import com.banamex.cops.update_offer.kafka.KafkaSenderDashboard;
import com.banamex.cops.update_offer.mapper.UpdateOfferMapper;
import com.banamex.cops.update_offer.sender.EmsSender;
import com.banamex.cops.update_offer.sender.FulFillkafkaSender;
import com.banamex.cops.update_offer.service.UpdateOfferResponseFactory;
import com.banamex.cops.update_offer.service.UpdateOfferService;
import com.banamex.cops.update_offer.service.timeZoneValidation.TimeZoneValidationService;
import com.banamex.cops.update_offer.service.timeZoneValidation.UpdateOfferFlowConditions;
import com.banamex.cops.update_offer.suppressed.PerformSuppressionCheckService;
import com.banamex.cops.update_offer.validation.DtoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Equivale al proceso OpUpdateOffer de BW:
 * GetContext -> Log service request -> [RequestTimeZoneValidation] -> PerformSuppressionCheck
 *   -> JMS Queue Requestor (EMS) -> RsUpdateOffer -> KafkaSenderDashboard / FulFillKafkaSender
 *   -> SetContext -> Log service response -> End
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateOfferServiceImpl implements UpdateOfferService {

    /** Nombre de la operación para la validación de supresión (contains(CurrentOperationName, OperationName)). */
    private static final String OPERATION_NAME = "UpdateOffer";

    /** Header de la API del que sale {@code UpdatedBy} en el mensaje al EMS. */
    private static final String CLIENT_ID_HEADER = "client_id";

    private final EmsSender emsSender;
    private final UpdateOfferMapper updateOfferMapper;
    private final AuditLogService auditLogService;
    private final PerformSuppressionCheckService suppressionCheckService;
    private final TimeZoneValidationService timeZoneValidationService;
    private final KafkaSenderDashboard kafkaSenderDashboard;
    private final FulFillkafkaSender fulFillkafkaSender;
    private final DtoValidator dtoValidator;
    private final UpdateOfferResponseFactory responses;

    /** BW: lower-case($_globalVariables/COP/TimeZone/TranslateTimeZone) = "true" */
    @Value("${update-offer.timezone.translate-time-zone:true}")
    private boolean translateTimeZone;

    /** BW: $_globalVariables/ns6:GlobalVariables/COP/AbInitioTest. Con "Y" BW usa el camino de AbInitio. */
    @Value("${update-offer.ab-initio-test:N}")
    private String abInitioTest;

    /** Start/root/SysMode de TimeZoneValidation. {@code System} es el ChannelId del GetContext (header ChannelId). */
    @Value("${update-offer.timezone.sys-mode:RealTime}")
    private String tzSysMode;

    /**
     * {@inheritDoc}
     * Escribe el log del request, procesa cada oferta (zona horaria, supresión, EMS y Kafka)
     * y escribe el log de la respuesta.
     */
    @Override
    public List<UpdateOfferResponse> updateOffer(List<UpdateOfferRequest> requests, UpdateOfferHeaders headers) {
        auditLogService.logRequest(headers, requests);            // lo que recibe el micro: headers + JSON
        List<UpdateOfferResponse> responses = requests.stream()
                .map(request -> procesarOferta(request, headers))
                .toList();
        auditLogService.logResponse(headers, responses);          // End: lo que responde el micro (JSON)
        return responses;
    }

    private UpdateOfferResponse procesarOferta(UpdateOfferRequest request, UpdateOfferHeaders headers) {
        LocalDateTime requestTime = LocalDateTime.now();                 // hora en que se recibió la oferta

        try {
            return ejecutar(request, headers, requestTime);
        } catch (StepFailedException e) {
            log.error("Falló '{}' uuid={}: {}", e.getStep(), headers.getUuid(), e.getCause().toString());
            return responses.error(e.getStep(), e.getCause());
        } catch (Exception e) {
            log.error("Falló el proceso de la oferta uuid={}", headers.getUuid(), e);
            return responses.error("Proceso de la oferta", e);
        }
    }

    /** Ejecuta un paso y, si falla, lo marca con su nombre para decir en qué falló. */
    private <T> T paso(String nombre, java.util.function.Supplier<T> accion) {
        try {
            return accion.get();
        } catch (Exception e) {
            throw new StepFailedException(nombre, e);
        }
    }

    /** Un paso del flujo falló: lleva el nombre del paso y la causa. */
    private static final class StepFailedException extends RuntimeException {
        private final String step;

        StepFailedException(String step, Throwable cause) {
            super(cause);
            this.step = step;
        }

        String getStep() {
            return step;
        }
    }

    private UpdateOfferResponse ejecutar(UpdateOfferRequest request, UpdateOfferHeaders headers, LocalDateTime requestTime) {
        // 1) RequestTimeZoneValidation (solo si TranslateTimeZone = "true")
        String tzResponseCode = null;
        MapRequestResultDTO tz = null;
        if (translateTimeZone) {
            TimeZoneValidationStartDTO start = TimeZoneValidationStartDTO.builder()
                    .system(headers.getChannelId()).sysMode(tzSysMode).type("Request")
                    .date(dates(request)).time(times(request))
                    .build();
            TimeZoneValidationResult validation = paso("Validación de zona horaria", () -> {
                dtoValidator.validateOrThrow(start);
                return timeZoneValidationService.validate(start);
            });
            if (!UpdateOfferFlowConditions.continueToSuppression(validation.getResponseCode())) {
                UpdateOfferResponse res = new UpdateOfferResponse();
                res.setCode(validation.getResponseCode());
                res.setDescription(validation.getResponseText());
                return res;
            }
            tzResponseCode = validation.getResponseCode();
            tz = validation.getMapResult();
        }

        // 2) PerformSuppressionCheck: si la operación está suprimida, se responde sin llamar al EMS
        UpdateOfferResponse suppressed = paso("Validación de supresión", () -> checkSuppression(request, headers));
        if (suppressed != null) {
            return suppressed;
        }

        // 2.1) AbInitioTest = 'Y': en BW la respuesta sale de AbInitio (casos 3A y 3B del mapper). Aún no se migró.
        if ("Y".equals(abInitioTest)) {
            log.warn("AbInitioTest = Y uuid={}: el camino de AbInitio no está migrado; la oferta sigue por el JMS", headers.getUuid());
        }

        // 3) JMS Queue Requestor: arma el XML, lo envía al EMS y ESPERA su respuesta. No arma la respuesta del micro:
        //    sus ResponseCode y ResponseText son los dos valores que necesita Kafka, que va después.
        final String codigoZona = tzResponseCode;
        final MapRequestResultDTO zona = tz;
        String xml = paso("Armado del XML para el EMS", () -> updateOfferMapper.toEmsXml(request, requestTime, headers.get(CLIENT_ID_HEADER), codigoZona, zona));
        java.util.Map<String, String> propiedades = paso("Armado de las propiedades del mensaje", () ->
                updateOfferMapper.toEmsProperties(request, requestTime, headers.get(CLIENT_ID_HEADER), codigoZona, zona));
        String reply = paso("Envío al EMS", () -> emsSender.send(xml, propiedades));
        UpdateOfferResponse emsResponse = leerRespuestaDelEms(reply);

        // 4) KafkaSenderDashboard -> FulFillKafkaSender: dispara y olvida, no afecta la respuesta
        sendToKafka(request, headers, requestTime, emsResponse);

        // 5) End: la respuesta la arma el micro (éxito con la fecha de ECRMTimeZone; OfferStatus U, como el Map-Data de BW)
        return responses.success(request);
    }

    /** ResponseCode y ResponseText de la respuesta del JMS; {@code null} si no hubo respuesta o no se puede leer. */
    private UpdateOfferResponse leerRespuestaDelEms(String reply) {
        if (reply == null || reply.isBlank()) {
            return null;
        }
        try {
            return updateOfferMapper.toRestResponse(reply);
        } catch (Exception e) {
            log.warn("No se pudo leer la respuesta del EMS: {}", e.getMessage());
            return null;
        }
    }

    private void sendToKafka(UpdateOfferRequest request, UpdateOfferHeaders headers, LocalDateTime requestTime,
                             UpdateOfferResponse response) {
        try {
            if (fulFillkafkaSender.isEnabled()) {
                fulFillkafkaSender.sendAsync(kafkaSenderDashboard.build(request, headers, requestTime, response));
            } else {
                log.info("[KAFKA] Resultado: {}", responses.kafkaDisabledText());
            }
        } catch (Exception e) {
            log.error("KAFKA-ERROR no se pudo encolar uuid={}: {}", headers.getUuid(), e.toString(), e);
        }
    }

    private List<ValueItemDTO> dates(UpdateOfferRequest r) {
        return hasText(r.getAppointmentDate())
                ? List.of(new ValueItemDTO(r.getAppointmentDate(), "AppointmentDate")) : List.of();
    }

    /** Start/root/Time: hora de inicio y de fin (los nombres se usan después en OfferEventMapper). */
    private List<ValueItemDTO> times(UpdateOfferRequest r) {
        List<ValueItemDTO> out = new ArrayList<>();
        if (hasText(r.getAppointmentStartTime())) {
            out.add(new ValueItemDTO(r.getAppointmentStartTime(), "AppointmentStartTime"));
        }
        if (hasText(r.getAppointmentEndTime())) {
            out.add(new ValueItemDTO(r.getAppointmentEndTime(), "AppointmentEndTime"));
        }
        return out;
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    /** Devuelve la respuesta de supresión o null si la operación no está suprimida. */
    private UpdateOfferResponse checkSuppression(UpdateOfferRequest request, UpdateOfferHeaders headers) {
        StartRequestParams params = new StartRequestParams();
        params.setRequestingChannel(request.getChannelCode());
        params.setCountryCode(headers.getCountryCode());
        params.setCurrentOperationName(OPERATION_NAME);

        StartRequest startRequest = new StartRequest();
        startRequest.setRequestParams(params);
        dtoValidator.validateOrThrow(startRequest);

        FilteredSuppressionList filtered = suppressionCheckService.perform(startRequest);
        if (filtered == null || filtered.getResults() == null) {
            return null;
        }
        return filtered.getResults().stream()
                .filter(r -> r.getOperationDetails() != null)
                .flatMap(r -> r.getOperationDetails().stream())
                .filter(d -> "true".equalsIgnoreCase(d.getIndicator()))
                .findFirst()
                .map(d -> {
                    UpdateOfferResponse res = new UpdateOfferResponse();
                    res.setCode(d.getResponseCode());
                    res.setDescription(d.getResponseText());
                    return res;
                })
                .orElse(null);
    }
}

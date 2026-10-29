package com.banamex.cops.update_offer.service.impl;

import com.banamex.cops.update_offer.audit.AuditLogService;
import com.banamex.cops.update_offer.audit.TransactionContext;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import com.banamex.cops.update_offer.kafka.KafkaSenderDashboard;
import com.banamex.cops.update_offer.mapper.UpdateOfferMapper;
import com.banamex.cops.update_offer.sender.EmsSender;
import com.banamex.cops.update_offer.sender.FulFillkafkaSender;
import com.banamex.cops.update_offer.service.RqHeaderFactory;
import com.banamex.cops.update_offer.service.UpdateOfferService;
import com.banamex.cops.update_offer.service.timeZoneValidation.TimeZoneValidationService;
import com.banamex.cops.update_offer.service.timeZoneValidation.UpdateOfferFlowConditions;
import com.banamex.cops.update_offer.suppressed.PerformSuppressionCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final EmsSender emsSender;
    private final UpdateOfferMapper updateOfferMapper;
    private final RqHeaderFactory rqHeaderFactory;
    private final AuditLogService auditLogService;
    private final PerformSuppressionCheckService suppressionCheckService;
    private final TimeZoneValidationService timeZoneValidationService;
    private final KafkaSenderDashboard kafkaSenderDashboard;
    private final FulFillkafkaSender fulFillkafkaSender;

    /** BW: lower-case($_globalVariables/COP/TimeZone/TranslateTimeZone) = "true" */
    @Value("${update-offer.timezone.translate-time-zone:false}")
    private boolean translateTimeZone;

    /** Start/root/System y Start/root/SysMode de TimeZoneValidation (SUPUESTOS, ajustar). */
    @Value("${update-offer.timezone.system:COPS}")
    private String tzSystem;

    @Value("${update-offer.timezone.sys-mode:RT}")
    private String tzSysMode;

    @Override
    public List<UpdateOfferResponse> updateOffer(List<UpdateOfferRequest> requests, UpdateOfferHeaders headers) {
        return requests.stream()
                .map(request -> procesarOferta(request, headers))
                .toList();
    }

    private UpdateOfferResponse procesarOferta(UpdateOfferRequest request, UpdateOfferHeaders headers) {
        RqHeaderDTO rqHeader = rqHeaderFactory.create(headers);          // GetContext
        TransactionContext ctx = TransactionContext.from(rqHeader);

        UpdateOfferResponse response;
        try {
            auditLogService.logRequest(ctx, updateOfferMapper.toLogRequestXml(request, rqHeader)); // Log service request
            response = ejecutar(request, headers, rqHeader);
        } catch (Exception e) {
            log.error("Error procesando oferta", e);
            response = new UpdateOfferResponse();
            response.setCode("ERROR");
            response.setDescription("Error procesando oferta: " + e.getMessage());
        }
        // Log service response (también cuando la respuesta fue de supresión, de zona horaria o de error)
        auditLogService.logResponse(ctx, updateOfferMapper.toLogResponseXml(response));
        return response;
    }

    private UpdateOfferResponse ejecutar(UpdateOfferRequest request, UpdateOfferHeaders headers, RqHeaderDTO rqHeader) {
        // 1) RequestTimeZoneValidation (solo si TranslateTimeZone = "true")
        String tzResponseCode = null;
        MapRequestResultDTO tz = null;
        if (translateTimeZone) {
            TimeZoneValidationResult validation = timeZoneValidationService.validate(
                    tzSystem, tzSysMode, "Request", dates(request), times(request), List.of());
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
        UpdateOfferResponse suppressed = checkSuppression(request, headers);
        if (suppressed != null) {
            return suppressed;
        }

        // 3) JMS Queue Requestor: arma el XML, lo envía al EMS y convierte la respuesta (RsUpdateOffer)
        String xml = updateOfferMapper.toEmsXml(request, rqHeader, tzResponseCode, tz);
        String reply = emsSender.send(xml);
        UpdateOfferResponse response = updateOfferMapper.toRestResponse(reply);

        // 4) KafkaSenderDashboard -> FulFillKafkaSender: dispara y olvida, no afecta la respuesta
        sendToKafka(request, rqHeader, response);
        return response;
    }

    private void sendToKafka(UpdateOfferRequest request, RqHeaderDTO rqHeader, UpdateOfferResponse response) {
        try {
            if (fulFillkafkaSender.isEnabled()) {
                fulFillkafkaSender.sendAsync(kafkaSenderDashboard.build(request, rqHeader, response));
            }
        } catch (Exception e) {
            log.error("KAFKA-ERROR no se pudo encolar uuid={}: {}", rqHeader.getUuid(), e.toString(), e);
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

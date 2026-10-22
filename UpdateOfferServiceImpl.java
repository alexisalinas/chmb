package com.banamex.cops.update_offer.service.impl;

import com.banamex.cops.update_offer.audit.AuditLogService;
import com.banamex.cops.update_offer.audit.TransactionContext;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.mapper.UpdateOfferMapper;
import com.banamex.cops.update_offer.sender.EmsSender;
import com.banamex.cops.update_offer.service.RqHeaderFactory;
import com.banamex.cops.update_offer.service.UpdateOfferService;
import com.banamex.cops.update_offer.service.timeZoneValidation.TimeZoneValidationService;
import com.banamex.cops.update_offer.service.timeZoneValidation.UpdateOfferFlowConditions;
import com.banamex.cops.update_offer.suppressed.PerformSuppressionCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

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
        try {
            RqHeaderDTO rqHeader = rqHeaderFactory.create(headers);
            TransactionContext ctx = TransactionContext.from(rqHeader);

            // 1) RequestTimeZoneValidation (solo si TranslateTimeZone = "true")
            String tzResponseCode = null;
            MapRequestResultDTO tz = null;
            if (translateTimeZone) {
                TimeZoneValidationResult validation = timeZoneValidationService.validate(
                        tzSystem, tzSysMode, "Request",
                        dates(request), times(request), List.of());
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

            String xml = updateOfferMapper.toEmsXml(request, rqHeader, tzResponseCode, tz);

            // 3) Log service request -> EMS -> Log service response
            auditLogService.logRequest(ctx, xml);
            String xmlResponse = emsSender.send(xml);
            auditLogService.logResponse(ctx, xmlResponse);

            return updateOfferMapper.toRestResponse(xmlResponse);

        } catch (Exception e) {
            log.error("Error procesando oferta", e);
            UpdateOfferResponse errorResponse = new UpdateOfferResponse();
            errorResponse.setCode("ERROR");
            errorResponse.setDescription("Error procesando oferta: " + e.getMessage());
            return errorResponse;
        }
    }

    private List<ValueItemDTO> dates(UpdateOfferRequest r) {
        return hasText(r.getAppointmentDate())
                ? List.of(new ValueItemDTO(r.getAppointmentDate(), "AppointmentDate")) : List.of();
    }

    /** Solo la hora de inicio (en BW se usa Time[1]); ApptEndTime sigue pendiente. */
    private List<ValueItemDTO> times(UpdateOfferRequest r) {
        return hasText(r.getAppointmentStartTime())
                ? List.of(new ValueItemDTO(r.getAppointmentStartTime(), "AppointmentStartTime")) : List.of();
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

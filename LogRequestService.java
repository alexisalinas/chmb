package com.banamex.cops.getofferlistbykey.service;

import com.banamex.cops.getofferlistbykey.config.LoggingProperties;
import com.banamex.cops.getofferlistbykey.dto.RqHeaderDTO;
import com.banamex.cops.getofferlistbykey.dto.ClientDetailsDTO;
import com.banamex.cops.getofferlistbykey.dto.logging.LogDTO;
import com.banamex.cops.getofferlistbykey.dto.logging.LogHeaderDTO;
import com.banamex.cops.getofferlistbykey.dto.logging.RqLogDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Migración de /BW/Logging/LogRequest: Start -> (audit?) Map Data -> Log -> End. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogRequestService {

    private static final String SERVICE_VERSION = "1.0";
    private static final String LF = "\n";

    private final LoggingProperties props;
    private final ObjectMapper objectMapper;

    public void logRequest(RqLogDTO rqLog) {
        // Transición "Check Audit Enabled" vs "Bypass Audit Log"
        if (!isAuditEnabled()) {
            return;
        }
        try {
            LogDTO logDto = mapData(rqLog);   // Map Data
            writeLog(logDto);                 // Write To Log
        } catch (JsonProcessingException e) {
            // El log de auditoría nunca debe romper el flujo del servicio
            log.warn("No se pudo registrar el log de request", e);
        }
    }

    public void logRequest(RqHeaderDTO rqHeader, Object rqBody) {
        logRequest(RqLogDTO.builder().rqHeader(rqHeader).rqBody(rqBody).build());
    }

    private boolean isAuditEnabled() {
        String v = props.getAuditPointsEnabled();
        return v != null && v.startsWith("1");
    }

    private LogDTO mapData(RqLogDTO rqLog) throws JsonProcessingException {
        RqHeaderDTO h = rqLog.getRqHeader();
        ClientDetailsDTO c = h != null ? h.getClientDetails() : null;

        LogHeaderDTO header = LogHeaderDTO.builder()
                .applicationId(props.getApplicationName() + "_" + props.getApplicationCsiId())
                .componentName(props.getComponentName())
                .timestamp(h != null ? h.getDateTimeStamp() : null)
                .transactionId(" ESBUUID: " + (h != null ? h.getEsbUuid() : "")
                        + " UUID: " + (h != null ? h.getUuid() : "") + " ")
                .countryCode(c != null ? c.getDestCountryCode() : null)
                .serviceName(rqLog.getRqBody() != null ? rqLog.getRqBody().getClass().getSimpleName() : null)
                .serviceVersion(SERVICE_VERSION)
                .build();

        return LogDTO.builder()
                .header(header)
                .payload(LF + toPrettyJson(rqLog.getRqBody()) + LF)
                .build();
    }

    private void writeLog(LogDTO logDto) throws JsonProcessingException {
        String message = "Logging Service request" + LF
                + toPrettyJson(logDto.getHeader()) + LF
                + logDto.getPayload();
        log.info(message);
    }

    private String toPrettyJson(Object o) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
    }
}

package com.banamex.cops.update_offer.audit;

import com.banamex.cops.update_offer.dto.log.LogHeaderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Equivalente a /BW/Logging/LogRequest y LogResponse:
 * 1) revisa si la auditoria esta activa (Check Audit Enabled / Bypass Audit Log),
 * 2) arma el Header (Map Data),
 * 3) escribe: titulo + header en XML + payload en XML (Log).
 *
 * El payload ya llega como XML (lo generan los mappers), por eso se quitó JAXB.
 */
@Slf4j
@Component
public class AuditLogService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    // Deja solo los ultimos 4 digitos de la tarjeta (acepta <CardNO>, <CardNumber> y <ns1:CardNumber xmlns:ns1="..">)
    private static final Pattern CARD =
            Pattern.compile("(<(?:\\w+:)?(?:CardNumber|CardNO)(?:\\s[^>]*)?>)\\d*(\\d{4})(</)");

    @Value("${audit.enabled:true}")
    private boolean enabled;

    @Value("${audit.application-id:cop_163621}")
    private String applicationId;

    @Value("${audit.component-name:COPS}")
    private String componentName;

    @Value("${audit.country-code:MX}")
    private String countryCode;

    @Value("${audit.service-version:1.0}")
    private String serviceVersion;

    @Value("${audit.request-service-name:v3_0_0_0}")
    private String requestServiceName;

    @Value("${audit.response-service-name:Offer}")
    private String responseServiceName;

    /** xml = request en formato de log (UpdateOfferMapper.toLogRequestXml). */
    public void logRequest(TransactionContext ctx, String xml) {
        if (!enabled) {
            return;
        }
        write("Logging Service request:", buildHeader(ctx, requestServiceName, countryCode), xml);
    }

    /** xml = response en formato de log. El header del response no lleva CountryCode. */
    public void logResponse(TransactionContext ctx, String xml) {
        if (!enabled) {
            return;
        }
        write("Logging Service response:", buildHeader(ctx, responseServiceName, null), xml);
    }

    // Solo se llenan los campos obligatorios; Hostname, CorrelationID, etc. quedan vacios
    private LogHeaderDTO buildHeader(TransactionContext ctx, String serviceName, String country) {
        return LogHeaderDTO.builder()
                .applicationId(applicationId)
                .componentName(componentName)
                .timestamp(LocalDateTime.parse(ctx.getTimestamp()))
                .transactionId(ctx.transactionId())
                .countryCode(country)
                .serviceName(serviceName)
                .serviceVersion(serviceVersion)
                .build();
    }

    private void write(String title, LogHeaderDTO header, String xml) {
        try {
            String bodyXml = xml == null ? "" : CARD.matcher(xml).replaceAll("$1************$2$3");
            log.info("{}\n{}\n{}", title, headerXml(header), bodyXml);
        } catch (Exception e) {
            // La auditoria nunca debe romper el flujo principal
            log.warn("No se pudo escribir el log de auditoria: {}", e.getMessage());
        }
    }

    private String headerXml(LogHeaderDTO h) {
        StringBuilder x = new StringBuilder("<Header>\n");
        tag(x, "ApplicationID", h.getApplicationId());
        tag(x, "ComponentName", h.getComponentName());
        tag(x, "Timestamp", h.getTimestamp() == null ? null : h.getTimestamp().format(TS));
        tag(x, "TransactionID", h.getTransactionId());
        tag(x, "CountryCode", h.getCountryCode());
        tag(x, "ServiceName", h.getServiceName());
        tag(x, "ServiceVersion", h.getServiceVersion());
        return x.append("</Header>").toString();
    }

    private void tag(StringBuilder x, String name, String value) {
        if (value == null) return;
        String v = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        x.append("  <").append(name).append('>').append(v).append("</").append(name).append(">\n");
    }
}

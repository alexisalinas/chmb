package com.banamex.cops.update_offer.audit;

import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Log de entrada y de salida del micro, en JSON:
 *  - logRequest: lo que recibe el micro (los headers y el JSON del body).
 *  - logResponse: lo que responde el micro (el JSON de la respuesta; es el "End").
 *
 * No hay logs en los subprocesos. Los datos sensibles se enmascaran: el número de tarjeta (solo se ven
 * los últimos 4 dígitos) y los headers de seguridad (Authorization, Cookie, ...).
 *
 * AuditPointsEnabled (global de BW) es una cadena de banderas, no un booleano: la posición 1 activa el log
 * del request y la posición 4 el de la respuesta. Con 1001 salen los dos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie", "x-api-key", "api-key", "client_secret");

    private final JsonMapper jsonMapper;

    /** BW: COP/Logging/AuditPointsEnabled (ej. "1001"). */
    @Value("${audit.points-enabled:1001}")
    private String pointsEnabled;

    /** Lo que recibe el micro: headers + JSON del body. */
    public void logRequest(UpdateOfferHeaders headers, List<UpdateOfferRequest> requests) {
        if (!requestAuditEnabled()) {
            return;
        }
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("headers", receivedHeaders(headers));
            message.put("body", maskedBody(requests));
            log.info("[REQUEST] uuid={}\n{}", uuid(headers), pretty(message));
        } catch (Exception e) {
            log.warn("No se pudo escribir el log del request: {}", e.getMessage());
        }
    }

    /** Lo que responde el micro (el JSON de la respuesta final). */
    public void logResponse(UpdateOfferHeaders headers, List<UpdateOfferResponse> responses) {
        if (!responseAuditEnabled()) {
            return;
        }
        try {
            log.info("[RESPONSE] uuid={}\n{}", uuid(headers), pretty(responses));
        } catch (Exception e) {
            log.warn("No se pudo escribir el log de la respuesta: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ banderas de AuditPointsEnabled

    /** LogRequest: substring(AuditPointsEnabled, 1, 1) = "1"  -> posición 1. */
    private boolean requestAuditEnabled() {
        return pointsEnabled != null && pointsEnabled.length() >= 1 && pointsEnabled.charAt(0) == '1';
    }

    /**
     * LogResponse: substring(AuditPointsEnabled, 4, 1) = "1"  -> posición 4.
     * Si la cadena tiene menos de 4 caracteres o no existe, la condición es falsa (se toma el "Bypass Audit Log").
     */
    private boolean responseAuditEnabled() {
        return pointsEnabled != null && pointsEnabled.length() >= 4 && pointsEnabled.charAt(3) == '1';
    }

    // ------------------------------------------------------------------ armado y enmascarado

    /**
     * Serializa el objeto a JSON con sangría.
     * Atrapa {@code Exception} para compilar igual con Jackson 2 (excepción comprobada) y con Jackson 3
     * (excepción sin comprobar).
     *
     * @param value objeto a serializar
     * @return el JSON con sangría
     * @throws IllegalStateException si no se puede serializar
     */
    private String pretty(Object value) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar a JSON: " + e.getMessage(), e);
        }
    }

    private static String uuid(UpdateOfferHeaders headers) {
        return headers == null ? null : headers.getUuid();
    }

    /** Todos los headers recibidos; si no llegó el mapa completo, los del swagger. Los de seguridad van enmascarados. */
    private Map<String, Object> receivedHeaders(UpdateOfferHeaders h) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (h == null) {
            return out;
        }
        Map<String, String> source = new LinkedHashMap<>();
        if (h.getAll() != null && !h.getAll().isEmpty()) {
            source.putAll(h.getAll());
        } else {
            put(source, "uuid", h.getUuid());
            put(source, "host", h.getHost());
            put(source, "ChannelId", h.getChannelId());
            put(source, "countryCode", h.getCountryCode());
            put(source, "businessCode", h.getBusinessCode());
            put(source, "sid", h.getSid());
            put(source, "Accept-Language", h.getAcceptLanguage());
        }
        source.forEach((name, value) ->
                out.put(name, SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT)) ? "****" : value));
        return out;
    }

    private static void put(Map<String, String> map, String name, String value) {
        if (value != null) {
            map.put(name, value);
        }
    }

    /** El body tal como llegó, con el número de tarjeta enmascarado. */
    @SuppressWarnings("unchecked")
    private List<Object> maskedBody(List<UpdateOfferRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream().map(rq -> {
            Map<String, Object> fields = jsonMapper.convertValue(rq, Map.class);
            if (fields != null && fields.get("cardNumber") instanceof String card) {
                fields.put("cardNumber", mask(card));
            }
            return (Object) fields;
        }).toList();
    }

    /** Deja solo los últimos 4 dígitos. */
    static String mask(String s) {
        if (s == null || s.length() <= 4) {
            return s;
        }
        return "*".repeat(s.length() - 4) + s.substring(s.length() - 4);
    }
}

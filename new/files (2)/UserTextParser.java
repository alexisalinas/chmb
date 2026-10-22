package com.banamex.cops.update_offer.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el JSON de userText y devuelve UserField1..100 (índice -> valor).
 * En BW los UserFieldN llegaban como elementos sueltos; en el contrato REST nuevo viajan dentro de userText.
 *
 * Acepta: {"parameterName":"userField1","parameterValue":"..."}, un arreglo de esos objetos,
 * o {"userField1":"..."}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTextParser {

    private static final Pattern USER_FIELD = Pattern.compile("(?i)^userfield(\\d{1,3})$");

    private final JsonMapper jsonMapper;

    public Map<Integer, String> parse(String userText) {
        Map<Integer, String> out = new TreeMap<>();
        if (userText == null || userText.isBlank()) return out;
        try {
            JsonNode root = jsonMapper.readTree(userText);
            if (root.isArray()) root.forEach(n -> collect(n, out)); else collect(root, out);
        } catch (Exception e) {
            log.warn("userText no es un JSON válido; se ignoran los UserFields: {}", e.getMessage());
        }
        return out;
    }

    private void collect(JsonNode n, Map<Integer, String> out) {
        if (n.hasNonNull("parameterName")) {
            add(out, n.get("parameterName").asText(), n.path("parameterValue").asText(""));
            return;
        }
        n.properties().forEach(e -> add(out, e.getKey(), e.getValue().asText("")));
    }

    private void add(Map<Integer, String> out, String name, String value) {
        Matcher m = USER_FIELD.matcher(name == null ? "" : name.trim());
        if (m.matches()) {
            int i = Integer.parseInt(m.group(1));
            if (i >= 1 && i <= 100) out.put(i, value);
        }
    }
}

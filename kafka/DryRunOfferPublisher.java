package com.banamex.cops.update_offer.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Modo de prueba: solo escribe el JSON en el log (con la tarjeta enmascarada) y no envía nada. */
@Slf4j
@Component
@ConditionalOnProperty(name = "offer.kafka.mode", havingValue = "dry-run", matchIfMissing = true)
public class DryRunOfferPublisher implements OfferPublisher {

    private static final Pattern CARD = Pattern.compile("(\"CardNumber\"\\s*:\\s*\")(\\d*)(\\d{4})(\")");

    @Override
    public String publish(String refId, String json) {
        String masked = CARD.matcher(json).replaceAll(m ->
                m.group(1) + "*".repeat(m.group(2).length()) + m.group(3) + m.group(4));
        log.info("[KAFKA-DRY-RUN] refId={} JSON que se enviaría (tarjeta enmascarada):\n{}", refId, masked);
        return "dry-run";
    }
}

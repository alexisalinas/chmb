package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.service.CopOfferKafkaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Paso 2 (BW: /BW/Kafka/DataFetch). Consulta los datos de la oferta a través de la librería cops-bd
 * (CopOfferKafkaService), igual que el lookup de zona horaria.
 * Si la librería o la BD no están disponibles lanza IllegalStateException (falla de infraestructura: se reintenta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferDataClient {

    private final ObjectProvider<CopOfferKafkaService> service;

    /** @param waveKey WAVCH.EXTID$   @param offerKey OFFPAL.EXTID$ */
    public Optional<CopOfferKafkaData> find(String waveKey, String offerKey) {
        CopOfferKafkaService s = service.getIfAvailable();
        if (s == null) {
            throw new IllegalStateException(
                    "CopOfferKafkaService no está disponible: revisa que la librería cops-bd esté actualizada y la BD configurada");
        }
        List<CopOfferKafkaData> rows = s.findByWaveKeyAndOfferKey(waveKey, offerKey);
        if (rows != null && rows.size() > 1) {
            log.warn("KAFKA-MULTI-ROWS la consulta devolvió {} filas; se usa la primera. waveKey={} offerKey={}",
                    rows.size(), waveKey, offerKey);
        }
        return rows == null ? Optional.empty() : rows.stream().findFirst();
    }
}

package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.app.model.CopOfferFulfillment;
import com.banamex.cops.app.service.CopOfferFulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Paso 2 (BW: /BW/Kafka/DataFetch). Consulta los datos de la oferta a través de la librería
 * {@code chnn-cop-lib-operation-db} ({@code CopOfferFulfillmentService}), igual que el lookup de zona horaria.
 * Los beans de la librería los registra {@code OfferFulfillmentConfig}.
 * Si la librería o la BD no están disponibles lanza IllegalStateException (falla de infraestructura: se reintenta).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfferDataClient {

    private final ObjectProvider<CopOfferFulfillmentService> service;

    /**
     * Trae los datos de cumplimiento de una oferta. Si la consulta devuelve varias filas usa la primera.
     *
     * @param waveKey  clave de la ola con canal (D_WAVECHINFO.EXTID$)
     * @param offerKey clave de la oferta (D_OFFERPALETTE.EXTID$)
     * @return los datos de la oferta, o vacío si no hay ninguna fila
     * @throws IllegalStateException si el servicio de la consulta no está registrado en el contexto
     */
    public Optional<CopOfferFulfillment> find(String waveKey, String offerKey) {
        CopOfferFulfillmentService s = service.orderedStream().findFirst().orElse(null);
        if (s == null) {
            throw new IllegalStateException("CopOfferFulfillmentService no está disponible: revisa que exista "
                    + "OfferFulfillmentConfig (registra la consulta de la librería) y que haya un spring.datasource configurado");
        }
        List<CopOfferFulfillment> rows = s.findByWaveKeyAndOfferKey(waveKey, offerKey);
        if (rows != null && rows.size() > 1) {
            log.warn("KAFKA-MULTI-ROWS la consulta devolvió {} filas; se usa la primera. waveKey={} offerKey={}",
                    rows.size(), waveKey, offerKey);
        }
        return rows == null ? Optional.empty() : rows.stream().findFirst();
    }
}

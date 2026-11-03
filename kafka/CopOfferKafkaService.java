package com.banamex.cops.app.service;

import com.banamex.cops.app.model.CopOfferKafkaData;

import java.util.List;

public interface CopOfferKafkaService {

    /**
     * Datos de la oferta para el envío a Kafka.
     * No hay llave primaria garantizada, así que puede devolver más de una fila (quien llama usa la primera).
     *
     * @param waveKey  WAVCH.EXTID$ (por ejemplo wvch:OLA:CANAL)
     * @param offerKey OFFPAL.EXTID$ (por ejemplo off:mx:CAMPAÑA:OLA:CLIENTE[:CUENTA])
     */
    List<CopOfferKafkaData> findByWaveKeyAndOfferKey(String waveKey, String offerKey);
}

package com.banamex.cops.update_offer.kafka;

/** Paso 5: envía { RefId, jsonString } al destino. Devuelve el identificador que asigna el destino. */
public interface OfferPublisher {
    String publish(String refId, String json) throws Exception;
}

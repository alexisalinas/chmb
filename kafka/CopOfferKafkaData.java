package com.banamex.cops.app.model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Fila de la consulta de datos de la oferta para el envío a Kafka
 * (D_OFFERPALETTE + D_CAMPAIGN + D_PRODUCT + D_WAVECHINFO).
 *
 * userFields trae los 100 USERFIELD_n en orden: el índice 0 es USERFIELD_1 y el 99 es USERFIELD_100.
 * apptEndTimeDur es la duración en minutos que calcula la consulta (to_char), como texto.
 */
public record CopOfferKafkaData(
        String crpCategoryId,
        String prdType,
        String reasonCode,
        String reasonDesc,
        String prdDescription,
        String fulfillmentProcId,
        List<String> userFields,
        Timestamp apptStartDate,
        String apptEndTimeDur,
        Timestamp expiryDate,
        Timestamp offerCreationDate,
        String prdCode,
        String prdFamily,
        String availability) {
}

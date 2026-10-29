package com.banamex.cops.update_offer.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Paso 0: evento de entrada (lo que arma KafkaSenderDashboard en BW). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferInput {

    // header
    private String uuid;             // RqHeader.UUID (viene del header HTTP)
    private String esbUuid;          // RqHeader.ESBUUID  (va como "sid")
    private String channel;          // RequestingChannel
    private String transactionTime;

    // body
    private String refId;
    private String campaignId;
    private String waveId;
    private String customerNo;       // tal como llega, sin ceros
    private String accountNo;
    private String cardNumber;
    private String offerStatus;
    private String actionId;         // no se llena en ninguna capa de BW
    private String responseTime;
    private String responseCode;     // respuesta de la llamada al EMS
    private String responseText;
}

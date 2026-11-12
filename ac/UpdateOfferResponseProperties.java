package com.banamex.cops.update_offer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Mensajes de la respuesta que arma el micro por cada oferta (el {@code End} de BW).
 * <p>
 * Incluye también los textos del log con el resultado del envío a Kafka.
 * <p>
 * Los textos aceptan estos marcadores: {@code {dateTime}} (fecha y hora de {@code ECRMTimeZone}, con el formato de
 * {@code dateFormat}), {@code {step}} (paso donde falló) y {@code {detail}} (causa del error).
 */
@Data
@Component
@ConfigurationProperties(prefix = "update-offer.response")
public class UpdateOfferResponseProperties {

    /** Código cuando la oferta se procesó bien (el de los logs de BW: COP_UPDOF_0). */
    private String successCode = "COP_UPDOF_0";

    /** Texto de éxito. */
    private String successText = "Successfully update offer {dateTime}";

    /** Código cuando algo falla. */
    private String errorCode = "ERROR";

    /** Texto de error: la fecha y en qué paso falló. */
    private String errorText = "Error update offer {dateTime}. Falló en {step}: {detail}";

    /** Formato de la fecha ({@link java.time.format.DateTimeFormatter}): YYYY/MM/DD HH24:MI:SS. */
    private String dateFormat = "yyyy/MM/dd HH:mm:ss";

    /**
     * Si es {@code true}, las ofertas con {@code OfferStatus = "U"} responden con el formato del {@code Map-Data}
     * de BW (caso 2): código y texto armados con la petición y la llave {@code off:mx:...}, sin fecha.
     */
    private boolean statusUBwFormat = true;

    /** Caso {@code U}: código cuando la oferta se procesó (COP_UPDOF_0). */
    private String statusUSuccessCode = "COP_UPDOF_0";

    /** Caso {@code U}: texto de éxito; {@code {offerKey}} es la llave {@code off:mx:campaña_ola_cliente[_cuenta][_tarjeta]}. */
    private String statusUSuccessText = "Successfully update offer {offerKey}";

    /** Caso {@code U}: código cuando la petición no trae {@code CampaignID} (COP_UPDOF_432). */
    private String statusUCampaignMissingCode = "COP_UPDOF_432";

    /** Caso {@code U}: texto cuando falta {@code CampaignID}; {@code {campaignId}} es el valor recibido. */
    private String statusUCampaignMissingText = "Campaign cmp: {campaignId} is not present cannot proceed with the Update Offer";

    /** Texto del log cuando el evento se publicó en Kafka. */
    private String kafkaSuccessText = "Successfully send offer to Kafka {dateTime}";

    /** Texto del log cuando Kafka está desactivado ({@code offer.kafka.enabled = false}). */
    private String kafkaDisabledText = "Kafka desactivado (offer.kafka.enabled = false): no se envió nada {dateTime}";

    /** Texto del log cuando el modo de Kafka es dry-run (no se publica nada). */
    private String kafkaDryRunText = "Kafka en dry-run: el mensaje no se publicó {dateTime}";

    /** Texto del log cuando el evento no se pudo enviar a Kafka: la fecha y en qué falló. */
    private String kafkaErrorText = "Error send offer to Kafka {dateTime}. Falló en {step}: {detail}";
}

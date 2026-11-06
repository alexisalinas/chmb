package com.banamex.cops.update_offer.sender;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Piezas con las que BW arma el destinationQueue (variables globales):
 *   sector, Business, Región, ApplicationName, Application_CSI_ID, BankCardEnv y UpdateOffer.
 * countryCode es el DestCountryCode (va en minúscula en el nombre de la cola): su origen aún no está confirmado,
 * por ahora es el país de la cola (mx).
 */
@Data
@Component
@ConfigurationProperties(prefix = "update-offer.ems.queue-parts")
public class EmsQueueProperties {

    private String sector = "citi";
    private String business = "gcg";
    private String region = "la";
    private String applicationName = "cop";
    private String applicationCsiId = "163621";
    private String countryCode = "mx";
    private String bankCardEnv = "card";
    private String operation = "offer.update";
}

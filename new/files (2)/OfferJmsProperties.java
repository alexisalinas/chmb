package com.banamex.cops.update_offer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Globales de BW que usa el mapper del JMS Queue Requestor para CustomerNO:
 *  customerPadding        <- $_globalVariables/COP/Padding/CustomerPadding ("true"/"false")
 *  customerPaddingLength  <- $_globalVariables/COP/Padding/CustomerNo       (longitud de tib:pad-front)
 *  customerPaddingChar    <- $_globalVariables/COP/Padding/PadCharacter     (carácter de relleno)
 */
@Data
@Component
@ConfigurationProperties(prefix = "update-offer.jms-requestor")
public class OfferJmsProperties {

    private boolean customerPadding;
    private int customerPaddingLength = 10;
    private char customerPaddingChar = '0';
}

package com.banamex.cops.update_offer.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * destinationQueue del JMS Queue Requestor:
 *   concat(sector, '.', Business, '.', Región, '.', ApplicationName, '_', Application_CSI_ID, '.',
 *          DestCountryCode, '_', BankCardEnv, '.', UpdateOffer)
 * Con los valores de las globales: citi.gcg.la.cop_163621.mx_card.offer.update
 * El país va siempre en minúscula (el EMS distingue mayúsculas de minúsculas en el nombre de la cola).
 */
@Component
@RequiredArgsConstructor
public class EmsQueueNameBuilder {

    private final EmsQueueProperties p;

    /** Usa el país configurado en update-offer.ems.queue-parts.country-code (MX). */
    public String build() {
        return build(p.getCountryCode());
    }

    /** Para cuando se confirme de dónde sale el DestCountryCode. */
    public String build(String countryCode) {
        String country = countryCode == null ? "" : countryCode.toLowerCase(java.util.Locale.ROOT);
        return p.getSector() + "." + p.getBusiness() + "." + p.getRegion() + "."
                + p.getApplicationName() + "_" + p.getApplicationCsiId() + "."
                + country + "_" + p.getBankCardEnv() + "." + p.getOperation();
    }
}

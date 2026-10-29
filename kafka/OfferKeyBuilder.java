package com.banamex.cops.update_offer.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Arma las claves EXTID$ (y el RefID, que es la misma clave de la oferta). Usa los valores SIN modificar. */
@Component
@RequiredArgsConstructor
public class OfferKeyBuilder {

    private final OfferKafkaProps props;

    /** OFFPAL.EXTID$ = 'off:mx:' + CampaignID + sep + WaveID + sep + CustomerNo [+ sep + AccountNo]. */
    public String offerKey(String campaignId, String waveId, String customerNo, String accountNo) {
        String sep = props.getKeySeparator();
        return props.getOfferPrefix() + campaignId + sep + waveId + sep + customerNo
                + (isBlank(accountNo) ? "" : sep + accountNo);
    }

    /** WAVCH.EXTID$ = 'wvch:' + WaveID + sep + Channel. */
    public String waveKey(String waveId, String channel) {
        return props.getWavePrefix() + waveId + props.getKeySeparator() + channel;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}

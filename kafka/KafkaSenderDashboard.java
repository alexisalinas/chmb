package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Paso 0 (BW: actividad KafkaSenderDashboard dentro de OpUpdateOffer).
 * Toma el request, el RqHeader y la respuesta del EMS (UpdateOfferRes -> ResponseCode / ResponseText).
 * UUID = RqHeader.UUID y sid = RqHeader.ESBUUID. El RefID es la misma clave de la oferta (OFFPAL.EXTID$).
 */
@Component
@RequiredArgsConstructor
public class KafkaSenderDashboard {

    private final OfferKeyBuilder keys;

    public OfferInput build(UpdateOfferRequest rq, RqHeaderDTO header, UpdateOfferResponse rs) {
        String now = OffsetDateTime.now().toString();
        return OfferInput.builder()
                .uuid(header.getUuid())
                .esbUuid(header.getEsbUuid())
                .channel(rq.getChannelCode())                  // RequestingChannel
                .transactionTime(now)
                .refId(keys.offerKey(rq.getCampaignId(), rq.getWaveId(), rq.getCustomerId(), rq.getAccountNumber()))
                .campaignId(rq.getCampaignId())
                .waveId(rq.getWaveId())
                .customerNo(rq.getCustomerId())                // sin modificar (sin ceros)
                .accountNo(rq.getAccountNumber())
                .cardNumber(rq.getCardNumber())
                .offerStatus(rq.getOfferStatus())
                .actionId(null)                                // no se llena en ninguna capa de BW
                .responseTime(now)
                .responseCode(rs != null ? rs.getCode() : null)
                .responseText(rs != null ? rs.getDescription() : null)
                .build();
    }
}

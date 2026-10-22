package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fachada que usa el service (equivale a updateofferMapper.toSoapRequest / toRestResponse).
 * Solo delega en los mappers de XML.
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferMapper {

    private final UpdateOfferRequestXmlMapper requestXmlMapper;
    private final UpdateOfferResponseXmlMapper responseXmlMapper;

    /** XML que se envía al EMS (Envelope + RqHeader + evento UpdateOffer). */
    public String toEmsXml(UpdateOfferRequest rq, RqHeaderDTO header, String tzResponseCode, MapRequestResultDTO tz) {
        return requestXmlMapper.toEmsXml(rq, header, tzResponseCode, tz);
    }

    /** XML del request para "Log service request". */
    public String toLogRequestXml(UpdateOfferRequest rq, RqHeaderDTO header) {
        return requestXmlMapper.toLogXml(rq, header);
    }

    /** Respuesta del EMS (XML o JSON) -> respuesta REST. */
    public UpdateOfferResponse toRestResponse(String reply) {
        return responseXmlMapper.fromReply(reply);
    }

    /** XML de la respuesta REST para "Log service response". */
    public String toLogResponseXml(UpdateOfferResponse rs) {
        return responseXmlMapper.toLogXml(rs);
    }
}

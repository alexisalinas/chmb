package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fachada que usa el service (equivale a updateofferMapper.toSoapRequest / toRestResponse).
 * Solo delega en los mappers de XML.
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferMapper {

    private final UpdateOfferRequestXmlMapper requestXmlMapper;
    private final UpdateOfferResponseXmlMapper responseXmlMapper;

    /** Propiedades del mensaje JMS al EMS ({@code OtherProperties}): _nm_, _ns_ y los campos del evento. */
    public java.util.Map<String, String> toEmsProperties(UpdateOfferRequest rq, LocalDateTime requestTime, String updatedBy,
                                                         String tzResponseCode, MapRequestResultDTO tz) {
        return requestXmlMapper.toEmsProperties(rq, requestTime, updatedBy, tzResponseCode, tz);
    }

    /** XML que se envía al EMS (el evento UpdateOffer con sus campos). */
    public String toEmsXml(UpdateOfferRequest rq, LocalDateTime requestTime, String updatedBy,
                           String tzResponseCode, MapRequestResultDTO tz) {
        return requestXmlMapper.toEmsXml(rq, requestTime, updatedBy, tzResponseCode, tz);
    }

    /** Una respuesta por oferta (el EMS contesta con un UpdateOfferRes por cada oferta del mensaje). */
    public List<UpdateOfferResponse> toRestResponses(String reply) {
        return responseXmlMapper.fromReplies(reply);
    }

    /** Respuesta del EMS (XML o JSON) -> una sola respuesta REST. */
    public UpdateOfferResponse toRestResponse(String reply) {
        return responseXmlMapper.fromReply(reply);
    }
}

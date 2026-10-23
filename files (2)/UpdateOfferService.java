package com.banamex.cops.update_offer.service;

import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;

import java.util.List;

/**
 * Service contract for handling offer update operations.
 */
public interface UpdateOfferService {

    /**
     * Updates existing offers with the provided request data.
     *
     * @param requests the DTOs containing the offer data to update
     * @param headers  the HTTP headers (uuid, ChannelId, countryCode, ...) of the call
     * @return one response per request
     */
    List<UpdateOfferResponse> updateOffer(List<UpdateOfferRequest> requests, UpdateOfferHeaders headers);
}

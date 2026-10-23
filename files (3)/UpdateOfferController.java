package com.banamex.cops.update_offer.controller;

import com.banamex.cops.update_offer.config.HeaderValidation;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.service.UpdateOfferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rest controller that exposes the endpoint to update an existing offer.
 * Delegates the update logic to UpdateOfferService.
 *
 * Validaciones:
 *  - Headers requeridos (swagger): HeaderValidation responde 400 antes de llegar aquí.
 *  - countryCode: 2 letras (ISO 3166).
 *  - Body: cada UpdateOfferRequest se valida con Bean Validation (@Valid).
 */
@Slf4j
@RestController
@RequestMapping("/sd-campaing-management/v1/update-offer")
@RequiredArgsConstructor
@HeaderValidation.Required({"uuid", "ChannelId", "sid", "countryCode", "businessCode", "Accept", "Content-Type"})
public class UpdateOfferController {

    private final UpdateOfferService updateOfferService;

    /** Returns HTTP 200 with the update offer details when the request is processed successfully. */
    @PostMapping
    public ResponseEntity<List<UpdateOfferResponse>> updateOffer(
            @RequestHeader(value = "uuid", required = false) String uuid,
            @RequestHeader(value = "ChannelId", required = false) String channelId,
            @RequestHeader(value = "countryCode", required = false)
            @Pattern(regexp = "^[A-Za-z]{2}$", message = "countryCode debe tener 2 letras (ISO 3166)") String countryCode,
            @RequestHeader(value = "businessCode", required = false) String businessCode,
            @RequestHeader(value = "sid", required = false) String sid,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            @RequestBody List<@Valid UpdateOfferRequest> requests) {

        UpdateOfferHeaders headers = UpdateOfferHeaders.builder()
                .uuid(uuid)
                .channelId(channelId)
                .countryCode(countryCode)
                .businessCode(businessCode)
                .sid(sid)
                .acceptLanguage(acceptLanguage)
                .build();

        List<UpdateOfferResponse> responses = updateOfferService.updateOffer(requests, headers);

        return ResponseEntity.ok(responses);
    }
}

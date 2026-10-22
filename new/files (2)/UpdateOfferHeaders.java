package com.banamex.cops.update_offer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Headers HTTP del swagger que se usan para armar el RqHeader. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferHeaders {
    private String uuid;
    private String channelId;
    private String countryCode;
    private String businessCode;
    private String sid;
    private String acceptLanguage;
}

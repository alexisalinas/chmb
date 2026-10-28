package com.banamex.cops.update_offer.service;

import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Arma el RqHeader: el UUID viene del header uuid (NO se genera); solo el ESBUUID se genera = UUID + yyyyMMddHHmmss. */
@Component
public class RqHeaderFactory {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${update-offer.header.version:3.0}")
    private String version;
    @Value("${update-offer.header.org:Banamex}")
    private String org;
    @Value("${update-offer.header.org-unit:COPS}")
    private String orgUnit;
    @Value("${update-offer.header.channel-id:CAT}")
    private String defaultChannelId;
    @Value("${update-offer.header.terminal-id:1}")
    private String terminalId;
    @Value("${update-offer.header.user-id:ATM}")
    private String userId;
    @Value("${update-offer.header.country-code:MX}")
    private String defaultCountryCode;
    @Value("${update-offer.header.session-language-code:ES}")
    private String defaultLanguage;

    public RqHeaderDTO create(UpdateOfferHeaders h) {
        if (!hasText(h.getUuid())) {
            throw new IllegalArgumentException("El header uuid es obligatorio: el UUID no se genera, viene en el header");
        }
        String uuid = h.getUuid();
        LocalDateTime now = LocalDateTime.now();
        String country = hasText(h.getCountryCode()) ? h.getCountryCode() : defaultCountryCode;

        return RqHeaderDTO.builder()
                .dateTimeStamp(now)
                .uuid(uuid)
                .esbUuid(uuid + now.format(TS))
                .version(version)
                .clientDetails(ClientDetailsDTO.builder()
                        .org(org)
                        .orgUnit(orgUnit)
                        .channelID(hasText(h.getChannelId()) ? h.getChannelId() : defaultChannelId)
                        .terminalID(terminalId)
                        .userID(userId)
                        .srcCountryCode(country)
                        .destCountryCode(country)
                        .sessionLanguageCode(language(h.getAcceptLanguage()))
                        .build())
                .build();
    }

    /** "es-MX,es;q=0.9" -> "ES" */
    private String language(String acceptLanguage) {
        if (!hasText(acceptLanguage)) return defaultLanguage;
        String first = acceptLanguage.split("[,;]")[0].trim();
        String lang = first.split("-")[0].trim();
        return lang.isEmpty() ? defaultLanguage : lang.toUpperCase();
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
}

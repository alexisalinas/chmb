package com.banamex.cops.update_offer.dto.timeZoneValidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Salida de RequestTimeZoneValidation (ResponseCode, ResponseText, Formats y grupos Date/Time/DateTime). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeZoneValidationResult {
    private String responseCode;
    private String responseText;
    private TzFormatsDTO formats;
    private MapRequestResultDTO mapResult;
}

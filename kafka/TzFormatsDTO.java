package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** TimeZoneVar/Formats: los cinco campos son obligatorios (sin ?); pueden venir vacíos pero no ausentes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TzFormatsDTO {

    @NotNull private String timeZoneOffset;
    @NotNull private String dateFormat;
    @NotNull private String timeFormat;
    @NotNull private String dateTimeFormat;
    @NotNull private String timeZone;
}

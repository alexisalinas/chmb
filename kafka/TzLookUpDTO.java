package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** GetVariable/root/LookUp* (lista): los ocho campos son obligatorios (sin ?). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TzLookUpDTO {

    @NotNull private String systemName;
    @NotNull private String mode;
    @NotNull private String timeZone;
    @NotNull private String type;
    @NotNull private String timeOffset;
    @NotNull private String dateFormat;
    @NotNull private String timeFormat;
    @NotNull private String dateTimeFormat;
}

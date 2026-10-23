package com.banamex.cops.update_offer.dto.timeZoneValidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un elemento de GetVariable/root/LookUp (TZ_SYSTEM_NAME, TZ_MODE, ...). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TzLookUpDTO {
    private String systemName;
    private String mode;
    private String timeZone;
    private String type;
    private String timeOffset;
    private String dateFormat;
    private String timeFormat;
    private String dateTimeFormat;
}

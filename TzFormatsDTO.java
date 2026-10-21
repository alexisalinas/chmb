package com.banamex.cops.dto.timezonevalidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TzFormatsDTO {
    private String timeZoneOffset;
    private String dateFormat;
    private String timeFormat;
    private String dateTimeFormat;
    private String timeZone;
}

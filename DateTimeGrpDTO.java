package com.banamex.cops.dto.timezonevalidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeGrpDTO {
    private String dateTime;
    private String dateTimeStatus;
    private String dateTimeName;
}

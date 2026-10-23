package com.banamex.cops.update_offer.dto.timeZoneValidation;

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

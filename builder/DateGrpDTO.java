package com.banamex.cops.update_offer.dto.timeZoneValidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateGrpDTO {
    private String date;
    private String dateStatus;
    private String dateName;
}

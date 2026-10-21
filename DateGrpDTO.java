package com.banamex.cops.dto.timezonevalidation;

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

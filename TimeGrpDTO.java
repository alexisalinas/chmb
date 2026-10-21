package com.banamex.cops.dto.timezonevalidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeGrpDTO {
    private String time;
    private String timeStatus;
    private String timeName;
}

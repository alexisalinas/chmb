package com.banamex.cops.dto.timezonevalidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapRequestResultDTO {
    @Builder.Default private List<DateGrpDTO> dateGrp = new ArrayList<>();
    @Builder.Default private List<TimeGrpDTO> timeGrp = new ArrayList<>();
    @Builder.Default private List<DateTimeGrpDTO> dateTimeGrp = new ArrayList<>();
}

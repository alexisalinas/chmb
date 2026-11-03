package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Salida de MapRequest: DateGrp*, TimeGrp* y DateTimeGrp* son listas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapRequestResultDTO {

    @NotNull @Valid @Builder.Default private List<DateGrpDTO> dateGrp = new ArrayList<>();
    @NotNull @Valid @Builder.Default private List<TimeGrpDTO> timeGrp = new ArrayList<>();
    @NotNull @Valid @Builder.Default private List<DateTimeGrpDTO> dateTimeGrp = new ArrayList<>();
}

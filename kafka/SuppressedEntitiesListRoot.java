package com.banamex.cops.update_offer.dto.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** SuppressedEntitiesList: SuppressedEntities* es una lista. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppressedEntitiesListRoot {

    @NotNull
    @Valid
    @Builder.Default
    @JsonProperty("SuppressedEntities")
    private List<SuppressedEntityDTO> suppressedEntities = new ArrayList<>();
}

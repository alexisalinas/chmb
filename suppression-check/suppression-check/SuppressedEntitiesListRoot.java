package com.banamex.cops.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SuppressedEntitiesListRoot {

    @Builder.Default
    @JsonProperty("SuppressedEntities")
    private List<SuppressedEntityDTO> suppressedEntities = new ArrayList<>();
}

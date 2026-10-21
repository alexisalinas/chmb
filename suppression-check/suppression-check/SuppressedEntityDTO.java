package com.banamex.cops.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** SuppressedEntitiesList/SuppressedEntities (*). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppressedEntityDTO {

    /** Opcional */
    @JsonProperty("CountryCode")
    private String countryCode;

    @JsonProperty("Mode")
    private String mode;

    /** Opcional */
    @JsonProperty("ChannelId")
    private String channelId;

    @JsonProperty("Suppression_Flag")
    private String suppressionFlag;

    @JsonProperty("Operation_Flag")
    private String operationFlag;

    @JsonProperty("SourceSystem")
    private String sourceSystem;

    @Builder.Default
    @JsonProperty("OperationDetails")
    private List<SuppressedOperationDetailDTO> operationDetails = new ArrayList<>();
}

package com.banamex.cops.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SuppressedEntities/OperationDetails (OperationName, Operation_Flag, Req_Channel). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppressedOperationDetailDTO {

    @JsonProperty("OperationName")
    private String operationName;

    @JsonProperty("Operation_Flag")
    private String operationFlag;

    @JsonProperty("Req_Channel")
    private String reqChannel;
}

package com.banamex.cops.update_offer.dto.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** SuppressedEntities/OperationDetails* (lista). OperationName, Operation_Flag y Req_Channel son obligatorios. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationDetailsDTO {

    @NotBlank(message = "El campo OperationName es obligatorio")
    @JsonProperty("OperationName")
    private String operationName;

    @NotBlank(message = "El campo Operation_Flag es obligatorio")
    @JsonProperty("Operation_Flag")
    private String operationFlag;

    @NotBlank(message = "El campo Req_Channel es obligatorio")
    @JsonProperty("Req_Channel")
    private String reqChannel;
}

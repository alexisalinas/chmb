package com.cop.suppression.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
import lombok.Data;
import java.util.List;

/**
 * DTO que representa el nodo "SuppressedEntities" (cada elemento de la lista).
 */
@Data
public class SuppressedEntityDTO {

    // Tiene (?) -> Opcional
    @Nullable
    private String countryCode;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String mode;

    // Tiene (?) -> Opcional
    @Nullable
    private String channelId;

    // Sin símbolo -> Obligatorio (Usamos JsonProperty por el guion bajo)
    @NotNull
    @NotEmpty
    @JsonProperty("Suppression_Flag")
    private String suppressionFlag;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    @JsonProperty("Operation_Flag")
    private String operationFlag;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String sourceSystem;

    // Tiene un (*) -> Es una lista obligatoria
    @NotNull
    @Valid
    private List<OperationDetailsDTO> operationDetails;
}
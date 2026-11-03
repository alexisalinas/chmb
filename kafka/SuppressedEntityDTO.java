package com.banamex.cops.update_offer.dto.suppression;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** SuppressedEntitiesList/SuppressedEntities* (lista). CountryCode y ChannelId son opcionales (?). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppressedEntityDTO {

    // --- OPCIONALES (?) ---
    @JsonProperty("CountryCode")
    private String countryCode;

    @JsonProperty("ChannelId")
    private String channelId;

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo Mode es obligatorio")
    @JsonProperty("Mode")
    private String mode;

    @NotBlank(message = "El campo Suppression_Flag es obligatorio")
    @JsonProperty("Suppression_Flag")
    private String suppressionFlag;

    @NotBlank(message = "El campo Operation_Flag es obligatorio")
    @JsonProperty("Operation_Flag")
    private String operationFlag;

    @NotBlank(message = "El campo SourceSystem es obligatorio")
    @JsonProperty("SourceSystem")
    private String sourceSystem;

    // --- LISTA (*) ---
    @NotNull
    @Valid
    @Builder.Default
    @JsonProperty("OperationDetails")
    private List<OperationDetailsDTO> operationDetails = new ArrayList<>();
}

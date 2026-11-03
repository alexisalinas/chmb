package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** FilteredSuppressionList/Results* (cada elemento de la lista). Todos los campos simples son obligatorios. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilteredSuppressionResult {

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo CountryCode es obligatorio")
    private String countryCode;

    @NotBlank(message = "El campo ChannelID es obligatorio")
    private String channelID;

    @NotBlank(message = "El campo SuppressionFlag es obligatorio")
    private String suppressionFlag;

    @NotBlank(message = "El campo OperationFlag es obligatorio")
    private String operationFlag;

    @NotBlank(message = "El campo Mode es obligatorio")
    private String mode;

    @NotBlank(message = "El campo SourceSystem es obligatorio")
    private String sourceSystem;

    // --- LISTA (*) ---
    @NotNull
    @Valid
    @Builder.Default
    private List<OperationDetailOutput> operationDetails = new ArrayList<>();
}

package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de salida del nodo "OperationDetails" dentro de FilteredSuppressionList. Indicator es opcional (?). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationDetailOutput {

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo OperationName es obligatorio")
    private String operationName;

    @NotBlank(message = "El campo ReqChannel es obligatorio")
    private String reqChannel;

    @NotBlank(message = "El campo OperationFlag es obligatorio")
    private String operationFlag;

    @NotBlank(message = "El campo CountryCode es obligatorio")
    private String countryCode;

    @NotBlank(message = "El campo ResponseCode es obligatorio")
    private String responseCode;

    @NotBlank(message = "El campo ResponseText es obligatorio")
    private String responseText;

    // --- OPCIONAL (?) ---
    private String indicator;
}

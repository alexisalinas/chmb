package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Salida del End de PerformSuppressionCheck: ResponseDetails* (lista). Indicator es opcional (?). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDetailsDTO {

    // --- OPCIONAL (?) ---
    /** "true" / "false" */
    private String indicator;

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo ResponseText es obligatorio")
    private String responseText;

    @NotBlank(message = "El campo ResponseCode es obligatorio")
    private String responseCode;
}

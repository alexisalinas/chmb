package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Start/Request/RequestParams. RequestingChannel es opcional (?); CountryCode y CurrentOperationName son obligatorios. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartRequestParams {

    // --- OPCIONAL (?) ---
    private String requestingChannel;

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo CountryCode es obligatorio")
    private String countryCode;

    @NotBlank(message = "El campo CurrentOperationName es obligatorio")
    private String currentOperationName;
}

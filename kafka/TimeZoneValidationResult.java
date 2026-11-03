package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Salida de RequestTimeZoneValidation: ResponseCode y ResponseText obligatorios; Formats y el detalle, opcionales. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeZoneValidationResult {

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo ResponseCode es obligatorio")
    private String responseCode;

    @NotBlank(message = "El campo ResponseText es obligatorio")
    private String responseText;

    // --- OPCIONALES (?) ---
    private TzFormatsDTO formats;

    @Valid
    private MapRequestResultDTO mapResult;
}

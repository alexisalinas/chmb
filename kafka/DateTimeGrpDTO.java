package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DateTimeGrp* (lista del resultado de MapRequest): DateTime y DateTimeStatus son obligatorios
 * (el status es "" cuando todo está bien); DateTimeName es opcional (?).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeGrpDTO {

    // --- OBLIGATORIOS (sin ?) ---
    @NotNull private String dateTime;
    @NotNull private String dateTimeStatus;

    // --- OPCIONAL (?) ---
    private String dateTimeName;
}

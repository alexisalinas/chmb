package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DateGrp* (lista del resultado de MapRequest): Date y DateStatus son obligatorios
 * (el status es "" cuando todo está bien); DateName es opcional (?).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateGrpDTO {

    // --- OBLIGATORIOS (sin ?) ---
    @NotNull private String date;
    @NotNull private String dateStatus;

    // --- OPCIONAL (?) ---
    private String dateName;
}

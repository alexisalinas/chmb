package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TimeGrp* (lista del resultado de MapRequest): Time y TimeStatus son obligatorios
 * (el status es "" cuando todo está bien); TimeName es opcional (?).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeGrpDTO {

    // --- OBLIGATORIOS (sin ?) ---
    @NotNull private String time;
    @NotNull private String timeStatus;

    // --- OPCIONAL (?) ---
    private String timeName;
}

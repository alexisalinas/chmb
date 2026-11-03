package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Start de /BW/Common/TimeZoneValidation: System, SysMode y Type obligatorios; Date*, Time* y DateTime* son listas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeZoneValidationStartDTO {

    // --- OBLIGATORIOS (sin ?) ---
    @NotBlank(message = "El campo System es obligatorio")
    private String system;

    @NotBlank(message = "El campo SysMode es obligatorio")
    private String sysMode;

    @NotBlank(message = "El campo Type es obligatorio")
    private String type;

    // --- LISTAS (*) ---
    @NotNull
    @Valid
    @Builder.Default
    private List<ValueItemDTO> date = new ArrayList<>();

    @NotNull
    @Valid
    @Builder.Default
    private List<ValueItemDTO> time = new ArrayList<>();

    @NotNull
    @Valid
    @Builder.Default
    private List<ValueItemDTO> dateTime = new ArrayList<>();
}

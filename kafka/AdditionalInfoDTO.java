package com.banamex.cops.update_offer.dto.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Log/AdditionalInfo* (lista). Name y Value son obligatorios. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoDTO {

    @NotBlank(message = "El campo Name es obligatorio")
    @JsonProperty("Name")
    private String name;

    @NotBlank(message = "El campo Value es obligatorio")
    @JsonProperty("Value")
    private String value;
}

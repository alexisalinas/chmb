package com.banamex.cops.getofferlistbykey.dto.logging;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoDTO {

    @NotBlank
    @JsonProperty("Name")
    private String name;

    @NotBlank
    @JsonProperty("Value")
    private String value;
}

package com.banamex.cops.suppression;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartRequestParams {

    /** Opcional (RequestingChannel?) */
    private String requestingChannel;

    @NotBlank
    private String countryCode;

    @NotBlank
    private String currentOperationName;
}

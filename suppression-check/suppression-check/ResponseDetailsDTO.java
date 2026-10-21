package com.banamex.cops.suppression;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Salida del End de PerformSuppressionCheck (ResponseDetails). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDetailsDTO {

    /** "true" / "false" */
    private String indicator;
    private String responseText;
    private String responseCode;
}

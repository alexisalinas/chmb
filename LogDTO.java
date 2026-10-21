package com.banamex.cops.getofferlistbykey.dto.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Salida del Map Data (BW: Log). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogDTO {

    @JsonProperty("Header")
    private LogHeaderDTO header;

    @JsonProperty("Payload")
    private String payload;

    @JsonProperty("ProcessState")
    private String processState;

    @JsonProperty("PayloadType")
    private String payloadType;
}

package com.banamex.cops.update_offer.dto.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Salida del Map Data de LogRequest (nodo Log). Todos los hijos de Log son opcionales (?) salvo la lista
 * AdditionalInfo (*). TimeDuration, TraceLevel, AdditionalInfo, Payload, ProcessState y PayloadType son
 * hermanos de Header (no hijos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogDTO {

    // --- OPCIONALES (?) ---

    @Valid
    @JsonProperty("Header")
    private LogHeaderDTO header;

    @JsonProperty("TimeDuration")
    private Integer timeDuration;

    @JsonProperty("TraceLevel")
    private String traceLevel;

    @JsonProperty("Payload")
    private String payload;

    @JsonProperty("ProcessState")
    private String processState;

    @JsonProperty("PayloadType")
    private String payloadType;

    // --- LISTA (*) ---

    @NotNull
    @Valid
    @Builder.Default
    @JsonProperty("AdditionalInfo")
    private List<AdditionalInfoDTO> additionalInfo = new ArrayList<>();
}

package com.banamex.cops.getofferlistbykey.dto.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogHeaderDTO {

    // --- Obligatorios ---
    @JsonProperty("ApplicationID")
    private String applicationId;

    @JsonProperty("ComponentName")
    private String componentName;

    @JsonProperty("Timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("TransactionID")
    private String transactionId;

    // --- Opcionales ---
    @JsonProperty("Hostname")
    private String hostname;

    @JsonProperty("TransactionDomain")
    private String transactionDomain;

    @JsonProperty("CorrelationID")
    private String correlationId;

    @JsonProperty("CountryCode")
    private String countryCode;

    @JsonProperty("ServiceName")
    private String serviceName;

    @JsonProperty("ServiceVersion")
    private String serviceVersion;

    @JsonProperty("OperationCode")
    private String operationCode;

    @JsonProperty("OperationRevision")
    private String operationRevision;

    @JsonProperty("TimeDuration")
    private Integer timeDuration;

    @JsonProperty("TraceLevel")
    private String traceLevel;

    @JsonProperty("AdditionalInfo")
    private List<AdditionalInfoDTO> additionalInfo;
}

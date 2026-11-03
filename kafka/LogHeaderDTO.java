package com.banamex.cops.update_offer.dto.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Log/Header. Esquema de BW: sin símbolo = obligatorio, ? = opcional. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogHeaderDTO {

    // --- OBLIGATORIOS (sin ?) ---

    @NotBlank(message = "El campo ApplicationID es obligatorio")
    @JsonProperty("ApplicationID")
    private String applicationId;

    @NotBlank(message = "El campo ComponentName es obligatorio")
    @JsonProperty("ComponentName")
    private String componentName;

    @NotNull(message = "El campo Timestamp es obligatorio")
    @JsonProperty("Timestamp")
    private LocalDateTime timestamp;

    @NotBlank(message = "El campo TransactionID es obligatorio")
    @JsonProperty("TransactionID")
    private String transactionId;

    // --- OPCIONALES (?) ---

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
}

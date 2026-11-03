package com.banamex.cops.update_offer.dto.log.header;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** RqHeader. Esquema de BW: sin símbolo = obligatorio, ? = opcional. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cabecera de la petición (Request Header)")
public class RqHeaderDTO {

    // --- OBLIGATORIOS (sin ?) ---

    @NotNull(message = "El DateAndTimeStamp es obligatorio")
    @JsonProperty("DateAndTimeStamp")
    @Schema(description = "Marca de tiempo de la petición", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime dateTimeStamp;

    @NotBlank(message = "El UUID es obligatorio")
    @JsonProperty("UUID")
    @Schema(description = "Identificador único universal de la petición (viene en el header HTTP uuid)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;

    // @Valid: al validar el RqHeader se validan también los obligatorios de ClientDetails
    @NotNull(message = "El objeto ClientDetails no puede ser nulo")
    @Valid
    @JsonProperty("ClientDetails")
    @Schema(description = "Objeto anidado con los detalles del cliente", requiredMode = Schema.RequiredMode.REQUIRED)
    private ClientDetailsDTO clientDetails;

    // --- OPCIONALES (?) ---

    @JsonProperty("ESBUUID")
    @Schema(description = "UUID del ESB (uuid + yyyyMMddHHmmss)")
    private String esbUuid;

    @JsonProperty("Version")
    @Schema(description = "Versión del mensaje")
    private String version;
}

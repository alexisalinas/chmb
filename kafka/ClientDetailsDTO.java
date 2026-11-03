package com.banamex.cops.update_offer.dto.log.header;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** RqHeader/ClientDetails. Esquema de BW: sin símbolo = obligatorio, ? = opcional. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalles del cliente")
public class ClientDetailsDTO {

    // --- OBLIGATORIOS (sin ?) ---

    @NotBlank(message = "El campo Org es obligatorio")
    @JsonProperty("Org")
    @Schema(description = "Organización", requiredMode = Schema.RequiredMode.REQUIRED)
    private String org;

    @NotBlank(message = "El campo OrgUnit es obligatorio")
    @JsonProperty("OrgUnit")
    @Schema(description = "Unidad de la organización", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orgUnit;

    @NotBlank(message = "El campo ChannelID es obligatorio")
    @JsonProperty("ChannelID")
    @Schema(description = "ID del canal", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelID;

    @NotBlank(message = "El campo TerminalID es obligatorio")
    @JsonProperty("TerminalID")
    @Schema(description = "ID del terminal", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalID;

    @NotBlank(message = "El campo UserID es obligatorio")
    @JsonProperty("UserID")
    @Schema(description = "ID del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userID;

    @NotBlank(message = "El campo SrcCountryCode es obligatorio")
    @JsonProperty("SrcCountryCode")
    @Schema(description = "Código de país de origen", requiredMode = Schema.RequiredMode.REQUIRED)
    private String srcCountryCode;

    @NotBlank(message = "El campo DestCountryCode es obligatorio")
    @JsonProperty("DestCountryCode")
    @Schema(description = "Código de país de destino", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destCountryCode;

    // --- OPCIONALES (?) ---

    @JsonProperty("ClientIPAddress")
    @Schema(description = "Dirección IP del cliente")
    private String clientIPAddress;

    @JsonProperty("UserDomicileBranchCode")
    @Schema(description = "Código de sucursal de domicilio del usuario")
    private String userDomicileBranchCode;

    @JsonProperty("UserDomicileResponsibilityCentre")
    @Schema(description = "Centro de responsabilidad de domicilio del usuario")
    private String userDomicileResponsibilityCentre;

    @JsonProperty("ProcessingBranchCode")
    @Schema(description = "Código de sucursal de procesamiento")
    private String processingBranchCode;

    @JsonProperty("ProcessingResponsibilityCentre")
    @Schema(description = "Centro de responsabilidad de procesamiento")
    private String processingResponsibilityCentre;

    @JsonProperty("UserGroup")
    @Schema(description = "Grupo del usuario")
    private String userGroup;

    @JsonProperty("SessionLanguageCode")
    @Schema(description = "Código de idioma de la sesión")
    private String sessionLanguageCode;
}

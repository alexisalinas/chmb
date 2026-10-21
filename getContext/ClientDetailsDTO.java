import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalles del cliente")
public class ClientDetailsDTO {

    // --- CAMPOS OBLIGATORIOS (Asumidos por lógica de negocio) ---

    @NotBlank(message = "El campo Org es obligatorio")
    @JsonProperty("Org")
    @Schema(description = "Organización", requiredMode = Schema.RequiredMode.REQUIRED)
    private String org;

    @NotBlank(message = "El campo UserID es obligatorio")
    @JsonProperty("UserID")
    @Schema(description = "ID del usuario", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userID;

    @NotBlank(message = "El campo ChannelID es obligatorio")
    @JsonProperty("ChannelID")
    @Schema(description = "ID del canal", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelID;

    // --- CAMPOS OPCIONALES (Sin anotaciones de validación obligatoria) ---

    @JsonProperty("OrgUnit")
    @Schema(description = "Unidad de la organización")
    private String orgUnit;

    @JsonProperty("TerminalID")
    @Schema(description = "ID del terminal")
    private String terminalID;

    @JsonProperty("ClientIPAddress")
    @Schema(description = "Dirección IP del cliente")
    private String clientIPAddress;

    @JsonProperty("SrcCountryCode")
    @Schema(description = "Código de país de origen")
    private String srcCountryCode;

    @JsonProperty("DestCountryCode")
    @Schema(description = "Código de país de destino")
    private String destCountryCode;

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
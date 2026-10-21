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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cabecera de la petición (Request Header)")
public class RqHeaderDTO {

    // --- CAMPOS OBLIGATORIOS ---

    @NotNull(message = "El DateTimeStamp es obligatorio")
    @JsonProperty("DateTimeStamp")
    @Schema(description = "Marca de tiempo de la petición", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime dateTimeStamp; 

    @NotBlank(message = "El UUID es obligatorio")
    @JsonProperty("UUID")
    @Schema(description = "Identificador único universal de la petición", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;

    @NotBlank(message = "La Version es obligatoria")
    @JsonProperty("Version")
    @Schema(description = "Versión del mensaje", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    // @Valid se usa aquí para que, al validar el RqHeader, se validen también los campos obligatorios dentro de ClientDetails
    @NotNull(message = "El objeto ClientDetails no puede ser nulo")
    @Valid 
    @JsonProperty("ClientDetails")
    @Schema(description = "Objeto anidado con los detalles del cliente", requiredMode = Schema.RequiredMode.REQUIRED)
    private ClientDetailsDTO clientDetails;

    // --- CAMPOS OPCIONALES ---

    @JsonProperty("ESBUUID")
    @Schema(description = "UUID del ESB (Enterprise Service Bus)")
    private String esbUuid;
}
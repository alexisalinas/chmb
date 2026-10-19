import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO que representa el nodo "Results" (cada elemento de la lista).
 */
@Data
public class FilteredSuppressionResult {

    // Todos los campos RBC sin (?) son obligatorios
    @NotNull
    @NotEmpty
    private String countryCode;

    @NotNull
    @NotEmpty
    private String channelID;

    @NotNull
    @NotEmpty
    private String suppressionFlag;

    @NotNull
    @NotEmpty
    private String operationFlag;

    @NotNull
    @NotEmpty
    private String mode;

    @NotNull
    @NotEmpty
    private String sourceSystem;

    // El nodo OperationDetails tiene un (*) -> Es una lista obligatoria
    @NotNull
    @Valid
    private List<OperationDetail> operationDetails;
}
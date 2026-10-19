import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO que representa el nodo "OperationDetails" (cada elemento de la lista).
 */
@Data
public class OperationDetailsDTO {

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String operationName;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    @JsonProperty("Operation_Flag")
    private String operationFlag;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    @JsonProperty("Req_Channel")
    private String reqChannel;
}
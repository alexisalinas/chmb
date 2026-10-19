import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
import lombok.Data;

/**
 * DTO que representa el nodo "Date".
 */
@Data
public class TimeZoneValidationStartDate {

    // Con (?) -> Opcional
    @Nullable
    private String dateVal;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String dateName;
}
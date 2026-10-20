import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
import lombok.Data;

/**
 * DTO que representa el nodo "DateTime".
 */
@Data
public class TimeZoneValidationStartDateTime {

    // Con (?) -> Opcional
    @Nullable
    private String dateTimeVal;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String dateTimeName;
}
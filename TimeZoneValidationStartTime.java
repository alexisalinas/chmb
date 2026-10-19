import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
import lombok.Data;

/**
 * DTO que representa el nodo "Time".
 */
@Data
public class TimeZoneValidationStartTime {

    // Con (?) -> Opcional
    @Nullable
    private String timeVal;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String timeName;
}
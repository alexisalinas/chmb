import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO Principal que representa el nodo "root" para el paso GetVariable.
 */
@Data
public class TimeZoneValidationGetVariableRoot {

    // El nodo LookUp tiene un (*) -> Es una lista.
    // Al no tener (?) asumimos que la lista en sí es obligatoria (debe venir en el JSON).
    @NotNull
    @Valid
    private List<TimeZoneValidationGetVariableLookUp> lookUp;
}
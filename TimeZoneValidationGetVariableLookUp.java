import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO que representa el nodo "LookUp".
 * Todos los campos son de tipo RBC (String) y no tienen (?) por lo que son obligatorios.
 */
@Data
public class TimeZoneValidationGetVariableLookUp {

    @NotNull
    @NotEmpty
    private String systemName;

    @NotNull
    @NotEmpty
    private String mode;

    @NotNull
    @NotEmpty
    private String timeZone;

    @NotNull
    @NotEmpty
    private String type;

    @NotNull
    @NotEmpty
    private String timeOffset;

    @NotNull
    @NotEmpty
    private String dateFormat;

    @NotNull
    @NotEmpty
    private String timeFormat;

    @NotNull
    @NotEmpty
    private String dateTimeFormat;
}
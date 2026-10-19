import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO Principal que representa el nodo "root" dentro de "Start".
 */
@Data
public class TimeZoneValidationStartRoot {

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String system;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String sysMode;

    // Sin símbolo -> Obligatorio
    @NotNull
    @NotEmpty
    private String type;

    // Con (*) -> Lista (puede haber muchos)
    @NotNull
    private List<TimeZoneValidationStartDate> date;

    // Con (*) -> Lista (puede haber muchos)
    @NotNull
    private List<TimeZoneValidationStartTime> time;

    // Con (*) -> Lista (puede haber muchos)
    @NotNull
    private List<TimeZoneValidationStartDateTime> dateTime;
}
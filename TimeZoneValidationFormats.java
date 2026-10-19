import lombok.Builder;
import lombok.Data;

/**
 * Resultado del paso InitTZ: nodo "Formats".
 */
@Data
@Builder
public class TimeZoneValidationFormats {

    private String timeOffset;
    private String dateFormat;
    private String timeFormat;
    private String dateTimeFormat;
    private String timeZone;
}

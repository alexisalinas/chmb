import lombok.Data;

/**
 * DTO de salida del nodo "OperationDetails" dentro de FilteredSuppressionList.
 */
@Data
public class OperationDetailOutput {
    private String operationName;
    private String reqChannel;
    private String operationFlag;
    private String countryCode;
    private String indicator;
    private String responseCode;
    private String responseText;
}

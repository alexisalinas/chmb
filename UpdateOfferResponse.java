import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOfferResponse {

    /**
     * El código de retorno. Obligatorio (*)
     */
    @NotNull(message = "El campo code es obligatorio")
    private String code; 

    /**
     * La descripción de la operación. Obligatorio (*)
     */
    @NotNull(message = "El campo description es obligatorio")
    private String description; 

    private String customerReferenceNumber; 

    private String startDate; 

    private String endDate; 

    private String surveyModeCode; 

    /**
     * Lista de campos de control. (Usa el DTO de los 100 userFields).
     */
    @Valid
    private List<ControlFieldDTO> contextType;
}
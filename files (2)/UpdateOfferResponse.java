package com.banamex.cops.update_offer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
     * Lista de ControlField (solo contextType). Antes: controlField.
     */
    @Valid
    private List<ControlFieldDTO> contextType;
}
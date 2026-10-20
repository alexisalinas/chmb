package com.cop.suppression.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO Principal que representa el nodo "Request".
 */
@Data
public class StartRequest {

    // El nodo RequestParams no tiene (*) ni (?), por lo que es un objeto único y obligatorio.
    @NotNull
    @Valid // Asegura que se validen también los campos internos de RequestParams
    private StartRequestParams requestParams;
}
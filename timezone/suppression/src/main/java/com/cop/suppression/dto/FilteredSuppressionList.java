package com.cop.suppression.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO Principal que representa el nodo "FilteredSuppressionList".
 */
@Data
public class FilteredSuppressionList {

    // El nodo Results tiene un (*) -> Es una lista obligatoria
    @NotNull(message = "La lista de Results es obligatoria")
    @Valid // Valida los campos dentro de cada objeto de la lista
    private List<FilteredSuppressionResult> results;
}
package com.cop.suppression.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * DTO Principal que representa el nodo "SuppressedEntitiesList".
 */
@Data
public class SuppressedEntitiesListRoot {

    // Tiene un (*) -> Es una lista obligatoria
    @NotNull(message = "La lista suppressedEntities es obligatoria")
    @Valid // Para que valide los campos dentro de cada objeto de la lista
    private List<SuppressedEntityDTO> suppressedEntities;
}
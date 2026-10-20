package com.cop.suppression.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.Nullable;
import lombok.Data;

/**
 * DTO que representa el nodo "RequestParams".
 */
@Data
public class StartRequestParams {

    // Tiene un (?) -> Es opcional
    @Nullable
    private String requestingChannel;

    // No tiene símbolo -> Es obligatorio
    @NotNull
    @NotEmpty
    private String countryCode;

    // No tiene símbolo -> Es obligatorio
    @NotNull
    @NotEmpty
    private String currentOperationName;
}
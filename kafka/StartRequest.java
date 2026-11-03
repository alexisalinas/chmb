package com.banamex.cops.update_offer.dto.suppression;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Start/Request: RequestParams es obligatorio. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartRequest {

    @NotNull(message = "El nodo RequestParams es obligatorio")
    @Valid
    private StartRequestParams requestParams;
}

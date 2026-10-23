package com.banamex.cops.update_offer.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta de error estándar (headers faltantes o inválidos). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ErrorType type;
    private String details;
    private String code;
    private String location;
    private String moreInfo;
    private String uuid;
    private String timestamp;
}

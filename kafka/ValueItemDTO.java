package com.banamex.cops.update_offer.dto.timeZoneValidation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Start/root/Date*, Time* y DateTime* (listas): DateVal?/TimeVal?/DateTimeVal? opcional y *Name obligatorio. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueItemDTO {

    /** DateVal / TimeVal / DateTimeVal (?) */
    private String value;

    /** DateName / TimeName / DateTimeName (obligatorio) */
    @NotBlank(message = "El nombre del valor es obligatorio")
    private String name;
}

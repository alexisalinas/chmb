package com.banamex.cops.update_offer.dto.timeZoneValidation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueItemDTO {
    /** DateVal / TimeVal / DateTimeVal */
    private String value;
    /** DateName / TimeName / DateTimeName */
    private String name;
}

package com.banamex.cops.dto.timezonevalidation;

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

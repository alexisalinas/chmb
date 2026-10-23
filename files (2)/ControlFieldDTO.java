package com.banamex.cops.update_offer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ControlField del swagger: solo contextType (example: controlField). Los userFields viajan en userText. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlFieldDTO {

    private String contextType;
}

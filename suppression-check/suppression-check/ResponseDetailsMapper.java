package com.banamex.cops.suppression;

import org.springframework.stereotype.Component;

import java.util.List;

/** Mapeo del End: When exists(OperationDetails) -> For-Each; Otherwise -> Indicator "false". */
@Component
public class ResponseDetailsMapper {

    public List<ResponseDetailsDTO> map(FilteredSuppressionList list) {
        List<ResponseDetailsDTO> details = list == null || list.getResults() == null
                ? List.of()
                : list.getResults().stream()
                    .filter(r -> r.getOperationDetails() != null)
                    .flatMap(r -> r.getOperationDetails().stream())
                    .map(d -> ResponseDetailsDTO.builder()
                            .indicator(d.getIndicator())
                            .responseText(d.getResponseText())
                            .responseCode(d.getResponseCode())
                            .build())
                    .toList();

        if (!details.isEmpty()) {
            return details;
        }
        return List.of(ResponseDetailsDTO.builder()
                .indicator("false").responseText("").responseCode("").build());
    }
}

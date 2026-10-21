package com.banamex.cops.suppression;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Equivale al subproceso /BW/Common/PerformSuppressionCheck
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformSuppressionCheckService {

    private final SuppressedEntitiesClient suppressedEntitiesClient; // GetListOfSuppressedEntities
    private final SuppressionCheckService checkService;              // CheckIfSuppressed
    private final ResponseDetailsMapper mapper;                  // End (ResponseDetails)

    // Global Variable: COP/Suppression/CheckForSuppression
    @Value("${cop.suppression.check-for-suppression:N}")
    private String checkForSuppression;

    public List<ResponseDetailsDTO> perform(StartRequest request) {
        try {
            // Transicion "Yes": CheckForSuppression = "Y"
            if ("Y".equals(checkForSuppression)) {
                SuppressedEntitiesListRoot suppressed =
                        suppressedEntitiesClient.getListOfSuppressedEntities(request.getRequestParams());
                return mapper.map(checkService.checkIfSuppressed(request, suppressed));
            }

            // Transicion "Otherwise": Start -> End
            return mapper.map(empty());

        } catch (Exception ex) {
            // Catch -> ErrorLog -> End
            log.error("Error en PerformSuppressionCheck", ex);
            return mapper.map(empty());
        }
    }

    private FilteredSuppressionList empty() {
        FilteredSuppressionList out = new FilteredSuppressionList();
        out.setResults(List.of());
        return out;
    }
}

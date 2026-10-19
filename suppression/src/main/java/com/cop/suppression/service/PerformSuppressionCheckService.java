package com.cop.suppression.service;

import com.cop.suppression.client.SuppressedEntitiesClient;
import com.cop.suppression.dto.FilteredSuppressionList;
import com.cop.suppression.dto.StartRequest;
import com.cop.suppression.dto.SuppressedEntitiesListRoot;

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

    // Global Variable: COP/Suppression/CheckForSuppression
    @Value("${cop.suppression.check-for-suppression:N}")
    private String checkForSuppression;

    public FilteredSuppressionList perform(StartRequest request) {
        try {
            // Transicion "Yes": CheckForSuppression = "Y"
            if ("Y".equals(checkForSuppression)) {
                SuppressedEntitiesListRoot suppressed =
                        suppressedEntitiesClient.getListOfSuppressedEntities(request.getRequestParams());
                return checkService.checkIfSuppressed(request, suppressed);
            }

            // Transicion "Otherwise": Start -> End
            return empty();

        } catch (Exception ex) {
            // Catch -> ErrorLog -> End
            log.error("Error en PerformSuppressionCheck", ex);
            return empty();
        }
    }

    private FilteredSuppressionList empty() {
        FilteredSuppressionList out = new FilteredSuppressionList();
        out.setResults(List.of());
        return out;
    }
}

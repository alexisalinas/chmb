package com.banamex.cops.update_offer.suppressed;

import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Equivale al subproceso /BW/Common/PerformSuppressionCheck
 */
@Slf4j
@Service
public class PerformSuppressionCheckService {

    private final SuppressedEntitiesClient suppressedEntitiesClient; // GetListOfSuppressedEntities
    private final SuppressionCheckService checkService;              // CheckIfSuppressed

    /**
     * Crea el servicio.
     *
     * @param suppressedEntitiesClient la lista de entidades suprimidas (se pide por nombre: la de la base de datos)
     * @param checkService             el filtro de la lista contra la petición
     */
    public PerformSuppressionCheckService(@Qualifier("dbSuppressedEntitiesClient") SuppressedEntitiesClient suppressedEntitiesClient,
                                          SuppressionCheckService checkService) {
        this.suppressedEntitiesClient = suppressedEntitiesClient;
        this.checkService = checkService;
    }

    // Global Variable: COP/Suppression/CheckForSuppression
    @Value("${cop.suppression.check-for-suppression:N}")
    private String checkForSuppression;

    /**
     * Consulta la lista de entidades suprimidas y devuelve las que coinciden con la petición.
     * Si la consulta falla, escribe el error y devuelve una lista vacía.
     *
     * @param request país, canal y operación a revisar
     * @return las coincidencias de supresión (vacía si no hay o si falló la consulta)
     */
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

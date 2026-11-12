package com.banamex.cops.update_offer.suppressed;

import com.banamex.cops.app.model.CopSuppressionChannel;
import com.banamex.cops.app.model.CopSuppressionOperation;
import com.banamex.cops.app.service.CopSuppressionService;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Equivale a {@code GetListOfSuppressedEntities} y a la variable compartida de BW.
 * <p>
 * En BW la lista se carga una sola vez al arrancar ({@code /BW/StartUp/startUpSuppression}) y se guarda en una
 * variable compartida. Aquí se carga la primera vez que se necesita, con las dos consultas de la librería, se arma
 * con {@link StartUpSuppressionLoader} y se queda en memoria. Si la carga falla no se guarda nada: el error sube
 * a {@code PerformSuppressionCheckService} (que lo registra y sigue) y la siguiente petición lo vuelve a intentar.
 * {@link #reload()} la vuelve a cargar.
 * <p>
 * No es {@code @Primary}: si hay otras implementaciones de {@code SuppressedEntitiesClient} en el proyecto que sí lo
 * sean, Spring no podría elegir. {@code PerformSuppressionCheckService} la pide por su nombre
 * ({@code dbSuppressedEntitiesClient}), así que las demás no estorban.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DbSuppressedEntitiesClient implements SuppressedEntitiesClient {

    private final ObjectProvider<CopSuppressionService> suppressionService;
    private final StartUpSuppressionLoader loader;

    /** Timeout de las consultas (BW: COP/QueryRecords/TimeoutDefault). */
    @Value("${cop.suppression.query-timeout-seconds:30}")
    private int queryTimeoutSeconds = 30;

    /** Filas máximas de las consultas (BW: COP/Connections/BSDB/MaxConns); 0 = sin límite. */
    @Value("${cop.suppression.max-rows:0}")
    private int maxRows = 0;

    /** La "variable compartida": la lista ya armada. */
    private volatile SuppressedEntitiesListRoot cached;

    /**
     * Devuelve la lista de entidades suprimidas; la primera vez la carga de la base.
     *
     * @param params parámetros de la petición (no se usan: la lista es la misma para todas)
     * @return la lista de entidades suprimidas
     * @throws IllegalStateException si el servicio de la librería no está disponible
     */
    @Override
    public SuppressedEntitiesListRoot getListOfSuppressedEntities(StartRequestParams params) {
        SuppressedEntitiesListRoot list = cached;
        if (list == null) {
            synchronized (this) {
                list = cached;
                if (list == null) {
                    list = load();
                    cached = list;
                }
            }
        }
        return list;
    }

    /**
     * Vuelve a cargar la lista de la base y reemplaza la que está en memoria.
     *
     * @return la lista nueva
     */
    public synchronized SuppressedEntitiesListRoot reload() {
        SuppressedEntitiesListRoot list = load();
        cached = list;
        return list;
    }

    private SuppressedEntitiesListRoot load() {
        CopSuppressionService service = suppressionService.orderedStream().findFirst().orElseThrow(() ->
                new IllegalStateException("CopSuppressionService no está disponible: revisa que exista "
                        + "OfferFulfillmentConfig (registra las consultas de la librería) y que haya un spring.datasource configurado"));
        List<CopSuppressionChannel> channels = service.findAllChannels(queryTimeoutSeconds, maxRows);          // FetchChannels
        List<CopSuppressionOperation> operations = service.findAllOperations(queryTimeoutSeconds, maxRows);    // FetchOperations
        SuppressedEntitiesListRoot list = loader.build(channels, operations);                                   // SetVariable
        log.debug("startUpSuppression: {} entidades suprimidas cargadas", list.getSuppressedEntities().size());
        return list;
    }
}

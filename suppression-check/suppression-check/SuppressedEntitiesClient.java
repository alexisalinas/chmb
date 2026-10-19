/**
 * Equivale a la actividad GetListOfSuppressedEntities del proceso BW.
 * Pendiente de implementar segun el tipo de actividad (JDBC, SOAP o REST).
 */
public interface SuppressedEntitiesClient {
    SuppressedEntitiesListRoot getListOfSuppressedEntities(StartRequestParams params);
}

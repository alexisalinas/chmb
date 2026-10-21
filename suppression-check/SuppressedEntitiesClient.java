package com.banamex.cops.suppression;

/**
 * Equivale a la actividad GetListOfSuppressedEntities (Get Shared Variable) del proceso BW.
 * Implementación por defecto: InMemorySuppressedEntitiesClient.
 */
public interface SuppressedEntitiesClient {
    SuppressedEntitiesListRoot getListOfSuppressedEntities(StartRequestParams params);
}

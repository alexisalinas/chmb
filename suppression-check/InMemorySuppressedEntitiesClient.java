package com.banamex.cops.suppression;

import org.springframework.stereotype.Component;

/**
 * Equivale a GetListOfSuppressedEntities (Get Shared Variable): lee una lista en memoria.
 * Quien la cargue (el SetVariable de BW, p. ej. al arrancar desde BD) debe llamar a refresh().
 */
@Component
public class InMemorySuppressedEntitiesClient implements SuppressedEntitiesClient {

    private volatile SuppressedEntitiesListRoot cache = new SuppressedEntitiesListRoot();

    @Override
    public SuppressedEntitiesListRoot getListOfSuppressedEntities(StartRequestParams params) {
        return cache;
    }

    public void refresh(SuppressedEntitiesListRoot newList) {
        this.cache = newList != null ? newList : new SuppressedEntitiesListRoot();
    }
}

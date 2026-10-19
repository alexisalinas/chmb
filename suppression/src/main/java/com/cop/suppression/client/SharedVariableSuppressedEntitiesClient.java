package com.cop.suppression.client;

import com.cop.suppression.dto.StartRequestParams;
import com.cop.suppression.dto.SuppressedEntitiesListRoot;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Equivale a la actividad GetListOfSuppressedEntities (Get Shared Variable).
 *
 * La lista vive en memoria, igual que una Shared Variable de BW:
 *  - getListOfSuppressedEntities() = Get Shared Variable
 *  - update()                      = Set Shared Variable
 *
 * Algun proceso debe llamar a update() para cargarla (al arrancar o
 * periodicamente), igual que el proceso de BW que hace el Set Shared Variable.
 */
@Component
public class SharedVariableSuppressedEntitiesClient implements SuppressedEntitiesClient {

    private final AtomicReference<SuppressedEntitiesListRoot> shared =
            new AtomicReference<>(emptyRoot());

    @Override
    public SuppressedEntitiesListRoot getListOfSuppressedEntities(StartRequestParams params) {
        return shared.get();
    }

    /** Equivale a Set Shared Variable: reemplaza la lista completa de forma atomica. */
    public void update(SuppressedEntitiesListRoot newValue) {
        shared.set(newValue != null ? newValue : emptyRoot());
    }

    private static SuppressedEntitiesListRoot emptyRoot() {
        SuppressedEntitiesListRoot root = new SuppressedEntitiesListRoot();
        root.setSuppressedEntities(List.of());
        return root;
    }
}

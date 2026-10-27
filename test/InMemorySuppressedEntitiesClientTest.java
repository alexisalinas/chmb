package com.banamex.cops.update_offer.suppressed;

import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Borra este archivo si borraste InMemorySuppressedEntitiesClient (te quedas con SharedVariable...). */
class InMemorySuppressedEntitiesClientTest {

    private final InMemorySuppressedEntitiesClient client = new InMemorySuppressedEntitiesClient();

    @Test
    void alInicioLaListaEstaVacia() {
        assertThat(client.getListOfSuppressedEntities(null).getSuppressedEntities()).isEmpty();
    }

    @Test
    void refreshReemplazaLaLista() {
        SuppressedEntitiesListRoot nueva = new SuppressedEntitiesListRoot();
        client.refresh(nueva);
        assertThat(client.getListOfSuppressedEntities(null)).isSameAs(nueva);
    }

    @Test
    void refreshConNullDejaUnaListaVaciaEnLugarDeNull() {
        client.refresh(null);
        assertThat(client.getListOfSuppressedEntities(null)).isNotNull();
        assertThat(client.getListOfSuppressedEntities(null).getSuppressedEntities()).isEmpty();
    }
}

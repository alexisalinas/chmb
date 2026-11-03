package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.service.CopOfferKafkaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferDataClientTest {

    @Mock ObjectProvider<CopOfferKafkaService> provider;
    @Mock CopOfferKafkaService service;
    @InjectMocks OfferDataClient client;

    private CopOfferKafkaData fila(String prdType) {
        return new CopOfferKafkaData("C", prdType, "R", "D", "P", "F", List.of(), null, "30", null, null, "PC", "PF", "Y");
    }

    @Test
    void devuelveLaPrimeraFila() {
        when(provider.getIfAvailable()).thenReturn(service);
        when(service.findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123")).thenReturn(List.of(fila("A")));

        Optional<CopOfferKafkaData> r = client.find("wvch:W1:CAT", "off:mx:C1:W1:123");

        assertThat(r).isPresent();
        assertThat(r.get().prdType()).isEqualTo("A");
    }

    @Test
    void conVariasFilas_usaLaPrimera() {
        when(provider.getIfAvailable()).thenReturn(service);
        when(service.findByWaveKeyAndOfferKey("a", "b")).thenReturn(List.of(fila("A"), fila("B")));

        assertThat(client.find("a", "b").get().prdType()).isEqualTo("A");
    }

    @Test
    void sinFilas_devuelveVacio() {
        when(provider.getIfAvailable()).thenReturn(service);
        when(service.findByWaveKeyAndOfferKey("a", "b")).thenReturn(List.of());

        assertThat(client.find("a", "b")).isEmpty();
    }

    @Test
    void listaNulaDeLaLibreria_seTrataComoSinFilas() {
        when(provider.getIfAvailable()).thenReturn(service);
        when(service.findByWaveKeyAndOfferKey("a", "b")).thenReturn(null);

        assertThat(client.find("a", "b")).isEmpty();
    }

    @Test
    void sinLibreriaOSinBD_lanzaIllegalStateParaQueSeReintenteYSeContePorFalla() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> client.find("a", "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CopOfferKafkaService");
    }
}

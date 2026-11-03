package com.banamex.cops.app.service.impl;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.repository.CopOfferKafkaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopOfferKafkaServiceImplTest {

    @Mock CopOfferKafkaRepository repository;
    @InjectMocks CopOfferKafkaServiceImpl service;

    @Test
    void delegaEnElRepositorioConLasDosClaves() {
        CopOfferKafkaData fila = new CopOfferKafkaData("C", "T", "R", "D", "P", "F", List.of(),
                null, "30", null, null, "PC", "PF", "Y");
        when(repository.findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123")).thenReturn(List.of(fila));

        assertThat(service.findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123")).containsExactly(fila);
        verify(repository).findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123");
    }

    @Test
    void sinFilasDevuelveListaVacia() {
        when(repository.findByWaveKeyAndOfferKey("a", "b")).thenReturn(List.of());
        assertThat(service.findByWaveKeyAndOfferKey("a", "b")).isEmpty();
    }
}

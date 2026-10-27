package com.banamex.cops.update_offer.suppressed;

import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformSuppressionCheckServiceTest {

    @Mock SuppressedEntitiesClient client;
    @Mock SuppressionCheckService checkService;
    @InjectMocks PerformSuppressionCheckService service;

    private StartRequest request;

    @BeforeEach
    void setUp() {
        StartRequestParams p = new StartRequestParams();
        p.setRequestingChannel("CAT");
        p.setCountryCode("MX");
        p.setCurrentOperationName("UpdateOffer");
        request = new StartRequest();
        request.setRequestParams(p);
    }

    @Test
    void conCheckForSuppressionY_consultaLaListaYAplicaLaValidacion() {
        ReflectionTestUtils.setField(service, "checkForSuppression", "Y");
        SuppressedEntitiesListRoot lista = new SuppressedEntitiesListRoot();
        FilteredSuppressionList esperado = new FilteredSuppressionList();
        when(client.getListOfSuppressedEntities(request.getRequestParams())).thenReturn(lista);
        when(checkService.checkIfSuppressed(request, lista)).thenReturn(esperado);

        assertThat(service.perform(request)).isSameAs(esperado);
        verify(client).getListOfSuppressedEntities(request.getRequestParams());
    }

    @Test
    void conCheckForSuppressionDistintoDeY_devuelveVacioSinConsultarNada() {
        ReflectionTestUtils.setField(service, "checkForSuppression", "N");

        FilteredSuppressionList r = service.perform(request);

        assertThat(r.getResults()).isEmpty();
        verifyNoInteractions(client, checkService);
    }

    @Test
    void laComparacionEsExactaConY_minusculaNoActiva() {
        ReflectionTestUtils.setField(service, "checkForSuppression", "y");
        assertThat(service.perform(request).getResults()).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void siFallaLaConsulta_devuelveVacioYNoLanzaExcepcion() {
        ReflectionTestUtils.setField(service, "checkForSuppression", "Y");
        when(client.getListOfSuppressedEntities(any())).thenThrow(new RuntimeException("sin lista"));

        assertThat(service.perform(request).getResults()).isEqualTo(List.of());
    }

    @Test
    void siFallaLaValidacion_devuelveVacioYNoLanzaExcepcion() {
        ReflectionTestUtils.setField(service, "checkForSuppression", "Y");
        when(client.getListOfSuppressedEntities(any())).thenReturn(new SuppressedEntitiesListRoot());
        when(checkService.checkIfSuppressed(any(), any())).thenThrow(new NullPointerException("detalle nulo"));

        assertThat(service.perform(request).getResults()).isEmpty();
    }
}

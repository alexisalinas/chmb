package com.banamex.cops.update_offer.validation;

import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DtoValidatorTest {

    private DtoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DtoValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    private StartRequest valido() {
        return StartRequest.builder()
                .requestParams(StartRequestParams.builder().countryCode("MX").currentOperationName("UpdateOffer").build())
                .build();
    }

    @Test
    void dtoValido_noTieneViolacionesNiLanzaExcepcion() {
        assertThat(validator.violations(valido())).isEmpty();
        assertThatCode(() -> validator.validateOrThrow(valido())).doesNotThrowAnyException();
    }

    @Test
    void violations_listaRutaYMensajeOrdenados() {
        assertThat(validator.violations(new ClientDetailsDTO()))
                .contains("channelID: El campo ChannelID es obligatorio", "org: El campo Org es obligatorio")
                .hasSize(7)
                .isSorted();
    }

    @Test
    void validateOrThrow_lanzaIllegalArgumentConElNombreDelDtoYElDetalle() {
        StartRequest malo = StartRequest.builder().requestParams(new StartRequestParams()).build();

        assertThatThrownBy(() -> validator.validateOrThrow(malo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Datos inválidos en StartRequest")
                .hasMessageContaining("requestParams.countryCode")
                .hasMessageContaining("requestParams.currentOperationName");
    }

    @Test
    void validaLosElementosAnidados() {
        assertThat(validator.violations(new StartRequest())).hasSize(1)
                .first().asString().startsWith("requestParams:");
    }

    @SuppressWarnings("unchecked")
    @Test
    void usaElValidatorDeSpringSiExiste() {
        ObjectProvider<Validator> provider = mock(ObjectProvider.class);
        Validator spring = mock(Validator.class);
        when(spring.validate(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptySet());
        when(provider.getIfAvailable()).thenReturn(spring);

        new DtoValidator(provider).validateOrThrow(new ClientDetailsDTO());   // el mock no reporta nada

        verify(spring).validate(org.mockito.ArgumentMatchers.any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void sinValidatorEnSpring_usaUnoPorDefecto() {
        ObjectProvider<Validator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        assertThat(new DtoValidator(provider).violations(new ClientDetailsDTO())).hasSize(7);
    }
}

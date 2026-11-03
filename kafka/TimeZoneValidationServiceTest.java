package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.DateGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateTimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationStartDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeZoneValidationServiceTest {

    @Mock TimeZoneLookupClient lookupClient;
    @Mock TimeZoneMapRequest mapRequest;

    private TimeZoneValidationService service;
    private final List<ValueItemDTO> dates = List.of(new ValueItemDTO("2026-10-25", "AppointmentDate"));

    @BeforeEach
    void setUp() {
        service = new TimeZoneValidationService(new InitTZ(), lookupClient, mapRequest);
        ReflectionTestUtils.setField(service, "invalidCode", "COP_TZCOD_101");
    }

    private TzLookUpDTO fila() {
        TzLookUpDTO l = new TzLookUpDTO();
        l.setSystemName("COPS");
        l.setMode("RT");
        l.setType("Request");
        l.setTimeOffset("-05:00");
        l.setDateFormat("yyyy-MM-dd");
        return l;
    }

    @Test
    void sinConfiguracionEnLaBD_devuelve114() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of());

        TimeZoneValidationResult r = service.validate("COPS", "RT", "Request", dates, List.of(), List.of());

        assertThat(r.getResponseCode()).isEqualTo("COP_TZCOD_114");
        assertThat(r.getResponseText()).isEqualTo("TimeZone not configured");
        assertThat(r.getFormats()).isNotNull();
        assertThat(r.getFormats().getTimeZoneOffset()).isEmpty();      // obligatorios: vacíos, no nulos
        assertThat(r.getFormats().getDateFormat()).isEmpty();
        assertThat(r.getFormats().getTimeFormat()).isEmpty();
        assertThat(r.getFormats().getDateTimeFormat()).isEmpty();
        assertThat(r.getFormats().getTimeZone()).isEmpty();
        assertThat(r.getMapResult().getDateGrp()).isEmpty();
        verify(mapRequest, never()).map(any(), any(), any(), any(), any());
    }

    @Test
    void consultaNulaDeLaBD_tambienEs114() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(null);
        assertThat(service.validate("COPS", "RT", "Request", dates, List.of(), List.of()).getResponseCode())
                .isEqualTo("COP_TZCOD_114");
    }

    @Test
    void conConfiguracionYFechasValidas_devuelve000() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of(fila()));
        MapRequestResultDTO ok = new MapRequestResultDTO();
        ok.getDateGrp().add(new DateGrpDTO("2026-10-25T00:00:00-0600", "", "AppointmentDate"));
        when(mapRequest.map(eq("Request"), any(TzFormatsDTO.class), eq(dates), any(), any())).thenReturn(ok);

        TimeZoneValidationResult r = service.validate("COPS", "RT", "Request", dates, List.of(), List.of());

        assertThat(r.getResponseCode()).isEqualTo("COP_TZCOD_000");
        assertThat(r.getResponseText()).isEqualTo("Success");
        assertThat(r.getMapResult()).isSameAs(ok);
        assertThat(r.getFormats().getTimeZoneOffset()).isEqualTo("-05:00");
    }

    @Test
    void fechaInvalida_devuelveElCodigoDeInvalido() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of(fila()));
        MapRequestResultDTO malo = new MapRequestResultDTO();
        malo.getDateGrp().add(new DateGrpDTO("xx", "Invalid TimeZone", null));
        when(mapRequest.map(any(), any(), any(), any(), any())).thenReturn(malo);

        TimeZoneValidationResult r = service.validate("COPS", "RT", "Request", dates, List.of(), List.of());

        assertThat(r.getResponseCode()).isEqualTo("COP_TZCOD_101");
        assertThat(r.getResponseText()).isEqualTo("Invalid TimeZone");
    }

    @Test
    void horaInvalida_devuelveElCodigoDeInvalido() {
        when(lookupClient.find(any(), any(), any())).thenReturn(List.of(fila()));
        MapRequestResultDTO malo = new MapRequestResultDTO();
        malo.getTimeGrp().add(new TimeGrpDTO("99:99", "Invalid TimeZone", null));
        when(mapRequest.map(any(), any(), any(), any(), any())).thenReturn(malo);

        assertThat(service.validate("COPS", "RT", "Request", List.of(), List.of(), List.of())
                .getResponseCode()).isEqualTo("COP_TZCOD_101");
    }

    @Test
    void fechaHoraInvalida_devuelveElCodigoDeInvalido() {
        when(lookupClient.find(any(), any(), any())).thenReturn(List.of(fila()));
        MapRequestResultDTO malo = new MapRequestResultDTO();
        malo.getDateTimeGrp().add(new DateTimeGrpDTO("zz", "Invalid TimeZone", null));
        when(mapRequest.map(any(), any(), any(), any(), any())).thenReturn(malo);

        assertThat(service.validate("COPS", "RT", "Request", List.of(), List.of(), List.of())
                .getResponseCode()).isEqualTo("COP_TZCOD_101");
    }

    @Test
    void laConfiguracionSeGuardaEnCache_laBDSoloSeConsultaUnaVez() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of(fila()));
        when(mapRequest.map(any(), any(), any(), any(), any())).thenReturn(new MapRequestResultDTO());

        service.validate("COPS", "RT", "Request", dates, List.of(), List.of());
        service.validate("COPS", "RT", "Request", dates, List.of(), List.of());

        verify(lookupClient, times(1)).find("COPS", "RT", "Request");
    }

    @Test
    void sinConfiguracion_laBDSeVuelveAConsultarEnLaSiguienteLlamada() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of());

        service.validate("COPS", "RT", "Request", dates, List.of(), List.of());
        service.validate("COPS", "RT", "Request", dates, List.of(), List.of());

        verify(lookupClient, times(2)).find("COPS", "RT", "Request");
    }

    @Test
    void elCodigoDeInvalidoEsConfigurable() {
        ReflectionTestUtils.setField(service, "invalidCode", "COP_TZCOD_999");
        when(lookupClient.find(any(), any(), any())).thenReturn(List.of(fila()));
        MapRequestResultDTO malo = new MapRequestResultDTO();
        malo.getDateGrp().add(new DateGrpDTO("xx", "Invalid TimeZone", null));
        when(mapRequest.map(any(), any(), any(), any(), any())).thenReturn(malo);

        assertThat(service.validate("COPS", "RT", "Request", dates, List.of(), List.of()).getResponseCode())
                .isEqualTo("COP_TZCOD_999");
    }

    @Test
    void validateConElNodoStart_equivaleALosParametrosSueltos() {
        when(lookupClient.find("COPS", "RT", "Request")).thenReturn(List.of());

        TimeZoneValidationStartDTO start = TimeZoneValidationStartDTO.builder()
                .system("COPS").sysMode("RT").type("Request").date(dates).build();

        TimeZoneValidationResult r = service.validate(start);

        assertThat(r.getResponseCode()).isEqualTo("COP_TZCOD_114");
        verify(lookupClient).find("COPS", "RT", "Request");
    }
}

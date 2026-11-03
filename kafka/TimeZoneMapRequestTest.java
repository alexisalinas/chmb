package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La zona ECRM es America/Mexico_City (-06:00 fijo desde 2022). */
class TimeZoneMapRequestTest {

    private TimeZoneMapRequest mapper;

    @BeforeEach
    void setUp() {
        mapper = new TimeZoneMapRequest();
        ReflectionTestUtils.setField(mapper, "ecrmTimeZone", "America/Mexico_City");
        ReflectionTestUtils.setField(mapper, "timeZoneDateTimeFormat", "yyyy-MM-dd'T'HH:mm:ssZ");
        ReflectionTestUtils.setField(mapper, "beTimeFormat", "HH:mm:ss");
        ReflectionTestUtils.setField(mapper, "beDateTimeFormat", "yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    private TzFormatsDTO formats() {
        TzFormatsDTO f = new TzFormatsDTO();
        f.setTimeZoneOffset("-05:00");
        f.setDateFormat("yyyy-MM-dd");
        f.setTimeFormat("HH:mm:ss");
        f.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        f.setTimeZone("America/Bogota");
        return f;
    }

    private List<ValueItemDTO> item(String value, String name) {
        return List.of(new ValueItemDTO(value, name));
    }

    // ------------------------------------------------------------ Request: fecha

    @Test
    void request_fechaValida_seConvierteAFechaConOffsetDeEcrm() {
        MapRequestResultDTO r = mapper.map("Request", formats(), item("2026-10-25", "AppointmentDate"), null, null);

        assertThat(r.getDateGrp()).hasSize(1);
        assertThat(r.getDateGrp().get(0).getDate()).startsWith("2026-10-25T00:00:00").matches(".*[+-]\\d{4}");
        assertThat(r.getDateGrp().get(0).getDate()).endsWith("-0600");
        assertThat(r.getDateGrp().get(0).getDateStatus()).isEmpty();
        assertThat(r.getDateGrp().get(0).getDateName()).isEqualTo("AppointmentDate");
    }

    @Test
    void request_fechaConOtroFormato_seInterpretaConElFormatoDeLaBD() {
        TzFormatsDTO f = formats();
        f.setDateFormat("dd/MM/yyyy");
        MapRequestResultDTO r = mapper.map("Request", f, item("25/10/2026", "AppointmentDate"), null, null);
        assertThat(r.getDateGrp().get(0).getDate()).startsWith("2026-10-25T00:00:00");
    }

    @Test
    void request_fechaInvalida_marcaInvalidTimeZone() {
        MapRequestResultDTO r = mapper.map("Request", formats(), item("25/10/2026", "AppointmentDate"), null, null);

        assertThat(r.getDateGrp()).hasSize(1);
        assertThat(r.getDateGrp().get(0).getDateStatus()).isEqualTo("Invalid TimeZone");
        assertThat(r.getDateGrp().get(0).getDate()).isEqualTo("25/10/2026");
    }

    @Test
    void request_fechaImposible_marcaInvalidTimeZone() {
        MapRequestResultDTO r = mapper.map("Request", formats(), item("2026-13-45", "AppointmentDate"), null, null);
        assertThat(r.getDateGrp().get(0).getDateStatus()).isEqualTo("Invalid TimeZone");
    }

    @Test
    void request_sinOffsetConfigurado_todoEsInvalido() {
        TzFormatsDTO f = formats();
        f.setTimeZoneOffset(null);
        MapRequestResultDTO r = mapper.map("Request", f, item("2026-10-25", "AppointmentDate"), null, null);
        assertThat(r.getDateGrp().get(0).getDateStatus()).isEqualTo("Invalid TimeZone");
    }

    @Test
    void request_fechaVaciaOEnBlanco_noGeneraGrupo() {
        assertThat(mapper.map("Request", formats(), item("", "AppointmentDate"), null, null).getDateGrp()).isEmpty();
        assertThat(mapper.map("Request", formats(), item("  ", "AppointmentDate"), null, null).getDateGrp()).isEmpty();
    }

    // ------------------------------------------------------------ Request: hora y fecha-hora

    @Test
    void request_horaValida_seConvierteDeLaZonaDelClienteALaDeEcrm() {
        // cliente -05:00 -> ECRM -06:00 (America/Mexico_City): 10:30 -> 09:30
        MapRequestResultDTO r = mapper.map("Request", formats(), null, item("10:30:00", "AppointmentStartTime"), null);

        assertThat(r.getTimeGrp()).hasSize(1);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("09:30:00");
        assertThat(r.getTimeGrp().get(0).getTimeStatus()).isEmpty();
        assertThat(r.getTimeGrp().get(0).getTimeName()).isEqualTo("AppointmentStartTime");
    }

    @Test
    void request_horaConFormatoDelCliente_salConFormatoDelBackend() {
        TzFormatsDTO f = formats();
        f.setTimeFormat("HH:mm");
        MapRequestResultDTO r = mapper.map("Request", f, null, item("10:30", "T"), null);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("09:30:00");   // BETimeFormat = HH:mm:ss
    }

    @Test
    void request_laHoraUsaElFormatoDelBackendConfigurable() {
        ReflectionTestUtils.setField(mapper, "beTimeFormat", "HHmm");
        MapRequestResultDTO r = mapper.map("Request", formats(), null, item("10:30:00", "T"), null);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("0930");
    }

    @Test
    void request_clienteYEcrmEnLaMismaZona_laHoraNoCambia() {
        TzFormatsDTO f = formats();
        f.setTimeZoneOffset("-06:00");
        MapRequestResultDTO r = mapper.map("Request", f, null, item("10:30:00", "T"), null);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("10:30:00");
    }

    @Test
    void request_horaInvalida_marcaInvalidTimeZone() {
        MapRequestResultDTO r = mapper.map("Request", formats(), null, item("99:99:99", "AppointmentStartTime"), null);
        assertThat(r.getTimeGrp().get(0).getTimeStatus()).isEqualTo("Invalid TimeZone");
    }

    @Test
    void request_fechaHoraConOffset_seConvierteALaZonaDeEcrmYAlFormatoDelBackend() {
        MapRequestResultDTO r = mapper.map("Request", formats(), null, null, item("2026-10-25T10:30:00-0600", "DT"));

        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("2026-10-25T10:30:00-06:00");
        assertThat(r.getDateTimeGrp().get(0).getDateTimeStatus()).isEmpty();
        assertThat(r.getDateTimeGrp().get(0).getDateTimeName()).isEqualTo("DT");
    }

    @Test
    void request_fechaHoraConOffsetDistintoAEcrm_seDesplaza() {
        // 10:30 en -05:00 son 09:30 en -06:00
        MapRequestResultDTO r = mapper.map("Request", formats(), null, null, item("2026-10-25T10:30:00-0500", "DT"));
        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("2026-10-25T09:30:00-06:00");
    }

    @Test
    void request_fechaHoraSinOffset_asumeLaZonaDelCliente() {
        TzFormatsDTO f = formats();
        f.setDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");
        MapRequestResultDTO r = mapper.map("Request", f, null, null, item("2026-10-25T10:30:00", "DT"));
        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("2026-10-25T09:30:00-06:00");
    }

    @Test
    void request_fechaHoraUsaElFormatoDelBackendConfigurable() {
        ReflectionTestUtils.setField(mapper, "beDateTimeFormat", "yyyyMMddHHmmss");
        MapRequestResultDTO r = mapper.map("Request", formats(), null, null, item("2026-10-25T10:30:00-0600", "DT"));
        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("20261025103000");
    }

    @Test
    void request_fechaHoraInvalida_marcaInvalidTimeZone() {
        MapRequestResultDTO malo = mapper.map("Request", formats(), null, null, item("hoy", "DT"));
        assertThat(malo.getDateTimeGrp().get(0).getDateTimeStatus()).isEqualTo("Invalid TimeZone");
        assertThat(malo.getDateTimeGrp().get(0).getDateTime()).isEqualTo("hoy");
    }

    @Test
    void request_variosElementosDeCadaTipo() {
        MapRequestResultDTO r = mapper.map("Request", formats(),
                List.of(new ValueItemDTO("2026-10-25", "D1"), new ValueItemDTO("2026-10-26", "D2")),
                List.of(new ValueItemDTO("10:00:00", "T1"), new ValueItemDTO("11:00:00", "T2")), null);
        assertThat(r.getDateGrp()).hasSize(2);
        assertThat(r.getTimeGrp()).hasSize(2);
    }

    @Test
    void tipoDesconocido_devuelveResultadoVacio() {
        MapRequestResultDTO r = mapper.map("Otro", formats(), item("2026-10-25", "X"), item("10:00:00", "Y"), null);
        assertThat(r.getDateGrp()).isEmpty();
        assertThat(r.getTimeGrp()).isEmpty();
        assertThat(r.getDateTimeGrp()).isEmpty();
    }

    @Test
    void elTipoNoDistingueMayusculas() {
        assertThat(mapper.map("REQUEST", formats(), item("2026-10-25", "X"), null, null).getDateGrp()).hasSize(1);
        assertThat(mapper.map("response", formats(), item("2026-10-25T10:30:00", "X"), null, null).getDateGrp()).hasSize(1);
    }

    @Test
    void listasNulas_noLanzanExcepcion() {
        MapRequestResultDTO r = mapper.map("Request", formats(), null, null, null);
        assertThat(r.getDateGrp()).isEmpty();
    }

    // ------------------------------------------------------------ Response

    @Test
    void response_fechaSeFormateaConElFormatoDeLaBD() {
        TzFormatsDTO f = formats();
        f.setDateFormat("dd/MM/yyyy");
        MapRequestResultDTO r = mapper.map("Response", f, item("2026-10-25T10:30:00-06:00", "D"), null, null);
        assertThat(r.getDateGrp().get(0).getDate()).isEqualTo("25/10/2026");
    }

    @Test
    void response_fechaSinOffsetTambienSeFormatea() {
        TzFormatsDTO f = formats();
        f.setDateFormat("dd/MM/yyyy");
        MapRequestResultDTO r = mapper.map("Response", f, item("2026-10-25T10:30:00", "D"), null, null);
        assertThat(r.getDateGrp().get(0).getDate()).isEqualTo("25/10/2026");
    }

    @Test
    void response_horaSeConvierteDeEcrmALaZonaDeLaBD() {
        TzFormatsDTO f = formats();
        f.setTimeFormat("HH:mm");
        MapRequestResultDTO r = mapper.map("Response", f, null, item("10:30:00", "T"), null);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("11:30");   // -06:00 -> -05:00
    }

    @Test
    void response_fechaHoraConOffsetConDosPuntos_seConvierteYPoneDosPuntosEnElOffset() {
        MapRequestResultDTO r = mapper.map("Response", formats(), null, null,
                item("2026-10-25T10:30:00-06:00", "DT"));
        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("2026-10-25T11:30:00-05:00");
    }

    @Test
    void response_fechaHoraConOffsetSinDosPuntos_usaElPatronGlobalYLaZonaDeLaBD() {
        MapRequestResultDTO r = mapper.map("Response", formats(), null, null,
                item("2026-10-25T10:30:00-0600", "DT"));
        assertThat(r.getDateTimeGrp().get(0).getDateTime()).isEqualTo("2026-10-25T11:30:00-05:00");
    }

    @Test
    void response_fechaHoraInvalida_lanzaIllegalArgument() {
        assertThatThrownBy(() -> mapper.map("Response", formats(), null, null, item("abc", "DT")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void response_sinOffsetUsaLaZonaPorNombre() {
        TzFormatsDTO f = formats();
        f.setTimeZoneOffset(null);
        f.setTimeFormat("HH:mm");
        MapRequestResultDTO r = mapper.map("Response", f, null, item("10:30:00", "T"), null);
        assertThat(r.getTimeGrp().get(0).getTime()).isEqualTo("11:30");     // America/Bogota = -05:00
    }

    @Test
    void response_sinOffsetNiZona_lanzaIllegalArgument() {
        TzFormatsDTO f = formats();
        f.setTimeZoneOffset(null);
        f.setTimeZone(null);
        assertThatThrownBy(() -> mapper.map("Response", f, null, item("10:30:00", "T"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void response_valoresVaciosNoGeneranGrupos() {
        MapRequestResultDTO r = mapper.map("Response", formats(), item("", "D"), item(" ", "T"), item(null, "DT"));
        assertThat(r.getDateGrp()).isEmpty();
        assertThat(r.getTimeGrp()).isEmpty();
        assertThat(r.getDateTimeGrp()).isEmpty();
    }
}

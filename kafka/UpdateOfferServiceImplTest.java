package com.banamex.cops.update_offer.service.impl;

import com.banamex.cops.update_offer.audit.AuditLogService;
import com.banamex.cops.update_offer.audit.TransactionContext;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionResult;
import com.banamex.cops.update_offer.dto.suppression.OperationDetailOutput;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import com.banamex.cops.update_offer.kafka.KafkaSenderDashboard;
import com.banamex.cops.update_offer.kafka.OfferInput;
import com.banamex.cops.update_offer.mapper.UpdateOfferMapper;
import com.banamex.cops.update_offer.sender.EmsSender;
import com.banamex.cops.update_offer.sender.FulFillkafkaSender;
import com.banamex.cops.update_offer.service.RqHeaderFactory;
import com.banamex.cops.update_offer.service.timeZoneValidation.TimeZoneValidationService;
import com.banamex.cops.update_offer.suppressed.PerformSuppressionCheckService;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateOfferServiceImplTest {

    @Mock EmsSender emsSender;
    @Mock UpdateOfferMapper updateOfferMapper;
    @Mock RqHeaderFactory rqHeaderFactory;
    @Mock AuditLogService auditLogService;
    @Mock PerformSuppressionCheckService suppressionCheckService;
    @Mock TimeZoneValidationService timeZoneValidationService;
    @Mock KafkaSenderDashboard kafkaSenderDashboard;
    @Mock FulFillkafkaSender fulFillkafkaSender;
    @InjectMocks UpdateOfferServiceImpl service;

    private final UpdateOfferHeaders headers = TestData.headers();
    private final RqHeaderDTO rqHeader = TestData.rqHeader();
    private UpdateOfferResponse emsResponse;
    private final OfferInput kafkaInput = new OfferInput();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "translateTimeZone", false);
        ReflectionTestUtils.setField(service, "tzSystem", "COPS");
        ReflectionTestUtils.setField(service, "tzSysMode", "RT");

        emsResponse = new UpdateOfferResponse();
        emsResponse.setCode("000");
        emsResponse.setDescription("OK");

        when(rqHeaderFactory.create(headers)).thenReturn(rqHeader);
        when(updateOfferMapper.toLogRequestXml(any(), any())).thenReturn("<logRq/>");
        when(updateOfferMapper.toLogResponseXml(any())).thenReturn("<logRs/>");
        when(updateOfferMapper.toEmsXml(any(), any(), any(), any())).thenReturn("<ems/>");
        when(emsSender.send("<ems/>")).thenReturn("<reply/>");
        when(updateOfferMapper.toRestResponse("<reply/>")).thenReturn(emsResponse);
        when(suppressionCheckService.perform(any())).thenReturn(sinSupresion());
        when(fulFillkafkaSender.isEnabled()).thenReturn(true);
        when(kafkaSenderDashboard.build(any(), any(), any())).thenReturn(kafkaInput);
    }

    private FilteredSuppressionList sinSupresion() {
        FilteredSuppressionList l = new FilteredSuppressionList();
        l.setResults(List.of());
        return l;
    }

    private FilteredSuppressionList conSupresion(String indicator) {
        OperationDetailOutput d = new OperationDetailOutput();
        d.setIndicator(indicator);
        d.setResponseCode("COP_OPSUP_001");
        d.setResponseText("The UpdateOffer operation is temporarily unavailable");
        FilteredSuppressionResult r = new FilteredSuppressionResult();
        r.setOperationDetails(List.of(d));
        FilteredSuppressionList l = new FilteredSuppressionList();
        l.setResults(List.of(r));
        return l;
    }

    // ------------------------------------------------------------ flujo normal

    @Test
    void flujoNormal_enviaAlEmsYDevuelveLaRespuestaConvertida() {
        List<UpdateOfferResponse> out = service.updateOffer(List.of(TestData.request()), headers);

        assertThat(out).containsExactly(emsResponse);
        verify(emsSender).send("<ems/>");
    }

    @Test
    void flujoNormal_escribeLogDeRequestAntesYLogDeResponseDespues() {
        service.updateOffer(List.of(TestData.request()), headers);

        InOrder o = inOrder(auditLogService, emsSender);
        o.verify(auditLogService).logRequest(any(TransactionContext.class), eq("<logRq/>"));
        o.verify(emsSender).send("<ems/>");
        o.verify(auditLogService).logResponse(any(TransactionContext.class), eq("<logRs/>"));
    }

    @Test
    void sinZonaHoraria_noLlamaALaValidacionYPasaNullAlMapper() {
        UpdateOfferRequest rq = TestData.request();
        service.updateOffer(List.of(rq), headers);

        verifyNoInteractions(timeZoneValidationService);
        verify(updateOfferMapper).toEmsXml(rq, rqHeader, null, null);
    }

    @Test
    void variasPeticiones_devuelveUnaRespuestaPorCada() {
        List<UpdateOfferResponse> out = service.updateOffer(
                List.of(TestData.request(), TestData.request()), headers);

        assertThat(out).hasSize(2);
        verify(emsSender, org.mockito.Mockito.times(2)).send("<ems/>");
    }

    // ------------------------------------------------------------ supresión

    @Test
    void operacionSuprimida_respondeSinLlamarAlEms() {
        when(suppressionCheckService.perform(any())).thenReturn(conSupresion("true"));

        List<UpdateOfferResponse> out = service.updateOffer(List.of(TestData.request()), headers);

        assertThat(out.get(0).getCode()).isEqualTo("COP_OPSUP_001");
        assertThat(out.get(0).getDescription()).contains("temporarily unavailable");
        verifyNoInteractions(emsSender);
        verify(auditLogService).logResponse(any(TransactionContext.class), anyString());
    }

    @Test
    void indicadorFalse_noSuprime() {
        when(suppressionCheckService.perform(any())).thenReturn(conSupresion("false"));

        List<UpdateOfferResponse> out = service.updateOffer(List.of(TestData.request()), headers);

        assertThat(out.get(0)).isSameAs(emsResponse);
    }

    @Test
    void laSupresionSeConsultaConCanalPaisYNombreDeOperacion() {
        service.updateOffer(List.of(TestData.request()), headers);

        ArgumentCaptor<StartRequest> c = ArgumentCaptor.forClass(StartRequest.class);
        verify(suppressionCheckService).perform(c.capture());
        assertThat(c.getValue().getRequestParams().getRequestingChannel()).isEqualTo("CAT");
        assertThat(c.getValue().getRequestParams().getCountryCode()).isEqualTo("MX");
        assertThat(c.getValue().getRequestParams().getCurrentOperationName()).isEqualTo("UpdateOffer");
    }

    @Test
    void supresionNulaOSinResultados_noSuprime() {
        when(suppressionCheckService.perform(any())).thenReturn(null);
        assertThat(service.updateOffer(List.of(TestData.request()), headers).get(0)).isSameAs(emsResponse);

        FilteredSuppressionList sinLista = new FilteredSuppressionList();
        sinLista.setResults(null);
        when(suppressionCheckService.perform(any())).thenReturn(sinLista);
        assertThat(service.updateOffer(List.of(TestData.request()), headers).get(0)).isSameAs(emsResponse);
    }

    // ------------------------------------------------------------ zona horaria

    private TimeZoneValidationResult validacion(String code, String text) {
        TimeZoneValidationResult v = new TimeZoneValidationResult();
        v.setResponseCode(code);
        v.setResponseText(text);
        v.setMapResult(new MapRequestResultDTO());
        return v;
    }

    @Test
    void zonaHorariaInvalida_respondeConElCodigoDeLaValidacionYNoLlamaAlEms() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(validacion("COP_TZCOD_101", "Invalid TimeZone"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out.getCode()).isEqualTo("COP_TZCOD_101");
        assertThat(out.getDescription()).isEqualTo("Invalid TimeZone");
        verifyNoInteractions(emsSender, suppressionCheckService);
    }

    @Test
    void zonaHorariaCorrecta_pasaCodigoYResultadoAlMapper() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        TimeZoneValidationResult v = validacion("COP_TZCOD_000", "Success");
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any())).thenReturn(v);
        UpdateOfferRequest rq = TestData.request();

        service.updateOffer(List.of(rq), headers);

        verify(updateOfferMapper).toEmsXml(rq, rqHeader, "COP_TZCOD_000", v.getMapResult());
    }

    @Test
    void zonaHorariaNoConfigurada114_continuaConElFlujo() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(validacion("COP_TZCOD_114", "TimeZone not configured"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out).isSameAs(emsResponse);
        verify(emsSender).send("<ems/>");
    }

    @Test
    @SuppressWarnings("unchecked")
    void zonaHoraria_enviaFechaYAmbasHorasConSusNombres() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(validacion("COP_TZCOD_000", "Success"));

        service.updateOffer(List.of(TestData.request()), headers);

        ArgumentCaptor<List<ValueItemDTO>> dates = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<ValueItemDTO>> times = ArgumentCaptor.forClass(List.class);
        verify(timeZoneValidationService).validate(eq("COPS"), eq("RT"), eq("Request"),
                dates.capture(), times.capture(), eq(List.of()));

        assertThat(dates.getValue()).extracting(ValueItemDTO::getName, ValueItemDTO::getValue)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("AppointmentDate", "2026-10-25"));
        assertThat(times.getValue()).extracting(ValueItemDTO::getName, ValueItemDTO::getValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("AppointmentStartTime", "10:30:00"),
                        org.assertj.core.groups.Tuple.tuple("AppointmentEndTime", "11:00:00"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void zonaHoraria_sinFechaNiHorasEnviaListasVacias() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(validacion("COP_TZCOD_000", "Success"));

        service.updateOffer(List.of(TestData.minimalRequest()), headers);

        ArgumentCaptor<List<ValueItemDTO>> dates = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<ValueItemDTO>> times = ArgumentCaptor.forClass(List.class);
        verify(timeZoneValidationService).validate(anyString(), anyString(), anyString(),
                dates.capture(), times.capture(), any());
        assertThat(dates.getValue()).isEmpty();
        assertThat(times.getValue()).isEmpty();
    }

    // ------------------------------------------------------------ errores

    @Test
    void errorDelEms_devuelveRespuestaERRORYAunAsiEscribeElLogDeRespuesta() {
        when(emsSender.send("<ems/>")).thenThrow(new RuntimeException("Timeout esperando respuesta del EMS"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out.getCode()).isEqualTo("ERROR");
        assertThat(out.getDescription()).contains("Timeout esperando respuesta del EMS");
        verify(auditLogService).logResponse(any(TransactionContext.class), eq("<logRs/>"));
    }

    @Test
    void errorAlLeerLaRespuesta_devuelveERROR() {
        when(updateOfferMapper.toRestResponse("<reply/>")).thenThrow(new IllegalStateException("respuesta ilegible"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out.getCode()).isEqualTo("ERROR");
        assertThat(out.getDescription()).contains("respuesta ilegible");
    }

    @Test
    void unaPeticionConErrorNoImpideProcesarLasDemas() {
        UpdateOfferRequest mala = TestData.request();
        mala.setCampaignId("MALA");
        when(updateOfferMapper.toEmsXml(eq(mala), any(), any(), any())).thenThrow(new RuntimeException("fallo"));

        List<UpdateOfferResponse> out = service.updateOffer(List.of(mala, TestData.request()), headers);

        assertThat(out.get(0).getCode()).isEqualTo("ERROR");
        assertThat(out.get(1)).isSameAs(emsResponse);
    }

    @Test
    void errorEnElLogDeRequest_seTratacomoErrorDeLaPeticion() {
        org.mockito.Mockito.doThrow(new RuntimeException("log roto"))
                .when(auditLogService).logRequest(any(), eq("<logRq/>"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out.getCode()).isEqualTo("ERROR");
        verify(emsSender, never()).send(anyString());
    }

    // ------------------------------------------------------------ Kafka (FulFillKafkaSender)

    @Test
    void exitoDelEms_encolaElEnvioAKafkaConLaRespuestaDelEms() {
        UpdateOfferRequest rq = TestData.request();

        service.updateOffer(List.of(rq), headers);

        verify(kafkaSenderDashboard).build(rq, rqHeader, emsResponse);
        verify(fulFillkafkaSender).sendAsync(kafkaInput);
    }

    @Test
    void kafkaDeshabilitado_noArmaNiEncolaNada() {
        when(fulFillkafkaSender.isEnabled()).thenReturn(false);

        service.updateOffer(List.of(TestData.request()), headers);

        verifyNoInteractions(kafkaSenderDashboard);
        verify(fulFillkafkaSender, never()).sendAsync(any());
    }

    @Test
    void operacionSuprimida_noEnviaAKafka() {
        when(suppressionCheckService.perform(any())).thenReturn(conSupresion("true"));

        service.updateOffer(List.of(TestData.request()), headers);

        verify(fulFillkafkaSender, never()).sendAsync(any());
    }

    @Test
    void zonaHorariaInvalida_noEnviaAKafka() {
        ReflectionTestUtils.setField(service, "translateTimeZone", true);
        when(timeZoneValidationService.validate(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(validacion("COP_TZCOD_101", "Invalid TimeZone"));

        service.updateOffer(List.of(TestData.request()), headers);

        verify(fulFillkafkaSender, never()).sendAsync(any());
    }

    @Test
    void errorDelEms_noEnviaAKafka() {
        when(emsSender.send("<ems/>")).thenThrow(new RuntimeException("timeout"));

        service.updateOffer(List.of(TestData.request()), headers);

        verify(fulFillkafkaSender, never()).sendAsync(any());
    }

    @Test
    void fallaAlEncolarEnKafka_noAfectaLaRespuesta() {
        org.mockito.Mockito.doThrow(new RuntimeException("cola llena")).when(fulFillkafkaSender).sendAsync(any());

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out).isSameAs(emsResponse);
    }

    @Test
    void fallaAlArmarElEventoDeKafka_noAfectaLaRespuesta() {
        when(kafkaSenderDashboard.build(any(), any(), any())).thenThrow(new IllegalStateException("sin datos"));

        UpdateOfferResponse out = service.updateOffer(List.of(TestData.request()), headers).get(0);

        assertThat(out).isSameAs(emsResponse);
        verify(auditLogService).logResponse(any(TransactionContext.class), eq("<logRs/>"));
    }
}

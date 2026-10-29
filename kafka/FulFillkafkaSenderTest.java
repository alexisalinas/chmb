package com.banamex.cops.update_offer.sender;

import com.banamex.cops.update_offer.kafka.KafkaMetrics;
import com.banamex.cops.update_offer.kafka.OfferDataRepository;
import com.banamex.cops.update_offer.kafka.OfferInput;
import com.banamex.cops.update_offer.kafka.OfferKafkaProps;
import com.banamex.cops.update_offer.kafka.OfferKeyBuilder;
import com.banamex.cops.update_offer.kafka.OfferPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FulFillkafkaSenderTest {

    private static final String WAVE_KEY = "wvch:W1:CAT";
    private static final Timestamp START = Timestamp.valueOf("2026-10-25 10:30:00");

    @Mock OfferDataRepository repository;
    @Mock OfferPublisher publisher;
    @Mock TaskExecutor executor;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private OfferKafkaProps props;
    private KafkaMetrics metrics;
    private FulFillkafkaSender sender;

    @BeforeEach
    void setUp() throws Exception {
        props = new OfferKafkaProps();
        props.setDelayMs(0);
        props.setMaxAttempts(2);
        props.setRetryBackoffMs(0);
        metrics = new KafkaMetrics();
        sender = new FulFillkafkaSender(props, new OfferKeyBuilder(props), repository, publisher, mapper, metrics, executor);

        when(repository.find(eq(WAVE_KEY), anyString())).thenReturn(Optional.of(dbRow()));
        when(publisher.publish(anyString(), anyString())).thenReturn("id-1");
    }

    private OfferInput input() {
        return OfferInput.builder()
                .uuid("uuid-1").esbUuid("esb-1").channel("CAT")
                .transactionTime("2026-10-26T10:00:00Z")
                .refId("off:mx:C1:W1:123:ACC")
                .campaignId("C1").waveId("W1").customerNo("123").accountNo("ACC")
                .cardNumber("1234567812345678").offerStatus("01")
                .responseTime("2026-10-26T10:00:01Z").responseCode("000").responseText("OK")
                .build();
    }

    private Map<String, Object> dbRow() {
        Map<String, Object> r = new HashMap<>();
        r.put("PRDTYPE", "T");
        r.put("REASONCODE", "R1");
        r.put("REASONDESC", "motivo");
        r.put("DESCRIPTION", "producto");
        r.put("PRDCODE", "P1");
        r.put("PRDFAMILY", "F1");
        r.put("CRPCATEGORYID", "CAT1");
        r.put("FULFILLMENTPROCID", "FP1");
        r.put("AVAILABILITY", "Y");
        r.put("APPTSTARTDATE_TM", START);
        r.put("EXPIRYDATE_TM", Timestamp.valueOf("2026-12-31 23:59:59"));
        r.put("APPTENDTIMEDUR", "30.4");
        r.put("USERFIELD_1", "db1");
        r.put("USERFIELD_5", "db5");
        return r;
    }

    private JsonNode publicado() throws Exception {
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(eq("off:mx:C1:W1:123:ACC"), json.capture());
        return mapper.readTree(json.getValue());
    }

    // ------------------------------------------------------------ flujo completo

    @Test
    void flujoCompleto_consultaLaBDYPublicaElEvento() throws Exception {
        sender.process(input());

        verify(repository).find(WAVE_KEY, "off:mx:C1:W1:123:ACC");
        assertThat(publicado().has("event")).isTrue();
        assertThat(metrics.getSent()).isEqualTo(1);
    }

    @Test
    void header_llevaLasConstantesYLosDatosDeLaEntrada() throws Exception {
        sender.process(input());
        JsonNode h = publicado().get("event").get("header");

        assertThat(h.get("name").asText()).isEqualTo("Updateofferrequest");
        assertThat(h.get("version").asText()).isEqualTo("1.0");
        assertThat(h.get("producerCSI").asText()).isEqualTo("163621");
        assertThat(h.get("countryCode").asText()).isEqualTo("MX");
        assertThat(h.get("businessCode").asText()).isEqualTo("DM");
        assertThat(h.get("Channel").asText()).isEqualTo("CAT");
        assertThat(h.get("UUID").asText()).isEqualTo("uuid-1");
        assertThat(h.get("sid").asText()).isEqualTo("esb-1");
        assertThat(h.get("transactionTime").asText()).isEqualTo("2026-10-26T10:00:00Z");
    }

    @Test
    void timeStamp_siempreEsLaHoraActual() throws Exception {
        sender.process(input());
        String ts = publicado().get("event").get("header").get("timeStamp").asText();
        OffsetDateTime parsed = OffsetDateTime.parse(ts);
        assertThat(parsed).isAfter(OffsetDateTime.now().minusMinutes(1));
    }

    @Test
    void transactionTimeVacio_seLlenaConLaHoraActual() throws Exception {
        OfferInput in = input();
        in.setTransactionTime("");
        sender.process(in);

        String tt = publicado().get("event").get("header").get("transactionTime").asText();
        assertThat(tt).isNotBlank();
        OffsetDateTime.parse(tt);
    }

    @Test
    void body_llevaLosDatosDeLaEntradaYLosDeLaBD() throws Exception {
        sender.process(input());
        JsonNode b = publicado().get("event").get("body");

        assertThat(b.get("RefID").asText()).isEqualTo("off:mx:C1:W1:123:ACC");
        assertThat(b.get("CampaignID").asText()).isEqualTo("C1");
        assertThat(b.get("WaveID").asText()).isEqualTo("W1");
        assertThat(b.get("AccountNo").asText()).isEqualTo("ACC");
        assertThat(b.get("CardNumber").asText()).isEqualTo("1234567812345678");
        assertThat(b.get("OfferStatus").asText()).isEqualTo("01");
        assertThat(b.get("ResponseCode").asText()).isEqualTo("000");
        assertThat(b.get("ResponseText").asText()).isEqualTo("OK");
        assertThat(b.get("ResponseTime").asText()).isEqualTo("2026-10-26T10:00:01Z");

        assertThat(b.get("ProductType").asText()).isEqualTo("T");
        assertThat(b.get("ReasonCode").asText()).isEqualTo("R1");
        assertThat(b.get("ReasonDesc").asText()).isEqualTo("motivo");
        assertThat(b.get("ProductDescription").asText()).isEqualTo("producto");
        assertThat(b.get("ProductCode").asText()).isEqualTo("P1");
        assertThat(b.get("ProductFamily").asText()).isEqualTo("F1");
        assertThat(b.get("CategoryID").asText()).isEqualTo("CAT1");
        assertThat(b.get("FulfillmentProcID").asText()).isEqualTo("FP1");
        assertThat(b.get("Availability").asText()).isEqualTo("Y");
        assertThat(b.get("AppointmentStartDate").asText()).isEqualTo(START.toInstant().toString());
    }

    @Test
    void userFields_vienenDeLaBD() throws Exception {
        sender.process(input());
        JsonNode b = publicado().get("event").get("body");

        assertThat(b.get("UserField1").asText()).isEqualTo("db1");
        assertThat(b.get("UserField5").asText()).isEqualTo("db5");
        assertThat(b.has("UserField2")).isFalse();      // nulo y omitNulls = true
    }

    // ------------------------------------------------------------ reglas de formato

    @Test
    void customerNo_seRellenaConCerosA9DespuesDeLaConsulta() throws Exception {
        sender.process(input());
        assertThat(publicado().get("event").get("body").get("CustomerNo").asText()).isEqualTo("000000123");
        // la clave de la consulta usó el valor SIN ceros
        verify(repository).find(WAVE_KEY, "off:mx:C1:W1:123:ACC");
    }

    @Test
    void customerNoDe9OMasDigitos_noSeModifica() {
        assertThat(FulFillkafkaSender.padLeft("123456789", 9)).isEqualTo("123456789");
        assertThat(FulFillkafkaSender.padLeft("1234567890", 9)).isEqualTo("1234567890");
        assertThat(FulFillkafkaSender.padLeft(null, 9)).isNull();
    }

    @Test
    void appointmentMinDur_soloSiEsMayorQueCero() throws Exception {
        Map<String, Object> row = dbRow();
        row.put("APPTENDTIMEDUR", "0");
        when(repository.find(eq(WAVE_KEY), anyString())).thenReturn(Optional.of(row));
        sender.process(input());
        assertThat(publicado().get("event").get("body").has("AppointmentMinDur")).isFalse();
    }

    @Test
    void appointmentMinDur_seRedondeaAEntero() throws Exception {
        sender.process(input());     // "30.4" -> 30
        assertThat(publicado().get("event").get("body").get("AppointmentMinDur").asInt()).isEqualTo(30);
    }

    @Test
    void toMinutes_casosBorde() {
        assertThat(FulFillkafkaSender.toMinutes(null)).isZero();
        assertThat(FulFillkafkaSender.toMinutes("abc")).isZero();
        assertThat(FulFillkafkaSender.toMinutes("29.5")).isEqualTo(30);
        assertThat(FulFillkafkaSender.toMinutes(".5")).isEqualTo(1);
        assertThat(FulFillkafkaSender.toMinutes("0.4")).isZero();
        assertThat(FulFillkafkaSender.toMinutes(45)).isEqualTo(45);
    }

    @Test
    void iso_convierteTimestampAInstant() {
        assertThat(FulFillkafkaSender.iso(START)).isEqualTo(START.toInstant().toString());
        assertThat(FulFillkafkaSender.iso("2026-01-01")).isEqualTo("2026-01-01");
        assertThat(FulFillkafkaSender.iso(null)).isNull();
    }

    @Test
    void maskCardNumber_dejaSoloLosUltimosCuatroDigitos() throws Exception {
        props.setMaskCardNumber(true);
        sender.process(input());
        assertThat(publicado().get("event").get("body").get("CardNumber").asText()).isEqualTo("************5678");
    }

    @Test
    void mask_casosBorde() {
        assertThat(FulFillkafkaSender.mask("1234")).isEqualTo("1234");
        assertThat(FulFillkafkaSender.mask(null)).isNull();
        assertThat(FulFillkafkaSender.mask("123456")).isEqualTo("**3456");
    }

    @Test
    void omitNullsFalso_losNulosSeEscribenComoNull() throws Exception {
        props.setOmitNulls(false);
        sender.process(input());

        JsonNode b = publicado().get("event").get("body");
        assertThat(b.has("Action_ID")).isTrue();
        assertThat(b.get("Action_ID").isNull()).isTrue();
        assertThat(b.has("UserField2")).isTrue();
    }

    @Test
    void omitNullsVerdadero_losNulosNoSeEscriben() throws Exception {
        sender.process(input());
        assertThat(publicado().get("event").get("body").has("Action_ID")).isFalse();
    }

    // ------------------------------------------------------------ descartes (no es una falla)

    @Test
    void sinFilasEnLaBD_descartaYNoPublica() throws Exception {
        when(repository.find(eq(WAVE_KEY), anyString())).thenReturn(Optional.empty());

        sender.process(input());

        verifyNoInteractions(publisher);
        assertThat(metrics.getNoRows()).isEqualTo(1);
        assertThat(metrics.getFailed()).isZero();
        verify(repository, times(1)).find(eq(WAVE_KEY), anyString());   // sin filas NO se reintenta
    }

    @Test
    void camposObligatoriosFaltantes_descartaSinConsultarLaBD() {
        for (String campo : new String[]{"refId", "campaignId", "waveId", "customerNo", "offerStatus"}) {
            OfferInput in = input();
            switch (campo) {
                case "refId" -> in.setRefId(null);
                case "campaignId" -> in.setCampaignId("");
                case "waveId" -> in.setWaveId(" ");
                case "customerNo" -> in.setCustomerNo(null);
                default -> in.setOfferStatus("");
            }
            sender.process(in);
        }
        verifyNoInteractions(repository, publisher);
        assertThat(metrics.getInvalid()).isEqualTo(5);
    }

    // ------------------------------------------------------------ reintentos y fallas

    @Test
    void fallaDeInfraestructuraEnLaBD_seReintentaYSiPersisteSeCuentaComoFallida() throws Exception {
        when(repository.find(eq(WAVE_KEY), anyString())).thenThrow(new RuntimeException("db caída"));

        sender.process(input());

        verify(repository, times(2)).find(eq(WAVE_KEY), anyString());
        verifyNoInteractions(publisher);
        assertThat(metrics.getFailed()).isEqualTo(1);
        assertThat(metrics.getSent()).isZero();
    }

    @Test
    void siElSegundoIntentoFunciona_publica() throws Exception {
        when(repository.find(eq(WAVE_KEY), anyString()))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(Optional.of(dbRow()));

        sender.process(input());

        verify(repository, times(2)).find(eq(WAVE_KEY), anyString());
        verify(publisher).publish(anyString(), anyString());
        assertThat(metrics.getSent()).isEqualTo(1);
        assertThat(metrics.getFailed()).isZero();
    }

    @Test
    void fallaAlPublicar_seReintentaYSeCuentaComoFallida() throws Exception {
        when(publisher.publish(anyString(), anyString())).thenThrow(new RuntimeException("cola caída"));

        sender.process(input());

        verify(publisher, times(2)).publish(anyString(), anyString());
        assertThat(metrics.getFailed()).isEqualTo(1);
        assertThat(metrics.getSent()).isZero();
    }

    @Test
    void processNuncaLanzaExcepcion() {
        when(repository.find(anyString(), anyString())).thenThrow(new IllegalStateException("inesperado"));
        sender.process(input());     // no debe propagar
        assertThat(metrics.getFailed()).isEqualTo(1);
    }

    // ------------------------------------------------------------ asíncrono

    @Test
    void sendAsync_encolaElTrabajoEnElEjecutorYAlCorrerloSePublica() throws Exception {
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        sender.sendAsync(input());

        verify(executor).execute(any(Runnable.class));
        verify(publisher).publish(anyString(), anyString());
    }

    @Test
    void sendAsync_noEjecutaNadaEnElHiloLlamador() throws Exception {
        sender.sendAsync(input());      // el ejecutor es un mock: no corre la tarea

        verify(executor).execute(any(Runnable.class));
        verify(publisher, never()).publish(anyString(), anyString());
    }

    @Test
    void sendAsync_conColaLlena_cuentaElRechazoYNoLanzaExcepcion() {
        doThrow(new TaskRejectedException("cola llena")).when(executor).execute(any(Runnable.class));

        sender.sendAsync(input());

        assertThat(metrics.getRejected()).isEqualTo(1);
    }

    @Test
    void isEnabled_reflejaLaConfiguracion() {
        assertThat(sender.isEnabled()).isFalse();
        props.setEnabled(true);
        assertThat(sender.isEnabled()).isTrue();
    }
}

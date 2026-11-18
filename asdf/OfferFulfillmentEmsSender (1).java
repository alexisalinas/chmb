package com.banamex.cops.update_offer.sender;

import com.banamex.cops.app.model.CopOfferFulfillment;
import com.banamex.cops.update_offer.kafka.KafkaMetrics;
import com.banamex.cops.update_offer.kafka.OfferDataClient;
import com.banamex.cops.update_offer.kafka.OfferInput;
import com.banamex.cops.update_offer.kafka.OfferKafkaProps;
import com.banamex.cops.update_offer.kafka.OfferKeyBuilder;
import com.banamex.cops.update_offer.kafka.EventDestinations;
import com.banamex.cops.update_offer.kafka.EventDestinations.EventDestination;
import com.banamex.cops.update_offer.service.UpdateOfferResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Publica en la cola del EMS (el bridge que alimenta Kafka) el evento de cumplimiento de la oferta
 * ({@code Updateofferrequest}).
 */
@Slf4j
@Service
public class OfferFulfillmentEmsSender {

    /** Zona de la hora del evento (timeStamp): la misma ECRMTimeZone de las globales (US/Central). */
    @Value("${update-offer.timezone.ecrm-time-zone:US/Central}")
    private String ecrmTimeZone = "US/Central";

    private static final DateTimeFormatter WITH_OFFSET_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter LOCAL_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter WITH_OFFSET_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final Pattern CARD_IN_JSON = Pattern.compile("(\"CardNumber\"\\s*:\\s*\")(\\d*?)(\\d{4})\"");

    private final OfferKafkaProps props;
    private final OfferKeyBuilder keys;
    private final OfferDataClient dataClient;
    private final EventDestinations destinations;
    private final ObjectMapper mapper;
    private final KafkaMetrics metrics;
    private final TaskExecutor executor;
    private final UpdateOfferResponseFactory responses;

    /** Crea el emisor del flujo de Kafka. */
    public OfferFulfillmentEmsSender(OfferKafkaProps props, OfferKeyBuilder keys, OfferDataClient dataClient,
                              EventDestinations destinations, ObjectMapper mapper, KafkaMetrics metrics,
                              @Qualifier("offerFulfillmentExecutor") TaskExecutor executor,
                              UpdateOfferResponseFactory responses) {
        this.props = props;
        this.keys = keys;
        this.dataClient = dataClient;
        this.destinations = destinations;
        this.mapper = mapper;
        this.metrics = metrics;
        this.executor = executor;
        this.responses = responses;
    }

    /** Indica si el evento se arma y tiene al menos un destino activo. */
    public boolean isEnabled() {
        return props.isEnabled() && !destinations.enabled().isEmpty();
    }

    /** Encola el envío. */
    public void sendAsync(OfferInput in) {
        List<EventDestination> activos = destinations.enabled();
        try {
            executor.execute(() -> process(in));
        } catch (TaskRejectedException e) {
            metrics.incRejected();
            log.warn("KAFKA-REJECTED cola llena, no se envió uuid={} refId={}", in.getUuid(), in.getRefId());
            resultado(tag(activos), responses.kafkaErrorText(names(activos), "Cola de envío llena", "no hay espacio en el ejecutor"));
        }
    }

    /** Pasos 1 a 5 (síncrono; lo ejecuta el hilo del ejecutor). */
    public void process(OfferInput in) {
        List<EventDestination> activos = destinations.enabled();
        String tag = tag(activos);
        String destino = names(activos);
        String paso = "Validación del evento";
        try {
            if (!valid(in)) {
                resultado(tag, responses.kafkaErrorText(destino, paso,
                        "faltan campos obligatorios (RefID, CampaignID, WaveID, CustomerNo, OfferStatus)"));
                return;
            }
            if (props.getDelayMs() > 0) {
                paso = "Espera previa a la consulta";
                Thread.sleep(props.getDelayMs());
            }
            paso = "Consulta a la base de datos";
            String waveKey = keys.waveKey(in.getWaveId(), in.getChannel());
            log.info("[{}] Consulta de la oferta waveKey={} offerKey={}", tag, waveKey, in.getOfferKey());
            Optional<CopOfferFulfillment> db =
                    retry("consulta a la BD", () -> dataClient.find(waveKey, in.getOfferKey()));
            if (db.isEmpty()) {
                metrics.incNoRows();
                if (!props.isSendWithoutDbRows()) {
                    log.error("KAFKA-NO-ROWS uuid={} refId={} waveKey={} offerKey={}",
                            in.getUuid(), in.getRefId(), waveKey, in.getOfferKey());
                    resultado(tag, responses.kafkaErrorText(destino, paso,
                            "no se encontró la oferta (waveKey=" + waveKey + ", offerKey=" + in.getOfferKey() + ")"));
                    return;
                }
                log.warn("KAFKA-NO-ROWS uuid={} refId={} waveKey={} offerKey={} se envía el evento sin los datos de la BD",
                        in.getUuid(), in.getRefId(), waveKey, in.getOfferKey());
                db = Optional.of(new CopOfferFulfillment(null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null));
            }
            paso = "Armado del evento";
            String json = mapper.writeValueAsString(assemble(in, db.get()));
            log.info("[{}] Envía: {}", tag, maskCardInJson(json));
            for (EventDestination d : activos) {
                publicar(in, json, d);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("KAFKA-ERROR interrumpido uuid={} refId={}", in.getUuid(), in.getRefId());
            resultado(tag, responses.kafkaErrorText(destino, paso, "el envío se interrumpió"));
        } catch (Exception e) {
            metrics.incFailed();
            log.error("KAFKA-ERROR uuid={} refId={} {}", in.getUuid(), in.getRefId(), e.toString(), e);
            resultado(tag, responses.kafkaErrorText(destino, paso, e));
        }
    }

    /** Publica en un destino; si falla, solo ese destino queda con error. */
    private void publicar(OfferInput in, String json, EventDestination d) throws InterruptedException {
        try {
            retry("publicación en " + d.name(), () -> d.publisher().publish(in.getRefId(), json));
            metrics.incSent();
            resultado(d.tag(), "dry-run".equals(props.getMode())
                    ? responses.kafkaDryRunText(d.name()) : responses.kafkaSuccessText(d.name()));
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            metrics.incFailed();
            log.error("KAFKA-ERROR uuid={} refId={} destino={} {}", in.getUuid(), in.getRefId(), d.name(), e.toString(), e);
            resultado(d.tag(), responses.kafkaErrorText(d.name(), "Publicación en la cola de " + d.name(), e));
        }
    }

    /** Escribe el resultado del envío (éxito o error, con la fecha de ECRMTimeZone). */
    private void resultado(String tag, String mensaje) {
        log.info("[{}] Resultado: {}", tag, mensaje);
    }

    private static String tag(List<EventDestination> activos) {
        return activos.isEmpty() ? "KAFKA" : String.join("+", activos.stream().map(EventDestination::tag).toList());
    }

    private static String names(List<EventDestination> activos) {
        return activos.isEmpty() ? "Kafka" : String.join(" y ", activos.stream().map(EventDestination::name).toList());
    }

    /** Minutos de duración de la cita: de {@code APPTSTARTDATE_TM} a {@code APPTENDDATE_TM}, redondeados. */
    static Long appointmentMinutes(Timestamp start, Timestamp end) {
        if (start == null || end == null) return null;
        long minutes = Math.round((end.getTime() - start.getTime()) / 60000.0);
        return minutes > 0 ? minutes : null;
    }

    /** Ensambla { "event": { "header": …, "body": … } } con los campos y el orden del ejemplo. */
    Map<String, Object> assemble(OfferInput in, CopOfferFulfillment db) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("name", props.getEventName());
        header.put("version", props.getEventVersion());
        header.put("producerCSI", props.getProducerCsi());
        header.put("Channel", in.getChannel());
        header.put("countryCode", props.getCountryCode());
        header.put("businessCode", props.getBusinessCode());
        header.put("UUID", in.getUuid());
        header.put("sid", isBlank(in.getEsbUuid()) ? in.getUuid() : in.getEsbUuid());
        header.put("transactionTime", isBlank(in.getTransactionTime())
                ? LocalDateTime.now().format(LOCAL_MILLIS) : in.getTransactionTime());
        header.put("timeStamp", OffsetDateTime.now(ZoneId.of(ecrmTimeZone)).format(WITH_OFFSET_MILLIS));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("RefID", in.getRefId());
        body.put("CampaignID", in.getCampaignId());
        body.put("WaveID", in.getWaveId());
        body.put("CustomerNo", padLeft(in.getCustomerNo(), 9));
        body.put("AccountNo", in.getAccountNo());
        body.put("CardNumber", props.isMaskCardNumber() ? mask(in.getCardNumber()) : in.getCardNumber());
        body.put("OfferStatus", in.getOfferStatus());
        body.put("ID_ProductType", db.prdType());
        body.put("CRP_Category_ID", db.crpCategoryId());
        body.put("Product_Description", db.prdDescription());
        body.put("Reason_Code", db.reasonCode());
        body.put("Reason_Description", db.reasonDesc());
        body.put("Action_ID", in.getActionId());
        body.put("ResponseTime", in.getResponseTime());
        body.put("ResponseCode", in.getResponseCode());
        body.put("ResponseText", in.getResponseText());
        body.put("FulfillmentID", db.fulfillmentProcId());
        List<String> userFields = db.userFields();
        for (int i = 1; i <= 100; i++) {
            body.put("UserField" + i, userFields != null && i <= userFields.size() ? userFields.get(i - 1) : null);
        }
        body.put("AppointmentDate", iso(db.apptStartDate()));
        body.put("AppointmentMinDur", appointmentMinutes(db.apptStartDate(), db.apptEndDate()));
        body.put("OfferExpiryDate", iso(db.expiryDate()));
        body.put("OfferStartDate", iso(db.offerCreationDate()));
        body.put("PrdCode", db.prdCode());
        body.put("PrdFamily", db.prdFamily());
        body.put("Availability", db.availability());

        if (props.isOmitNulls()) {
            header.values().removeIf(Objects::isNull);
            body.values().removeIf(Objects::isNull);
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("header", header);
        event.put("body", body);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("event", event);
        return root;
    }

    private boolean valid(OfferInput in) {
        if (isBlank(in.getRefId()) || isBlank(in.getOfferKey()) || isBlank(in.getCampaignId())
                || isBlank(in.getWaveId()) || isBlank(in.getCustomerNo()) || isBlank(in.getOfferStatus())) {
            metrics.incInvalid();
            log.error("KAFKA-INVALID uuid={} refId={} faltan campos obligatorios (RefID, CampaignID, WaveID, CustomerNo, OfferStatus)",
                    in.getUuid(), in.getRefId());
            return false;
        }
        return true;
    }

    /** Reintenta solo fallas de infraestructura (excepciones); un "sin filas" no es excepción y no se reintenta. */
    private <T> T retry(String what, Callable<T> action) throws Exception {
        long backoff = props.getRetryBackoffMs();
        for (int attempt = 1; ; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                if (attempt >= props.getMaxAttempts()) {
                    throw e;
                }
                log.warn("KAFKA-RETRY {} intento {}/{}: {}", what, attempt, props.getMaxAttempts(), e.toString());
                if (backoff > 0) {
                    Thread.sleep(backoff);
                }
                backoff *= 2;
            }
        }
    }

    /** "CardNumber":"1234567812345678" -> "CardNumber":"************5678" (solo para el log). */
    static String maskCardInJson(String json) {
        Matcher m = CARD_IN_JSON.matcher(json);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + "*".repeat(m.group(2).length()) + m.group(3) + "\""));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String padLeft(String s, int len) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < len; i++) sb.append('0');
        return sb.append(s).toString();
    }

    static String mask(String s) {
        if (s == null || s.length() <= 4) return s;
        return "*".repeat(s.length() - 4) + s.substring(s.length() - 4);
    }

    /**
     * Fechas de la BD como xs:dateTime con offset y sin fracciones, como en el ejemplo
     * (2026-12-31T00:00:00-06:00).
     */
    static String iso(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().atZone(ZoneId.systemDefault()).format(WITH_OFFSET_SECONDS);
        }
        return v.toString();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}

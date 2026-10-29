package com.banamex.cops.update_offer.sender;

import com.banamex.cops.update_offer.kafka.KafkaMetrics;
import com.banamex.cops.update_offer.kafka.OfferDataRepository;
import com.banamex.cops.update_offer.kafka.OfferInput;
import com.banamex.cops.update_offer.kafka.OfferKafkaProps;
import com.banamex.cops.update_offer.kafka.OfferKeyBuilder;
import com.banamex.cops.update_offer.kafka.OfferPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Subproceso /BW/Kafka/FulFillKafkaSender. Se dispara DESPUÉS de responder y nunca afecta la respuesta:
 *  0) evento de entrada (KafkaSenderDashboard)   1) espera de delayMs   2) consulta a la BD (DataFetch)
 *  3) ensamblado del evento                      4) reglas de formato   5) publicación { RefId, jsonString }
 *
 * Códigos del log: KAFKA-SENT, KAFKA-NO-ROWS, KAFKA-INVALID, KAFKA-RETRY, KAFKA-ERROR, KAFKA-REJECTED.
 */
@Slf4j
@Service
public class FulFillkafkaSender {

    private final OfferKafkaProps props;
    private final OfferKeyBuilder keys;
    private final OfferDataRepository repository;
    private final OfferPublisher publisher;
    private final JsonMapper mapper;
    private final KafkaMetrics metrics;
    private final TaskExecutor executor;

    public FulFillkafkaSender(OfferKafkaProps props, OfferKeyBuilder keys, OfferDataRepository repository,
                              OfferPublisher publisher, JsonMapper mapper, KafkaMetrics metrics,
                              @Qualifier("fulFillKafkaExecutor") TaskExecutor executor) {
        this.props = props;
        this.keys = keys;
        this.repository = repository;
        this.publisher = publisher;
        this.mapper = mapper;
        this.metrics = metrics;
        this.executor = executor;
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    /** Encola el envío. Si la cola está llena, lo descarta y lo cuenta (no bloquea la respuesta). */
    public void sendAsync(OfferInput in) {
        try {
            executor.execute(() -> process(in));
        } catch (TaskRejectedException e) {
            metrics.incRejected();
            log.warn("KAFKA-REJECTED cola llena, no se envió uuid={} refId={}", in.getUuid(), in.getRefId());
        }
    }

    /** Pasos 1 a 5 (síncrono; lo ejecuta el hilo del ejecutor). Nunca lanza excepciones. */
    public void process(OfferInput in) {
        try {
            if (!valid(in)) {
                return;
            }
            if (props.getDelayMs() > 0) {
                Thread.sleep(props.getDelayMs());                       // paso 1
            }
            String waveKey = keys.waveKey(in.getWaveId(), in.getChannel());
            String offerKey = in.getRefId();                            // misma clave que OFFPAL.EXTID$
            Optional<Map<String, Object>> db = retry("consulta a la BD", () -> repository.find(waveKey, offerKey)); // paso 2
            if (db.isEmpty()) {
                metrics.incNoRows();
                log.error("KAFKA-NO-ROWS uuid={} refId={} waveKey={} offerKey={}",
                        in.getUuid(), in.getRefId(), waveKey, offerKey);
                return;
            }
            String json = mapper.writeValueAsString(assemble(in, db.get()));   // pasos 3 y 4
            String id = retry("publicación", () -> publisher.publish(in.getRefId(), json));  // paso 5
            metrics.incSent();
            log.info("KAFKA-SENT uuid={} refId={} messageId={}", in.getUuid(), in.getRefId(), id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("KAFKA-ERROR interrumpido uuid={} refId={}", in.getUuid(), in.getRefId());
        } catch (Exception e) {
            metrics.incFailed();
            log.error("KAFKA-ERROR uuid={} refId={} {}", in.getUuid(), in.getRefId(), e.toString(), e);
        }
    }

    // ------------------------------------------------------------------ pasos 3 y 4

    /** Ensambla header y body; el resultado se serializa como { "event": { "header": …, "body": … } }. */
    Map<String, Object> assemble(OfferInput in, Map<String, Object> db) {
        String now = OffsetDateTime.now().toString();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("name", props.getEventName());
        header.put("version", props.getEventVersion());
        header.put("producerCSI", props.getProducerCsi());
        header.put("countryCode", props.getCountryCode());
        header.put("businessCode", props.getBusinessCode());
        header.put("Channel", in.getChannel());
        header.put("UUID", in.getUuid());
        header.put("sid", in.getEsbUuid());
        header.put("transactionTime", isBlank(in.getTransactionTime()) ? now : in.getTransactionTime());
        header.put("timeStamp", now);                                    // siempre la hora actual

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("RefID", in.getRefId());
        body.put("CampaignID", in.getCampaignId());
        body.put("WaveID", in.getWaveId());
        body.put("CustomerNo", padLeft(in.getCustomerNo(), 9));          // DESPUÉS de la consulta
        body.put("AccountNo", in.getAccountNo());
        body.put("CardNumber", props.isMaskCardNumber() ? mask(in.getCardNumber()) : in.getCardNumber());
        body.put("OfferStatus", in.getOfferStatus());
        body.put("Action_ID", in.getActionId());
        body.put("ResponseTime", in.getResponseTime());
        body.put("ResponseCode", in.getResponseCode());
        body.put("ResponseText", in.getResponseText());

        // Datos de la BD
        body.put("ReasonCode", db.get("REASONCODE"));
        body.put("ReasonDesc", db.get("REASONDESC"));
        body.put("ProductType", db.get("PRDTYPE"));
        body.put("ProductDescription", db.get("DESCRIPTION"));
        body.put("ProductCode", db.get("PRDCODE"));
        body.put("ProductFamily", db.get("PRDFAMILY"));
        body.put("CategoryID", db.get("CRPCATEGORYID"));
        body.put("FulfillmentProcID", db.get("FULFILLMENTPROCID"));
        body.put("Availability", db.get("AVAILABILITY"));
        body.put("AppointmentStartDate", iso(db.get("APPTSTARTDATE_TM")));
        body.put("ExpiryDate", iso(db.get("EXPIRYDATE_TM")));
        body.put("OfferCreationDate", iso(db.get("OFFERCREATIONDATE_TM")));
        int minutes = toMinutes(db.get("APPTENDTIMEDUR"));
        if (minutes > 0) {                                               // solo si es mayor que cero
            body.put("AppointmentMinDur", minutes);
        }
        for (int i = 1; i <= 100; i++) {
            body.put("UserField" + i, db.get("USERFIELD_" + i));         // los de la BD pisan a los de la entrada
        }

        if (props.isOmitNulls()) {
            header.values().removeIf(java.util.Objects::isNull);
            body.values().removeIf(java.util.Objects::isNull);
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("header", header);
        event.put("body", body);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("event", event);
        return root;
    }

    // ------------------------------------------------------------------ validación, reintentos y utilidades

    private boolean valid(OfferInput in) {
        if (isBlank(in.getRefId()) || isBlank(in.getCampaignId()) || isBlank(in.getWaveId())
                || isBlank(in.getCustomerNo()) || isBlank(in.getOfferStatus())) {
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

    /** xs:dateTime en ISO-8601 (formato exacto por confirmar con el consumidor). */
    static String iso(Object v) {
        if (v == null) return null;
        if (v instanceof Timestamp t) return t.toInstant().toString();
        return v.toString();
    }

    /** to_char(minutos) -> entero redondeado. */
    static int toMinutes(Object v) {
        if (v == null) return 0;
        try {
            return new BigDecimal(v.toString().trim()).setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}

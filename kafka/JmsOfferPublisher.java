package com.banamex.cops.update_offer.kafka;

import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opción A: deja el envoltorio { RefId, jsonString } en la cola JMS que lee el bridge hacia Kafka
 * (equivale al JMS Queue Sender "KafkaMsgSender" de BW). Formato del cuerpo (JSON o XML) por confirmar.
 */
@Component
@ConditionalOnProperty(name = "offer.kafka.mode", havingValue = "jms")
public class JmsOfferPublisher implements OfferPublisher {

    private final JmsTemplate jms;
    private final OfferKafkaProps props;
    private final JsonMapper mapper;

    public JmsOfferPublisher(@Qualifier("updateOfferJmsTemplate") @Lazy JmsTemplate jms,
                             OfferKafkaProps props, JsonMapper mapper) {
        this.jms = jms;
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public String publish(String refId, String json) throws Exception {
        Map<String, String> envelope = new LinkedHashMap<>();
        envelope.put("RefId", refId);
        envelope.put("jsonString", json);
        String body = mapper.writeValueAsString(envelope);

        AtomicReference<TextMessage> sent = new AtomicReference<>();
        jms.send(props.getQueue(), session -> {
            TextMessage m = session.createTextMessage(body);
            m.setJMSCorrelationID(refId);          // mejora: BW no lo llena
            sent.set(m);
            return m;
        });
        return sent.get().getJMSMessageID();       // BW no registra el MessageID
    }
}

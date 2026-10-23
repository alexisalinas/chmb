package com.banamex.cops.update_offer.sender;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Envía el XML al EMS y devuelve el XML de respuesta (la conversión la hace UpdateOfferMapper). */
@Slf4j
@Component
public class EmsSender {

    private static final Pattern CARD = Pattern.compile("(<(?:\\w+:)?(?:CardNumber|CardNO)(?:\\s[^>]*)?>)\\d*(\\d{4})(</)");

    private final JmsTemplate updateOfferJmsTemplate;

    @Value("${update-offer.ems.queue}")
    private String queueName;

    @Value("${update-offer.ems.timeout-seconds:30}")
    private long timeoutSeconds;

    /** 0 = sin expiración (BW: JMSExpiration). */
    @Value("${update-offer.ems.time-to-live-ms:0}")
    private long timeToLiveMs;

    public EmsSender(@Qualifier("updateOfferJmsTemplate") JmsTemplate updateOfferJmsTemplate) {
        this.updateOfferJmsTemplate = updateOfferJmsTemplate;
    }

    public String send(String xml) {
        log.info("Enviando al EMS: queue={}", queueName);
        log.debug("XML enviado: {}", CARD.matcher(xml).replaceAll("$1************$2$3"));

        try {
            // execute(..., true) inicia la conexión, necesario para poder recibir la respuesta
            String xmlResponse = updateOfferJmsTemplate.execute(session -> {
                TemporaryQueue replyQueue = session.createTemporaryQueue();
                try (MessageProducer producer = session.createProducer(session.createQueue(queueName));
                     MessageConsumer consumer = session.createConsumer(replyQueue)) {

                    TextMessage message = session.createTextMessage(xml);
                    message.setJMSReplyTo(replyQueue);
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    if (timeToLiveMs > 0) {
                        producer.setTimeToLive(timeToLiveMs);
                    }
                    producer.send(message);

                    Message reply = consumer.receive(timeoutSeconds * 1000);

                    if (reply instanceof TextMessage textReply) {
                        return textReply.getText();
                    } else if (reply == null) {
                        throw new IllegalStateException("Timeout esperando respuesta del EMS");
                    } else {
                        throw new IllegalStateException("Tipo de mensaje inesperado recibido");
                    }
                } finally {
                    replyQueue.delete();
                }
            }, true);

            log.debug("XML recibido: {}", xmlResponse);
            return xmlResponse;

        } catch (Exception e) {
            log.error("Error enviando al EMS", e);
            throw new RuntimeException("Error enviando al EMS: " + e.getMessage(), e);
        }
    }
}

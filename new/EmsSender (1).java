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
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Envía el XML al EMS y devuelve el XML de respuesta (la conversión la hace UpdateOfferMapper). */
@Slf4j
@Component
public class EmsSender {

    private static final Pattern CARD = Pattern.compile("(<(?:\\w+:)?(?:CardNumber|CardNO)(?:\\s[^>]*)?>)\\d*(\\d{4})(</)");

    /** Respuesta simulada cuando dry-run = true (el mapper busca Code y Description). */
    private static final String DRY_RUN_REPLY =
            "<Response><Code>DRY-RUN</Code>"
            + "<Description>Mensaje NO enviado al EMS (update-offer.ems.dry-run=true)</Description></Response>";

    private final JmsTemplate updateOfferJmsTemplate;

    @Value("${update-offer.ems.queue}")
    private String queueName;

    @Value("${update-offer.ems.timeout-seconds:30}")
    private long timeoutSeconds;

    /** 0 = sin expiración (BW: JMSExpiration). */
    @Value("${update-offer.ems.time-to-live-ms:0}")
    private long timeToLiveMs;

    /** true = solo escribe el XML en el log y NO se conecta al EMS (no hace falta certificado). */
    @Value("${update-offer.ems.dry-run:false}")
    private boolean dryRun;

    // @Lazy: la conexión al EMS solo se crea cuando realmente se usa (no en el arranque)
    public EmsSender(@Qualifier("updateOfferJmsTemplate") @Lazy JmsTemplate updateOfferJmsTemplate) {
        this.updateOfferJmsTemplate = updateOfferJmsTemplate;
    }

    public String send(String xml) {
        String masked = CARD.matcher(xml).replaceAll("$1************$2$3");

        if (dryRun) {
            log.info("[DRY-RUN] XML que se enviaría al EMS (queue={}):\n{}", queueName, masked);
            return DRY_RUN_REPLY;
        }

        log.info("Enviando al EMS: queue={}", queueName);
        log.debug("XML enviado: {}", masked);

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

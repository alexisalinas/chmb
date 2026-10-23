package com.banamex.cops.update_offer.sender;

import jakarta.jms.BytesMessage;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
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

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Envía el XML al EMS y devuelve la respuesta de la cola como texto
 * (la conversión a UpdateOfferResponse la hace UpdateOfferMapper).
 *
 * La respuesta se escribe completa en el log ([EMS] Respuesta recibida) para ver qué devuelve la cola:
 * tipo de mensaje, propiedades JMS y cuerpo.
 */
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

        log.info("[EMS] Enviando a queue={}:\n{}", queueName, masked);

        try {
            // execute(..., true) inicia la conexion, necesario para poder recibir la respuesta
            String reply = updateOfferJmsTemplate.execute(session -> {
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

                    Message response = consumer.receive(timeoutSeconds * 1000);
                    if (response == null) {
                        throw new IllegalStateException(
                                "Timeout esperando respuesta del EMS (" + timeoutSeconds + " s)");
                    }
                    return readReply(response);
                } finally {
                    replyQueue.delete();
                }
            }, true);

            log.info("[EMS] Respuesta final que se enviará al mapper:\n{}", reply);
            return reply;

        } catch (Exception e) {
            log.error("Error enviando al EMS", e);
            throw new RuntimeException("Error enviando al EMS: " + e.getMessage(), e);
        }
    }

    /** Extrae la respuesta sin importar el tipo de mensaje y la deja en el log. */
    private String readReply(Message reply) throws JMSException {
        StringBuilder props = new StringBuilder();
        Enumeration<?> names = reply.getPropertyNames();
        while (names.hasMoreElements()) {
            String n = (String) names.nextElement();
            props.append("  ").append(n).append(" = ").append(reply.getObjectProperty(n)).append('\n');
        }

        String body;
        if (reply instanceof TextMessage t) {
            body = t.getText();
        } else if (reply instanceof BytesMessage b) {
            byte[] data = new byte[(int) b.getBodyLength()];
            b.readBytes(data);
            body = new String(data, StandardCharsets.UTF_8);
        } else if (reply instanceof MapMessage m) {
            StringBuilder x = new StringBuilder("<Response>");
            Enumeration<?> keys = m.getMapNames();
            while (keys.hasMoreElements()) {
                String k = (String) keys.nextElement();
                x.append('<').append(k).append('>').append(m.getObject(k)).append("</").append(k).append('>');
            }
            body = x.append("</Response>").toString();
        } else {
            body = reply.toString();
        }

        log.info("[EMS] Respuesta recibida (tipo={}, JMSCorrelationID={})\nPropiedades JMS:\n{}Cuerpo:\n{}",
                reply.getClass().getSimpleName(), reply.getJMSCorrelationID(),
                props.length() == 0 ? "  (sin propiedades)\n" : props, body);

        // Si el cuerpo viene vacío, la respuesta puede estar en las propiedades (como los eventos de BE)
        if (body == null || body.isBlank()) {
            StringBuilder x = new StringBuilder("<Response>");
            Enumeration<?> pn = reply.getPropertyNames();
            while (pn.hasMoreElements()) {
                String k = (String) pn.nextElement();
                x.append('<').append(k).append('>').append(reply.getObjectProperty(k)).append("</").append(k).append('>');
            }
            body = x.append("</Response>").toString();
        }
        return body;
    }
}

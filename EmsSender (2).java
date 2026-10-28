package com.banamex.cops.update_offer.sender;

import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
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
                // Guarda en qué paso estamos; si algo falla, el log dice exactamente cuál fue
                String step = "0: inicio";
                TemporaryQueue replyQueue = null;
                MessageProducer producer = null;
                MessageConsumer consumer = null;

                try {
                    step = "1: crear cola temporal de respuesta (createTemporaryQueue)";
                    log.info("[EMS] Paso {}", step);
                    replyQueue = session.createTemporaryQueue();
                    log.info("[EMS] OK cola temporal: {}", replyQueue.getQueueName());

                    step = "2: crear destino de la cola de request (createQueue) -> " + queueName;
                    log.info("[EMS] Paso {}", step);
                    Queue requestQueue = session.createQueue(queueName);

                    step = "3: crear producer sobre la cola de request (createProducer) -> " + queueName;
                    log.info("[EMS] Paso {}", step);
                    producer = session.createProducer(requestQueue);

                    step = "4: crear consumer sobre la cola temporal (createConsumer)";
                    log.info("[EMS] Paso {}", step);
                    consumer = session.createConsumer(replyQueue);

                    step = "5: armar mensaje";
                    TextMessage message = session.createTextMessage(xml);
                    message.setJMSReplyTo(replyQueue);
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    if (timeToLiveMs > 0) {
                        producer.setTimeToLive(timeToLiveMs);
                    }

                    step = "6: enviar mensaje (producer.send) -> " + queueName;
                    log.info("[EMS] Paso {}", step);
                    producer.send(message);
                    log.info("[EMS] Mensaje enviado, id={}", message.getJMSMessageID());

                    step = "7: esperar respuesta (consumer.receive " + timeoutSeconds + "s)";
                    log.info("[EMS] Paso {}", step);
                    Message reply = consumer.receive(timeoutSeconds * 1000);

                    if (reply instanceof TextMessage textReply) {
                        log.info("[EMS] Respuesta recibida");
                        return textReply.getText();
                    } else if (reply == null) {
                        throw new IllegalStateException("Timeout esperando respuesta del EMS");
                    } else {
                        throw new IllegalStateException(
                                "Tipo de mensaje inesperado recibido: " + reply.getClass().getName());
                    }

                } catch (Exception e) {
                    // Aquí se ve exactamente qué paso falló y por qué
                    log.error("[EMS] FALLÓ en el paso {}", step);
                    logExceptionDetails(e);
                    throw new IllegalStateException("Fallo en el paso [" + step + "]: " + describe(e), e);

                } finally {
                    // Cada cierre va aparte y sin lanzar excepción, para no tapar el error original
                    closeQuietly("consumer", consumer);
                    closeQuietly("producer", producer);
                    deleteQuietly(replyQueue);
                }
            }, true);

            log.debug("XML recibido: {}", xmlResponse);
            return xmlResponse;

        } catch (Exception e) {
            // Falla fuera del callback: conexión, sesión, connection factory, etc.
            log.error("Error enviando al EMS (fuera del callback: conexión/sesión)");
            logExceptionDetails(e);
            throw new RuntimeException("Error enviando al EMS: " + describe(e), e);
        }
    }

    /** Imprime toda la cadena de causas y los datos propios de JMSException (código y excepción ligada). */
    private void logExceptionDetails(Throwable top) {
        int level = 0;
        Throwable t = top;
        while (t != null && level < 15) {
            log.error("[EMS][causa {}] {}: {}", level, t.getClass().getName(), t.getMessage());

            if (t instanceof JMSException jmsEx) {
                log.error("[EMS][causa {}]   errorCode={}", level, jmsEx.getErrorCode());
                Exception linked = jmsEx.getLinkedException();
                if (linked != null) {
                    log.error("[EMS][causa {}]   linkedException={}: {}",
                            level, linked.getClass().getName(), linked.getMessage());
                }
            }

            Throwable next = t.getCause();
            if (next == t) {
                break;
            }
            t = next;
            level++;
        }
        // Stack trace completo de la excepción raíz
        log.error("[EMS] Stack trace completo:", top);
    }

    /** Mensaje corto con la causa raíz, para que llegue al response y no solo al log. */
    private String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return e.getClass().getSimpleName() + " -> raíz: "
                + root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private void closeQuietly(String name, AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception ex) {
            log.warn("[EMS] No se pudo cerrar {}: {}", name, ex.getMessage());
        }
    }

    private void deleteQuietly(TemporaryQueue queue) {
        if (queue == null) {
            return;
        }
        try {
            queue.delete();
        } catch (Exception ex) {
            log.warn("[EMS] No se pudo borrar la cola temporal: {}", ex.getMessage());
        }
    }
}

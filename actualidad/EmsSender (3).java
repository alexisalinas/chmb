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
import java.util.Map;
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
    /** Cómo va el mensaje: xml | properties | both. */
    @Value("${update-offer.ems.message-format:both}")
    private String messageFormat = "both";

    private static final String DRY_RUN_REPLY =
            "<Response><Code>DRY-RUN</Code>"
            + "<Description>Mensaje NO enviado al EMS (update-offer.ems.dry-run=true)</Description></Response>";

    private final JmsTemplate updateOfferJmsTemplate;

    /** Cola única a la que se envía (la misma que resulta del concat de las globales de BW). */
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
    /**
     * Crea el emisor al EMS.
     *
     * @param updateOfferJmsTemplate plantilla JMS (con carga diferida: la conexión se crea al usarla)
     */
    public EmsSender(@Qualifier("updateOfferJmsTemplate") @Lazy JmsTemplate updateOfferJmsTemplate) {
        this.updateOfferJmsTemplate = updateOfferJmsTemplate;
    }

    /**
     * Envía el XML a la cola del EMS y espera la respuesta. Escribe en el log lo que envía y lo que responde
     * (con la tarjeta enmascarada). En modo {@code dry-run} no se conecta: solo escribe el XML y devuelve una
     * respuesta de prueba.
     *
     * @param xml mensaje a enviar
     * @return el cuerpo de la respuesta del EMS, o {@code null} si no contestó dentro del tiempo de espera
     *         (o si {@code timeout-seconds} es 0, que manda el mensaje sin esperar respuesta)
     * @throws RuntimeException si falla el envío; el mensaje incluye la causa raíz
     */
    public String send(String xml) {
        return send(xml, Map.of());
    }

    /**
     * Envía el mensaje a la cola del EMS y espera la respuesta. Según {@code update-offer.ems.message-format}:
     * {@code xml} manda solo el XML en el cuerpo; {@code properties} manda solo las propiedades del mensaje JMS
     * ({@code OtherProperties} de BW) con el cuerpo vacío; {@code both} (por defecto) manda las dos cosas.
     * Escribe en el log lo que envía (con la tarjeta enmascarada) y lo que responde.
     *
     * @param xml        XML del evento (cuerpo)
     * @param properties propiedades del mensaje (_nm_, _ns_ y los campos del evento); puede ser vacío o nulo
     * @return el cuerpo de la respuesta del EMS, o {@code null} si no contestó dentro del tiempo de espera
     * @throws RuntimeException si falla el envío; el mensaje incluye la causa raíz
     */
    public String send(String xml, Map<String, String> properties) {
        String queue = queueName;
        boolean soloXml = "xml".equalsIgnoreCase(messageFormat);
        boolean soloPropiedades = "properties".equalsIgnoreCase(messageFormat);
        Map<String, String> props = soloXml || properties == null ? Map.of() : properties;
        String body = soloPropiedades ? "" : xml;
        String masked = CARD.matcher(xml).replaceAll("$1************$2$3");
        String detalle = (soloPropiedades ? "(cuerpo vacío)" : masked) + describeProperties(props);

        if (dryRun) {
            log.info("[EMS] dry-run: no se envió. Enviaría a queue={}:\n{}", queue, detalle);
            return DRY_RUN_REPLY;
        }

        log.info("[EMS] Envía a queue={}:\n{}", queue, detalle);

        try {
            // execute(..., true) inicia la conexion, necesario para poder recibir la respuesta
            String reply = updateOfferJmsTemplate.execute(session -> {
                boolean esperar = timeoutSeconds > 0;
                TemporaryQueue replyQueue = esperar ? session.createTemporaryQueue() : null;
                MessageConsumer consumer = esperar ? session.createConsumer(replyQueue) : null;
                try (MessageProducer producer = session.createProducer(session.createQueue(queue))) {

                    TextMessage message = session.createTextMessage(body);
                    for (Map.Entry<String, String> p : props.entrySet()) {
                        if (p.getValue() != null) {
                            message.setStringProperty(p.getKey(), p.getValue());
                        }
                    }
                    if (esperar) {
                        message.setJMSReplyTo(replyQueue);
                    }
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                    if (timeToLiveMs > 0) {
                        producer.setTimeToLive(timeToLiveMs);
                    }
                    producer.send(message);

                    if (!esperar) {
                        return null;
                    }
                    Message response = consumer.receive(timeoutSeconds * 1000);
                    return response == null ? null : readReply(response);
                } finally {
                    if (consumer != null) {
                        consumer.close();
                    }
                    if (replyQueue != null) {
                        replyQueue.delete();
                    }
                }
            }, true);

            log.info("[EMS] Responde:\n{}", reply != null ? reply : "(sin respuesta)");
            return reply;

        } catch (Exception e) {
            // Spring envuelve el error real en "Uncategorized exception occurred during JMS processing":
            // la causa de verdad (SSL, usuario, red...) está en la cadena de causas.
            log.error("[EMS] Falló el envío a queue={}. Causas: {}", queue, causeChain(e), e);
            throw new RuntimeException("Error enviando al EMS: " + rootCause(e), e);
        }
    }

    /** Causa más profunda como "Clase: mensaje" (lo que se devuelve en la respuesta). */
    /** Las propiedades del mensaje para el log, una por línea; la tarjeta va enmascarada. */
    static String describeProperties(Map<String, String> props) {
        if (props == null || props.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\nPropiedades del mensaje (OtherProperties):");
        props.forEach((k, v) -> sb.append("\n  ").append(k).append('=')
                .append(("CardNO".equals(k) || "CardNumber".equals(k)) && v != null && v.length() > 4
                        ? "************" + v.substring(v.length() - 4) : v));
        return sb.toString();
    }

    static String rootCause(Throwable t) {
        Throwable root = t;
        for (int i = 0; i < 20 && root.getCause() != null && root.getCause() != root; i++) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    /** Toda la cadena en una línea: "A: msg <- B: msg <- C: msg" (para ver la causa sin recorrer el stacktrace). */
    static String causeChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable c = t;
        for (int i = 0; i < 20 && c != null; i++) {
            if (i > 0) sb.append(" <- ");
            sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
            if (c.getCause() == c) break;
            c = c.getCause();
        }
        return sb.toString();
    }

    /** Extrae la respuesta sin importar el tipo de mensaje (el log de la respuesta lo escribe send). */
    private String readReply(Message reply) throws JMSException {
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

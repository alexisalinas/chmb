package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.update_offer.mapper.XmlUtil;
import jakarta.jms.DeliveryMode;
import jakarta.jms.MessageProducer;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica el evento en la cola JMS que lee el bridge hacia Kafka (equivale al JMS Queue Sender
 * {@code KafkaMsgSender} de BW).
 * <p>
 * Usa la misma conexión y la misma configuración que el JMS del EMS: entrega no persistente y la expiración de
 * {@code offer.kafka.time-to-live-ms} (en milisegundos; 0 = sin expiración).
 * <p>
 * Con {@code offer.kafka.message-format: json} (por defecto) el mensaje es el JSON del evento tal cual, todo junto; el
 * {@code RefId} va como {@code JMSCorrelationID} y como propiedad {@code RefId}. Con {@code envelope} el cuerpo es el XML que
 * arma BW en {@code Body/root}: {@code <root><RefId>..</RefId><jsonString>..</jsonString></root>}.
 * La cola es única y sale de {@code offer.kafka.queue}.
 */
@Component
@ConditionalOnProperty(name = "offer.kafka.mode", havingValue = "jms")
public class JmsOfferPublisher implements OfferPublisher {

    private final JmsTemplate jms;
    private final OfferKafkaProps props;

    /**
     * Crea el publicador por JMS.
     *
     * @param jms   plantilla JMS que se conecta al broker (la misma del EMS; carga diferida)
     * @param props propiedades de Kafka (cola del bridge y expiración)
     */
    public JmsOfferPublisher(@Qualifier("updateOfferJmsTemplate") @Lazy JmsTemplate jms, OfferKafkaProps props) {
        this.jms = jms;
        this.props = props;
    }

    /**
     * Deja en la cola el mensaje: el JSON del evento todo junto, o el envoltorio {@code <root>} (según
     * {@code offer.kafka.message-format}).
     *
     * @param refId identificador del mensaje (también va como JMSCorrelationID)
     * @param json  evento en JSON que se publica
     * @return el identificador del mensaje JMS publicado
     */
    @Override
    public String publish(String refId, String json) throws Exception {
        String body = "envelope".equalsIgnoreCase(props.getMessageFormat())
                ? "<root><RefId>" + XmlUtil.esc(refId) + "</RefId><jsonString>" + XmlUtil.esc(json) + "</jsonString></root>"
                : json;

        return jms.execute(session -> {
            try (MessageProducer producer = session.createProducer(session.createQueue(props.getQueue()))) {
                TextMessage message = session.createTextMessage(body);
                message.setJMSCorrelationID(refId);           // mejora: BW no lo llena
                message.setStringProperty("RefId", refId);    // el RefId también como propiedad del mensaje
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                if (props.getTimeToLiveMs() > 0) {
                    producer.setTimeToLive(props.getTimeToLiveMs());
                }
                producer.send(message);
                return message.getJMSMessageID();             // BW no registra el MessageID
            }
        }, false);
    }
}

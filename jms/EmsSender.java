package com.banamex.cops.update_offer.sender;

import com.banamex.cops.update_offer.dto.soap.updateofferRq;
import com.banamex.cops.update_offer.dto.soap.updateofferRs;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;

@Slf4j
@Component
public class EmsSender {

    private final JmsTemplate updateOfferJmsTemplate;

    @Value("${update-offer.ems.queue}")
    private String queueName;

    @Value("${update-offer.ems.timeout-seconds:30}")
    private long timeoutSeconds;

    public EmsSender(@Qualifier("updateOfferJmsTemplate") JmsTemplate updateOfferJmsTemplate) {
        this.updateOfferJmsTemplate = updateOfferJmsTemplate;
    }

    public updateofferRs send(updateofferRq request) {
        log.info("Enviando al EMS: campaignID={}, queue={}", request.getCampaignID(), queueName);

        try {
            String xml = serializeToXml(request);
            log.debug("XML enviado: {}", xml);

            // execute(..., true) inicia la conexion, necesario para poder recibir la respuesta
            String xmlResponse = updateOfferJmsTemplate.execute(session -> {
                TemporaryQueue replyQueue = session.createTemporaryQueue();
                try (MessageProducer producer = session.createProducer(session.createQueue(queueName));
                     MessageConsumer consumer = session.createConsumer(replyQueue)) {

                    TextMessage message = session.createTextMessage(xml);
                    message.setJMSReplyTo(replyQueue);
                    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
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
            return deserializeFromXml(xmlResponse, updateofferRs.class);

        } catch (Exception e) {
            log.error("Error enviando al EMS", e);
            throw new RuntimeException("Error enviando al EMS: " + e.getMessage(), e);
        }
    }

    private String serializeToXml(Object obj) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(obj.getClass());
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
        StringWriter sw = new StringWriter();
        m.marshal(obj, sw);
        return sw.toString();
    }

    private <T> T deserializeFromXml(String xml, Class<T> clazz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(clazz);
        Unmarshaller u = ctx.createUnmarshaller();
        return clazz.cast(u.unmarshal(new StringReader(xml)));
    }
}

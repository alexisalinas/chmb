package com.banamex.cops.update_offer.kafka;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Destinos activos del evento: el bridge de Kafka y el EMS alterno, cada uno con su interruptor en el yml. */
@Component
public class EventDestinations {

    /** Un destino: etiqueta del log, nombre en los textos y quién publica. */
    public record EventDestination(String tag, String name, OfferPublisher publisher) {
    }

    /** Modo dry-run (offer.kafka.mode): no envía nada. */
    private static final OfferPublisher DRY_RUN = (refId, json) -> "dry-run";

    private Supplier<List<EventDestination>> destinations;

    /** Arma los destinos desde el yml en cada envío, así un cambio de configuración se respeta. */
    public EventDestinations(OfferKafkaProps kafka, AlternateEmsProps alternate, BeanFactory beans) {
        this.destinations = () -> {
            List<EventDestination> out = new ArrayList<>();
            boolean dryRun = "dry-run".equals(kafka.getMode());
            if (kafka.isBridgeEnabled()) {
                out.add(new EventDestination("KAFKA", "Kafka", dryRun ? DRY_RUN : new JmsOfferPublisher(
                        template(beans, "updateOfferJmsTemplate"), kafka.getQueue(), kafka.getMessageFormat(), kafka.getTimeToLiveMs())));
            }
            if (alternate.isEnabled()) {
                out.add(new EventDestination("EMS-ALTERNO", "EMS alterno", dryRun ? DRY_RUN : new JmsOfferPublisher(
                        template(beans, alternate.getJmsTemplateName()), alternate.getQueue(),
                        alternate.getMessageFormat(), alternate.getTimeToLiveMs())));
            }
            return out;
        };
    }

    /** Destinos fijos (para pruebas). */
    public static EventDestinations of(EventDestination... fixed) {
        EventDestinations result = new EventDestinations(null, null, null);
        result.destinations = () -> List.of(fixed);
        return result;
    }

    /** Destinos activos, en orden: Kafka y luego el EMS alterno. */
    public List<EventDestination> enabled() {
        return destinations.get();
    }

    private static Supplier<JmsTemplate> template(BeanFactory beans, String name) {
        return () -> {
            try {
                return beans.getBean(name, JmsTemplate.class);
            } catch (NoSuchBeanDefinitionException e) {
                throw new IllegalStateException("no existe el bean JmsTemplate '" + name
                        + "': revisa su conexión en digital-integration.messaging.jms (jmsTemplateName) o en JmsConfig", e);
            }
        };
    }
}

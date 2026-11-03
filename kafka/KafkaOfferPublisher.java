package com.banamex.cops.update_offer.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Opción B (OPCIONAL): publica directo en Kafka. Requiere la dependencia spring-kafka y la configuración
 * de seguridad del broker. NO la actives junto con el bridge JMS: el consumidor recibiría cada oferta duplicada.
 * Copia este archivo a com.banamex.cops.update_offer.kafka solo si vas a usar mode=kafka.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "offer.kafka.mode", havingValue = "kafka")
public class KafkaOfferPublisher implements OfferPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final OfferKafkaProps props;

    public KafkaOfferPublisher(KafkaTemplate<String, String> kafka, OfferKafkaProps props) {
        this.kafka = kafka;
        this.props = props;
    }

    /** El replicator de DR solo copia los tópicos que empiezan con lac.* */
    @PostConstruct
    void validarTopico() {
        if (props.getTopic() == null || !props.getTopic().startsWith("lac.")) {
            log.warn("KAFKA-CONFIG el tópico '{}' no empieza con 'lac.': el replicator de DR no lo copiará", props.getTopic());
        }
    }

    @Override
    public String publish(String refId, String json) throws Exception {
        var md = kafka.send(props.getTopic(), refId, json).get(10, TimeUnit.SECONDS).getRecordMetadata();
        return md.partition() + "-" + md.offset();
    }
}

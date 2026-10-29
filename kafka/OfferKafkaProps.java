package com.banamex.cops.update_offer.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuración del flujo FulFillKafkaSender (propiedades offer.kafka.* del yml). */
@Data
@Component
@ConfigurationProperties(prefix = "offer.kafka")
public class OfferKafkaProps {

    /** BW: condición de la transición hacia KafkaSenderDashboard (global por confirmar). */
    private boolean enabled = false;

    /** dry-run (solo escribe el JSON en el log) | jms (cola del bridge) | kafka (directo). */
    private String mode = "dry-run";

    /** BW: COP/Kafka/EHKafkaUpdate_Delay (ms). */
    private long delayMs = 200;

    private String queue;
    private String topic;

    private String offerPrefix = "off:mx:";
    private String wavePrefix = "wvch:";
    /** CONFIRMAR contra el XPath original de DataFetch. */
    private String keySeparator = ":";

    /** true = los campos nulos no se escriben en el JSON (formato de Render JSON por confirmar). */
    private boolean omitNulls = true;
    /** true = CardNumber sale con solo los últimos 4 dígitos en el mensaje (acordar con seguridad). */
    private boolean maskCardNumber = false;

    private int maxAttempts = 3;
    private long retryBackoffMs = 500;

    // Constantes del header (KafkaSenderDashboard)
    private String producerCsi = "163621";
    private String eventName = "Updateofferrequest";
    private String eventVersion = "1.0";
    private String countryCode = "MX";
    private String businessCode = "DM";

    // Ejecutor asíncrono con cola acotada
    private int executorCoreSize = 2;
    private int executorMaxSize = 4;
    private int executorQueueCapacity = 100;
}

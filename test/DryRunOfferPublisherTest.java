package com.banamex.cops.update_offer.kafka;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/** Requiere Logback como implementación de logging. */
class DryRunOfferPublisherTest {

    private final DryRunOfferPublisher publisher = new DryRunOfferPublisher();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(DryRunOfferPublisher.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void noEnviaNadaYDevuelveDryRun() throws Exception {
        assertThat(publisher.publish("ref1", "{\"a\":1}")).isEqualTo("dry-run");
    }

    @Test
    void escribeElJsonEnElLog() {
        publisher.publish("ref1", "{\"a\":1}");

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("[KAFKA-DRY-RUN]", "refId=ref1", "{\"a\":1}");
    }

    @Test
    void enmascaraLaTarjetaEnElLog() {
        publisher.publish("ref1", "{\"body\":{\"CardNumber\":\"1234567812345678\",\"x\":\"y\"}}");

        String msg = appender.list.get(0).getFormattedMessage();
        assertThat(msg).contains("\"CardNumber\":\"************5678\"").doesNotContain("1234567812345678");
    }
}

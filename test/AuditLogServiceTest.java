package com.banamex.cops.update_offer.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Requiere Logback como implementación de logging (la de Spring Boot por defecto). */
class AuditLogServiceTest {

    private AuditLogService service;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private final TransactionContext ctx = TransactionContext.from(TestData.rqHeader());

    @BeforeEach
    void setUp() {
        service = new AuditLogService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "applicationId", "cop_163621");
        ReflectionTestUtils.setField(service, "componentName", "COPS");
        ReflectionTestUtils.setField(service, "countryCode", "MX");
        ReflectionTestUtils.setField(service, "serviceVersion", "1.0");
        ReflectionTestUtils.setField(service, "requestServiceName", "v3_0_0_0");
        ReflectionTestUtils.setField(service, "responseServiceName", "Offer");

        logger = (Logger) LoggerFactory.getLogger(AuditLogService.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> eventos() {
        return appender.list;
    }

    @Test
    void logRequest_escribeTituloHeaderYPayload() {
        service.logRequest(ctx, "<root><x>1</x></root>");

        assertThat(eventos()).hasSize(1);
        String msg = eventos().get(0).getFormattedMessage();
        assertThat(msg).startsWith("Logging Service request:")
                .contains("<Header>", "<ApplicationID>cop_163621</ApplicationID>",
                        "<ComponentName>COPS</ComponentName>",
                        "<Timestamp>2026-10-26T10:54:55.852</Timestamp>",
                        "<TransactionID> ESBUUID: uuid-120261026105455 UUID: uuid-1 </TransactionID>",
                        "<CountryCode>MX</CountryCode>",
                        "<ServiceName>v3_0_0_0</ServiceName>",
                        "<ServiceVersion>1.0</ServiceVersion>",
                        "<root><x>1</x></root>");
    }

    @Test
    void logResponse_noLlevaCountryCodeYUsaElNombreDeServicioDeRespuesta() {
        service.logResponse(ctx, "<root/>");

        String msg = eventos().get(0).getFormattedMessage();
        assertThat(msg).startsWith("Logging Service response:")
                .contains("<ServiceName>Offer</ServiceName>")
                .doesNotContain("<CountryCode>");
    }

    @Test
    void conAuditoriaApagada_noEscribeNada() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.logRequest(ctx, "<root/>");
        service.logResponse(ctx, "<root/>");

        assertThat(eventos()).isEmpty();
    }

    @Test
    void enmascaraElNumeroDeTarjetaDejandoLosUltimosCuatroDigitos() {
        service.logRequest(ctx, "<a><CardNO>1234567812345678</CardNO><CardNumber>9999888877776666</CardNumber></a>");

        String msg = eventos().get(0).getFormattedMessage();
        assertThat(msg).contains("<CardNO>************5678</CardNO>", "<CardNumber>************6666</CardNumber>")
                .doesNotContain("1234567812345678", "9999888877776666");
    }

    @Test
    void enmascaraTambienConPrefijoOAtributos() {
        service.logRequest(ctx, "<a><x:CardNumber xmlns:x=\"u\">1111222233334444</x:CardNumber></a>");

        assertThat(eventos().get(0).getFormattedMessage()).doesNotContain("1111222233334444").contains("4444");
    }

    @Test
    void payloadNulo_noLanzaExcepcionYSeEscribeElHeader() {
        service.logRequest(ctx, null);

        assertThat(eventos()).hasSize(1);
        assertThat(eventos().get(0).getFormattedMessage()).contains("<Header>");
    }

    @Test
    void escapaLosCaracteresEspecialesDelHeader() {
        ReflectionTestUtils.setField(service, "componentName", "A&B<C>");
        service.logRequest(ctx, "<root/>");
        assertThat(eventos().get(0).getFormattedMessage()).contains("<ComponentName>A&amp;B&lt;C&gt;</ComponentName>");
    }

    @Test
    void elNivelDeEscrituraEsInfo() {
        service.logRequest(ctx, "<root/>");
        assertThat(eventos().get(0).getLevel()).isEqualTo(Level.INFO);
    }
}

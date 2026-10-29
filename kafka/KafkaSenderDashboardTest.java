package com.banamex.cops.update_offer.kafka;

import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaSenderDashboardTest {

    private final KafkaSenderDashboard dashboard = new KafkaSenderDashboard(new OfferKeyBuilder(new OfferKafkaProps()));

    private UpdateOfferResponse respuesta() {
        UpdateOfferResponse r = new UpdateOfferResponse();
        r.setCode("000");
        r.setDescription("OK");
        return r;
    }

    @Test
    void armaElEventoDeEntradaConLosDatosDelRequestElHeaderYLaRespuesta() {
        OfferInput in = dashboard.build(TestData.request(), TestData.rqHeader(), respuesta());

        assertThat(in.getUuid()).isEqualTo("uuid-1");                          // RqHeader.UUID
        assertThat(in.getEsbUuid()).isEqualTo("uuid-120261026105455");         // va como sid
        assertThat(in.getChannel()).isEqualTo("CAT");
        assertThat(in.getCampaignId()).isEqualTo("CAMP001");
        assertThat(in.getWaveId()).isEqualTo("WAVE01");
        assertThat(in.getCustomerNo()).isEqualTo("12345678");                  // sin ceros
        assertThat(in.getAccountNo()).isEqualTo("00112233445566");
        assertThat(in.getCardNumber()).isEqualTo("1234567812345678");
        assertThat(in.getOfferStatus()).isEqualTo("01");
        assertThat(in.getResponseCode()).isEqualTo("000");
        assertThat(in.getResponseText()).isEqualTo("OK");
        assertThat(in.getActionId()).isNull();
    }

    @Test
    void elRefIdEsLaMismaClaveDeLaOferta() {
        OfferInput in = dashboard.build(TestData.request(), TestData.rqHeader(), respuesta());
        assertThat(in.getRefId()).isEqualTo("off:mx:CAMP001:WAVE01:12345678:00112233445566");
    }

    @Test
    void lasHorasSeLlenanConLaFechaActual() {
        OfferInput in = dashboard.build(TestData.request(), TestData.rqHeader(), respuesta());
        assertThat(in.getTransactionTime()).isNotBlank();
        assertThat(in.getResponseTime()).isNotBlank();
        java.time.OffsetDateTime.parse(in.getTransactionTime());
    }

    @Test
    void sinRespuesta_elCodigoYElTextoQuedanNulos() {
        OfferInput in = dashboard.build(TestData.request(), TestData.rqHeader(), null);
        assertThat(in.getResponseCode()).isNull();
        assertThat(in.getResponseText()).isNull();
    }
}

package com.banamex.cops.update_offer.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaMetricsTest {

    @Test
    void empiezanEnCeroYCadaContadorEsIndependiente() {
        KafkaMetrics m = new KafkaMetrics();
        assertThat(m.getSent() + m.getNoRows() + m.getInvalid() + m.getFailed() + m.getRejected()).isZero();

        m.incSent();
        m.incSent();
        m.incNoRows();
        m.incInvalid();
        m.incFailed();
        m.incRejected();
        m.incRejected();
        m.incRejected();

        assertThat(m.getSent()).isEqualTo(2);
        assertThat(m.getNoRows()).isEqualTo(1);
        assertThat(m.getInvalid()).isEqualTo(1);
        assertThat(m.getFailed()).isEqualTo(1);
        assertThat(m.getRejected()).isEqualTo(3);
    }
}

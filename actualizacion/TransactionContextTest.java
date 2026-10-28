package com.banamex.cops.update_offer.audit;

import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionContextTest {

    @Test
    void from_reutilizaLosDatosDelRqHeader() {
        TransactionContext c = TransactionContext.from(TestData.rqHeader());

        assertThat(c.getUuid()).isEqualTo("uuid-1");
        assertThat(c.getEsbUuid()).isEqualTo("uuid-120261026105455");
        assertThat(c.getTimestamp()).isEqualTo("2026-10-26T10:54:55.852");
    }

    @Test
    void from_sinFechaEnElHeaderUsaLaFechaActual() {
        RqHeaderDTO h = TestData.rqHeader();
        h.setDateTimeStamp(null);
        assertThat(TransactionContext.from(h).getTimestamp()).matches("\\d{4}-\\d{2}-\\d{2}T.*");
    }

    @Test
    void transactionId_tieneElFormatoDelLogDeBW() {
        TransactionContext c = TransactionContext.from(TestData.rqHeader());
        assertThat(c.transactionId()).isEqualTo(" ESBUUID: uuid-120261026105455 UUID: uuid-1 ");
    }
}

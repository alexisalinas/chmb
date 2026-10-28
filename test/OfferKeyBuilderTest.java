package com.banamex.cops.update_offer.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfferKeyBuilderTest {

    private OfferKafkaProps props;
    private OfferKeyBuilder keys;

    @BeforeEach
    void setUp() {
        props = new OfferKafkaProps();
        keys = new OfferKeyBuilder(props);
    }

    @Test
    void offerKey_conCuenta() {
        assertThat(keys.offerKey("C1", "W1", "123", "ACC")).isEqualTo("off:mx:C1:W1:123:ACC");
    }

    @Test
    void offerKey_sinCuentaNoDejaSeparadorFinal() {
        assertThat(keys.offerKey("C1", "W1", "123", null)).isEqualTo("off:mx:C1:W1:123");
        assertThat(keys.offerKey("C1", "W1", "123", "")).isEqualTo("off:mx:C1:W1:123");
        assertThat(keys.offerKey("C1", "W1", "123", "  ")).isEqualTo("off:mx:C1:W1:123");
    }

    @Test
    void waveKey_usaPrefijoDeOlaYCanal() {
        assertThat(keys.waveKey("W1", "CAT")).isEqualTo("wvch:W1:CAT");
    }

    @Test
    void prefijosYSeparadorSonConfigurables() {
        props.setOfferPrefix("o:");
        props.setWavePrefix("w:");
        props.setKeySeparator("|");
        assertThat(keys.offerKey("C1", "W1", "123", "ACC")).isEqualTo("o:C1|W1|123|ACC");
        assertThat(keys.waveKey("W1", "CAT")).isEqualTo("w:W1|CAT");
    }

    @Test
    void elCustomerNoSeUsaSinCerosEnLaClave() {
        assertThat(keys.offerKey("C1", "W1", "7", null)).isEqualTo("off:mx:C1:W1:7");
    }
}

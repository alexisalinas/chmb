package com.banamex.cops.update_offer.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyProcessEnumTest {

    @Test
    void fromValue_aceptaMayusculasYMinusculas() {
        assertThat(KeyProcessEnum.fromValue("V1")).isEqualTo(KeyProcessEnum.V1);
        assertThat(KeyProcessEnum.fromValue("v2")).isEqualTo(KeyProcessEnum.V2);
        assertThat(KeyProcessEnum.fromValue("V3")).isEqualTo(KeyProcessEnum.V3);
    }

    @Test
    void getValue_devuelveElTextoOriginal() {
        assertThat(KeyProcessEnum.V1.getValue()).isEqualTo("V1");
        assertThat(KeyProcessEnum.V3.getValue()).isEqualTo("V3");
    }

    @Test
    void valorInvalido_lanzaIllegalArgument() {
        assertThatThrownBy(() -> KeyProcessEnum.fromValue("V9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("V9");
        assertThatThrownBy(() -> KeyProcessEnum.fromValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void soloExistenTresVersiones() {
        assertThat(KeyProcessEnum.values()).containsExactly(KeyProcessEnum.V1, KeyProcessEnum.V2, KeyProcessEnum.V3);
    }
}

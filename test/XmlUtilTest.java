package com.banamex.cops.update_offer.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XmlUtilTest {

    @Test
    void esc_escapaLosCincoCaracteresEspeciales() {
        assertThat(XmlUtil.esc("a & b < c > d \" e ' f"))
                .isEqualTo("a &amp; b &lt; c &gt; d &quot; e &apos; f");
    }

    @Test
    void tag_escribeElElementoConIndentacionYEscapaElValor() {
        StringBuilder x = new StringBuilder();
        XmlUtil.tag(x, "  ", "Name", "a&b");
        assertThat(x.toString()).isEqualTo("  <Name>a&amp;b</Name>\n");
    }

    @Test
    void tag_omiteElElementoCuandoElValorEsNull() {
        StringBuilder x = new StringBuilder();
        XmlUtil.tag(x, "  ", "Name", null);
        assertThat(x).isEmpty();
    }

    @Test
    void tag_conValorVacioEscribeElementoAbiertoYCerrado() {
        StringBuilder x = new StringBuilder();
        XmlUtil.tag(x, "", "Name", "");
        assertThat(x.toString()).isEqualTo("<Name></Name>\n");
    }

    @Test
    void tagOrEmpty_nullOVacioSeEscribeAutocerrado() {
        StringBuilder x = new StringBuilder();
        XmlUtil.tagOrEmpty(x, "", "A", null);
        XmlUtil.tagOrEmpty(x, "", "B", "");
        assertThat(x.toString()).isEqualTo("<A/>\n<B/>\n");
    }

    @Test
    void tagOrEmpty_conValorEscribeElElemento() {
        StringBuilder x = new StringBuilder();
        XmlUtil.tagOrEmpty(x, "", "A", "1");
        assertThat(x.toString()).isEqualTo("<A>1</A>\n");
    }
}

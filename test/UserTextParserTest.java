package com.banamex.cops.update_offer.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserTextParserTest {

    private UserTextParser parser;

    @BeforeEach
    void setUp() {
        parser = new UserTextParser(JsonMapper.builder().build());
    }

    @Test
    void nullOVacio_devuelveMapaVacio() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void objetoConClaves_seLeenPorIndice() {
        Map<Integer, String> r = parser.parse("{\"userField1\":\"a\",\"UserField2\":\"b\",\"USERFIELD30\":\"c\"}");
        assertThat(r).containsEntry(1, "a").containsEntry(2, "b").containsEntry(30, "c").hasSize(3);
    }

    @Test
    void formatoParameterNameParameterValue_delSwagger() {
        Map<Integer, String> r = parser.parse("{\"parameterName\":\"userField1\",\"parameterValue\":\"This offer is exclusive\"}");
        assertThat(r).containsOnlyKeys(1).containsEntry(1, "This offer is exclusive");
    }

    @Test
    void arregloDeParameterNameParameterValue() {
        Map<Integer, String> r = parser.parse(
                "[{\"parameterName\":\"userField1\",\"parameterValue\":\"a\"},"
              + "{\"parameterName\":\"userField7\",\"parameterValue\":\"g\"}]");
        assertThat(r).containsEntry(1, "a").containsEntry(7, "g").hasSize(2);
    }

    @Test
    void ignoraLosNombresFueraDeRango() {
        Map<Integer, String> r = parser.parse(
                "{\"userField0\":\"x\",\"userField101\":\"y\",\"userField100\":\"ok\",\"otroCampo\":\"z\"}");
        assertThat(r).containsOnlyKeys(100).containsEntry(100, "ok");
    }

    @Test
    void jsonInvalido_noLanzaExcepcionYDevuelveVacio() {
        assertThat(parser.parse("{esto no es json")).isEmpty();
    }

    @Test
    void valorNulo_seTomaComoCadenaVaciaONull() {
        Map<Integer, String> r = parser.parse("{\"userField1\":null}");
        assertThat(r).containsKey(1);
    }

    @Test
    void elMapaVieneOrdenadoPorIndice() {
        Map<Integer, String> r = parser.parse("{\"userField10\":\"j\",\"userField2\":\"b\",\"userField1\":\"a\"}");
        assertThat(r.keySet()).containsExactly(1, 2, 10);
    }
}

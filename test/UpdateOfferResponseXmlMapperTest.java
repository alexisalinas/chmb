package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.ControlFieldDTO;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.testutil.XmlTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateOfferResponseXmlMapperTest {

    private UpdateOfferResponseXmlMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UpdateOfferResponseXmlMapper(JsonMapper.builder().build());
    }

    // ------------------------------------------------------------ XML

    @Test
    void xml_conNombresNuevos() {
        UpdateOfferResponse r = mapper.fromReply(
                "<Response><Code>000</Code><Description>OK</Description>"
              + "<CustomerReferenceNumber>REF1</CustomerReferenceNumber><StartDate>2026-10-01</StartDate>"
              + "<EndDate>2026-10-31</EndDate><SurveyModeCode>M1</SurveyModeCode></Response>");

        assertThat(r.getCode()).isEqualTo("000");
        assertThat(r.getDescription()).isEqualTo("OK");
        assertThat(r.getCustomerReferenceNumber()).isEqualTo("REF1");
        assertThat(r.getStartDate()).isEqualTo("2026-10-01");
        assertThat(r.getEndDate()).isEqualTo("2026-10-31");
        assertThat(r.getSurveyModeCode()).isEqualTo("M1");
    }

    @Test
    void xml_conNombresAntiguosDeBW() {
        UpdateOfferResponse r = mapper.fromReply(
                "<Rs><ReferenceContacto>R9</ReferenceContacto><ModeCmp>MC</ModeCmp>"
              + "<Code>0</Code><Description>ok</Description></Rs>");
        assertThat(r.getCustomerReferenceNumber()).isEqualTo("R9");
        assertThat(r.getSurveyModeCode()).isEqualTo("MC");
    }

    @Test
    void xml_conPrefijosDeNamespaceSeEncuentraIgual() {
        UpdateOfferResponse r = mapper.fromReply(
                "<a:Rs xmlns:a=\"urn:x\"><a:Code>7</a:Code><a:Description>desc</a:Description></a:Rs>");
        assertThat(r.getCode()).isEqualTo("7");
        assertThat(r.getDescription()).isEqualTo("desc");
    }

    @Test
    void xml_controlFieldSeConvierteAContextType() {
        UpdateOfferResponse r = mapper.fromReply(
                "<Rs><Code>0</Code><Description>d</Description><ControlField>c1</ControlField><ControlField>c2</ControlField></Rs>");
        assertThat(r.getContextType()).extracting(ControlFieldDTO::getContextType).containsExactly("c1", "c2");
    }

    @Test
    void xml_sinCodeNiDescription_usaValoresPorDefecto() {
        UpdateOfferResponse r = mapper.fromReply("<Rs><Otra>x</Otra></Rs>");
        assertThat(r.getCode()).isEqualTo("ERROR");
        assertThat(r.getDescription()).isNotBlank();
        assertThat(r.getContextType()).isNull();
    }

    @Test
    void xml_mal_formado_lanzaIllegalState() {
        assertThatThrownBy(() -> mapper.fromReply("<Rs><Code>0</Rs>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void xml_conDoctype_seRechaza() {
        assertThatThrownBy(() -> mapper.fromReply("<!DOCTYPE a [<!ENTITY x \"y\">]><a><Code>&x;</Code></a>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void respuestaVaciaONula_lanzaIllegalState() {
        assertThatThrownBy(() -> mapper.fromReply(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> mapper.fromReply("  ")).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------ JSON

    @Test
    void json_conNombresNuevosYAntiguos() {
        UpdateOfferResponse r = mapper.fromReply(
                "{\"code\":\"000\",\"description\":\"OK\",\"referenceContacto\":\"R1\",\"modeCmp\":\"M\"}");
        assertThat(r.getCode()).isEqualTo("000");
        assertThat(r.getDescription()).isEqualTo("OK");
        assertThat(r.getCustomerReferenceNumber()).isEqualTo("R1");
        assertThat(r.getSurveyModeCode()).isEqualTo("M");
    }

    @Test
    void json_anidadoSeBuscaRecursivamente() {
        UpdateOfferResponse r = mapper.fromReply("{\"response\":{\"data\":{\"Code\":\"5\",\"Description\":\"anidado\"}}}");
        assertThat(r.getCode()).isEqualTo("5");
        assertThat(r.getDescription()).isEqualTo("anidado");
    }

    @Test
    void json_conArreglo() {
        UpdateOfferResponse r = mapper.fromReply("{\"items\":[{\"code\":\"9\",\"description\":\"d\"}]}");
        assertThat(r.getCode()).isEqualTo("9");
    }

    @Test
    void json_sinCode_usaError() {
        UpdateOfferResponse r = mapper.fromReply("{\"x\":1}");
        assertThat(r.getCode()).isEqualTo("ERROR");
    }

    @Test
    void json_invalido_lanzaIllegalState() {
        assertThatThrownBy(() -> mapper.fromReply("{no es json")).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------ XML del log de respuesta

    @Test
    void logXml_sinUrlsYBienFormado() {
        UpdateOfferResponse rs = new UpdateOfferResponse();
        rs.setCode("000");
        rs.setDescription("OK");
        rs.setCustomerReferenceNumber("REF1");
        rs.setSurveyModeCode("M1");
        rs.setContextType(List.of(ControlFieldDTO.builder().contextType("c1").build()));

        String xml = mapper.toLogXml(rs);

        XmlTestUtil.parse(xml);
        assertThat(XmlTestUtil.hasUrls(xml)).isFalse();
        assertThat(xml).contains("<root>", "<UpdateOfferRs>", "<Code>000</Code>", "<Description>OK</Description>",
                "<ReferenceContacto>REF1</ReferenceContacto>", "<ModeCmp>M1</ModeCmp>",
                "<ControlField>c1</ControlField>");
    }

    @Test
    void logXml_losCamposNulosVanAutocerrados() {
        UpdateOfferResponse rs = new UpdateOfferResponse();
        rs.setCode("0");
        rs.setDescription("d");
        String xml = mapper.toLogXml(rs);
        assertThat(xml).contains("<ReferenceContacto/>", "<StartDate/>", "<EndDate/>", "<ModeCmp/>");
    }
}

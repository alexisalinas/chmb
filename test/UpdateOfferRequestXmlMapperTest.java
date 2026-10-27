package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.config.OfferJmsProperties;
import com.banamex.cops.update_offer.testutil.TestData;
import com.banamex.cops.update_offer.testutil.XmlTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateOfferRequestXmlMapperTest {

    private UpdateOfferRequestXmlMapper mapper;

    @BeforeEach
    void setUp() {
        OfferJmsProperties props = new OfferJmsProperties();
        props.setCustomerPadding(true);
        UserTextParser parser = new UserTextParser(JsonMapper.builder().build());
        OfferEventMapper event = new OfferEventMapper(props, parser);
        ReflectionTestUtils.setField(event, "translateTimeZone", false);
        ReflectionTestUtils.setField(event, "ecrmTimeZone", "America/Mexico_City");
        mapper = new UpdateOfferRequestXmlMapper(new RqHeaderXmlMapper(), event, parser);
        ReflectionTestUtils.setField(mapper, "bodyElement", "UpdateOffer");
    }

    // ------------------------------------------------------------ XML hacia el EMS

    @Test
    void emsXml_tieneLaEstructuraEnvelopeHeaderBody() {
        String xml = mapper.toEmsXml(TestData.request(), TestData.rqHeader(), null, null);
        Document doc = XmlTestUtil.parse(xml);

        assertThat(doc.getDocumentElement().getNodeName()).isEqualTo("Envelope");
        assertThat(XmlTestUtil.childNames(doc, "Envelope")).containsExactly("Header", "Body");
        assertThat(XmlTestUtil.childNames(doc, "Header")).containsExactly("RqHeader");
        assertThat(XmlTestUtil.childNames(doc, "Body")).containsExactly("UpdateOffer");
    }

    @Test
    void emsXml_noTieneUrlsNiPrefijos() {
        String xml = mapper.toEmsXml(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(XmlTestUtil.hasUrls(xml)).isFalse();
        assertThat(xml).doesNotContain("soapenv:", "head:", "ns0:", "ns1:", "ns:");
    }

    @Test
    void emsXml_llevaLosCamposTransformados() {
        String xml = mapper.toEmsXml(TestData.request(), TestData.rqHeader(), null, null);

        assertThat(xml).contains("<RequestingChannel>CAT</RequestingChannel>",
                "<CardNO>1234567812345678</CardNO>",
                "<CustomerNO>0012345678</CustomerNO>",
                "<ApptStartTime>10:30</ApptStartTime>",
                "<UserField1>valor 1</UserField1>",
                "<UserField100>valor 100</UserField100>",
                "<UserField3></UserField3>",
                "<UpdatedBy>ATM</UpdatedBy>",
                "<UUID>uuid-1</UUID>");
    }

    @Test
    void emsXml_ningunElementoDelEventoSeRepite() {
        Document doc = XmlTestUtil.parse(mapper.toEmsXml(TestData.request(), TestData.rqHeader(), null, null));
        List<String> hijos = XmlTestUtil.childNames(doc, "UpdateOffer");
        assertThat(hijos).isNotEmpty().doesNotHaveDuplicates();
    }

    @Test
    void emsXml_escapaLosCaracteresEspeciales() {
        var rq = TestData.request();
        rq.setComments("a & b < c");
        String xml = mapper.toEmsXml(rq, TestData.rqHeader(), null, null);
        assertThat(xml).contains("<Comments>a &amp; b &lt; c</Comments>");
        XmlTestUtil.parse(xml);
    }

    @Test
    void emsXml_usaElNombreDeElementoConfigurable() {
        ReflectionTestUtils.setField(mapper, "bodyElement", "OtroEvento");
        String xml = mapper.toEmsXml(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(xml).contains("<OtroEvento>", "</OtroEvento>");
    }

    // ------------------------------------------------------------ XML del log

    @Test
    void logXml_tieneRootRqHeaderYUpdateOfferRq_sinUrls() {
        String xml = mapper.toLogXml(TestData.request(), TestData.rqHeader());
        Document doc = XmlTestUtil.parse(xml);

        assertThat(doc.getDocumentElement().getNodeName()).isEqualTo("root");
        assertThat(XmlTestUtil.childNames(doc, "root")).containsExactly("RqHeader", "UpdateOfferRq");
        assertThat(XmlTestUtil.hasUrls(xml)).isFalse();
    }

    @Test
    void logXml_usaLosNombresDeBWSinTransformaciones() {
        String xml = mapper.toLogXml(TestData.request(), TestData.rqHeader());

        assertThat(xml).contains("<CustomerNo>12345678</CustomerNo>",
                "<AccountNo>00112233445566</AccountNo>",
                "<CardNumber>1234567812345678</CardNumber>",
                "<IsConfirmed>Y</IsConfirmed>",
                "<AppointmentStartTime>10:30:00</AppointmentStartTime>",
                "<ContactNumber>5555555555</ContactNumber>",
                "<Sysversion>1.0</Sysversion>",
                "<Agentname>Agente Uno</Agentname>",
                "<FulfillmentRefNo>FT1</FulfillmentRefNo>",
                "<ChannelResponse>SMS</ChannelResponse>");
    }

    @Test
    void logXml_losCamposSinDatoVanAutocerrados() {
        String xml = mapper.toLogXml(TestData.minimalRequest(), TestData.rqHeader());
        assertThat(xml).contains("<AccountNo/>", "<UserField1/>", "<UserField30/>", "<Amount/>");
        assertThat(xml).doesNotContain("<UserField31");
    }

    @Test
    void logXml_controlFieldSeRepitePorCadaElementoDeContextType() {
        String xml = mapper.toLogXml(TestData.request(), TestData.rqHeader());
        assertThat(xml).contains("<ControlField>controlField1</ControlField>",
                "<ControlField>controlField2</ControlField>");
    }

    @Test
    void logXml_userFieldsMasAllaDel30SoloSalenSiVienen() {
        String xml = mapper.toLogXml(TestData.request(), TestData.rqHeader());
        assertThat(xml).contains("<UserField100>valor 100</UserField100>").doesNotContain("<UserField99");
    }
}

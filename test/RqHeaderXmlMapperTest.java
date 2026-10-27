package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.testutil.TestData;
import com.banamex.cops.update_offer.testutil.XmlTestUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RqHeaderXmlMapperTest {

    private final RqHeaderXmlMapper mapper = new RqHeaderXmlMapper();

    @Test
    void envelopeHeader_traeLosDatosYNoTieneUrls() {
        String xml = mapper.toEnvelopeHeader(TestData.rqHeader(), "");

        assertThat(xml).contains("<RqHeader>", "</RqHeader>",
                "<DateAndTimeStamp>2026-10-26T10:54:55.852</DateAndTimeStamp>",
                "<UUID>uuid-1</UUID>", "<ESBUUID>uuid-120261026105455</ESBUUID>", "<Version>3.0</Version>",
                "<Org>Banamex</Org>", "<ChannelID>CAT</ChannelID>", "<UserID>ATM</UserID>",
                "<SrcCountryCode>MX</SrcCountryCode>", "<SessionLanguageCode>ES</SessionLanguageCode>");
        assertThat(XmlTestUtil.hasUrls(xml)).isFalse();
        XmlTestUtil.parse(xml);
    }

    @Test
    void envelopeHeader_omiteLosOpcionalesNulos() {
        String xml = mapper.toEnvelopeHeader(TestData.rqHeader(), "");
        assertThat(xml).doesNotContain("ClientIPAddress", "UserGroup", "UserDomicileBranchCode");
    }

    @Test
    void envelopeHeader_sinClientDetailsNoEscribeEseNodo() {
        RqHeaderDTO h = TestData.rqHeader();
        h.setClientDetails(null);
        String xml = mapper.toEnvelopeHeader(h, "");
        assertThat(xml).doesNotContain("ClientDetails");
        XmlTestUtil.parse(xml);
    }

    @Test
    void logHeader_escribeLosOpcionalesVaciosAutocerrados() {
        String xml = mapper.toLogHeader(TestData.rqHeader(), "");

        assertThat(xml).contains("<ClientIPAddress/>", "<UserDomicileBranchCode/>",
                "<UserDomicileResponsibilityCentre/>", "<ProcessingBranchCode/>",
                "<ProcessingResponsibilityCentre/>", "<UserGroup/>");
        assertThat(XmlTestUtil.hasUrls(xml)).isFalse();
        XmlTestUtil.parse(xml);
    }

    @Test
    void logHeader_laFechaLlevaOffset() {
        String xml = mapper.toLogHeader(TestData.rqHeader(), "");
        assertThat(xml).containsPattern(
                "<DateAndTimeStamp>2026-10-26T10:54:55\\.852([+-]\\d{2}:\\d{2}|Z)</DateAndTimeStamp>");
    }

    @Test
    void logHeader_sinClientDetailsEscribeTodoVacio() {
        RqHeaderDTO h = TestData.rqHeader();
        h.setClientDetails(null);
        String xml = mapper.toLogHeader(h, "");
        assertThat(xml).contains("<Org/>", "<SessionLanguageCode/>");
        XmlTestUtil.parse(xml);
    }
}

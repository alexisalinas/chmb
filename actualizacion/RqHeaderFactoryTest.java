package com.banamex.cops.update_offer.service;

import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RqHeaderFactoryTest {

    private RqHeaderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RqHeaderFactory();
        ReflectionTestUtils.setField(factory, "version", "3.0");
        ReflectionTestUtils.setField(factory, "org", "Banamex");
        ReflectionTestUtils.setField(factory, "orgUnit", "COPS");
        ReflectionTestUtils.setField(factory, "defaultChannelId", "CAT");
        ReflectionTestUtils.setField(factory, "terminalId", "1");
        ReflectionTestUtils.setField(factory, "userId", "ATM");
        ReflectionTestUtils.setField(factory, "defaultCountryCode", "MX");
        ReflectionTestUtils.setField(factory, "defaultLanguage", "ES");
    }

    private UpdateOfferHeaders soloUuid() {
        return UpdateOfferHeaders.builder().uuid("uuid-1").build();
    }

    // ------------------------------------------------------------ uuid y esbUuid

    @Test
    void elUuidSeTomaDelHeaderTalCualYNoSeGenera() {
        RqHeaderDTO h = factory.create(TestData.headers());
        assertThat(h.getUuid()).isEqualTo("uuid-1");
    }

    @Test
    void soloElEsbUuidSeGenera_uuidMasFechaYyyyMMddHHmmss() {
        RqHeaderDTO h = factory.create(TestData.headers());

        assertThat(h.getEsbUuid()).matches("uuid-1\\d{14}");
        assertThat(h.getDateTimeStamp()).isNotNull();
        assertThat(h.getVersion()).isEqualTo("3.0");
    }

    @Test
    void elEsbUuidUsaLaMismaFechaQueElDateTimeStamp() {
        RqHeaderDTO h = factory.create(TestData.headers());
        String esperado = h.getDateTimeStamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        assertThat(h.getEsbUuid()).isEqualTo("uuid-1" + esperado);
    }

    @Test
    void sinUuid_lanzaIllegalArgumentEnLugarDeGenerarUno() {
        assertThatThrownBy(() -> factory.create(new UpdateOfferHeaders()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uuid");
    }

    @Test
    void uuidEnBlanco_tambienLanzaIllegalArgument() {
        UpdateOfferHeaders in = soloUuid();
        in.setUuid("   ");
        assertThatThrownBy(() -> factory.create(in)).isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------ ClientDetails

    @Test
    void clientDetails_tomaCanalYPaisDelHeaderYElRestoDeLaConfiguracion() {
        ClientDetailsDTO c = factory.create(TestData.headers()).getClientDetails();

        assertThat(c.getChannelID()).isEqualTo("CAT");
        assertThat(c.getSrcCountryCode()).isEqualTo("MX");
        assertThat(c.getDestCountryCode()).isEqualTo("MX");
        assertThat(c.getOrg()).isEqualTo("Banamex");
        assertThat(c.getOrgUnit()).isEqualTo("COPS");
        assertThat(c.getTerminalID()).isEqualTo("1");
        assertThat(c.getUserID()).isEqualTo("ATM");
    }

    @Test
    void clientDetails_usaValoresPorDefectoSiSoloVieneElUuid() {
        ClientDetailsDTO c = factory.create(soloUuid()).getClientDetails();
        assertThat(c.getChannelID()).isEqualTo("CAT");
        assertThat(c.getSrcCountryCode()).isEqualTo("MX");
        assertThat(c.getSessionLanguageCode()).isEqualTo("ES");
    }

    @Test
    void elPaisDelHeaderSeUsaComoOrigenYDestino() {
        UpdateOfferHeaders in = TestData.headers();
        in.setCountryCode("CO");
        ClientDetailsDTO c = factory.create(in).getClientDetails();
        assertThat(c.getSrcCountryCode()).isEqualTo("CO");
        assertThat(c.getDestCountryCode()).isEqualTo("CO");
    }

    @Test
    void acceptLanguage_seConvierteAIdiomaEnMayusculas() {
        UpdateOfferHeaders in = TestData.headers();
        in.setAcceptLanguage("en-US,en;q=0.9");
        assertThat(factory.create(in).getClientDetails().getSessionLanguageCode()).isEqualTo("EN");

        in.setAcceptLanguage("es");
        assertThat(factory.create(in).getClientDetails().getSessionLanguageCode()).isEqualTo("ES");

        in.setAcceptLanguage("pt;q=0.8");
        assertThat(factory.create(in).getClientDetails().getSessionLanguageCode()).isEqualTo("PT");
    }

    @Test
    void acceptLanguageVacioUsaElIdiomaPorDefecto() {
        UpdateOfferHeaders in = TestData.headers();
        in.setAcceptLanguage(" ");
        assertThat(factory.create(in).getClientDetails().getSessionLanguageCode()).isEqualTo("ES");
    }
}

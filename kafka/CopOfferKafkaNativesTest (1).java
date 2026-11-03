package com.banamex.cops.app.natives;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CopOfferKafkaNativesTest {

    private final String sql = CopOfferKafkaNatives.FIND_BY_WAVE_KEY_AND_OFFER_KEY;

    @Test
    void traeLas100ColumnasDeUserField() {
        assertThat(sql.split("OFFPAL\\.USERFIELD_", -1).length - 1).isEqualTo(100);
        assertThat(sql).contains("OFFPAL.USERFIELD_1,", "OFFPAL.USERFIELD_100");
    }

    @Test
    void usaLasTablasYLasUnionesDelModelo() {
        assertThat(sql).contains("D_OFFERPALETTE OFFPAL", "D_CAMPAIGN CAMP", "D_PRODUCT PRD", "D_WAVECHINFO WAVCH",
                "OFFPAL.campaignid = CAMP.cmpid", "CAMP.PRDCODE = PRD.PRDCODE");
    }

    @Test
    void calculaLaDuracionEnMinutos() {
        assertThat(sql).contains("to_char((OFFPAL.APPTENDDATE_TM - OFFPAL.APPTSTARTDATE_TM) * 1440) AS APPTENDTIMEDUR");
    }

    @Test
    void tieneDosParametrosPosicionalesEnElOrdenWaveKeyYOfferKey() {
        assertThat(sql.chars().filter(c -> c == '?').count()).isEqualTo(2);
        assertThat(sql).endsWith("WAVCH.EXTID$ = ? AND OFFPAL.EXTID$ = ?");
    }

    @Test
    void traeLasColumnasDeProductoYFechas() {
        assertThat(sql).contains("WAVCH.CRPCATEGORYID", "CAMP.PRDTYPE", "OFFPAL.REASONCODE", "OFFPAL.REASONDESC",
                "PRD.DESCRIPTION", "CAMP.FULFILLMENTPROCID", "OFFPAL.APPTSTARTDATE_TM", "OFFPAL.EXPIRYDATE_TM",
                "OFFPAL.OFFERCREATIONDATE_TM", "PRD.PRDCODE", "PRD.PRDFAMILY", "OFFPAL.AVAILABILITY");
    }
}

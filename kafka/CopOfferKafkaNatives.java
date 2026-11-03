package com.banamex.cops.app.natives;

/**
 * Queries nativos de la consulta de datos de la oferta para Kafka.
 * Parámetros posicionales (?) para poder fijar timeout y máximo de filas por sentencia:
 *   1) WAVCH.EXTID$ (clave de la ola con canal)   2) OFFPAL.EXTID$ (clave de la oferta).
 * SQL de Oracle: validar contra el motor real.
 */
public final class CopOfferKafkaNatives {

    private CopOfferKafkaNatives() {
    }

    public static final String FIND_BY_WAVE_KEY_AND_OFFER_KEY = buildSql();

    private static String buildSql() {
        StringBuilder cols = new StringBuilder(
                "WAVCH.CRPCATEGORYID, CAMP.PRDTYPE, OFFPAL.REASONCODE, OFFPAL.REASONDESC, "
              + "PRD.DESCRIPTION, CAMP.FULFILLMENTPROCID");
        for (int i = 1; i <= 100; i++) {
            cols.append(", OFFPAL.USERFIELD_").append(i);
        }
        cols.append(", OFFPAL.APPTSTARTDATE_TM")
            .append(", to_char((OFFPAL.APPTENDDATE_TM - OFFPAL.APPTSTARTDATE_TM) * 1440) AS APPTENDTIMEDUR")
            .append(", OFFPAL.EXPIRYDATE_TM, OFFPAL.OFFERCREATIONDATE_TM")
            .append(", PRD.PRDCODE, PRD.PRDFAMILY, OFFPAL.AVAILABILITY");
        return "SELECT " + cols
             + " FROM D_OFFERPALETTE OFFPAL, D_CAMPAIGN CAMP, D_PRODUCT PRD, D_WAVECHINFO WAVCH"
             + " WHERE OFFPAL.campaignid = CAMP.cmpid AND CAMP.PRDCODE = PRD.PRDCODE"
             + " AND WAVCH.EXTID$ = ? AND OFFPAL.EXTID$ = ?";
    }
}

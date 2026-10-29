package com.banamex.cops.update_offer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Paso 2: subproceso /BW/Kafka/DataFetch. Un solo SELECT con parámetros preparados
 * (timeout 10 s, máximo 100 filas, se usa la primera: Record[1]).
 * SQL de Oracle: validar contra el motor real (to_char sobre número, sufijo $ en los nombres).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OfferDataRepository {

    private static final String SQL = buildSql();

    private final ObjectProvider<CitiDataStore> citiDataStore;
    private final ObjectProvider<JdbcTemplate> defaultJdbc;

    /** @param waveKey WAVCH.EXTID$   @param offerKey OFFPAL.EXTID$ */
    public Optional<Map<String, Object>> find(String waveKey, String offerKey) {
        JdbcTemplate jdbc = jdbc();
        List<Map<String, Object>> rows = jdbc.query(con -> {
            PreparedStatement ps = con.prepareStatement(SQL);
            ps.setQueryTimeout(10);
            ps.setMaxRows(100);
            ps.setString(1, waveKey);
            ps.setString(2, offerKey);
            return ps;
        }, new ColumnMapRowMapper());

        if (rows.size() > 1) {
            log.warn("KAFKA-MULTI-ROWS la consulta devolvió {} filas; se usa la primera. waveKey={} offerKey={}",
                    rows.size(), waveKey, offerKey);
        }
        return rows.stream().findFirst();
    }

    private JdbcTemplate jdbc() {
        CitiDataStore dedicated = citiDataStore.getIfAvailable();
        if (dedicated != null) {
            return dedicated.getJdbcTemplate();
        }
        JdbcTemplate main = defaultJdbc.getIfAvailable();
        if (main == null) {
            throw new IllegalStateException(
                    "No hay conexión a la base: configura spring.datasource u offer.kafka.datasource.url");
        }
        return main;
    }

    static String buildSql() {
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

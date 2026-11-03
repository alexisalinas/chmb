package com.banamex.cops.app.repository;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.natives.CopOfferKafkaNatives;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Ejecuta el query nativo de datos de la oferta para Kafka.
 * Usa JDBC (como CopTimezoneLookupRepository); la sentencia lleva timeout de 10 s y máximo de 100 filas.
 */
@Repository
@RequiredArgsConstructor
public class CopOfferKafkaRepository {

    static final int QUERY_TIMEOUT_SECONDS = 10;
    static final int MAX_ROWS = 100;

    private static final RowMapper<CopOfferKafkaData> ROW_MAPPER = (rs, rowNum) -> {
        List<String> userFields = new ArrayList<>(100);
        for (int i = 1; i <= 100; i++) {
            userFields.add(rs.getString("USERFIELD_" + i));
        }
        return new CopOfferKafkaData(
                rs.getString("CRPCATEGORYID"),
                rs.getString("PRDTYPE"),
                rs.getString("REASONCODE"),
                rs.getString("REASONDESC"),
                rs.getString("DESCRIPTION"),
                rs.getString("FULFILLMENTPROCID"),
                userFields,
                rs.getTimestamp("APPTSTARTDATE_TM"),
                rs.getString("APPTENDTIMEDUR"),
                rs.getTimestamp("EXPIRYDATE_TM"),
                rs.getTimestamp("OFFERCREATIONDATE_TM"),
                rs.getString("PRDCODE"),
                rs.getString("PRDFAMILY"),
                rs.getString("AVAILABILITY"));
    };

    private final NamedParameterJdbcTemplate jdbc;

    public List<CopOfferKafkaData> findByWaveKeyAndOfferKey(String waveKey, String offerKey) {
        return jdbc.getJdbcOperations().query(con -> {
            PreparedStatement ps = con.prepareStatement(CopOfferKafkaNatives.FIND_BY_WAVE_KEY_AND_OFFER_KEY);
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            ps.setMaxRows(MAX_ROWS);
            ps.setString(1, waveKey);
            ps.setString(2, offerKey);
            return ps;
        }, ROW_MAPPER);
    }
}

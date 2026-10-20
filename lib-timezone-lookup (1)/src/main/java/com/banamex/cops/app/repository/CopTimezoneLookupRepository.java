package com.banamex.cops.app.repository;

import com.banamex.cops.app.model.CopTimezoneLookup;
import com.banamex.cops.app.natives.CopTimezoneLookupNatives;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Ejecuta el query nativo sobre COP_TIMEZONE_LOOKUP.
 * Usa JDBC porque la tabla no tiene llave primaria y no se puede mapear como @Entity.
 */
@Repository
@RequiredArgsConstructor
public class CopTimezoneLookupRepository {

    private static final RowMapper<CopTimezoneLookup> ROW_MAPPER = (rs, rowNum) ->
            new CopTimezoneLookup(
                    rs.getString("TZ_SYSTEM_NAME"),
                    rs.getString("TZ_MODE"),
                    rs.getString("TZ_TIMEZONE"),
                    rs.getString("TZ_OPERATION_TYPE"),
                    rs.getString("TZ_TIME_OFFSET"),
                    rs.getString("TZ_DATETIME_FORMAT"),
                    rs.getString("TZ_DATE_FORMAT"),
                    rs.getString("TZ_TIME_FORMAT"));

    private final NamedParameterJdbcTemplate jdbc;

    public List<CopTimezoneLookup> findBySystemNameAndModeAndOperationType(
            String tzSystemName, String tzMode, String tzOperationType) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tzSystemName", tzSystemName)
                .addValue("tzMode", tzMode)
                .addValue("tzOperationType", tzOperationType);

        return jdbc.query(
                CopTimezoneLookupNatives.FIND_BY_SYSTEM_NAME_AND_MODE_AND_OPERATION_TYPE,
                params,
                ROW_MAPPER);
    }
}

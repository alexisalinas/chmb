package com.banamex.cops.repository.timezonevalidation;

import com.banamex.cops.dto.timezonevalidation.TimeZoneValidationLookupRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Equivalente a la actividad "LookupTZ" (JDBC Query).
 */
@Repository
public class LookupTZ {

    // TODO: completa el nombre de la tabla (en la captura se corta en COP_TIMEZ...)
    // y confirma las columnas del WHERE (no se ve en la captura).
    private static final String SQL = """
            SELECT TZ_SYSTEM_NAME, TZ_MODE, TZ_TIMEZONE, TZ_OPERATION_TYPE,
                   TZ_TIME_OFFSET, TZ_DATETIME_FORMAT, TZ_DATE_FORMAT, TZ_TIME_FORMAT
              FROM COP_TIMEZONE_TABLE
             WHERE TZ_SYSTEM_NAME = ?
               AND TZ_MODE = ?
               AND TZ_OPERATION_TYPE = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public LookupTZ(JdbcTemplate jdbcTemplate,
                    @Value("${cop.query-records.timeout-default:30}") int timeout,
                    @Value("${cop.query-records.max-rows-default:100}") int maxRows) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcTemplate.setQueryTimeout(timeout);   // Timeout(sec)
        this.jdbcTemplate.setMaxRows(maxRows);        // Maximum Rows
    }

    public List<TimeZoneValidationLookupRecord> find(String system, String mode, String type) {
        return jdbcTemplate.query(SQL,
                (rs, i) -> TimeZoneValidationLookupRecord.builder()
                        .tzSystemName(rs.getString("TZ_SYSTEM_NAME"))
                        .tzMode(rs.getString("TZ_MODE"))
                        .tzTimezone(rs.getString("TZ_TIMEZONE"))
                        .tzOperationType(rs.getString("TZ_OPERATION_TYPE"))
                        .tzTimeOffset(rs.getString("TZ_TIME_OFFSET"))
                        .tzDatetimeFormat(rs.getString("TZ_DATETIME_FORMAT"))
                        .tzDateFormat(rs.getString("TZ_DATE_FORMAT"))
                        .tzTimeFormat(rs.getString("TZ_TIME_FORMAT"))
                        .build(),
                trim(system), trim(mode), trim(type));   // tib:trim(...)
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}

package com.banamex.cops.app.model;

/**
 * Fila de la tabla COP_TIMEZONE_LOOKUP.
 *
 * La tabla no tiene llave primaria, por eso se usa un record mapeado con JDBC
 * y no una @Entity de JPA (que exige @Id).
 */
public record CopTimezoneLookup(
        String tzSystemName,
        String tzMode,
        String tzTimezone,
        String tzOperationType,
        String tzTimeOffset,
        String tzDatetimeFormat,
        String tzDateFormat,
        String tzTimeFormat) {
}

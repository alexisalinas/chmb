package com.banamex.cops.app.natives;

/**
 * Queries nativos sobre COP_TIMEZONE_LOOKUP.
 */
public final class CopTimezoneLookupNatives {

    private CopTimezoneLookupNatives() {
    }

    public static final String FIND_BY_SYSTEM_NAME_AND_MODE_AND_OPERATION_TYPE = """
            SELECT TZ_SYSTEM_NAME,
                   TZ_MODE,
                   TZ_TIMEZONE,
                   TZ_OPERATION_TYPE,
                   TZ_TIME_OFFSET,
                   TZ_DATETIME_FORMAT,
                   TZ_DATE_FORMAT,
                   TZ_TIME_FORMAT
              FROM COP_TIMEZONE_LOOKUP
             WHERE TZ_SYSTEM_NAME    = :tzSystemName
               AND TZ_MODE           = :tzMode
               AND TZ_OPERATION_TYPE = :tzOperationType
            """;
}

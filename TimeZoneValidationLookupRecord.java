package com.banamex.cops.dto.timezonevalidation;

import lombok.Builder;
import lombok.Data;

/**
 * Output de LookupTZ (JDBC Query): un Record del resultSet.
 */
@Data
@Builder
public class TimeZoneValidationLookupRecord {

    private String tzSystemName;
    private String tzMode;
    private String tzTimezone;
    private String tzOperationType;
    private String tzTimeOffset;
    private String tzDatetimeFormat;   // opcional (ABC?)
    private String tzDateFormat;
    private String tzTimeFormat;
}

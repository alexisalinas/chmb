package com.banamex.cops.service.timezonevalidation;

import com.banamex.cops.dto.timezonevalidation.TimeZoneValidationFormats;
import com.banamex.cops.dto.timezonevalidation.TimeZoneValidationGetVariableRoot;
import com.banamex.cops.dto.timezonevalidation.TimeZoneValidationStartRoot;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Equivalente al Assign "InitTZ" de BW/Common/TimeZoneValidation.
 * Devuelve vacío cuando no hay coincidencia (rama "Otherwise" -> LookupTZ).
 */
@Component
public class InitTZ {

    public Optional<TimeZoneValidationFormats> execute(TimeZoneValidationStartRoot start,
                                                       TimeZoneValidationGetVariableRoot getVariable) {

        if (getVariable == null || getVariable.getLookUp() == null) {
            return Optional.empty();
        }

        return getVariable.getLookUp().stream()
                .filter(l -> start.getSystem().equals(l.getSystemName())
                          && start.getSysMode().equals(l.getMode())
                          && start.getType().equals(l.getType()))
                .findFirst()
                .map(l -> TimeZoneValidationFormats.builder()
                        .timeOffset(l.getTimeOffset())
                        .dateFormat(l.getDateFormat())
                        .timeFormat(l.getTimeFormat())
                        .dateTimeFormat(l.getDateTimeFormat())
                        .timeZone(l.getTimeZone())
                        .build());
    }
}

package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Equivalente al Assign "InitTZ" de BW/Common/TimeZoneValidation.
 * Devuelve vacío cuando no hay coincidencia (rama "Otherwise" -> LookupTZ).
 */
@Component
public class InitTZ {

    public Optional<TzFormatsDTO> execute(String system, String sysMode, String type, List<TzLookUpDTO> lookUp) {
        if (lookUp == null) {
            return Optional.empty();
        }
        return lookUp.stream()
                .filter(l -> Objects.equals(system, l.getSystemName())
                        && Objects.equals(sysMode, l.getMode())
                        && Objects.equals(type, l.getType()))
                .findFirst()
                .map(l -> TzFormatsDTO.builder()
                        .timeZoneOffset(l.getTimeOffset())
                        .dateFormat(l.getDateFormat())
                        .timeFormat(l.getTimeFormat())
                        .dateTimeFormat(l.getDateTimeFormat())
                        .timeZone(l.getTimeZone())
                        .build());
    }
}

package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.app.model.CopTimezoneLookup;
import com.banamex.cops.app.service.CopTimezoneLookupService;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Equivale a la actividad JDBC LookupTZ de BW: consulta COP_TIMEZONE_LOOKUP
 * a través de la librería cops-bd (CopTimezoneLookupService).
 */
@Component
@RequiredArgsConstructor
public class CopTimezoneLookupClient implements TimeZoneLookupClient {

    private final CopTimezoneLookupService lookupService;

    @Override
    public List<TzLookUpDTO> find(String system, String sysMode, String type) {
        return lookupService.findBySystemNameAndModeAndOperationType(system, sysMode, type).stream()
                .map(this::toDto)
                .toList();
    }

    private TzLookUpDTO toDto(CopTimezoneLookup r) {
        return TzLookUpDTO.builder()
                .systemName(r.tzSystemName())
                .mode(r.tzMode())
                .timeZone(r.tzTimezone())
                .type(r.tzOperationType())
                .timeOffset(r.tzTimeOffset())
                .dateFormat(r.tzDateFormat())
                .timeFormat(r.tzTimeFormat())
                .dateTimeFormat(r.tzDatetimeFormat())
                .build();
    }
}

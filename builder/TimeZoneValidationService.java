package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * /BW/Common/TimeZoneValidation:
 * GetVariable (cache) -> InitTZ -> (si no existe) LookupTZ -> SetVariable (cache) -> MapRequest.
 *
 * Códigos de respuesta (SUPUESTOS, ajustar a los reales):
 *  - COP_TZCOD_000: validación correcta
 *  - COP_TZCOD_114: no hay configuración de zona horaria (el flujo principal igual continúa)
 *  - invalid-code : alguna fecha/hora no es válida (el flujo se detiene)
 */
@Service
@RequiredArgsConstructor
public class TimeZoneValidationService {

    public static final String OK = "COP_TZCOD_000";
    public static final String NOT_CONFIGURED = "COP_TZCOD_114";
    private static final String INVALID_STATUS = "Invalid TimeZone";

    private final InitTZ initTZ;
    private final TimeZoneLookupClient lookupClient;
    private final TimeZoneMapRequest mapRequest;

    @Value("${update-offer.timezone.invalid-code:COP_TZCOD_101}")
    private String invalidCode;

    /** Equivale a la variable compartida LookUp. */
    private final List<TzLookUpDTO> cache = new CopyOnWriteArrayList<>();

    public TimeZoneValidationResult validate(String system, String sysMode, String type,
                                             List<ValueItemDTO> dates, List<ValueItemDTO> times,
                                             List<ValueItemDTO> dateTimes) {
        Optional<TzFormatsDTO> formats = initTZ.execute(system, sysMode, type, cache);

        if (formats.isEmpty()) {                                   // Otherwise -> LookupTZ
            List<TzLookUpDTO> found = lookupClient.find(system, sysMode, type);
            if (found != null && !found.isEmpty()) {               // exists -> SetTZ + SetVariable
                cache.addAll(found);
                formats = initTZ.execute(system, sysMode, type, found);
            }
        }

        if (formats.isEmpty()) {
            return result(NOT_CONFIGURED, "TimeZone not configured", new TzFormatsDTO(), new MapRequestResultDTO());
        }

        MapRequestResultDTO map = mapRequest.map(type, formats.get(), dates, times, dateTimes);
        boolean invalid = map.getDateGrp().stream().anyMatch(g -> INVALID_STATUS.equals(g.getDateStatus()))
                || map.getTimeGrp().stream().anyMatch(g -> INVALID_STATUS.equals(g.getTimeStatus()))
                || map.getDateTimeGrp().stream().anyMatch(g -> INVALID_STATUS.equals(g.getDateTimeStatus()));

        return result(invalid ? invalidCode : OK, invalid ? INVALID_STATUS : "Success", formats.get(), map);
    }

    /** Se arma con setters (sin builder) para no depender de @Builder en los DTOs. */
    private TimeZoneValidationResult result(String code, String text, TzFormatsDTO formats, MapRequestResultDTO map) {
        TimeZoneValidationResult r = new TimeZoneValidationResult();
        r.setResponseCode(code);
        r.setResponseText(text);
        r.setFormats(formats);
        r.setMapResult(map);
        return r;
    }
}

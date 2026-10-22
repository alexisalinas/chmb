package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;

import java.util.List;

/**
 * Equivale a la actividad JDBC LookupTZ del proceso BW (consulta por TZ_SYSTEM_NAME, TZ_MODE y tipo).
 * Pendiente de implementar con tu fuente real (BD, librería cops-bd, etc.).
 */
public interface TimeZoneLookupClient {
    List<TzLookUpDTO> find(String system, String sysMode, String type);
}

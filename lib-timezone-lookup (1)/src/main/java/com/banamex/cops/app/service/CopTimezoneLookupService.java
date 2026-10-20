package com.banamex.cops.app.service;

import com.banamex.cops.app.model.CopTimezoneLookup;

import java.util.List;

public interface CopTimezoneLookupService {

    /**
     * Busca en COP_TIMEZONE_LOOKUP por sistema, modo y tipo de operacion.
     * La tabla no tiene llave primaria, asi que puede devolver mas de una fila.
     */
    List<CopTimezoneLookup> findBySystemNameAndModeAndOperationType(
            String tzSystemName, String tzMode, String tzOperationType);
}

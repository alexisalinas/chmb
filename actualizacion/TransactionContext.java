package com.banamex.cops.update_offer.audit;

import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Datos de la transaccion que BusinessWorks recibia en GetContext (RqHeader).
 * El uuid viene del header y el esbUuid lo arma RqHeaderFactory (uuid + yyyyMMddHHmmss); aqui no se genera nada.
 * Se crea UNA vez por peticion y se reutiliza en el log del request y del response.
 */
@Value
public class TransactionContext {

    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    String uuid;
    String esbUuid;
    String timestamp;

    /** Reutiliza el uuid / esbUuid / fecha del RqHeader que ya armó RqHeaderFactory. */
    public static TransactionContext from(RqHeaderDTO h) {
        LocalDateTime ts = h.getDateTimeStamp() != null ? h.getDateTimeStamp() : LocalDateTime.now();
        return new TransactionContext(h.getUuid(), h.getEsbUuid(), ts.format(LOG_FORMAT));
    }

    /** Mismo formato que el log original, con un espacio al inicio y al final. */
    public String transactionId() {
        return " ESBUUID: " + esbUuid + " UUID: " + uuid + " ";
    }
}

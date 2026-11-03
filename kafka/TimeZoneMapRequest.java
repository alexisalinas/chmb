package com.banamex.cops.update_offer.service.timeZoneValidation;

import com.banamex.cops.update_offer.dto.timeZoneValidation.DateGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateTimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Migración de MapRequest (TimeZoneValidation).
 *
 * Request  (cliente -> backend): valida con los formatos del cliente (DateFormat / TimeFormat / DateTimeFormat),
 *          convierte hora y fecha-hora de la zona del cliente a la zona ECRM y las escribe con los formatos del
 *          backend (BETimeFormat / BEDateTimeFormat). La fecha solo se reformatea a ISO con el offset de ECRM.
 * Response (backend -> cliente): hace el camino inverso con los formatos del cliente.
 *
 * Zona del cliente = TimeZoneOffset de la BD; si viene vacío, se usa TimeZone (nombre de zona).
 */
@Service
@RequiredArgsConstructor
public class TimeZoneMapRequest {

    private static final String INVALID = "Invalid TimeZone";

    /** BW: $_globalVariables/COP/TimeZone/ECRMTimeZone (zona tipo America/Mexico_City u offset -06:00). */
    @Value("${update-offer.timezone.ecrm-time-zone}")
    private String ecrmTimeZone;

    /** BW: $_globalVariables/COP/TimeZone/TimeZoneDateTimeFormat (patrón de origen para getConvertedDateTime). */
    @Value("${update-offer.timezone.time-zone-date-time-format}")
    private String timeZoneDateTimeFormat;

    /** BW: $_globalVariables/COP/DateTime/BETimeFormat (formato de hora del backend). */
    @Value("${update-offer.timezone.be-time-format:HH:mm:ss}")
    private String beTimeFormat;

    /** BW: $_globalVariables/COP/DateTime/BEDateTimeFormat (formato de fecha-hora del backend). */
    @Value("${update-offer.timezone.be-date-time-format:yyyy-MM-dd'T'HH:mm:ssXXX}")
    private String beDateTimeFormat;

    public MapRequestResultDTO map(String type, TzFormatsDTO f,
                                   List<ValueItemDTO> dates,
                                   List<ValueItemDTO> times,
                                   List<ValueItemDTO> dateTimes) {
        MapRequestResultDTO out = new MapRequestResultDTO();
        boolean request = "request".equalsIgnoreCase(type);
        boolean response = "response".equalsIgnoreCase(type);
        if (!request && !response) {
            return out;
        }

        for (ValueItemDTO d : nz(dates)) {
            DateGrpDTO g = mapDate(request, f, d);
            if (g != null) out.getDateGrp().add(g);
        }

        for (ValueItemDTO t : nz(times)) {
            String v = t.getValue();
            if (request) {
                if (invalid(f.getTimeZoneOffset(), f.getTimeFormat(), v)) {
                    out.getTimeGrp().add(new TimeGrpDTO(v, INVALID, null));
                } else if (hasText(v)) {
                    try {
                        out.getTimeGrp().add(new TimeGrpDTO(convertTimeRequest(f, v), "", t.getName()));
                    } catch (DateTimeException | IllegalArgumentException e) {
                        out.getTimeGrp().add(new TimeGrpDTO(v, INVALID, null));
                    }
                }
            } else if (hasText(v)) {
                out.getTimeGrp().add(new TimeGrpDTO(formatTimeResponse(f, v), "", t.getName()));
            }
        }

        for (ValueItemDTO dt : nz(dateTimes)) {
            String v = dt.getValue();
            if (request) {
                if (invalid(f.getTimeZoneOffset(), f.getDateTimeFormat(), v)) {
                    out.getDateTimeGrp().add(new DateTimeGrpDTO(v, INVALID, null));
                } else if (hasText(v)) {
                    try {
                        out.getDateTimeGrp().add(new DateTimeGrpDTO(convertDateTimeRequest(f, v), "", dt.getName()));
                    } catch (DateTimeException | IllegalArgumentException e) {
                        out.getDateTimeGrp().add(new DateTimeGrpDTO(v, INVALID, null));
                    }
                }
            } else if (hasText(v)) {
                out.getDateTimeGrp().add(new DateTimeGrpDTO(formatDateTimeResponse(f, v), "", dt.getName()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ fecha

    private DateGrpDTO mapDate(boolean request, TzFormatsDTO f, ValueItemDTO d) {
        String v = d.getValue();
        if (request) {
            if (invalid(f.getTimeZoneOffset(), f.getDateFormat(), v)) {
                return new DateGrpDTO(v, INVALID, null);
            }
            if (hasText(v)) {
                // concat(parse-date(DateFormat, DateVal), "T00:00:00", getTZOffset(ECRMTimeZone))
                return new DateGrpDTO(parseDate(f.getDateFormat(), v) + "T00:00:00" + tzOffset(), "", d.getName());
            }
            return null;
        }
        // Response: tib:format-date(DateFormat, DateVal)
        return hasText(v) ? new DateGrpDTO(formatDate(f.getDateFormat(), v), "", d.getName()) : null;
    }

    // ------------------------------------------------------------------ Request: hora y fecha-hora

    /** Hora del cliente (TimeFormat) -> zona ECRM -> BETimeFormat. Usa la fecha de hoy para respetar el horario de verano. */
    private String convertTimeRequest(TzFormatsDTO f, String v) {
        ZoneId client = clientZone(f);
        LocalTime t = LocalTime.parse(v.trim(), DateTimeFormatter.ofPattern(f.getTimeFormat()));
        ZonedDateTime src = ZonedDateTime.of(LocalDate.now(client), t, client);
        return src.withZoneSameInstant(ecrmZone()).toLocalTime().format(DateTimeFormatter.ofPattern(beTimeFormat));
    }

    /** Fecha-hora del cliente (DateTimeFormat) -> zona ECRM -> BEDateTimeFormat. Si el valor no trae offset, se asume el del cliente. */
    private String convertDateTimeRequest(TzFormatsDTO f, String v) {
        ZonedDateTime src = parseZoned(v.trim(), f.getDateTimeFormat(), clientZone(f));
        return src.withZoneSameInstant(ecrmZone()).format(DateTimeFormatter.ofPattern(beDateTimeFormat));
    }

    private ZonedDateTime parseZoned(String v, String pattern, ZoneId assumed) {
        DateTimeFormatter fm = DateTimeFormatter.ofPattern(pattern);
        try {
            TemporalAccessor ta = fm.parseBest(v, ZonedDateTime::from, OffsetDateTime::from, LocalDateTime::from);
            if (ta instanceof ZonedDateTime z) return z;
            if (ta instanceof OffsetDateTime o) return o.toZonedDateTime();
            return ((LocalDateTime) ta).atZone(assumed);
        } catch (DateTimeParseException e) {
            if (v.length() >= 3 && v.charAt(v.length() - 3) == ':') {      // offset con ":" que el patrón no cubre
                return OffsetDateTime.parse(v).toZonedDateTime();
            }
            throw e;
        }
    }

    // ------------------------------------------------------------------ Response

    /** Time Response: hora ECRM (hoy) -> zona del cliente -> TimeFormat. */
    private String formatTimeResponse(TzFormatsDTO f, String timeVal) {
        ZoneId ecrm = ecrmZone();
        ZonedDateTime src = ZonedDateTime.of(LocalDate.now(ecrm), LocalTime.parse(timeVal.trim()), ecrm);
        return src.withZoneSameInstant(clientZone(f)).toLocalTime().format(DateTimeFormatter.ofPattern(f.getTimeFormat()));
    }

    /** DateTime Response: dtvar + inserción de ":" en el offset si TimeZoneOffset lo contiene. */
    private String formatDateTimeResponse(TzFormatsDTO f, String v) {
        String offset = f.getTimeZoneOffset();
        String dtvar;
        if (v.length() >= 3 && v.charAt(v.length() - 3) == ':') {   // tib:left(tib:right(v,3),1)=':'
            dtvar = OffsetDateTime.parse(v.trim())
                    .atZoneSameInstant(clientZone(f))
                    .format(DateTimeFormatter.ofPattern(f.getDateTimeFormat()));
        } else {
            dtvar = convertedDateTime(v, f);                          // com.citi.cop:getConvertedDateTime
        }
        if (offset != null && offset.contains(":")) {
            int n = dtvar.length();
            return dtvar.substring(0, n - 2) + ":" + dtvar.substring(n - 2);
        }
        return dtvar;
    }

    /** Supuesto: interpreta v en la zona ECRM con el patrón global y lo muestra en f.TimeZone con DateTimeFormat. */
    private String convertedDateTime(String v, TzFormatsDTO f) {
        try {
            SimpleDateFormat in = new SimpleDateFormat(timeZoneDateTimeFormat);
            in.setTimeZone(TimeZone.getTimeZone(ecrmTimeZone));
            Date d = in.parse(v);
            SimpleDateFormat out = new SimpleDateFormat(f.getDateTimeFormat());
            out.setTimeZone(TimeZone.getTimeZone(f.getTimeZone()));
            return out.format(d);
        } catch (Exception e) {
            throw new IllegalArgumentException("DateTime inválido: " + v, e);
        }
    }

    // ------------------------------------------------------------------ validación y utilidades

    /** Equivale al if/else anidado del XPath: devuelve true si el resultado es "Invalid Timezone". */
    private boolean invalid(String offset, String format, String value) {
        if (isEmpty(offset)) return true;                 // string-length(TimeZoneOffset)=0
        if (!hasText(value)) return false;                // valor vacío -> Success
        return !validate(format, value);                  // tib:validate-dateTime
    }

    private boolean validate(String format, String value) {
        try {
            SimpleDateFormat s = new SimpleDateFormat(format);
            s.setLenient(false);
            s.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String parseDate(String format, String value) {
        try {
            SimpleDateFormat s = new SimpleDateFormat(format);
            s.setLenient(false);
            return new SimpleDateFormat("yyyy-MM-dd").format(s.parse(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Fecha inválida: " + value, e);
        }
    }

    private String formatDate(String format, String isoValue) {
        LocalDateTime ldt;
        try {
            ldt = OffsetDateTime.parse(isoValue).toLocalDateTime();
        } catch (Exception e) {
            ldt = LocalDateTime.parse(isoValue);
        }
        return ldt.format(DateTimeFormatter.ofPattern(format));
    }

    private ZoneId ecrmZone() {
        return ZoneId.of(ecrmTimeZone);
    }

    private ZoneOffset ecrmOffset() {
        return ecrmZone().getRules().getOffset(Instant.now());
    }

    /** getTZOffset(ECRMTimeZone): formato -0600 (sin ":"), por eso en BW lo arman con substring. */
    private String tzOffset() {
        ZoneOffset o = ecrmOffset();
        return o.getTotalSeconds() == 0 ? "+0000" : o.getId().replace(":", "");
    }

    /** Zona del cliente: TimeZoneOffset si viene; si no, el nombre de zona (TimeZone). */
    private ZoneId clientZone(TzFormatsDTO f) {
        if (hasText(f.getTimeZoneOffset())) {
            return ZoneOffset.of(f.getTimeZoneOffset().trim());
        }
        if (hasText(f.getTimeZone())) {
            return ZoneId.of(f.getTimeZone().trim());
        }
        throw new IllegalArgumentException("No hay TimeZoneOffset ni TimeZone del cliente en la configuración");
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
    private static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }
}

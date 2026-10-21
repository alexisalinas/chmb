package com.banamex.cops.service.timezonevalidation;

import com.banamex.cops.dto.timezonevalidation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/** Migración de MapRequest (TimeZoneValidation). */
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

    public MapRequestResultDTO map(String type, TzFormatsDTO f,
                                   List<ValueItemDTO> dates,
                                   List<ValueItemDTO> times,
                                   List<ValueItemDTO> dateTimes) {
        MapRequestResultDTO out = new MapRequestResultDTO();
        boolean request = "request".equalsIgnoreCase(type);
        boolean response = "response".equalsIgnoreCase(type);

        if (request || response) {
            for (ValueItemDTO d : nz(dates)) {
                DateGrpDTO g = mapDate(request, f, d);
                if (g != null) out.getDateGrp().add(g);
            }
            for (ValueItemDTO t : nz(times)) {
                String v = t.getValue();
                if (request && invalid(f.getTimeZoneOffset(), f.getTimeFormat(), v)) {
                    out.getTimeGrp().add(new TimeGrpDTO(v, INVALID, null));
                } else if (hasText(v)) {
                    String tv = request ? v : formatTimeResponse(f, v); // Request: supuesto, sin conversión
                    out.getTimeGrp().add(new TimeGrpDTO(tv, "", t.getName()));
                }
            }
            for (ValueItemDTO dt : nz(dateTimes)) {
                String v = dt.getValue();
                if (request && invalid(f.getTimeZoneOffset(), f.getDateTimeFormat(), v)) {
                    out.getDateTimeGrp().add(new DateTimeGrpDTO(v, INVALID, null));
                } else if (hasText(v)) {
                    String dv = request ? v : formatDateTimeResponse(f, v); // Request: supuesto, sin conversión
                    out.getDateTimeGrp().add(new DateTimeGrpDTO(dv, "", dt.getName()));
                }
            }
        }
        return out;
    }

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

    private ZoneOffset ecrmOffset() {
        return ZoneId.of(ecrmTimeZone).getRules().getOffset(Instant.now());
    }

    /** getTZOffset(ECRMTimeZone): formato -0600 (sin ":"), por eso en BW lo arman con substring. */
    private String tzOffset() {
        ZoneOffset o = ecrmOffset();
        return o.getTotalSeconds() == 0 ? "+0000" : o.getId().replace(":", "");
    }

    /** Time Response: translate-timezone(fecha de hoy + TimeVal + offset ECRM, TimeZoneOffset) -> format-time(TimeFormat). */
    private String formatTimeResponse(TzFormatsDTO f, String timeVal) {
        OffsetDateTime src = OffsetDateTime.of(LocalDate.now(), LocalTime.parse(timeVal.trim()), ecrmOffset());
        OffsetDateTime tgt = src.withOffsetSameInstant(ZoneOffset.of(f.getTimeZoneOffset()));
        return tgt.toLocalTime().format(DateTimeFormatter.ofPattern(f.getTimeFormat()));
    }

    /** DateTime Response: dtvar + inserción de ":" en el offset si TimeZoneOffset lo contiene. */
    private String formatDateTimeResponse(TzFormatsDTO f, String v) {
        String offset = f.getTimeZoneOffset();
        String dtvar;
        if (v.length() >= 3 && v.charAt(v.length() - 3) == ':') {   // tib:left(tib:right(v,3),1)=':'
            dtvar = OffsetDateTime.parse(v.trim())
                    .withOffsetSameInstant(ZoneOffset.of(offset))
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

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }
    private static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    private static <T> List<T> nz(List<T> l) { return l == null ? List.of() : l; }
}

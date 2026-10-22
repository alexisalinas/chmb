package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.config.OfferJmsProperties;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeGrpDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pestaña "Input" de la actividad JMS Queue Requestor de OpUpdateOffer (rama XSD Format):
 * UpdateOfferRequest (REST) -> OtherProperties (campos del evento UpdateOffer).
 * El orden de inserción es el mismo que en el mapper de BW.
 *
 * Cada campo "obligatorio" de BW (ABC sin ?) se escribe siempre, vacío si no hay dato;
 * los opcionales (ABC?) solo si traen valor.
 */
@Component
@RequiredArgsConstructor
public class OfferEventMapper {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter TS_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final OfferJmsProperties props;
    private final UserTextParser userTextParser;

    /** BW: lower-case($_globalVariables/COP/TimeZone/TranslateTimeZone) = "true" */
    @Value("${update-offer.timezone.translate-time-zone:false}")
    private boolean translateTimeZone;

    /** BW: $_globalVariables/COP/TimeZone/ECRMTimeZone */
    @Value("${update-offer.timezone.ecrm-time-zone:America/Mexico_City}")
    private String ecrmTimeZone;

    public Map<String, String> map(UpdateOfferRequest rq, RqHeaderDTO header,
                                   String tzResponseCode, MapRequestResultDTO tz) {
        // (substring(ResponseCode, string-length(ResponseCode), 1) = '0') and TranslateTimeZone = "true"
        boolean tzActive = translateTimeZone && tz != null && tzOk(tzResponseCode);
        Map<Integer, String> uf = userTextParser.parse(rq.getUserText());
        Map<String, String> o = new LinkedHashMap<>();

        req(o, "RequestingChannel", rq.getChannelCode());
        req(o, "CampaignID", rq.getCampaignId());
        req(o, "WaveID", rq.getWaveId());
        req(o, "AccountNO", rq.getAccountNumber());          // if(AccountNo != "") then AccountNo else ""
        req(o, "CardNO", rq.getCardNumber());                // if(CardNumber != "") then CardNumber else ""
        req(o, "AssignedBy", rq.getAssignedBy());
        opt(o, "AgentID", rq.getAssignedTo());               // BW: AssignedTo

        // ApptDate [If AppointmentDate]: con tz -> Date[DateName="AppointmentDate"]/DateVal, si no -> AppointmentDate
        if (hasText(rq.getAppointmentDate())) {
            String tzDate = tzActive ? tzDate(tz, "AppointmentDate") : null;
            o.put("ApptDate", tzDate != null ? tzDate : rq.getAppointmentDate());
        }
        // ApptStartTime [Choose]: con tz -> Time[1]/TimeVal, si no -> quita los segundos si trae 3 partes
        String tzStart = tzActive ? tzTime(tz, "AppointmentStartTime") : null;
        req(o, "ApptStartTime", tzStart != null ? tzStart : withoutSeconds(rq.getAppointmentStartTime()));
        // ApptEndTime [Choose]: no se ve en las capturas; se asume igual que ApptStartTime
        String tzEnd = tzActive ? tzTime(tz, "AppointmentEndTime") : null;
        req(o, "ApptEndTime", tzEnd != null ? tzEnd : withoutSeconds(rq.getAppointmentEndTime()));

        opt(o, "ApptConfirmation", rq.getOfferedNumber());   // BW: IsConfirmed
        req(o, "ReasonCode", rq.getReasonCode());
        req(o, "ReasonDescription", rq.getReasonDescription());
        req(o, "Comments", rq.getComments());
        req(o, "ContactNumber", rq.getContactIndicatorNumber());
        req(o, "OfferStatus", rq.getOfferStatus());
        req(o, "SysVersion", rq.getSchemaVersion());         // BW: Sysversion
        // _nm_ y _ns_ no son campos: son el nombre y el namespace del evento (ver UpdateOfferRequestXmlMapper)

        req(o, "CustomerNO", customerNo(rq.getCustomerId()));
        req(o, "OfferSource", rq.getOfferSource());
        for (int i = 1; i <= 20; i++) req(o, "UserField" + i, uf.get(i));
        req(o, "OfferAction", rq.getOfferActionCode());
        req(o, "GiftCode", rq.getGiftCode());
        req(o, "GiftConfirmationNO", rq.getGiftConfirmationNumber());
        opt(o, "FulfillmentReferencedNO", rq.getFulfillmentTrackerFlag());   // BW: FulfillmentRefNo
        opt(o, "ProbabilityAmount", rq.getAmount());
        opt(o, "Department", rq.getDepartmentId());
        opt(o, "Branch", rq.getBranchId());
        req(o, "ContactReference", rq.getCustomerReferenceNumber());
        opt(o, "Priority", rq.getOfferPriority());
        opt(o, "Workgroup", rq.getWorkgroupId());
        req(o, "LVCode", rq.getLevelCode());
        // UpdatedBy = GetContext/RqHeader/ClientDetails/UserID
        req(o, "UpdatedBy", header != null && header.getClientDetails() != null
                ? header.getClientDetails().getUserID() : null);
        // UpdateTime: con tz -> translate-timezone(current-dateTime(), ECRMTimeZone), si no -> RqHeader/DateAndTimeStamp
        req(o, "UpdateTime", tzActive
                ? OffsetDateTime.now(ZoneId.of(ecrmTimeZone)).format(TS_OFFSET)
                : (header != null && header.getDateTimeStamp() != null ? header.getDateTimeStamp().format(TS) : null));
        opt(o, "ControlField", contextTypeAt(rq, 0));        // ControlField[1]
        for (int i = 21; i <= 100; i++) {
            if (i == 29) req(o, "UserField29", uf.get(i)); else opt(o, "UserField" + i, uf.get(i));
        }

        opt(o, "AreaID", rq.getAreaIdentifier());
        opt(o, "DivisionID", rq.getDivisionCode());
        opt(o, "RegionID", rq.getBranchRegionIdentifier());  // BW: RegionID
        opt(o, "AgentName", rq.getAgentName());
        req(o, "ContactType", rq.getContactType());
        req(o, "ContactInfo", rq.getContactIndicatorNumber()); // BW: ContactInfo <- ContactNumber
        opt(o, "OfferCategory", rq.getOfferCategory());
        opt(o, "BranchName", rq.getBranchName());
        req(o, "DepartmentName", rq.getDepartmentName());
        opt(o, "ChannelResponse", rq.getResponseChannel());
        opt(o, "Purpose", rq.getPurpose());                  // en BW Purpose quedaba vacío; el contrato nuevo sí lo trae
        opt(o, "LocationIndicator", contextTypeAt(rq, 1));   // ControlField[2]
        opt(o, "CustomerEmail", rq.getCustomerEmailAddress());
        opt(o, "FulfillmentScript", rq.getFulfillmentCode());
        // Sin origen en el request (vacíos en BW): CategoryID, Operation, SendESMSFlag, FileName, LastRec, BatchStartTime, RecCount
        return o;
    }

    // ------------------------------------------------------------------ helpers

    private String tzDate(MapRequestResultDTO tz, String name) {
        return tz.getDateGrp().stream()
                .filter(g -> name.equalsIgnoreCase(g.getDateName()))
                .map(DateGrpDTO::getDate).findFirst().orElse(null);
    }

    private String tzTime(MapRequestResultDTO tz, String name) {
        return tz.getTimeGrp().stream()
                .filter(g -> name.equalsIgnoreCase(g.getTimeName()))
                .map(TimeGrpDTO::getTime).findFirst().orElse(null);
    }

    /** if(count(tib:tokenize(t, ':')) = 3) then tib:substring-before-last(t, ':') else t */
    private static String withoutSeconds(String t) {
        if (t == null || t.isEmpty()) return "";
        return t.split(":", -1).length == 3 ? t.substring(0, t.lastIndexOf(':')) : t;
    }

    /** substring(ResponseCode, string-length(ResponseCode), 1) = '0' */
    private static boolean tzOk(String responseCode) {
        return responseCode != null && !responseCode.isEmpty()
                && responseCode.charAt(responseCode.length() - 1) == '0';
    }

    /**
     * CustomerNO [Choose]:
     *  CustomerNo != "" y CustomerPadding="true"  -> tib:pad-front(CustomerNo, CustomerNo(global), PadCharacter)
     *  CustomerNo != "" y CustomerPadding="false" -> CustomerNo
     *  otherwise                                  -> ""
     */
    private String customerNo(String v) {
        if (v == null || v.isEmpty()) return "";
        return props.isCustomerPadding()
                ? padFront(v, props.getCustomerPaddingLength(), props.getCustomerPaddingChar()) : v;
    }

    private static String padFront(String v, int len, char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = v.length(); i < len; i++) sb.append(c);
        return sb.append(v).toString();
    }

    private String contextTypeAt(UpdateOfferRequest rq, int idx) {
        return rq.getContextType() != null && idx < rq.getContextType().size()
                && rq.getContextType().get(idx) != null
                ? rq.getContextType().get(idx).getContextType() : null;
    }

    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    /** Campo obligatorio: siempre se escribe (vacío si no hay dato). */
    private static void req(Map<String, String> m, String k, String v) { m.put(k, v == null ? "" : v); }

    /** Campo opcional: solo si trae valor. */
    private static void opt(Map<String, String> m, String k, String v) { if (v != null) m.put(k, v); }
}

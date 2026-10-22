package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.ControlFieldDTO;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.banamex.cops.update_offer.mapper.XmlUtil.esc;
import static com.banamex.cops.update_offer.mapper.XmlUtil.nsTag;
import static com.banamex.cops.update_offer.mapper.XmlUtil.tag;

/**
 * UpdateOfferRequest (REST) -> XML. Dos formatos:
 *  1) toEmsXml: lo que se manda al EMS (SOAP Envelope con RqHeader + evento UpdateOffer con los OtherProperties).
 *  2) toLogXml: el XML que BW escribía en "Log service request" (root + RqHeader + UpdateOfferRq con nombres ns1).
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferRequestXmlMapper {

    private static final String BASETYPES_NS = "http://www.citi.com/gcgi/entities/common/MX/basetypes/v3_0_MX_1";
    private static final String OFFER_NS = "http://www.citi.com/gcgi/services/common/MX/offermanagement/v3_0_MX_1";

    private final RqHeaderXmlMapper rqHeaderXmlMapper;
    private final OfferEventMapper offerEventMapper;
    private final UserTextParser userTextParser;

    /** BW: _nm_ y _ns_ del evento. */
    @Value("${update-offer.ems.body-element:UpdateOffer}")
    private String bodyElement;

    @Value("${update-offer.ems.body-namespace:www.tibco.com/be/ontology/OfferEngine/Events/UpdateOffer}")
    private String bodyNamespace;

    // =====================================================================
    //  1) XML hacia el EMS
    // =====================================================================
    public String toEmsXml(UpdateOfferRequest rq, RqHeaderDTO header,
                           String tzResponseCode, MapRequestResultDTO tz) {
        Map<String, String> fields = offerEventMapper.map(rq, header, tzResponseCode, tz);

        StringBuilder x = new StringBuilder();
        x.append("<soapenv:Envelope xmlns:soapenv=\"").append(RqHeaderXmlMapper.SOAP_NS)
                .append("\" xmlns:head=\"").append(RqHeaderXmlMapper.HEAD_NS).append("\">\n");
        x.append("   <soapenv:Header>\n");
        x.append(rqHeaderXmlMapper.toEnvelopeHeader(header, "      "));
        x.append("   </soapenv:Header>\n");
        x.append("   <soapenv:Body>\n");
        x.append("      <ns:").append(bodyElement).append(" xmlns:ns=\"").append(esc(bodyNamespace)).append("\">\n");
        fields.forEach((k, v) -> tag(x, "         ", k, v));
        x.append("      </ns:").append(bodyElement).append(">\n");
        x.append("   </soapenv:Body>\n");
        x.append("</soapenv:Envelope>");
        return x.toString();
    }

    // =====================================================================
    //  2) XML del log (Log service request)
    // =====================================================================
    public String toLogXml(UpdateOfferRequest rq, RqHeaderDTO header) {
        Map<Integer, String> uf = userTextParser.parse(rq.getUserText());
        StringBuilder x = new StringBuilder();
        x.append("<root>\n");
        x.append(rqHeaderXmlMapper.toLogHeader(header, "   "));
        x.append("   <ns0:UpdateOfferRq xmlns:ns0=\"").append(OFFER_NS)
                .append("\" xmlns:SOAP-ENV=\"").append(RqHeaderXmlMapper.SOAP_NS).append("\">\n");

        f(x, "RequestingChannel", rq.getChannelCode());
        f(x, "CampaignID", rq.getCampaignId());
        f(x, "WaveID", rq.getWaveId());
        f(x, "CustomerNo", rq.getCustomerId());
        f(x, "AccountNo", rq.getAccountNumber());
        f(x, "CardNumber", rq.getCardNumber());
        f(x, "OfferSource", rq.getOfferSource());
        f(x, "OfferPriority", rq.getOfferPriority());
        f(x, "AssignedBy", rq.getAssignedBy());
        f(x, "AssignedTo", rq.getAssignedTo());
        f(x, "DepartmentID", rq.getDepartmentId());
        f(x, "BranchID", rq.getBranchId());
        f(x, "AppointmentDate", rq.getAppointmentDate());
        f(x, "AppointmentStartTime", rq.getAppointmentStartTime());
        f(x, "AppointmentEndTime", rq.getAppointmentEndTime());
        f(x, "IsConfirmed", rq.getOfferedNumber());
        f(x, "OfferStatus", rq.getOfferStatus());
        f(x, "ReasonCode", rq.getReasonCode());
        f(x, "ReasonDescription", rq.getReasonDescription());
        f(x, "Amount", rq.getAmount());
        f(x, "OfferAction", rq.getOfferActionCode());
        f(x, "Comments", rq.getComments());
        for (int i = 1; i <= 20; i++) f(x, "UserField" + i, uf.get(i));
        f(x, "ContactNumber", rq.getContactIndicatorNumber());
        f(x, "ContactReference", rq.getCustomerReferenceNumber());
        f(x, "GiftCode", rq.getGiftCode());
        f(x, "GiftConfirmationNo", rq.getGiftConfirmationNumber());
        f(x, "FulfillmentRefNo", rq.getFulfillmentTrackerFlag());
        f(x, "LVCode", rq.getLevelCode());
        f(x, "Sysversion", rq.getSchemaVersion());
        f(x, "Workgroup", rq.getWorkgroupId());
        f(x, "Agentname", rq.getAgentName());
        for (int i = 21; i <= 30; i++) f(x, "UserField" + i, uf.get(i));
        f(x, "DivisionID", rq.getDivisionCode());
        f(x, "RegionID", rq.getBranchRegionIdentifier());
        f(x, "AreaID", rq.getAreaIdentifier());
        f(x, "ContactType", rq.getContactType());
        f(x, "OfferCategory", rq.getOfferCategory());
        f(x, "BranchName", rq.getBranchName());
        f(x, "Purpose", rq.getPurpose());
        f(x, "DepartmentName", rq.getDepartmentName());
        f(x, "ChannelResponse", rq.getResponseChannel());
        f(x, "CustomerEmail", rq.getCustomerEmailAddress());
        f(x, "FulfillmentScript", rq.getFulfillmentCode());
        for (int i = 31; i <= 100; i++) {                     // fuera del ejemplo: solo si vienen
            if (uf.containsKey(i)) f(x, "UserField" + i, uf.get(i));
        }
        if (rq.getContextType() != null) {
            for (ControlFieldDTO c : rq.getContextType()) f(x, "ControlField", c.getContextType());
        }

        x.append("   </ns0:UpdateOfferRq>\n");
        x.append("</root>");
        return x.toString();
    }

    private void f(StringBuilder x, String name, String value) {
        nsTag(x, "      ", "ns1", name, BASETYPES_NS, value);
    }
}

package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.banamex.cops.update_offer.mapper.XmlUtil.tag;
import static com.banamex.cops.update_offer.mapper.XmlUtil.tagOrEmpty;

/**
 * RqHeaderDTO -> XML del RqHeader (namespace http://www.citi.com/gcgi/shared/system/header).
 * Equivale al nodo RqHeader que BW recibía en GetContext y copiaba al mensaje / al log.
 */
@Component
public class RqHeaderXmlMapper {

    static final String HEAD_NS = "http://www.citi.com/gcgi/shared/system/header";
    static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter TS_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /** RqHeader sin prefijos ni URLs (namespaces). Omite lo que viene null. */
    public String toEnvelopeHeader(RqHeaderDTO h, String indent) {
        StringBuilder x = new StringBuilder();
        String i1 = indent + "   ";
        String i2 = i1 + "   ";
        x.append(indent).append("<RqHeader>\n");
        tag(x, i1, "DateAndTimeStamp", h.getDateTimeStamp() == null ? null : h.getDateTimeStamp().format(TS));
        tag(x, i1, "UUID", h.getUuid());
        tag(x, i1, "ESBUUID", h.getEsbUuid());
        tag(x, i1, "Version", h.getVersion());
        ClientDetailsDTO c = h.getClientDetails();
        if (c != null) {
            x.append(i1).append("<ClientDetails>\n");
            tag(x, i2, "Org", c.getOrg());
            tag(x, i2, "OrgUnit", c.getOrgUnit());
            tag(x, i2, "ChannelID", c.getChannelID());
            tag(x, i2, "TerminalID", c.getTerminalID());
            tag(x, i2, "ClientIPAddress", c.getClientIPAddress());
            tag(x, i2, "UserID", c.getUserID());
            tag(x, i2, "SrcCountryCode", c.getSrcCountryCode());
            tag(x, i2, "DestCountryCode", c.getDestCountryCode());
            tag(x, i2, "UserDomicileBranchCode", c.getUserDomicileBranchCode());
            tag(x, i2, "UserDomicileResponsibilityCentre", c.getUserDomicileResponsibilityCentre());
            tag(x, i2, "ProcessingBranchCode", c.getProcessingBranchCode());
            tag(x, i2, "ProcessingResponsibilityCentre", c.getProcessingResponsibilityCentre());
            tag(x, i2, "UserGroup", c.getUserGroup());
            tag(x, i2, "SessionLanguageCode", c.getSessionLanguageCode());
            x.append(i1).append("</ClientDetails>\n");
        }
        x.append(indent).append("</RqHeader>\n");
        return x.toString();
    }

    /** Formato del log de BW (ns0:, con offset en la fecha y los opcionales vacíos como &lt;ns0:X/&gt;). */
    public String toLogHeader(RqHeaderDTO h, String indent) {
        StringBuilder x = new StringBuilder();
        String i1 = indent + "   ";
        String i2 = i1 + "   ";
        x.append(indent).append("<ns0:RqHeader xmlns:ns0=\"").append(HEAD_NS)
                .append("\" xmlns:SOAP-ENV=\"").append(SOAP_NS).append("\">\n");
        String ts = h.getDateTimeStamp() == null ? null
                : h.getDateTimeStamp().atZone(ZoneId.systemDefault()).format(TS_OFFSET);
        tag(x, i1, "ns0:DateAndTimeStamp", ts);
        tag(x, i1, "ns0:UUID", h.getUuid());
        tag(x, i1, "ns0:ESBUUID", h.getEsbUuid());
        tag(x, i1, "ns0:Version", h.getVersion());
        ClientDetailsDTO c = h.getClientDetails() != null ? h.getClientDetails() : new ClientDetailsDTO();
        x.append(i1).append("<ns0:ClientDetails>\n");
        tagOrEmpty(x, i2, "ns0:Org", c.getOrg());
        tagOrEmpty(x, i2, "ns0:OrgUnit", c.getOrgUnit());
        tagOrEmpty(x, i2, "ns0:ChannelID", c.getChannelID());
        tagOrEmpty(x, i2, "ns0:TerminalID", c.getTerminalID());
        tagOrEmpty(x, i2, "ns0:ClientIPAddress", c.getClientIPAddress());
        tagOrEmpty(x, i2, "ns0:UserID", c.getUserID());
        tagOrEmpty(x, i2, "ns0:SrcCountryCode", c.getSrcCountryCode());
        tagOrEmpty(x, i2, "ns0:DestCountryCode", c.getDestCountryCode());
        tagOrEmpty(x, i2, "ns0:UserDomicileBranchCode", c.getUserDomicileBranchCode());
        tagOrEmpty(x, i2, "ns0:UserDomicileResponsibilityCentre", c.getUserDomicileResponsibilityCentre());
        tagOrEmpty(x, i2, "ns0:ProcessingBranchCode", c.getProcessingBranchCode());
        tagOrEmpty(x, i2, "ns0:ProcessingResponsibilityCentre", c.getProcessingResponsibilityCentre());
        tagOrEmpty(x, i2, "ns0:UserGroup", c.getUserGroup());
        tagOrEmpty(x, i2, "ns0:SessionLanguageCode", c.getSessionLanguageCode());
        x.append(i1).append("</ns0:ClientDetails>\n");
        x.append(indent).append("</ns0:RqHeader>\n");
        return x.toString();
    }
}

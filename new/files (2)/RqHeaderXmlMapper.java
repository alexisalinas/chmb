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

    /** Formato del Envelope SOAP (prefijo head:, el namespace se declara en el Envelope). Omite lo que viene null. */
    public String toEnvelopeHeader(RqHeaderDTO h, String indent) {
        StringBuilder x = new StringBuilder();
        String i1 = indent + "   ";
        String i2 = i1 + "   ";
        x.append(indent).append("<head:RqHeader>\n");
        tag(x, i1, "head:DateAndTimeStamp", h.getDateTimeStamp() == null ? null : h.getDateTimeStamp().format(TS));
        tag(x, i1, "head:UUID", h.getUuid());
        tag(x, i1, "head:ESBUUID", h.getEsbUuid());
        tag(x, i1, "head:Version", h.getVersion());
        ClientDetailsDTO c = h.getClientDetails();
        if (c != null) {
            x.append(i1).append("<head:ClientDetails>\n");
            tag(x, i2, "head:Org", c.getOrg());
            tag(x, i2, "head:OrgUnit", c.getOrgUnit());
            tag(x, i2, "head:ChannelID", c.getChannelID());
            tag(x, i2, "head:TerminalID", c.getTerminalID());
            tag(x, i2, "head:ClientIPAddress", c.getClientIPAddress());
            tag(x, i2, "head:UserID", c.getUserID());
            tag(x, i2, "head:SrcCountryCode", c.getSrcCountryCode());
            tag(x, i2, "head:DestCountryCode", c.getDestCountryCode());
            tag(x, i2, "head:UserDomicileBranchCode", c.getUserDomicileBranchCode());
            tag(x, i2, "head:UserDomicileResponsibilityCentre", c.getUserDomicileResponsibilityCentre());
            tag(x, i2, "head:ProcessingBranchCode", c.getProcessingBranchCode());
            tag(x, i2, "head:ProcessingResponsibilityCentre", c.getProcessingResponsibilityCentre());
            tag(x, i2, "head:UserGroup", c.getUserGroup());
            tag(x, i2, "head:SessionLanguageCode", c.getSessionLanguageCode());
            x.append(i1).append("</head:ClientDetails>\n");
        }
        x.append(indent).append("</head:RqHeader>\n");
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

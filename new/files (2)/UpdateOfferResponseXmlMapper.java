package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.ControlFieldDTO;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.banamex.cops.update_offer.mapper.XmlUtil.nsTag;

/**
 * Respuesta del EMS -> UpdateOfferResponse (REST), y UpdateOfferResponse -> XML para el log.
 * Equivale al "Parse JSON" + mapper "RsUpdateOffer" de BW. Acepta XML o JSON.
 *
 * Los nombres de campo de la respuesta son supuestos: se prueban los del contrato nuevo y los antiguos de BW
 * (referenceContacto -> customerReferenceNumber, modeCmp -> surveyModeCode, controlField -> contextType).
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferResponseXmlMapper {

    private static final String BASETYPES_NS = "http://www.citi.com/gcgi/entities/common/MX/basetypes/v3_0_MX_1";
    private static final String OFFER_NS = "http://www.citi.com/gcgi/services/common/MX/offermanagement/v3_0_MX_1";

    private final JsonMapper jsonMapper;

    // =====================================================================
    //  EMS -> REST
    // =====================================================================
    public UpdateOfferResponse fromReply(String reply) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalStateException("Respuesta vacía del EMS");
        }
        return reply.trim().startsWith("{") ? fromJson(reply) : fromXml(reply);
    }

    private UpdateOfferResponse fromXml(String xml) {
        try {
            Document doc = parse(xml);
            List<ControlFieldDTO> controlFields = new ArrayList<>();
            for (Element e : elements(doc, "ControlField")) {
                controlFields.add(ControlFieldDTO.builder().contextType(e.getTextContent()).build());
            }
            return build(
                    xmlFirst(doc, "Code", "ResponseCode", "ReturnCode"),
                    xmlFirst(doc, "Description", "ResponseText", "Message"),
                    xmlFirst(doc, "CustomerReferenceNumber", "ReferenceContacto", "ContactReference"),
                    xmlFirst(doc, "StartDate"),
                    xmlFirst(doc, "EndDate"),
                    xmlFirst(doc, "SurveyModeCode", "ModeCmp"),
                    controlFields);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la respuesta XML del EMS: " + e.getMessage(), e);
        }
    }

    private UpdateOfferResponse fromJson(String json) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            return build(
                    jsonFirst(root, "code", "responseCode", "returnCode"),
                    jsonFirst(root, "description", "responseText", "message"),
                    jsonFirst(root, "customerReferenceNumber", "referenceContacto", "contactReference"),
                    jsonFirst(root, "startDate"),
                    jsonFirst(root, "endDate"),
                    jsonFirst(root, "surveyModeCode", "modeCmp"),
                    List.of());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la respuesta JSON del EMS: " + e.getMessage(), e);
        }
    }

    private UpdateOfferResponse build(String code, String description, String reference, String start,
                                      String end, String surveyMode, List<ControlFieldDTO> controlFields) {
        return UpdateOfferResponse.builder()
                .code(code != null ? code : "ERROR")
                .description(description != null ? description : "Respuesta del EMS sin descripción")
                .customerReferenceNumber(reference)
                .startDate(start)
                .endDate(end)
                .surveyModeCode(surveyMode)
                .contextType(controlFields.isEmpty() ? null : controlFields)
                .build();
    }

    // =====================================================================
    //  REST -> XML del log (Log service response)
    // =====================================================================
    public String toLogXml(UpdateOfferResponse rs) {
        StringBuilder x = new StringBuilder();
        x.append("<root>\n");
        x.append("   <ns0:UpdateOfferRs xmlns:ns0=\"").append(OFFER_NS).append("\">\n");
        f(x, "Code", rs.getCode());
        f(x, "Description", rs.getDescription());
        f(x, "ReferenceContacto", rs.getCustomerReferenceNumber());      // nombre BW de customerReferenceNumber
        f(x, "StartDate", rs.getStartDate());
        f(x, "EndDate", rs.getEndDate());
        f(x, "ModeCmp", rs.getSurveyModeCode());                         // nombre BW de surveyModeCode
        if (rs.getContextType() != null) {
            for (ControlFieldDTO c : rs.getContextType()) f(x, "ControlField", c.getContextType());  // contextType
        }
        x.append("   </ns0:UpdateOfferRs>\n");
        x.append("</root>");
        return x.toString();
    }

    private void f(StringBuilder x, String name, String value) {
        nsTag(x, "      ", "ns1", name, BASETYPES_NS, value);
    }

    // =====================================================================
    //  helpers XML / JSON
    // =====================================================================
    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String xmlFirst(Document doc, String... names) {
        for (String name : names) {
            List<Element> found = elements(doc, name);
            if (!found.isEmpty()) {
                String t = found.get(0).getTextContent();
                if (t != null && !t.trim().isEmpty()) return t.trim();
            }
        }
        return null;
    }

    private List<Element> elements(Document doc, String localName) {
        List<Element> out = new ArrayList<>();
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element e = (Element) all.item(i);
            String ln = e.getLocalName() != null ? e.getLocalName() : e.getTagName();
            if (ln.equalsIgnoreCase(localName)) out.add(e);
        }
        return out;
    }

    /** Busca recursivamente el primer valor simple cuyo nombre coincida (sin importar mayúsculas). */
    private String jsonFirst(JsonNode root, String... names) {
        for (String name : names) {
            AtomicReference<String> found = new AtomicReference<>();
            search(root, name, found);
            if (found.get() != null) return found.get();
        }
        return null;
    }

    private void search(JsonNode n, String name, AtomicReference<String> found) {
        if (found.get() != null || n == null) return;
        if (n.isObject()) {
            n.properties().forEach(e -> {
                if (found.get() != null) return;
                if (e.getKey().equalsIgnoreCase(name) && e.getValue().isValueNode() && !e.getValue().isNull()) {
                    found.set(e.getValue().asText());
                } else {
                    search(e.getValue(), name, found);
                }
            });
        } else if (n.isArray()) {
            n.forEach(c -> search(c, name, found));
        }
    }
}

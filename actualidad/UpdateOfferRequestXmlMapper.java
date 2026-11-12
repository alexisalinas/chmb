package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

import static com.banamex.cops.update_offer.mapper.XmlUtil.tag;

/**
 * UpdateOfferRequest (REST) -> XML que se manda al EMS: el evento {@code UpdateOffer} con los campos que arma
 * el JMS Queue Requestor (los {@code OtherProperties}), dentro de {@code Envelope/Body}, sin URLs.
 * No lleva RqHeader ni ClientDetails. (El log del request no usa XML: ver AuditLogService.)
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferRequestXmlMapper {

    private final OfferEventMapper offerEventMapper;

    /** Si es {@code true}, el evento va dentro de {@code <Envelope><Body>}; si es {@code false}, va solo el evento. */
    @Value("${update-offer.ems.envelope:true}")
    private boolean envelope;

    /** BW: _ns_ del evento (namespace). Solo va en las propiedades del mensaje JMS, no en el XML. */
    @Value("${update-offer.ems.event-namespace:www.tibco.com/be/ontology/OfferEngine/Events/UpdateOffer}")
    private String eventNamespace = "www.tibco.com/be/ontology/OfferEngine/Events/UpdateOffer";

    /** BW: _nm_ del evento (nombre del elemento del cuerpo). */
    @Value("${update-offer.ems.body-element:UpdateOffer}")
    private String bodyElement;

    // =====================================================================
    //  1) XML hacia el EMS
    // =====================================================================
    /**
     * Arma el XML que se manda al EMS: el evento {@code UpdateOffer} con sus campos, en elementos simples,
     * sin URLs, namespaces ni prefijos. Va dentro de {@code Envelope/Body} (propiedad {@code update-offer.ems.envelope}).
     *
     * @param rq             oferta recibida
     * @param requestTime    hora en que se recibió la petición
     * @param updatedBy      {@code client_id} del header: va en {@code UpdatedBy} (puede ser nulo)
     * @param tzResponseCode código de respuesta de la zona horaria (puede ser nulo)
     * @param tz             resultado de la zona horaria (puede ser nulo)
     * @return el XML listo para enviarse
     */
    public String toEmsXml(UpdateOfferRequest rq, LocalDateTime requestTime, String updatedBy,
                           String tzResponseCode, MapRequestResultDTO tz) {
        Map<String, String> fields = offerEventMapper.map(rq, requestTime, updatedBy, tzResponseCode, tz);

        // Sin namespaces ni URLs: elementos simples
        String indent = envelope ? "      " : "";
        StringBuilder x = new StringBuilder();
        if (envelope) {
            x.append("<Envelope>\n");
            x.append("   <Body>\n");
        }
        x.append(indent).append("<").append(bodyElement).append(">\n");
        fields.forEach((k, v) -> tag(x, indent + "   ", k, v));
        x.append(indent).append("</").append(bodyElement).append(">");
        if (envelope) {
            x.append("\n   </Body>\n");
            x.append("</Envelope>");
        }
        return x.toString();
    }

    /**
     * Arma las propiedades del mensaje JMS (el {@code OtherProperties} del {@code JMS Queue Requestor} de BW): {@code _nm_}
     * (nombre del evento), {@code _ns_} (su namespace) y los campos del evento, cada uno como una propiedad de texto.
     *
     * @param rq             oferta recibida
     * @param requestTime    hora en que se recibió la petición
     * @param updatedBy      {@code client_id} del header: va en {@code UpdatedBy} (puede ser nulo)
     * @param tzResponseCode código de respuesta de la zona horaria (puede ser nulo)
     * @param tz             resultado de la zona horaria (puede ser nulo)
     * @return las propiedades, en orden: primero {@code _nm_} y {@code _ns_}
     */
    public Map<String, String> toEmsProperties(UpdateOfferRequest rq, LocalDateTime requestTime, String updatedBy,
                                               String tzResponseCode, MapRequestResultDTO tz) {
        Map<String, String> properties = new java.util.LinkedHashMap<>();
        properties.put("_nm_", bodyElement);
        properties.put("_ns_", eventNamespace);
        properties.putAll(offerEventMapper.map(rq, requestTime, updatedBy, tzResponseCode, tz));
        return properties;
    }
}

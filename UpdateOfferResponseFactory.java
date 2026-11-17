package com.banamex.cops.update_offer.service;

import com.banamex.cops.update_offer.config.UpdateOfferResponseProperties;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Arma la respuesta que el micro devuelve por cada oferta (el {@code End} de BW). */
@Component
@RequiredArgsConstructor
public class UpdateOfferResponseFactory {

    private final UpdateOfferResponseProperties props;

    /** Zona de la fecha: la misma ECRMTimeZone de las globales (US/Central). */
    @Value("${update-offer.timezone.ecrm-time-zone:US/Central}")
    private String ecrmTimeZone = "US/Central";

    /** Reloj (se cambia en las pruebas). */
    private Clock clock = Clock.systemUTC();

    /** Respuesta de éxito. */
    public UpdateOfferResponse success() {
        return build(props.getSuccessCode(), props.getSuccessText(), "", "");
    }

    /** Respuesta de éxito de una oferta. */
    public UpdateOfferResponse success(UpdateOfferRequest request) {
        if (request == null || !props.isStatusUBwFormat() || !"U".equals(request.getOfferStatus())) {
            return success();
        }
        UpdateOfferResponse r = new UpdateOfferResponse();
        if (isEmpty(request.getCampaignId())) {
            r.setCode(props.getStatusUCampaignMissingCode());
            r.setDescription(props.getStatusUCampaignMissingText()
                    .replace("{campaignId}", request.getCampaignId() == null ? "" : request.getCampaignId()));
        } else {
            r.setCode(props.getStatusUSuccessCode());
            r.setDescription(props.getStatusUSuccessText().replace("{offerKey}", updateKey(request)));
        }
        return r;
    }

    /** Respuesta de éxito de una oferta, teniendo en cuenta lo que contestó el EMS. */
    public UpdateOfferResponse success(UpdateOfferRequest request, UpdateOfferResponse emsResponse) {
        boolean esU = request != null && props.isStatusUBwFormat() && "U".equals(request.getOfferStatus());
        if (!esU && "ems".equalsIgnoreCase(props.getSource()) && emsResponse != null
                && emsResponse.getCode() != null && !emsResponse.getCode().isBlank()) {
            return emsResponse;
        }
        return success(request);
    }

    /** Llave de la oferta que arma el {@code Map-Data} de BW en el caso {@code U}. */
    static String updateKey(UpdateOfferRequest rq) {
        StringBuilder key = new StringBuilder("off:mx:")
                .append(nz(rq.getCampaignId())).append('_')
                .append(nz(rq.getWaveId())).append('_')
                .append(nz(rq.getCustomerId()));
        if (!isEmpty(rq.getAccountNumber())) {
            key.append('_').append(rq.getAccountNumber());
        }
        if (!isEmpty(rq.getCardNumber())) {
            key.append('_').append(rq.getCardNumber());
        }
        return key.toString();
    }

    /** XPath {@code string-length(x) = 0}: nulo o vacío (un texto de solo espacios SÍ cuenta como con valor). */
    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Respuesta de error. */
    public UpdateOfferResponse error(String step, Throwable cause) {
        return build(props.getErrorCode(), props.getErrorText(), step, detail(cause));
    }

    /** Éxito del envío a Kafka. */
    public String kafkaSuccessText() {
        return kafkaSuccessText("Kafka");
    }

    /** Éxito del envío al destino indicado. */
    public String kafkaSuccessText(String destination) {
        return format(props.getKafkaSuccessText(), "", "").replace("{destination}", nz(destination));
    }

    /** El evento no tiene ningún destino activo. */
    public String kafkaDisabledText() {
        return format(props.getKafkaDisabledText(), "", "");
    }

    /** Modo dry-run en Kafka. */
    public String kafkaDryRunText() {
        return kafkaDryRunText("Kafka");
    }

    /** Modo dry-run en el destino indicado. */
    public String kafkaDryRunText(String destination) {
        return format(props.getKafkaDryRunText(), "", "").replace("{destination}", nz(destination));
    }

    /** Error del envío a Kafka en el paso indicado. */
    public String kafkaErrorText(String step, String detail) {
        return kafkaErrorText("Kafka", step, detail);
    }

    /** Igual que {@link #kafkaErrorText(String, String)}, con la causa más profunda de la excepción. */
    public String kafkaErrorText(String step, Throwable cause) {
        return kafkaErrorText("Kafka", step, cause);
    }

    /** Error del envío al destino indicado. */
    public String kafkaErrorText(String destination, String step, String detail) {
        return format(props.getKafkaErrorText(), step, detail).replace("{destination}", nz(destination));
    }

    /** Error del envío al destino indicado, con la causa más profunda de la excepción. */
    public String kafkaErrorText(String destination, String step, Throwable cause) {
        return kafkaErrorText(destination, step, detail(cause));
    }

    private UpdateOfferResponse build(String code, String textTemplate, String step, String detail) {
        UpdateOfferResponse r = new UpdateOfferResponse();
        r.setCode(code);
        r.setDescription(format(textTemplate, step, detail));
        return r;
    }

    private String format(String template, String step, String detail) {
        return template
                .replace("{dateTime}", dateTime())
                .replace("{step}", step == null ? "" : step)
                .replace("{detail}", detail == null ? "" : detail);
    }

    /** Fecha y hora actuales en la zona ECRM con el formato configurado. */
    String dateTime() {
        return ZonedDateTime.now(clock.withZone(ZoneId.of(ecrmTimeZone)))
                .format(DateTimeFormatter.ofPattern(props.getDateFormat()));
    }

    /** Mensaje de la causa más profunda; si no tiene, el nombre de su clase. */
    private static String detail(Throwable cause) {
        if (cause == null) return "";
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}

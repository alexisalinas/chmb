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

/**
 * Arma la respuesta que el micro devuelve por cada oferta (el {@code End} de BW).
 * <p>
 * La respuesta del JMS no se usa para esto: si el flujo funciona, el micro responde un mensaje de éxito con la
 * fecha de {@code ECRMTimeZone} ({@code YYYY/MM/DD HH24:MI:SS}); si algo falla, responde un error con esa fecha
 * y el paso donde falló.
 */
@Component
@RequiredArgsConstructor
public class UpdateOfferResponseFactory {

    private final UpdateOfferResponseProperties props;

    /** Zona de la fecha: la misma ECRMTimeZone de las globales (US/Central). */
    @Value("${update-offer.timezone.ecrm-time-zone:US/Central}")
    private String ecrmTimeZone = "US/Central";

    /** Reloj (se cambia en las pruebas). */
    private Clock clock = Clock.systemUTC();

    /**
     * Respuesta de éxito.
     *
     * @return el código y el texto de éxito, con la fecha de {@code ECRMTimeZone}
     */
    public UpdateOfferResponse success() {
        return build(props.getSuccessCode(), props.getSuccessText(), "", "");
    }

    /**
     * Respuesta de éxito de una oferta. Si la oferta tiene {@code OfferStatus = "U"} (y
     * {@code update-offer.response.status-u-bw-format} está activo), responde con el formato del {@code Map-Data}
     * de BW: {@code COP_UPDOF_432} si no trae {@code CampaignID}; si no, {@code COP_UPDOF_0} con la llave
     * {@code off:mx:...}. Cualquier otra oferta responde con {@link #success()} (con la fecha de ECRMTimeZone).
     *
     * @param request oferta que se procesó
     * @return la respuesta de éxito que corresponde a la oferta
     */
    public UpdateOfferResponse success(UpdateOfferRequest request) {
        if (request == null || !props.isStatusUBwFormat() || !"U".equals(request.getOfferStatus())) {
            return success();
        }
        UpdateOfferResponse r = new UpdateOfferResponse();
        if (isEmpty(request.getCampaignId())) {                      // string-length(CampaignID) = 0
            r.setCode(props.getStatusUCampaignMissingCode());
            r.setDescription(props.getStatusUCampaignMissingText()
                    .replace("{campaignId}", request.getCampaignId() == null ? "" : request.getCampaignId()));
        } else {
            r.setCode(props.getStatusUSuccessCode());
            r.setDescription(props.getStatusUSuccessText().replace("{offerKey}", updateKey(request)));
        }
        return r;
    }

    /**
     * Llave de la oferta que arma el {@code Map-Data} de BW en el caso {@code U}:
     * {@code off:mx:} + campaña + {@code _} + ola + {@code _} + cliente, y después la cuenta y la tarjeta
     * (cada una con su {@code _}) solo si tienen valor.
     *
     * @param rq oferta
     * @return la llave, por ejemplo {@code off:mx:CAMP_WAVE_123_ACC_CARD}
     */
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

    /**
     * Respuesta de error.
     *
     * @param step  paso donde falló (por ejemplo, "Envío al EMS")
     * @param cause causa del error (puede ser nula)
     * @return el código y el texto de error, con la fecha de {@code ECRMTimeZone}, el paso y la causa
     */
    public UpdateOfferResponse error(String step, Throwable cause) {
        return build(props.getErrorCode(), props.getErrorText(), step, detail(cause));
    }

    /**
     * Texto del log cuando el evento se publicó en Kafka.
     *
     * @return el mensaje de éxito con la fecha de {@code ECRMTimeZone}
     */
    public String kafkaSuccessText() {
        return format(props.getKafkaSuccessText(), "", "");
    }

    /**
     * Texto del log cuando Kafka está desactivado: no se envió nada.
     *
     * @return el mensaje con la fecha de {@code ECRMTimeZone}
     */
    public String kafkaDisabledText() {
        return format(props.getKafkaDisabledText(), "", "");
    }

    /**
     * Texto del log cuando el modo de Kafka es dry-run: el mensaje no se publica.
     *
     * @return el mensaje con la fecha de {@code ECRMTimeZone}
     */
    public String kafkaDryRunText() {
        return format(props.getKafkaDryRunText(), "", "");
    }

    /**
     * Texto del log cuando el evento no se envió a Kafka.
     *
     * @param step   paso donde falló (por ejemplo, "Consulta a la base de datos")
     * @param detail causa: qué pasó
     * @return el mensaje de error con la fecha de {@code ECRMTimeZone}, el paso y la causa
     */
    public String kafkaErrorText(String step, String detail) {
        return format(props.getKafkaErrorText(), step, detail);
    }

    /**
     * Igual que {@link #kafkaErrorText(String, String)}, tomando la causa más profunda de la excepción.
     *
     * @param step  paso donde falló
     * @param cause causa del error (puede ser nula)
     * @return el mensaje de error con la fecha, el paso y la causa
     */
    public String kafkaErrorText(String step, Throwable cause) {
        return format(props.getKafkaErrorText(), step, detail(cause));
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

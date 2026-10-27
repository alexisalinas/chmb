package com.banamex.cops.update_offer.config;

import com.banamex.cops.update_offer.dto.error.ErrorResponse;
import com.banamex.cops.update_offer.dto.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderValidationTest {

    public static class ConAnotacionEnMetodo {
        @HeaderValidation.Required({"uuid", "sid"})
        public void metodo() { }
    }

    @HeaderValidation.Required({"uuid"})
    public static class ConAnotacionEnClase {
        public void metodo() { }
    }

    @HeaderValidation.Required({"uuid"})
    public static class ConAmbas {
        @HeaderValidation.Required({"sid"})
        public void metodo() { }
    }

    public static class SinAnotacion {
        public void metodo() { }
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private HeaderValidation validation;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        validation = new HeaderValidation(mapper);
        request = new MockHttpServletRequest("POST", "/sd-campaing-management/v1/update-offer");
        response = new MockHttpServletResponse();
    }

    private HandlerMethod handler(Object bean) throws Exception {
        return new HandlerMethod(bean, bean.getClass().getMethod("metodo"));
    }

    private JsonNode cuerpo() throws Exception {
        return mapper.readTree(response.getContentAsString());
    }

    @Test
    void faltanHeaders_rechazaConElFormatoDeError() throws Exception {
        boolean ok = validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()));

        assertThat(ok).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = cuerpo();
        assertThat(body.get("type").asText()).isEqualTo("ERROR");
        assertThat(body.get("details").asText()).isEqualTo("Missing or invalid Parameters");
        assertThat(body.get("code").asText()).isEqualTo("InvalidRequest");
        assertThat(body.get("location").asText()).isEqualTo("header");
        assertThat(body.get("moreInfo").asText()).isEqualTo("Missing required headers: uuid sid");
        assertThat(body.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void soloListaLosQueFaltan() throws Exception {
        request.addHeader("uuid", "abc");

        boolean ok = validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()));

        assertThat(ok).isFalse();
        assertThat(cuerpo().get("moreInfo").asText()).isEqualTo("Missing required headers: sid");
    }

    @Test
    void unHeaderEnBlancoSeCuentaComoFaltante() throws Exception {
        request.addHeader("uuid", "   ");
        request.addHeader("sid", "s1");

        assertThat(validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()))).isFalse();
        assertThat(cuerpo().get("moreInfo").asText()).contains("uuid");
    }

    @Test
    void conTodosLosHeaders_continuaYNoEscribeRespuesta() throws Exception {
        request.addHeader("uuid", "abc");
        request.addHeader("sid", "s1");

        assertThat(validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()))).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void losNombresDeHeaderNoDistinguenMayusculas() throws Exception {
        request.addHeader("UUID", "abc");
        request.addHeader("SID", "s1");

        assertThat(validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()))).isTrue();
    }

    @Test
    void laAnotacionEnLaClaseTambienSeValida() throws Exception {
        assertThat(validation.preHandle(request, response, handler(new ConAnotacionEnClase()))).isFalse();
        assertThat(cuerpo().get("moreInfo").asText()).isEqualTo("Missing required headers: uuid");
    }

    @Test
    void sumaLosHeadersDeLaClaseYDelMetodoSinRepetir() throws Exception {
        assertThat(validation.preHandle(request, response, handler(new ConAmbas()))).isFalse();
        assertThat(cuerpo().get("moreInfo").asText()).isEqualTo("Missing required headers: uuid sid");
    }

    @Test
    void sinAnotacion_noValidaNada() throws Exception {
        assertThat(validation.preHandle(request, response, handler(new SinAnotacion()))).isTrue();
    }

    @Test
    void siElHandlerNoEsUnMetodo_continua() throws Exception {
        assertThat(validation.preHandle(request, response, "recurso estatico")).isTrue();
    }

    @Test
    void elUuidDelErrorEsElDelHeaderSiVino() throws Exception {
        request.addHeader("uuid", "mi-uuid-123");

        validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()));

        assertThat(cuerpo().get("uuid").asText()).isEqualTo("mi-uuid-123");
    }

    @Test
    void siNoVinoUuid_generaUnoNuevo() throws Exception {
        validation.preHandle(request, response, handler(new ConAnotacionEnMetodo()));

        assertThat(cuerpo().get("uuid").asText())
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void invalidResponse_armaElErrorEstandar() {
        ErrorResponse e = HeaderValidation.invalidResponse("detalle", "u-1");

        assertThat(e.getType()).isEqualTo(ErrorType.ERROR);
        assertThat(e.getCode()).isEqualTo("InvalidRequest");
        assertThat(e.getLocation()).isEqualTo("header");
        assertThat(e.getMoreInfo()).isEqualTo("detalle");
        assertThat(e.getUuid()).isEqualTo("u-1");
        assertThat(e.getTimestamp()).isNotBlank();
    }
}

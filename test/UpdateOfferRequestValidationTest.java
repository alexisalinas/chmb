package com.banamex.cops.update_offer.dto;

import com.banamex.cops.update_offer.testutil.TestData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba las reglas de Bean Validation del contrato REST. */
class UpdateOfferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<String> campos(UpdateOfferRequest r) {
        return validator.validate(r).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void requestCompletoEsValido() {
        assertThat(validator.validate(TestData.request())).isEmpty();
    }

    @Test
    void soloConLosCuatroObligatorios_esValido() {
        assertThat(validator.validate(TestData.minimalRequest())).isEmpty();
    }

    @Test
    void sinLosObligatorios_fallanLosCuatro() {
        assertThat(campos(new UpdateOfferRequest()))
                .containsExactlyInAnyOrder("channelCode", "campaignId", "waveId", "customerId");
    }

    @Test
    void campaignIdWaveIdYCustomerId_maximo20Caracteres() {
        UpdateOfferRequest r = TestData.minimalRequest();
        r.setCampaignId("x".repeat(21));
        r.setWaveId("x".repeat(21));
        r.setCustomerId("x".repeat(21));
        assertThat(campos(r)).containsExactlyInAnyOrder("campaignId", "waveId", "customerId");

        r.setCampaignId("x".repeat(20));
        r.setWaveId("x");
        r.setCustomerId("x".repeat(20));
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void campaignIdVacioNoCumpleElMinimo() {
        UpdateOfferRequest r = TestData.minimalRequest();
        r.setCampaignId("");
        assertThat(campos(r)).containsExactly("campaignId");
    }

    @Test
    void limitesDeTamanoDeLosOpcionales() {
        UpdateOfferRequest r = TestData.minimalRequest();
        r.setAccountNumber("x".repeat(36));
        r.setCardNumber("x".repeat(20));
        r.setOfferPriority("x".repeat(6));
        r.setOfferStatus("123");
        r.setSchemaVersion("1.00");
        assertThat(campos(r)).containsExactlyInAnyOrder(
                "accountNumber", "cardNumber", "offerPriority", "offerStatus", "schemaVersion");
    }

    @Test
    void limitesDeTamanoEnElMaximoSonValidos() {
        UpdateOfferRequest r = TestData.minimalRequest();
        r.setAccountNumber("x".repeat(35));
        r.setCardNumber("x".repeat(19));
        r.setOfferPriority("x".repeat(5));
        r.setOfferStatus("12");
        r.setSchemaVersion("1.0");
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void userText_debeEmpezarConLlaveYTerminarConLlave() {
        UpdateOfferRequest r = TestData.minimalRequest();

        r.setUserText("no es json");
        assertThat(campos(r)).containsExactly("userText");

        r.setUserText("[{\"a\":1}]");
        assertThat(campos(r)).containsExactly("userText");

        r.setUserText("{\"userField1\":\"a\"}");
        assertThat(validator.validate(r)).isEmpty();
    }

    @Test
    void userText_aceptaSaltosDeLinea() {
        UpdateOfferRequest r = TestData.minimalRequest();
        r.setUserText("{\n  \"userField1\": \"a\",\n  \"userField2\": \"b\"\n}");
        assertThat(validator.validate(r)).isEmpty();
    }
}

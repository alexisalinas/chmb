package com.banamex.cops.update_offer.dto;

import com.banamex.cops.update_offer.dto.log.AdditionalInfoDTO;
import com.banamex.cops.update_offer.dto.log.LogDTO;
import com.banamex.cops.update_offer.dto.log.LogHeaderDTO;
import com.banamex.cops.update_offer.dto.log.RqLogDTO;
import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionList;
import com.banamex.cops.update_offer.dto.suppression.FilteredSuppressionResult;
import com.banamex.cops.update_offer.dto.suppression.OperationDetailOutput;
import com.banamex.cops.update_offer.dto.suppression.OperationDetailsDTO;
import com.banamex.cops.update_offer.dto.suppression.ResponseDetailsDTO;
import com.banamex.cops.update_offer.dto.suppression.StartRequest;
import com.banamex.cops.update_offer.dto.suppression.StartRequestParams;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntitiesListRoot;
import com.banamex.cops.update_offer.dto.suppression.SuppressedEntityDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateTimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationResult;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeZoneValidationStartDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzFormatsDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TzLookUpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.ValueItemDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas de los esquemas de BW: sin símbolo = obligatorio, ? = opcional, * = lista.
 * Obligatorio  -> la prueba exige que falle si falta.
 * Opcional (?) -> la prueba verifica que NO falle si falta.
 * Lista (*)    -> nunca es nula y se valida cada elemento (ruta lista[0].campo).
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> paths(Object o) {
        return validator.validate(o).stream().map(ConstraintViolation::getPropertyPath)
                .map(Object::toString).collect(Collectors.toSet());
    }

    private static ClientDetailsDTO clientOk() {
        return ClientDetailsDTO.builder().org("Banamex").orgUnit("COPS").channelID("CAT").terminalID("1")
                .userID("ATM").srcCountryCode("MX").destCountryCode("MX").build();
    }

    // ============================================================ header

    @Nested
    class Header {
        @Test
        void clientDetails_vacio_fallanLosSieteObligatorios() {
            assertThat(paths(new ClientDetailsDTO())).containsExactlyInAnyOrder(
                    "org", "orgUnit", "channelID", "terminalID", "userID", "srcCountryCode", "destCountryCode");
        }

        @Test
        void clientDetails_losOpcionalesNoSonRequeridos() {
            assertThat(paths(clientOk())).isEmpty();   // sin ClientIPAddress, UserGroup, SessionLanguageCode, etc.
        }

        @Test
        void rqHeader_vacio_fallanLosObligatorios() {
            assertThat(paths(new RqHeaderDTO())).containsExactlyInAnyOrder("dateTimeStamp", "uuid", "clientDetails");
        }

        @Test
        void rqHeader_esbUuidYVersionSonOpcionales() {
            RqHeaderDTO h = RqHeaderDTO.builder().dateTimeStamp(LocalDateTime.now()).uuid("u").clientDetails(clientOk()).build();
            assertThat(paths(h)).isEmpty();
        }

        @Test
        void rqHeader_validaElClientDetailsAnidado() {
            RqHeaderDTO h = RqHeaderDTO.builder().dateTimeStamp(LocalDateTime.now()).uuid("u")
                    .clientDetails(new ClientDetailsDTO()).build();
            assertThat(paths(h)).contains("clientDetails.org", "clientDetails.userID", "clientDetails.terminalID");
        }

        @Test
        void rqLog_exigeHeaderYBody() {
            assertThat(paths(new RqLogDTO())).containsExactlyInAnyOrder("rqHeader", "rqBody");
        }
    }

    // ============================================================ log

    @Nested
    class Log {
        @Test
        void logHeader_vacio_fallanLosCuatroObligatorios() {
            assertThat(paths(new LogHeaderDTO())).containsExactlyInAnyOrder(
                    "applicationId", "componentName", "timestamp", "transactionId");
        }

        @Test
        void logHeader_losOpcionalesNoSonRequeridos() {
            LogHeaderDTO h = LogHeaderDTO.builder().applicationId("a").componentName("c")
                    .timestamp(LocalDateTime.now()).transactionId("t").build();
            assertThat(paths(h)).isEmpty();
        }

        @Test
        void log_todoEsOpcionalExceptoQueLaListaNoSeaNula() {
            assertThat(paths(new LogDTO())).isEmpty();
            assertThat(new LogDTO().getAdditionalInfo()).isNotNull().isEmpty();
        }

        @Test
        void log_validaElHeaderAnidado() {
            LogDTO l = LogDTO.builder().header(new LogHeaderDTO()).build();
            assertThat(paths(l)).contains("header.applicationId", "header.transactionId");
        }

        @Test
        void log_additionalInfoEsListaYSeValidaCadaElemento() {
            LogDTO l = LogDTO.builder().additionalInfo(List.of(new AdditionalInfoDTO(), new AdditionalInfoDTO("k", "v"))).build();
            assertThat(paths(l)).containsExactlyInAnyOrder("additionalInfo[0].name", "additionalInfo[0].value");
        }
    }

    // ============================================================ supresión

    @Nested
    class Suppression {
        @Test
        void startRequestParams_requestingChannelEsOpcional() {
            assertThat(paths(new StartRequestParams())).containsExactlyInAnyOrder("countryCode", "currentOperationName");
            assertThat(paths(StartRequestParams.builder().countryCode("MX").currentOperationName("UpdateOffer").build())).isEmpty();
        }

        @Test
        void startRequest_exigeParamsYLosValidaAdentro() {
            assertThat(paths(new StartRequest())).containsExactly("requestParams");
            assertThat(paths(StartRequest.builder().requestParams(new StartRequestParams()).build()))
                    .contains("requestParams.countryCode", "requestParams.currentOperationName");
        }

        @Test
        void operationDetails_lostresCamposSonObligatorios() {
            assertThat(paths(new OperationDetailsDTO())).containsExactlyInAnyOrder("operationName", "operationFlag", "reqChannel");
        }

        @Test
        void suppressedEntity_obligatoriosOpcionalesYLista() {
            assertThat(paths(new SuppressedEntityDTO())).containsExactlyInAnyOrder(
                    "mode", "suppressionFlag", "operationFlag", "sourceSystem");     // CountryCode? y ChannelId? opcionales
            assertThat(new SuppressedEntityDTO().getOperationDetails()).isNotNull().isEmpty();

            SuppressedEntityDTO e = SuppressedEntityDTO.builder().mode("RT").suppressionFlag("Y").operationFlag("Y")
                    .sourceSystem("ECRM").operationDetails(List.of(new OperationDetailsDTO())).build();
            assertThat(paths(e)).contains("operationDetails[0].operationName");
        }

        @Test
        void suppressedEntitiesList_esListaYValidaCadaElemento() {
            SuppressedEntitiesListRoot r = SuppressedEntitiesListRoot.builder()
                    .suppressedEntities(List.of(new SuppressedEntityDTO())).build();
            assertThat(paths(r)).contains("suppressedEntities[0].mode");
            assertThat(new SuppressedEntitiesListRoot().getSuppressedEntities()).isNotNull().isEmpty();
        }

        @Test
        void operationDetailOutput_indicatorEsOpcional() {
            assertThat(paths(new OperationDetailOutput())).containsExactlyInAnyOrder(
                    "operationName", "reqChannel", "operationFlag", "countryCode", "responseCode", "responseText");
        }

        @Test
        void filteredSuppressionResult_camposObligatoriosYListaDeDetalles() {
            assertThat(paths(new FilteredSuppressionResult())).containsExactlyInAnyOrder(
                    "countryCode", "channelID", "suppressionFlag", "operationFlag", "mode", "sourceSystem");
            assertThat(new FilteredSuppressionResult().getOperationDetails()).isNotNull().isEmpty();
        }

        @Test
        void filteredSuppressionList_esListaYValidaCadaResultado() {
            FilteredSuppressionList l = FilteredSuppressionList.builder().results(List.of(new FilteredSuppressionResult())).build();
            assertThat(paths(l)).contains("results[0].countryCode", "results[0].mode");
            assertThat(new FilteredSuppressionList().getResults()).isNotNull().isEmpty();
        }

        @Test
        void responseDetails_indicatorOpcional() {
            assertThat(paths(new ResponseDetailsDTO())).containsExactlyInAnyOrder("responseText", "responseCode");
        }
    }

    // ============================================================ zona horaria

    @Nested
    class TimeZone {
        @Test
        void start_obligatoriosYListasNoNulas() {
            assertThat(paths(new TimeZoneValidationStartDTO())).containsExactlyInAnyOrder("system", "sysMode", "type");
            TimeZoneValidationStartDTO s = new TimeZoneValidationStartDTO();
            assertThat(s.getDate()).isNotNull().isEmpty();
            assertThat(s.getTime()).isNotNull().isEmpty();
            assertThat(s.getDateTime()).isNotNull().isEmpty();
        }

        @Test
        void start_validaCadaElementoDeLasListas() {
            TimeZoneValidationStartDTO s = TimeZoneValidationStartDTO.builder().system("COPS").sysMode("RT").type("Request")
                    .date(List.of(new ValueItemDTO("2026-10-25", null)))
                    .time(List.of(new ValueItemDTO("10:30:00", "T")))
                    .build();
            assertThat(paths(s)).containsExactly("date[0].name");
        }

        @Test
        void valueItem_elValorEsOpcionalYElNombreObligatorio() {
            assertThat(paths(new ValueItemDTO())).containsExactly("name");
            assertThat(paths(new ValueItemDTO(null, "AppointmentDate"))).isEmpty();
        }

        @Test
        void formatsYLookUp_todosObligatorios() {
            assertThat(paths(new TzFormatsDTO())).hasSize(5);
            assertThat(paths(new TzLookUpDTO())).hasSize(8);
        }

        @Test
        void grupos_statusObligatorioPeroPuedeIrVacio_yNameOpcional() {
            assertThat(paths(new DateGrpDTO())).containsExactlyInAnyOrder("date", "dateStatus");
            assertThat(paths(new TimeGrpDTO())).containsExactlyInAnyOrder("time", "timeStatus");
            assertThat(paths(new DateTimeGrpDTO())).containsExactlyInAnyOrder("dateTime", "dateTimeStatus");
            assertThat(paths(new DateGrpDTO("2026-10-25T00:00:00-0600", "", null))).isEmpty();
        }

        @Test
        void mapRequestResult_listasNoNulasYValidaCadaGrupo() {
            assertThat(paths(new MapRequestResultDTO())).isEmpty();
            MapRequestResultDTO r = new MapRequestResultDTO();
            r.getDateGrp().add(new DateGrpDTO());
            r.getTimeGrp().add(new TimeGrpDTO());
            assertThat(paths(r)).contains("dateGrp[0].date", "timeGrp[0].timeStatus");
        }

        @Test
        void resultado_codeYTextObligatorios_formatsYDetalleOpcionales() {
            assertThat(paths(new TimeZoneValidationResult())).containsExactlyInAnyOrder("responseCode", "responseText");
            TimeZoneValidationResult ok = new TimeZoneValidationResult();
            ok.setResponseCode("COP_TZCOD_000");
            ok.setResponseText("Success");
            assertThat(paths(ok)).isEmpty();
        }
    }
}

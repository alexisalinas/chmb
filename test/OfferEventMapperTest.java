package com.banamex.cops.update_offer.mapper;

import com.banamex.cops.update_offer.config.OfferJmsProperties;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.timeZoneValidation.DateGrpDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.MapRequestResultDTO;
import com.banamex.cops.update_offer.dto.timeZoneValidation.TimeGrpDTO;
import com.banamex.cops.update_offer.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OfferEventMapperTest {

    private OfferJmsProperties props;
    private OfferEventMapper mapper;

    @BeforeEach
    void setUp() {
        props = new OfferJmsProperties();
        props.setCustomerPadding(true);
        props.setCustomerPaddingLength(10);
        props.setCustomerPaddingChar('0');
        mapper = new OfferEventMapper(props, new UserTextParser(JsonMapper.builder().build()));
        ReflectionTestUtils.setField(mapper, "translateTimeZone", false);
        ReflectionTestUtils.setField(mapper, "ecrmTimeZone", "America/Mexico_City");
    }

    private MapRequestResultDTO tzResult() {
        MapRequestResultDTO tz = new MapRequestResultDTO();
        tz.getDateGrp().add(new DateGrpDTO("2026-10-25T00:00:00-0600", "", "AppointmentDate"));
        tz.getTimeGrp().add(new TimeGrpDTO("10:31:00", "", "AppointmentStartTime"));
        tz.getTimeGrp().add(new TimeGrpDTO("11:01:00", "", "AppointmentEndTime"));
        return tz;
    }

    // ------------------------------------------------------------ obligatorios / opcionales

    @Test
    void camposObligatoriosSiempreSeEscribenVaciosSiNoHayDato() {
        Map<String, String> m = mapper.map(TestData.minimalRequest(), null, null, null);

        assertThat(m).containsEntry("RequestingChannel", "CAT")
                .containsEntry("CampaignID", "CAMP001")
                .containsEntry("WaveID", "WAVE01");
        for (String k : List.of("AccountNO", "CardNO", "AssignedBy", "ApptStartTime", "ApptEndTime", "ReasonCode",
                "ReasonDescription", "Comments", "ContactNumber", "OfferStatus", "SysVersion", "OfferSource",
                "UserField1", "UserField20", "UserField29", "OfferAction", "GiftCode", "GiftConfirmationNO",
                "ContactReference", "LVCode", "UpdatedBy", "UpdateTime", "ContactType", "ContactInfo",
                "DepartmentName")) {
            assertThat(m).as(k).containsEntry(k, "");
        }
    }

    @Test
    void camposOpcionalesNoSeEscribenSinValor() {
        Map<String, String> m = mapper.map(TestData.minimalRequest(), null, null, null);

        assertThat(m).doesNotContainKeys("AgentID", "ApptDate", "ApptConfirmation", "FulfillmentReferencedNO",
                "ProbabilityAmount", "Department", "Branch", "Priority", "Workgroup", "ControlField",
                "LocationIndicator", "AreaID", "DivisionID", "RegionID", "AgentName", "OfferCategory",
                "BranchName", "ChannelResponse", "Purpose", "CustomerEmail", "FulfillmentScript",
                "UserField21", "UserField100");
    }

    // ------------------------------------------------------------ renombres REST -> evento

    @Test
    void losNombresNuevosSeMapeanAlCampoDelEvento() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);

        assertThat(m)
                .containsEntry("RequestingChannel", "CAT")          // channelCode
                .containsEntry("AccountNO", "00112233445566")       // accountNumber
                .containsEntry("CardNO", "1234567812345678")        // cardNumber
                .containsEntry("AgentID", "AGENT1")                 // assignedTo
                .containsEntry("ApptConfirmation", "Y")             // offeredNumber (IsConfirmed)
                .containsEntry("ContactNumber", "5555555555")       // contactIndicatorNumber
                .containsEntry("ContactInfo", "5555555555")
                .containsEntry("SysVersion", "1.0")                 // schemaVersion
                .containsEntry("OfferAction", "UPD")                // offerActionCode
                .containsEntry("GiftConfirmationNO", "GC1")         // giftConfirmationNumber
                .containsEntry("FulfillmentReferencedNO", "FT1")    // fulfillmentTrackerFlag
                .containsEntry("ProbabilityAmount", "100.50")       // amount
                .containsEntry("Department", "DEP1")                // departmentId
                .containsEntry("Branch", "BR1")                     // branchId
                .containsEntry("ContactReference", "REF123")        // customerReferenceNumber
                .containsEntry("Priority", "1")                     // offerPriority
                .containsEntry("Workgroup", "WG1")                  // workgroupId
                .containsEntry("LVCode", "LV1")                     // levelCode
                .containsEntry("AreaID", "AREA1")                   // areaIdentifier
                .containsEntry("DivisionID", "DIV1")                // divisionCode
                .containsEntry("RegionID", "REG1")                  // branchRegionIdentifier
                .containsEntry("AgentName", "Agente Uno")
                .containsEntry("ChannelResponse", "SMS")            // responseChannel
                .containsEntry("CustomerEmail", "a@b.com")          // customerEmailAddress
                .containsEntry("FulfillmentScript", "SCRIPT1")      // fulfillmentCode
                .containsEntry("Purpose", "Proposito")
                .containsEntry("UpdatedBy", "ATM");                 // header ClientDetails/UserID
    }

    @Test
    void controlFieldYLocationIndicatorSalenDeContextType() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("ControlField", "controlField1")
                .containsEntry("LocationIndicator", "controlField2");
    }

    @Test
    void contextTypeConUnSoloElementoNoEscribeLocationIndicator() {
        UpdateOfferRequest r = TestData.request();
        r.setContextType(List.of(r.getContextType().get(0)));
        Map<String, String> m = mapper.map(r, TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("ControlField", "controlField1").doesNotContainKey("LocationIndicator");
    }

    // ------------------------------------------------------------ UserFields

    @Test
    void userFieldsVienenDeUserTextYSalenCorridos() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);

        assertThat(m).containsEntry("UserField1", "valor 1")
                .containsEntry("UserField2", "valor 2")
                .containsEntry("UserField3", "")
                .containsEntry("UserField30", "valor 30")
                .containsEntry("UserField100", "valor 100")
                .doesNotContainKeys("UserField21", "UserField99");

        List<String> keys = new ArrayList<>(m.keySet());
        int first = keys.indexOf("UserField1");
        int last = keys.indexOf("UserField100");
        List<String> esperado = new ArrayList<>();
        for (int i = 1; i <= 20; i++) esperado.add("UserField" + i);
        esperado.add("UserField29");
        esperado.add("UserField30");
        esperado.add("UserField100");
        assertThat(keys.subList(first, last + 1)).containsExactlyElementsOf(esperado);
    }

    @Test
    void elElementoUserField29SiempreSeEscribe() {
        Map<String, String> m = mapper.map(TestData.minimalRequest(), null, null, null);
        assertThat(m).containsEntry("UserField29", "");
    }

    // ------------------------------------------------------------ horas, fechas

    @Test
    void horasSinZonaHoraria_quitanLosSegundosCuandoVienenTresPartes() {
        UpdateOfferRequest r = TestData.request();
        r.setAppointmentStartTime("10:30:00");
        r.setAppointmentEndTime("11:00");
        Map<String, String> m = mapper.map(r, TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("ApptStartTime", "10:30").containsEntry("ApptEndTime", "11:00");
    }

    @Test
    void fechaSinZonaHoraria_seEnviaTalCual() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("ApptDate", "2026-10-25");
    }

    @Test
    void sinFechaDeCita_noSeEscribeApptDate() {
        UpdateOfferRequest r = TestData.request();
        r.setAppointmentDate("  ");
        Map<String, String> m = mapper.map(r, TestData.rqHeader(), null, null);
        assertThat(m).doesNotContainKey("ApptDate");
    }

    // ------------------------------------------------------------ CustomerNO

    @Test
    void customerNo_conPaddingRellenaALaIzquierda() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("CustomerNO", "0012345678");
    }

    @Test
    void customerNo_sinPaddingQuedaIgual() {
        props.setCustomerPadding(false);
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("CustomerNO", "12345678");
    }

    @Test
    void customerNo_vacioQuedaVacioAunConPadding() {
        UpdateOfferRequest r = TestData.request();
        r.setCustomerId("");
        Map<String, String> m = mapper.map(r, TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("CustomerNO", "");
    }

    @Test
    void customerNo_masLargoQueLaLongitudNoSeCorta() {
        props.setCustomerPaddingLength(4);
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("CustomerNO", "12345678");
    }

    // ------------------------------------------------------------ UpdateTime y zona horaria

    @Test
    void sinZonaHoraria_updateTimeEsLaFechaDelHeader() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(m).containsEntry("UpdateTime", "2026-10-26T10:54:55.852");
    }

    @Test
    void conZonaHorariaActiva_usaLosValoresConvertidos() {
        ReflectionTestUtils.setField(mapper, "translateTimeZone", true);
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), "COP_TZCOD_000", tzResult());

        assertThat(m).containsEntry("ApptDate", "2026-10-25T00:00:00-0600")
                .containsEntry("ApptStartTime", "10:31:00")
                .containsEntry("ApptEndTime", "11:01:00");
        assertThat(m.get("UpdateTime")).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}([+-]\\d{2}:\\d{2}|Z)");
    }

    @Test
    void conCodigoQueNoTerminaEnCero_noUsaLaZonaHoraria() {
        ReflectionTestUtils.setField(mapper, "translateTimeZone", true);
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), "COP_TZCOD_114", tzResult());

        assertThat(m).containsEntry("ApptDate", "2026-10-25")
                .containsEntry("ApptStartTime", "10:30")
                .containsEntry("UpdateTime", "2026-10-26T10:54:55.852");
    }

    @Test
    void conTranslateTimeZoneApagado_noUsaLaZonaHorariaAunqueElCodigoSeaCorrecto() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), "COP_TZCOD_000", tzResult());
        assertThat(m).containsEntry("ApptDate", "2026-10-25").containsEntry("ApptStartTime", "10:30");
    }

    @Test
    void conZonaHorariaActivaPeroSinResultado_usaLosValoresOriginales() {
        ReflectionTestUtils.setField(mapper, "translateTimeZone", true);
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), "COP_TZCOD_000", null);
        assertThat(m).containsEntry("ApptDate", "2026-10-25").containsEntry("ApptStartTime", "10:30");
    }

    // ------------------------------------------------------------ orden

    @Test
    void elOrdenInicialEsElDelMapperDeBW() {
        List<String> keys = new ArrayList<>(mapper.map(TestData.request(), TestData.rqHeader(), null, null).keySet());
        assertThat(keys.subList(0, 8)).containsExactly("RequestingChannel", "CampaignID", "WaveID", "AccountNO",
                "CardNO", "AssignedBy", "AgentID", "ApptDate");
        assertThat(keys.indexOf("CustomerNO")).isLessThan(keys.indexOf("UserField1"));
        assertThat(keys.indexOf("UserField100")).isLessThan(keys.indexOf("OfferAction"));
    }

    @Test
    void ningunaClaveSeRepite() {
        Map<String, String> m = mapper.map(TestData.request(), TestData.rqHeader(), null, null);
        assertThat(new ArrayList<>(m.keySet())).doesNotHaveDuplicates();
    }
}

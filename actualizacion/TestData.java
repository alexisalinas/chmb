package com.banamex.cops.update_offer.testutil;

import com.banamex.cops.update_offer.dto.ControlFieldDTO;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.log.header.ClientDetailsDTO;
import com.banamex.cops.update_offer.dto.log.header.RqHeaderDTO;

import java.time.LocalDateTime;
import java.util.List;

/** Datos de prueba compartidos. */
public final class TestData {

    private TestData() { }

    public static final String USER_TEXT =
            "{\"userField1\":\"valor 1\",\"userField2\":\"valor 2\",\"userField30\":\"valor 30\",\"userField100\":\"valor 100\"}";

    public static RqHeaderDTO rqHeader() {
        return RqHeaderDTO.builder()
                .dateTimeStamp(LocalDateTime.of(2026, 10, 26, 10, 54, 55, 852_000_000))
                .uuid("uuid-1")
                .esbUuid("uuid-120261026105455")
                .version("3.0")
                .clientDetails(ClientDetailsDTO.builder()
                        .org("Banamex").orgUnit("COPS").channelID("CAT").terminalID("1")
                        .userID("ATM").srcCountryCode("MX").destCountryCode("MX")
                        .sessionLanguageCode("ES").build())
                .build();
    }

    public static UpdateOfferRequest request() {
        UpdateOfferRequest r = new UpdateOfferRequest();
        r.setChannelCode("CAT");
        r.setCampaignId("CAMP001");
        r.setWaveId("WAVE01");
        r.setCustomerId("12345678");
        r.setAccountNumber("00112233445566");
        r.setCardNumber("1234567812345678");
        r.setOfferSource("WEB");
        r.setOfferPriority("1");
        r.setAssignedBy("USER1");
        r.setAssignedTo("AGENT1");
        r.setDepartmentId("DEP1");
        r.setBranchId("BR1");
        r.setAppointmentDate("2026-10-25");
        r.setAppointmentStartTime("10:30:00");
        r.setAppointmentEndTime("11:00:00");
        r.setOfferedNumber("Y");
        r.setOfferStatus("01");
        r.setReasonCode("R01");
        r.setReasonDescription("Prueba");
        r.setAmount("100.50");
        r.setOfferActionCode("UPD");
        r.setComments("Comentario de prueba");
        r.setUserText(USER_TEXT);
        r.setContactIndicatorNumber("5555555555");
        r.setCustomerReferenceNumber("REF123");
        r.setGiftCode("GIFT1");
        r.setGiftConfirmationNumber("GC1");
        r.setFulfillmentTrackerFlag("FT1");
        r.setLevelCode("LV1");
        r.setSchemaVersion("1.0");
        r.setWorkgroupId("WG1");
        r.setAgentName("Agente Uno");
        r.setDivisionCode("DIV1");
        r.setBranchRegionIdentifier("REG1");
        r.setAreaIdentifier("AREA1");
        r.setContactType("PHONE");
        r.setOfferCategory("CAT1");
        r.setBranchName("Sucursal");
        r.setPurpose("Proposito");
        r.setDepartmentName("COPS");
        r.setResponseChannel("SMS");
        r.setCustomerEmailAddress("a@b.com");
        r.setFulfillmentCode("SCRIPT1");
        r.setContextType(List.of(
                ControlFieldDTO.builder().contextType("controlField1").build(),
                ControlFieldDTO.builder().contextType("controlField2").build()));
        return r;
    }

    /** Request con solo los 4 campos obligatorios. */
    public static UpdateOfferRequest minimalRequest() {
        UpdateOfferRequest r = new UpdateOfferRequest();
        r.setChannelCode("CAT");
        r.setCampaignId("CAMP001");
        r.setWaveId("WAVE01");
        r.setCustomerId("12345678");
        return r;
    }

    public static UpdateOfferHeaders headers() {
        return UpdateOfferHeaders.builder()
                .uuid("uuid-1").host("api.test").channelId("CAT").countryCode("MX")
                .businessCode("COP").sid("sid-1").acceptLanguage("es-MX").build();
    }
}

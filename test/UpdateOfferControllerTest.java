package com.banamex.cops.update_offer.controller;

import com.banamex.cops.update_offer.config.HeaderValidation;
import com.banamex.cops.update_offer.dto.UpdateOfferHeaders;
import com.banamex.cops.update_offer.dto.UpdateOfferRequest;
import com.banamex.cops.update_offer.dto.UpdateOfferResponse;
import com.banamex.cops.update_offer.service.UpdateOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UpdateOfferControllerTest {

    private static final String URL = "/sd-campaing-management/v1/update-offer";
    private static final String BODY =
            "[{\"channelCode\":\"CAT\",\"campaignId\":\"C1\",\"waveId\":\"W1\",\"customerId\":\"123\"}]";

    @Mock UpdateOfferService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UpdateOfferController(service))
                .addInterceptors(new HeaderValidation(JsonMapper.builder().build()))
                .build();
    }

    private MockHttpServletRequestBuilder conTodosLosHeaders() {
        return post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("uuid", "uuid-1")
                .header("ChannelId", "CAT")
                .header("sid", "sid-1")
                .header("countryCode", "MX")
                .header("businessCode", "COP")
                .header("Accept-Language", "es-MX")
                .content(BODY);
    }

    @Test
    void sinHeaders_responde400ConElErrorEstandarYNoLlegaAlService() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("InvalidRequest"))
                .andExpect(jsonPath("$.location").value("header"))
                .andExpect(jsonPath("$.moreInfo").value(org.hamcrest.Matchers.containsString("uuid")));

        verifyNoInteractions(service);
    }

    @Test
    void faltaUnSoloHeader_loIndicaEnMoreInfo() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("uuid", "u").header("ChannelId", "CAT").header("countryCode", "MX")
                        .header("businessCode", "COP")
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.moreInfo").value("Missing required headers: sid"));

        verifyNoInteractions(service);
    }

    @Test
    void conTodoCorrecto_responde200ConLaListaDeRespuestas() throws Exception {
        UpdateOfferResponse rs = new UpdateOfferResponse();
        rs.setCode("000");
        rs.setDescription("OK");
        when(service.updateOffer(anyList(), any())).thenReturn(List.of(rs));

        mvc.perform(conTodosLosHeaders())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("000"))
                .andExpect(jsonPath("$[0].description").value("OK"));
    }

    @Test
    void pasaAlServiceLosHeadersYElBody() throws Exception {
        when(service.updateOffer(anyList(), any())).thenReturn(List.of());

        mvc.perform(conTodosLosHeaders()).andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UpdateOfferRequest>> requests = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<UpdateOfferHeaders> headers = ArgumentCaptor.forClass(UpdateOfferHeaders.class);
        verify(service).updateOffer(requests.capture(), headers.capture());

        assertThat(requests.getValue()).hasSize(1);
        assertThat(requests.getValue().get(0).getCampaignId()).isEqualTo("C1");
        assertThat(headers.getValue().getUuid()).isEqualTo("uuid-1");
        assertThat(headers.getValue().getChannelId()).isEqualTo("CAT");
        assertThat(headers.getValue().getCountryCode()).isEqualTo("MX");
        assertThat(headers.getValue().getBusinessCode()).isEqualTo("COP");
        assertThat(headers.getValue().getSid()).isEqualTo("sid-1");
        assertThat(headers.getValue().getAcceptLanguage()).isEqualTo("es-MX");
    }

    @Test
    void acceptLanguageEsOpcional() throws Exception {
        when(service.updateOffer(anyList(), any())).thenReturn(List.of());

        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .header("uuid", "u").header("ChannelId", "CAT").header("sid", "s")
                        .header("countryCode", "MX").header("businessCode", "COP")
                        .content(BODY))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateOfferHeaders> headers = ArgumentCaptor.forClass(UpdateOfferHeaders.class);
        verify(service).updateOffer(anyList(), headers.capture());
        assertThat(headers.getValue().getAcceptLanguage()).isNull();
    }

    @Test
    void sinBody_responde400() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                        .header("uuid", "u").header("ChannelId", "CAT").header("sid", "s")
                        .header("countryCode", "MX").header("businessCode", "COP"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void jsonMalFormado_responde400() throws Exception {
        mvc.perform(conTodosLosHeaders().content("[{no es json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}

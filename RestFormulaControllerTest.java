package com.banamex.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.banamex.app.controller.RestFormulaController;
import com.banamex.app.model.EvaluateFormulaRequest;
import com.banamex.app.model.EvaluateFormulaResponse;
import com.banamex.app.model.SearchEvaluateFormulaRequest;
import com.banamex.app.service.EvaluateFormulaService;

import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link RestFormulaController}.
 * This class tests the REST endpoints using MockMvc to simulate HTTP requests.
 */
@WebMvcTest(RestFormulaController.class)
class RestFormulaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EvaluateFormulaService service;

    /**
     * Test method for {@link RestFormulaController#evaluate(SearchEvaluateFormulaRequest)}.
     * Given: A valid SearchEvaluateFormulaRequest and a mocked service response.
     * When: The evaluate endpoint is called.
     * Then: HTTP 200 OK is returned with the expected JSON body.
     */
    @Test
    void evaluate_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        EvaluateFormulaRequest innerRequest = new EvaluateFormulaRequest();
        innerRequest.setChannelCode("testChannel");
        innerRequest.setEvaluationResult("testFormula");
        SearchEvaluateFormulaRequest searchRequest = new SearchEvaluateFormulaRequest();
        searchRequest.setEvaluateFormulaRequest(innerRequest);

        EvaluateFormulaResponse mockResponse = new EvaluateFormulaResponse();
        mockResponse.setCode("200");
        mockResponse.setValue("resultValue");
        mockResponse.setDescription("Success");
        when(service.invoke(any(EvaluateFormulaRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.value").value("resultValue"))
                .andExpect(jsonPath("$.description").value("Success"));

        verify(service, times(1)).invoke(any(EvaluateFormulaRequest.class));
    }

    /**
     * Test method for {@link RestFormulaController#evaluate(SearchEvaluateFormulaRequest)}.
     * Given: A SearchEvaluateFormulaRequest with a null inner EvaluateFormulaRequest.
     * When: The evaluate endpoint is called.
     * Then: HTTP 400 Bad Request is returned.
     */
    @Test
    void evaluate_ShouldReturnBadRequest_WhenEvaluateFormulaRequestIsNull() throws Exception {
        SearchEvaluateFormulaRequest searchRequest = new SearchEvaluateFormulaRequest();
        searchRequest.setEvaluateFormulaRequest(null);

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(service, never()).invoke(any(EvaluateFormulaRequest.class));
    }

    /**
     * Test method for {@link RestFormulaController#evaluate(SearchEvaluateFormulaRequest)}.
     * Given: A SearchEvaluateFormulaRequest where the inner evaluation result is null.
     * When: The evaluate endpoint is called.
     * Then: HTTP 400 Bad Request is returned.
     */
    @Test
    void evaluate_ShouldReturnBadRequest_WhenFormulaIsNull() throws Exception {
        EvaluateFormulaRequest innerRequest = new EvaluateFormulaRequest();
        innerRequest.setEvaluationResult(null);
        SearchEvaluateFormulaRequest searchRequest = new SearchEvaluateFormulaRequest();
        searchRequest.setEvaluateFormulaRequest(innerRequest);

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(service, never()).invoke(any(EvaluateFormulaRequest.class));
    }
}

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link RestFormulaController}.
 * This class tests the REST endpoints using MockMvc to simulate HTTP requests.
 */
@WebMvcTest(RestFormulaController.class)
class RestFormulaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluateFormulaService service;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test method for {@link RestFormulaController#evaluate(SearchEvaluateFormulaRequest)}.
     * /* Given: A valid SearchEvaluateFormulaRequest and a mocked service response. */
    /* When: The evaluate endpoint is called. */
    /* Then: HTTP 200 OK is returned with the expected JSON body. */
     */
    @Test
    void evaluate_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        EvaluateFormulaRequest innerRequest = new EvaluateFormulaRequest();
        innerRequest.setFormula("testFormula");
        innerRequest.setChannel("testChannel");
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
     * /* Given: A SearchEvaluateFormulaRequest with a null inner EvaluateFormulaRequest. */
    /* When: The evaluate endpoint is called. */
    /* Then: HTTP 400 Bad Request is returned. */
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
     * /* Given: A SearchEvaluateFormulaRequest where the inner formula is null. */
    /* When: The evaluate endpoint is called. */
    /* Then: HTTP 400 Bad Request is returned. */
     */
    @Test
    void evaluate_ShouldReturnBadRequest_WhenFormulaIsNull() throws Exception {
        EvaluateFormulaRequest innerRequest = new EvaluateFormulaRequest();
        innerRequest.setFormula(null);
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
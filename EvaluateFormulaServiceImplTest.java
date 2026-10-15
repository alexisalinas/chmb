package com.banamex.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.banamex.app.model.EvaluateFormulaRequest;
import com.banamex.app.model.EvaluateFormulaResponse;
import com.banamex.app.service.impl.EvaluateFormulaServiceImpl;
import com.banamex.app.task.EvaluateFormulaTask;
import com.banamex.cops.commons.errors.model.BadRequestException;

/**
 * Unit tests for {@link EvaluateFormulaServiceImpl}.
 * This class tests the service layer logic, including the main invoke method and the fallback method.
 */
@ExtendWith(MockitoExtension.class)
class EvaluateFormulaServiceImplTest {

    // Debe coincidir con la constante usada en EvaluateFormulaServiceImpl
    private static final String EVALUATE_SUCCESS_MSG = "Formula Evaluated Successfully";

    @Mock
    private EvaluateFormulaTask task;

    @InjectMocks
    private EvaluateFormulaServiceImpl service;

    /**
     * Test method for {@link EvaluateFormulaServiceImpl#invoke(EvaluateFormulaRequest)}.
     * Given: A valid request and a mocked task that returns a successful value.
     * When: The service invoke method is called.
     * Then: A successful response is returned with the expected values.
     */
    @Test
    void invoke_ShouldReturnSuccessResponse_WhenTaskSucceeds() throws Exception {
        EvaluateFormulaRequest request = new EvaluateFormulaRequest();
        request.setChannelCode("testChannel");
        request.setEvaluationResult("testFormula");
        when(task.invoke("testChannel", "testFormula")).thenReturn("resultValue");

        EvaluateFormulaResponse response = service.invoke(request);

        assertNotNull(response);
        assertEquals("", response.getCode());
        assertEquals("resultValue", response.getValue());
        assertEquals(EVALUATE_SUCCESS_MSG, response.getDescription());
        verify(task, times(1)).invoke("testChannel", "testFormula");
    }

    /**
     * Test method for {@link EvaluateFormulaServiceImpl#invoke(EvaluateFormulaRequest)}.
     * Given: A request whose evaluation result (formula) is null.
     * When: The service invoke method is called.
     * Then: A BadRequestException is thrown and the task is never invoked.
     */
    @Test
    void invoke_ShouldThrowBadRequestException_WhenFormulaIsNull() throws Exception {
        EvaluateFormulaRequest request = new EvaluateFormulaRequest();
        request.setChannelCode("testChannel");
        request.setEvaluationResult(null);

        assertThrows(BadRequestException.class, () -> service.invoke(request));

        verify(task, never()).invoke(anyString(), anyString());
    }

    /**
     * Test method for {@link EvaluateFormulaServiceImpl#evaluateFallback(EvaluateFormulaRequest, IOException)}.
     * Given: A request and an IOException.
     * When: The fallback method is invoked via reflection.
     * Then: An error response with code 500 is returned.
     */
    @Test
    void evaluateFallback_ShouldReturnErrorResponse() {
        EvaluateFormulaRequest request = new EvaluateFormulaRequest();
        IOException exception = new IOException("Simulated timeout");

        EvaluateFormulaResponse response = (EvaluateFormulaResponse) ReflectionTestUtils.invokeMethod(
                service, "evaluateFallback", request, exception);

        assertNotNull(response);
        assertEquals("500", response.getCode());
        assertEquals("", response.getValue());
        assertEquals("The evaluate formual can't by process.", response.getDescription());
    }
}

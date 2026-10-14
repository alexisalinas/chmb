import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Asegúrate de importar tus clases reales
// import com.banamex.app.task.EvaluateFormulaTask;
// import com.banamex.cops.commons.errors.model.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

class EvaluateFormulaTaskTest {

    private EvaluateFormulaTask task;

    @BeforeEach
    void setUp() {
        task = new EvaluateFormulaTask();
    }

    // ========================================================================
    // Tests para métodos públicos
    // ========================================================================

    @Test
    void isNumeric_ShouldReturnTrue_WhenValueIsNumeric() {
        assertTrue(task.isNumeric("123.45"));
        assertTrue(task.isNumeric("0"));
        assertTrue(task.isNumeric("-50"));
    }

    @Test
    void isNumeric_ShouldReturnFalse_WhenValueIsNotNumeric() {
        assertFalse(task.isNumeric("abc"));
        assertFalse(task.isNumeric("12a"));
        assertFalse(task.isNumeric(null));
        assertFalse(task.isNumeric(""));
        assertFalse(task.isNumeric("   "));
    }

    @Test
    void eval_ShouldEvaluateBasicMath() {
        assertEquals(30.0, task.eval("10+20"), 0.001);
        assertEquals(10.0, task.eval("20-10"), 0.001);
        assertEquals(200.0, task.eval("10*20"), 0.001);
        assertEquals(5.0, task.eval("20/4"), 0.001);
    }

    @Test
    void eval_ShouldRespectOperatorPrecedence() {
        assertEquals(14.0, task.eval("2+3*4"), 0.001);
        assertEquals(20.0, task.eval("(2+3)*4"), 0.001);
    }

    @Test
    void eval_ShouldHandleAdvancedOperations() {
        assertEquals(8.0, task.eval("2^3"), 0.001); // POW
        assertEquals(1.0, task.eval("10|3"), 0.001); // MOD
        assertEquals(5.0, task.eval("10%50"), 0.001); // PCT
        assertEquals(-5.0, task.eval("-5"), 0.001); // Unary minus
        assertEquals(5.0, task.eval("+5"), 0.001); // Unary plus
    }

    @Test
    void eval_ShouldThrowRuntimeException_WhenFormulaIsInvalid() {
        assertThrows(RuntimeException.class, () -> task.eval("10++"));
    }

    @Test
    void invoke_ShouldReturnEvaluatedResult_WhenFormulaUsesEnumNames() throws Exception {
        // La fórmula usa los NOMBRES del Enum, no los símbolos.
        // "10 DIV 2" -> 5.0
        String formula = "10$$DIV$$2";
        String channel = "APP";

        String result = task.invoke(channel, formula);

        assertEquals("5.0", result);
    }
    
    @Test
    void invoke_ShouldTruncateResult_WhenTruncateOperationIsPresent() throws Exception {
        // "10 DIV 3" = 3.3333...
        // Queremos truncar a 2 dígitos
        String formula = "10$$DIV$$3$$TRUNCATE$$2$$/TRUNCATE$$";
        String channel = "APP";

        String result = task.invoke(channel, formula);

        // El resultado debe ser "3.33" en lugar de "3.333..."
        assertEquals("3.33", result);
    }

    @Test
    void invoke_ShouldThrowBadRequestException_WhenFormulaIsInvalid() {
        // Fórmula con una operación que no existe en el Enum
        String formula = "10$$INVALID_OP$$2";
        String channel = "APP";

        assertThrows(BadRequestException.class, () -> {
            task.invoke(channel, formula);
        });
    }

    // ========================================================================
    // Tests para métodos privados usando ReflectionTestUtils
    // ========================================================================

    @Test
    void truncateResult_ShouldTruncateToSpecifiedDigits() {
        String result = "10.12345";
        String digits = "2";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12", truncated);
    }

    @Test
    void truncateResult_ShouldReturnFullResult_WhenDigitsAreGreaterThanAvailable() {
        String result = "10.12";
        String digits = "5";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12", truncated);
    }

    @Test
    void truncateResult_ShouldReturnIntegerPart_WhenDigitsIsZero() {
        String result = "10.12345";
        String digits = "0";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10", truncated);
    }

    @Test
    void truncateResult_ShouldReturnOriginal_WhenDigitsIsEmpty() {
        String result = "10.12345";
        String digits = "";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12345", truncated);
    }

    @Test
    void formatForChannel_ShouldFormatWithCommas_WhenChannelIsATM() {
        String value = "1234567";
        String channel = "ATM";
        String formatted = (String) ReflectionTestUtils.invokeMethod(task, "formatForChannel", value, channel);
        assertEquals("1,234,567", formatted);
    }

    @Test
    void formatForChannel_ShouldReturnOriginalValue_WhenChannelIsNotATM() {
        String value = "1234567";
        String channel = "WEB";
        String formatted = (String) ReflectionTestUtils.invokeMethod(task, "formatForChannel", value, channel);
        assertEquals("1234567", formatted);
    }

    // ========================================================================
    // Tests para evalComparison (Usando los nombres del Enum)
    // ========================================================================

    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingGT() {
        String expression = "10>5";
        String comparisonName = "GT"; 
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingEQ() {
        // OJO: El operador en el Enum es "=" (un solo igual), no "=="
        String expression = "10=10";
        String comparisonName = "EQ";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    @Test
    void evalComparison_ShouldReturnFalse_WhenComparisonIsNotMet() {
        String expression = "10<5";
        String comparisonName = "LT";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("False", result);
    }
    
    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingNEQ() {
        String expression = "10!=5";
        String comparisonName = "NEQ";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    @Test
    void evalComparison_ShouldThrowIllegalArgumentException_WhenOperatorNotFound() {
        String expression = "10 5"; // Falta el operador
        String comparisonName = "GT";
        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        });
    }
}
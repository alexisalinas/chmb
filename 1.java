import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Asegúrate de importar tus clases reales
// import com.banamex.app.model.FormulaEnum;
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
        // Probamos el método eval directamente con expresiones matemáticas
        assertEquals(30.0, task.eval("10+20"), 0.001);
        assertEquals(10.0, task.eval("20-10"), 0.001);
        assertEquals(200.0, task.eval("10*20"), 0.001);
        assertEquals(5.0, task.eval("20/4"), 0.001);
    }

    @Test
    void eval_ShouldRespectOperatorPrecedence() {
        // 2 + 3 * 4 = 14 (no 20)
        assertEquals(14.0, task.eval("2+3*4"), 0.001);
        // (2 + 3) * 4 = 20
        assertEquals(20.0, task.eval("(2+3)*4"), 0.001);
    }

    @Test
    void eval_ShouldHandleAdvancedOperations() {
        // Exponente
        assertEquals(8.0, task.eval("2^3"), 0.001);
        // Módulo
        assertEquals(1.0, task.eval("10|3"), 0.001);
        // Porcentaje (según la implementación pct(x, y) = x / 100 * y)
        // Si es 10% de 50 -> 5.0
        assertEquals(5.0, task.eval("10%50"), 0.001);
        // Unarios
        assertEquals(-5.0, task.eval("-5"), 0.001);
        assertEquals(5.0, task.eval("+5"), 0.001);
    }

    @Test
    void eval_ShouldThrowRuntimeException_WhenFormulaIsInvalid() {
        assertThrows(RuntimeException.class, () -> task.eval("10++"));
    }

    @Test
    void invoke_ShouldReturnEvaluatedResult_WhenFormulaIsValid() throws Exception {
        // Fórmula simple sin truncado: "10$$/$$2" -> 5.0
        // Recuerda que el tokenizador usa "$$" como separador
        String formula = "10$$/$$2";
        String channel = "APP";

        String result = task.invoke(channel, formula);

        // Según la lógica, si no tiene punto decimal, se le agrega ".0" (líneas 126-128)
        assertEquals("5.0", result);
    }

    @Test
    void invoke_ShouldThrowBadRequestException_WhenFormulaIsInvalid() {
        // Fórmula que causará un error en el parseo
        String formula = "$$$$";
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

    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingGT() {
        String expression = "10>5";
        String comparisonName = "GT"; // Asumiendo que el Enum se llama así

        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);

        assertEquals("True", result);
    }

    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingEQ() {
        String expression = "10==10";
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
    void evalComparison_ShouldThrowIllegalArgumentException_WhenOperatorNotFound() {
        String expression = "10 5"; // Falta el operador
        String comparisonName = "GT";

        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        });
    }
}
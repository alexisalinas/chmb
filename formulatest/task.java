import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EvaluateFormulaTask}.
 * This class tests the core business logic, including parsing, evaluation, truncation, and formatting.
 */
class EvaluateFormulaTaskTest {

    private EvaluateFormulaTask task;

    @BeforeEach
    void setUp() {
        task = new EvaluateFormulaTask();
    }

    /**
     * Test method for {@link EvaluateFormulaTask#isNumeric(String)}.
     * /* Given: A numeric string. */
    /* When: isNumeric is called. */
    /* Then: True is returned. */
     */
    @Test
    void isNumeric_ShouldReturnTrue_WhenValueIsNumeric() {
        assertTrue(task.isNumeric("123.45"));
        assertTrue(task.isNumeric("0"));
        assertTrue(task.isNumeric("-50"));
    }

    /**
     * Test method for {@link EvaluateFormulaTask#isNumeric(String)}.
     * /* Given: Non-numeric strings or null values. */
    /* When: isNumeric is called. */
    /* Then: False is returned. */
     */
    @Test
    void isNumeric_ShouldReturnFalse_WhenValueIsNotNumeric() {
        assertFalse(task.isNumeric("abc"));
        assertFalse(task.isNumeric("12a"));
        assertFalse(task.isNumeric(null));
        assertFalse(task.isNumeric(""));
        assertFalse(task.isNumeric("   "));
    }

    /**
     * Test method for {@link EvaluateFormulaTask#eval(String)}.
     * /* Given: A basic mathematical expression. */
    /* When: eval is called. */
    /* Then: The correct result is returned. */
     */
    @Test
    void eval_ShouldEvaluateBasicMath() {
        assertEquals(30.0, task.eval("10+20"), 0.001);
        assertEquals(10.0, task.eval("20-10"), 0.001);
        assertEquals(200.0, task.eval("10*20"), 0.001);
        assertEquals(5.0, task.eval("20/4"), 0.001);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#eval(String)}.
     * /* Given: Expressions with operator precedence. */
    /* When: eval is called. */
    /* Then: The correct result is returned respecting precedence. */
     */
    @Test
    void eval_ShouldRespectOperatorPrecedence() {
        assertEquals(14.0, task.eval("2+3*4"), 0.001);
        assertEquals(20.0, task.eval("(2+3)*4"), 0.001);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#eval(String)}.
     * /* Given: Expressions with advanced operations (POW, MOD, PCT, unary). */
    /* When: eval is called. */
    /* Then: The correct result is returned. */
     */
    @Test
    void eval_ShouldHandleAdvancedOperations() {
        assertEquals(8.0, task.eval("2^3"), 0.001);
        assertEquals(1.0, task.eval("10|3"), 0.001);
        assertEquals(5.0, task.eval("10%50"), 0.001);
        assertEquals(-5.0, task.eval("-5"), 0.001);
        assertEquals(5.0, task.eval("+5"), 0.001);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#eval(String)}.
     * /* Given: An invalid mathematical expression. */
    /* When: eval is called. */
    /* Then: A RuntimeException is thrown. */
     */
    @Test
    void eval_ShouldThrowRuntimeException_WhenFormulaIsInvalid() {
        assertThrows(RuntimeException.class, () -> task.eval("10++"));
    }

    /**
     * Test method for {@link EvaluateFormulaTask#invoke(String, String)}.
     * /* Given: A formula string using Enum names (e.g., "10$$DIV$$2") and a channel. */
    /* When: The invoke method is called. */
    /* Then: The evaluated result string is returned. */
     */
    @Test
    void invoke_ShouldReturnEvaluatedResult_WhenFormulaUsesEnumNames() throws Exception {
        String formula = "10$$DIV$$2";
        String channel = "APP";
        String result = task.invoke(channel, formula);
        assertEquals("5.0", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#invoke(String, String)}.
     * /* Given: A formula containing the TRUNCATE operation. */
    /* When: The invoke method is called. */
    /* Then: The result is truncated to the specified digits. */
     */
    @Test
    void invoke_ShouldTruncateResult_WhenTruncateOperationIsPresent() throws Exception {
        String formula = "10$$DIV$$3$$TRUNCATE$$2$$/TRUNCATE$$";
        String channel = "APP";
        String result = task.invoke(channel, formula);
        assertEquals("3.33", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#invoke(String, String)}.
     * /* Given: A formula with an invalid Enum name. */
    /* When: The invoke method is called. */
    /* Then: A BadRequestException is thrown. */
     */
    @Test
    void invoke_ShouldThrowBadRequestException_WhenFormulaIsInvalid() {
        String formula = "10$$INVALID_OP$$2";
        String channel = "APP";
        assertThrows(BadRequestException.class, () -> {
            task.invoke(channel, formula);
        });
    }

    /**
     * Test method for {@link EvaluateFormulaTask#truncateResult(String, String)}.
     * /* Given: A result string and the number of digits to truncate. */
    /* When: truncateResult is invoked via reflection. */
    /* Then: The truncated result string is returned. */
     */
    @Test
    void truncateResult_ShouldTruncateToSpecifiedDigits() {
        String result = "10.12345";
        String digits = "2";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12", truncated);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#truncateResult(String, String)}.
     * /* Given: A result string with fewer digits than requested. */
    /* When: truncateResult is invoked via reflection. */
    /* Then: The full result is returned. */
     */
    @Test
    void truncateResult_ShouldReturnFullResult_WhenDigitsAreGreaterThanAvailable() {
        String result = "10.12";
        String digits = "5";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12", truncated);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#truncateResult(String, String)}.
     * /* Given: A result string and zero digits. */
    /* When: truncateResult is invoked via reflection. */
    /* Then: Only the integer part is returned. */
     */
    @Test
    void truncateResult_ShouldReturnIntegerPart_WhenDigitsIsZero() {
        String result = "10.12345";
        String digits = "0";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10", truncated);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#truncateResult(String, String)}.
     * /* Given: A result string and empty digits. */
    /* When: truncateResult is invoked via reflection. */
    /* Then: The original result is returned. */
     */
    @Test
    void truncateResult_ShouldReturnOriginal_WhenDigitsIsEmpty() {
        String result = "10.12345";
        String digits = "";
        String truncated = (String) ReflectionTestUtils.invokeMethod(task, "truncateResult", result, digits);
        assertEquals("10.12345", truncated);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#formatForChannel(String, String)}.
     * /* Given: A numeric string and the "ATM" channel. */
    /* When: formatForChannel is invoked via reflection. */
    /* Then: The string is formatted with commas. */
     */
    @Test
    void formatForChannel_ShouldFormatWithCommas_WhenChannelIsATM() {
        String value = "1234567";
        String channel = "ATM";
        String formatted = (String) ReflectionTestUtils.invokeMethod(task, "formatForChannel", value, channel);
        assertEquals("1,234,567", formatted);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#formatForChannel(String, String)}.
     * /* Given: A numeric string and a non-ATM channel. */
    /* When: formatForChannel is invoked via reflection. */
    /* Then: The original string is returned. */
     */
    @Test
    void formatForChannel_ShouldReturnOriginalValue_WhenChannelIsNotATM() {
        String value = "1234567";
        String channel = "WEB";
        String formatted = (String) ReflectionTestUtils.invokeMethod(task, "formatForChannel", value, channel);
        assertEquals("1234567", formatted);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#evalComparison(String, String)}.
     * /* Given: A comparison expression and the GT operator name. */
    /* When: evalComparison is invoked via reflection. */
    /* Then: "True" is returned. */
     */
    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingGT() {
        String expression = "10>5";
        String comparisonName = "GT";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#evalComparison(String, String)}.
     * /* Given: A comparison expression and the EQ operator name. */
    /* When: evalComparison is invoked via reflection. */
    /* Then: "True" is returned. */
     */
    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingEQ() {
        String expression = "10=10";
        String comparisonName = "EQ";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#evalComparison(String, String)}.
     * /* Given: A comparison expression that evaluates to false and the LT operator name. */
    /* When: evalComparison is invoked via reflection. */
    /* Then: "False" is returned. */
     */
    @Test
    void evalComparison_ShouldReturnFalse_WhenComparisonIsNotMet() {
        String expression = "10<5";
        String comparisonName = "LT";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("False", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#evalComparison(String, String)}.
     * /* Given: A comparison expression and the NEQ operator name. */
    /* When: evalComparison is invoked via reflection. */
    /* Then: "True" is returned. */
     */
    @Test
    void evalComparison_ShouldEvaluateCorrectly_WhenUsingNEQ() {
        String expression = "10!=5";
        String comparisonName = "NEQ";
        String result = (String) ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        assertEquals("True", result);
    }

    /**
     * Test method for {@link EvaluateFormulaTask#evalComparison(String, String)}.
     * /* Given: An expression missing the operator. */
    /* When: evalComparison is invoked via reflection. */
    /* Then: An IllegalArgumentException is thrown. */
     */
    @Test
    void evalComparison_ShouldThrowIllegalArgumentException_WhenOperatorNotFound() {
        String expression = "10 5";
        String comparisonName = "GT";
        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(task, "evalComparison", expression, comparisonName);
        });
    }
}
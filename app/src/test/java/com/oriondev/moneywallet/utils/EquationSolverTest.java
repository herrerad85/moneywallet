package com.oriondev.moneywallet.utils;

import com.oriondev.moneywallet.model.CurrencyUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class EquationSolverTest {

    @InjectMocks
    private EquationSolver equationSolver;

    private void mockCurrencyUnit(int decimals) {
        equationSolver.mCurrency = new CurrencyUnit("", "", "", decimals);
    }

    /*
    Order of tests:
    For each supported currency based on decimal count:
    - Test zero value input
    - Test positive value input with expected number of decimals
    - Test negative value input with expected number of decimals
    - Test positive value input with the last decimal missing (except for zero decimal currency)
    - Test negative value input with the last decimal missing (except for zero decimal currency)
    - Test positive value input with an extra decimal
    - Test negative value input with an extra decimal
     */

    // Zero Decimal Currency

    @Test
    public void testGetResult_zeroDecimalCurrencyAndZeroValue() {
        mockCurrencyUnit(0);
        equationSolver.mFirstNumber = "0";

        long result = equationSolver.getResult();

        assertEquals(result, 0L);
    }

    @Test
    public void testGetResult_zeroDecimalCurrencyAndPositiveValue() {
        mockCurrencyUnit(0);
        equationSolver.mFirstNumber = "64";

        long result = equationSolver.getResult();

        assertEquals(result, 64L);
    }

    @Test
    public void testGetResult_zeroDecimalCurrencyAndNegativeValue() {
        mockCurrencyUnit(0);
        equationSolver.mFirstNumber = "-64";

        long result = equationSolver.getResult();

        assertEquals(result, -64L);
    }

    @Test
    public void testGetResult_zeroDecimalCurrencyAndPositiveValueWithExtraDecimal() {
        mockCurrencyUnit(0);
        equationSolver.mFirstNumber = "64.9";

        long result = equationSolver.getResult();

        assertEquals(result, 64L);
    }

    @Test
    public void testGetResult_zeroDecimalCurrencyAndNegativeValueWithExtraDecimal() {
        mockCurrencyUnit(0);
        equationSolver.mFirstNumber = "-64.9";

        long result = equationSolver.getResult();

        assertEquals(result, -64L);
    }

    // One Decimal Currency

    @Test
    public void testGetResult_oneDecimalCurrencyAndZeroValue() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "0";

        long result = equationSolver.getResult();

        assertEquals(result, 0L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndPositiveValue() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "64.9";

        long result = equationSolver.getResult();

        assertEquals(result, 649L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndNegativeValue() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "-64.9";

        long result = equationSolver.getResult();

        assertEquals(result, -649L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndPositiveValueWithMissingLastDecimal() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "64";

        long result = equationSolver.getResult();

        assertEquals(result, 640L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndNegativeValueWithMissingLastDecimal() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "-64";

        long result = equationSolver.getResult();

        assertEquals(result, -640L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndPositiveValueWithExtraDecimal() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "64.99";

        long result = equationSolver.getResult();

        assertEquals(result, 649L);
    }

    @Test
    public void testGetResult_oneDecimalCurrencyAndNegativeValueWithExtraDecimal() {
        mockCurrencyUnit(1);
        equationSolver.mFirstNumber = "-64.99";

        long result = equationSolver.getResult();

        assertEquals(result, -649L);
    }

    // Two Decimal Currency

    @Test
    public void testGetResult_towDecimalCurrencyAndZeroValue() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "0";

        long result = equationSolver.getResult();

        assertEquals(result, 0L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndPositiveValue() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "64.99";

        long result = equationSolver.getResult();

        assertEquals(result, 6499L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndNegativeValue() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "-64.99";

        long result = equationSolver.getResult();

        assertEquals(result, -6499L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndPositiveValueWithMissingLastDecimal() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "64.9";

        long result = equationSolver.getResult();

        assertEquals(result, 6490L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndNegativeValueWithMissingLastDecimal() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "-64.9";

        long result = equationSolver.getResult();

        assertEquals(result, -6490L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndPositiveValueWithExtraDecimal() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "64.999";

        long result = equationSolver.getResult();

        assertEquals(result, 6499L);
    }

    @Test
    public void testGetResult_towDecimalCurrencyAndNegativeValueWithExtraDecimal() {
        mockCurrencyUnit(2);
        equationSolver.mFirstNumber = "-64.999";

        long result = equationSolver.getResult();

        assertEquals(result, -6499L);
    }


    // Three Decimal Currency

    @Test
    public void testGetResult_threeDecimalCurrencyAndZeroValue() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "0";

        long result = equationSolver.getResult();

        assertEquals(result, 0L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndPositiveValue() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "64.999";

        long result = equationSolver.getResult();

        assertEquals(result, 64999L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndNegativeValue() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "-64.999";

        long result = equationSolver.getResult();

        assertEquals(result, -64999L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndPositiveValueWithMissingLastDecimal() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "64.99";

        long result = equationSolver.getResult();

        assertEquals(result, 64990L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndNegativeValueWithMissingLastDecimal() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "-64.99";

        long result = equationSolver.getResult();

        assertEquals(result, -64990L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndPositiveValueWithExtraDecimal() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "64.9999";

        long result = equationSolver.getResult();

        assertEquals(result, 64999L);
    }

    @Test
    public void testGetResult_threeDecimalCurrencyAndNegativeValueWithExtraDecimal() {
        mockCurrencyUnit(3);
        equationSolver.mFirstNumber = "-64.9999";

        long result = equationSolver.getResult();

        assertEquals(result, -64999L);
    }

    // Arithmetic

    private void type(String keys) {
        for (char key : keys.toCharArray()) {
            if (key == '.') {
                equationSolver.appendPoint();
            } else {
                equationSolver.appendNumber(String.valueOf(key));
            }
        }
    }

    private void enter(String first, EquationSolver.Operation operation, String second) {
        type(first);
        equationSolver.appendOperation(operation);
        type(second);
    }

    @Test
    public void testExecute_addition() {
        enter("2.75", EquationSolver.Operation.ADDITION, "0.25");

        assertTrue(equationSolver.execute(false));
        assertEquals("3.00", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_subtraction() {
        enter("2.75", EquationSolver.Operation.SUBTRACTION, "0.25");

        assertTrue(equationSolver.execute(false));
        assertEquals("2.50", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_multiplication() {
        enter("2.5", EquationSolver.Operation.MULTIPLICATION, "4");

        assertTrue(equationSolver.execute(false));
        assertEquals("10", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_divisionOfIntegerDividendKeepsTheFraction() {
        mockCurrencyUnit(2);
        enter("10", EquationSolver.Operation.DIVISION, "4");

        assertTrue(equationSolver.execute(false));
        assertEquals("2.5", equationSolver.mFirstNumber);
        assertEquals(250L, equationSolver.getResult());
    }

    @Test
    public void testExecute_divisionIsIndependentOfTheDividendScale() {
        // The displayed string still carries the dividend's trailing zeros, the stored value is
        // what has to stop depending on how the dividend was typed.
        mockCurrencyUnit(2);

        enter("160", EquationSolver.Operation.DIVISION, "25");
        assertEquals(640L, equationSolver.getResult());

        equationSolver.clear();
        enter("160.00", EquationSolver.Operation.DIVISION, "25");
        assertEquals(640L, equationSolver.getResult());
    }

    @Test
    public void testExecute_chainedDivisionAndMultiplicationDoNotRoundBetweenSteps() {
        // Narrowing the quotient to the currency scale inside execute would round every step of a
        // chain: this one would come back as 8 with a zero decimal currency.
        mockCurrencyUnit(0);
        type("10.0");
        equationSolver.appendOperation(EquationSolver.Operation.DIVISION);
        type("4");
        equationSolver.appendOperation(EquationSolver.Operation.MULTIPLICATION);
        type("4");

        assertEquals(10L, equationSolver.getResult());
    }

    @Test
    public void testExecute_chainedDivisionKeepsTheDigitsBelowTheCurrencyScale() {
        mockCurrencyUnit(2);
        type("1.234");
        equationSolver.appendOperation(EquationSolver.Operation.DIVISION);
        type("2");
        equationSolver.appendOperation(EquationSolver.Operation.MULTIPLICATION);
        type("2");

        assertEquals(123L, equationSolver.getResult());
        assertEquals("1.234", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_recurringDivisionKeepsSixteenSignificantDigits() {
        // The second case is the one that fails if the division stops rounding half up or half
        // even and starts truncating.
        enter("1", EquationSolver.Operation.DIVISION, "3");
        assertTrue(equationSolver.execute(false));
        assertEquals("0.3333333333333333", equationSolver.mFirstNumber);

        equationSolver.clear();
        enter("2", EquationSolver.Operation.DIVISION, "3");
        assertTrue(equationSolver.execute(false));
        assertEquals("0.6666666666666667", equationSolver.mFirstNumber);
    }

    @Test
    public void testGetResult_recurringDivisionTruncatesAtTheCurrencyScale() {
        // A quotient that does not terminate keeps its digits on the display and loses the ones
        // below the currency scale in the conversion, the same way a typed 6.669 does.
        mockCurrencyUnit(2);
        enter("20", EquationSolver.Operation.DIVISION, "3");

        assertTrue(equationSolver.execute(false));
        assertEquals("6.666666666666667", equationSolver.mFirstNumber);
        assertEquals(666L, equationSolver.getResult());
    }

    @Test
    public void testExecute_multiplicationStaysInPlainNotation() {
        enter("0.001", EquationSolver.Operation.MULTIPLICATION, "0.0001");

        assertTrue(equationSolver.execute(false));
        assertEquals("0.0000001", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_divisionStaysInPlainNotation() {
        enter("100", EquationSolver.Operation.DIVISION, "0.5");

        assertTrue(equationSolver.execute(false));
        assertEquals("200", equationSolver.mFirstNumber);
    }

    @Test
    public void testExecute_divisionByZeroFailsAndLeavesTheEquationAlone() {
        mockCurrencyUnit(2);
        enter("10", EquationSolver.Operation.DIVISION, "0");

        assertFalse(equationSolver.execute(false));
        assertEquals("10", equationSolver.mFirstNumber);
        assertTrue(equationSolver.isPendingOperation());
    }

    @Test
    public void testGetResult_twoDecimalCurrencyAndDividedValue() {
        mockCurrencyUnit(2);
        enter("10", EquationSolver.Operation.DIVISION, "4");

        long result = equationSolver.getResult();

        assertEquals(250L, result);
    }
}
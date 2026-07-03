package com.oriondev.moneywallet.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;

/**
 * Pure JVM tests for the canonical money scaling arithmetic. No Android
 * dependencies are needed because {@link MoneyScale} is plain Java.
 */
public class MoneyScaleTest {

    // long minor units -> human amount -> long minor units must round trip exactly

    @Test
    public void roundTrip_zeroDecimalCurrency() {
        assertRoundTrip(0L, 0);
        assertRoundTrip(64L, 0);
        assertRoundTrip(-64L, 0);
        assertRoundTrip(123456789L, 0);
    }

    @Test
    public void roundTrip_twoDecimalCurrency() {
        assertRoundTrip(0L, 2);
        assertRoundTrip(6499L, 2);
        assertRoundTrip(-6499L, 2);
        assertRoundTrip(100L, 2);
        assertRoundTrip(-1L, 2);
    }

    @Test
    public void roundTrip_threeDecimalCurrency() {
        assertRoundTrip(0L, 3);
        assertRoundTrip(64999L, 3);
        assertRoundTrip(-64999L, 3);
        assertRoundTrip(1L, 3);
    }

    @Test
    public void roundTrip_largeValueNearDoublePrecisionEdge() {
        // 2^53 is the last integer a double can represent exactly. Values just
        // beyond it are what the old "money / Math.pow(10, d)" display path and
        // the old "(long)(money * Math.pow(10, offset))" migration path corrupt.
        long edge = 9007199254740993L; // 2^53 + 1, not representable as a double
        assertRoundTrip(edge, 0);
        assertRoundTrip(edge, 2);
        assertRoundTrip(-edge, 3);
        assertRoundTrip(Long.MAX_VALUE, 0);
    }

    @Test
    public void toHumanAmount_producesExactDecimalString() {
        assertEquals("64.99", MoneyScale.toHumanAmount(6499L, 2).toPlainString());
        assertEquals("-64.99", MoneyScale.toHumanAmount(-6499L, 2).toPlainString());
        assertEquals("0.001", MoneyScale.toHumanAmount(1L, 3).toPlainString());
        assertEquals("64", MoneyScale.toHumanAmount(64L, 0).toPlainString());
        // the old double path would have rendered this as a lossy 9.007199254740992E15
        assertEquals("9007199254740993", MoneyScale.toHumanAmount(9007199254740993L, 0).toPlainString());
    }

    // human amount -> long minor units truncates toward zero (the policy every
    // call site relied on, and the one EquationSolverTest pins)

    @Test
    public void toMinorUnits_truncatesExtraFractionTowardZero() {
        assertEquals(6499L, MoneyScale.toMinorUnits(new BigDecimal("64.999"), 2));
        assertEquals(-6499L, MoneyScale.toMinorUnits(new BigDecimal("-64.999"), 2));
        assertEquals(6490L, MoneyScale.toMinorUnits(new BigDecimal("64.9"), 2));
        assertEquals(64L, MoneyScale.toMinorUnits(new BigDecimal("64.9"), 0));
        assertEquals(-64L, MoneyScale.toMinorUnits(new BigDecimal("-64.9"), 0));
    }

    @Test
    public void toMinorUnits_nullAmountIsZero() {
        assertEquals(0L, MoneyScale.toMinorUnits(null, 2));
    }

    // exchange rate conversion across differing decimal counts

    @Test
    public void convert_zeroDecimalToTwoDecimal() {
        // 100000 JPY at 0.0068 USD per JPY = 680.00 USD = 68000 cents
        assertEquals(68000L, MoneyScale.convert(100000L, 0, 2, 0.0068d));
    }

    @Test
    public void convert_twoDecimalToZeroDecimal() {
        // 68000 cents (680.00 USD) at 147 JPY per USD = 99960 JPY
        assertEquals(99960L, MoneyScale.convert(68000L, 2, 0, 147.0d));
    }

    @Test
    public void convert_appliesRateExactlyOnce() {
        // 5.00 USD doubled must be 10.00 USD (1000 cents), not 5.00 and not 20.00
        assertEquals(1000L, MoneyScale.convert(500L, 2, 2, 2.0d));
    }

    @Test
    public void convert_sameCurrencyUnitRateIsIdentity() {
        assertEquals(1234L, MoneyScale.convert(1234L, 2, 2, 1.0d));
        assertEquals(0L, MoneyScale.convert(0L, 3, 3, 1.0d));
    }

    // rescale (used by MoneyFormatter.normalize, which feeds the FIX_MONEY_DECIMALS
    // database migration and the legacy database import)

    @Test
    public void rescale_matchesOldDoublePathWhereDoubleWasExact() {
        // widening the scale (offset > 0) is exact integer growth
        assertEquals(6490L, MoneyScale.rescale(649L, 1));
        assertEquals(64900L, MoneyScale.rescale(649L, 2));
        // narrowing the scale (offset < 0) truncates toward zero
        assertEquals(64L, MoneyScale.rescale(649L, -1));
        assertEquals(-64L, MoneyScale.rescale(-649L, -1));
        assertEquals(0L, MoneyScale.rescale(1234L, 0) - 1234L);
    }

    @Test
    public void rescale_isExactWhereOldDoublePathCorrupted() {
        // The migration used (long)(money * Math.pow(10, offset)). With offset 0
        // that is (long)(money * 1.0), which still routes money through a double
        // and loses the low bit past 2^53. The exact path must not.
        long money = 9007199254740993L; // 2^53 + 1
        long oldDoublePath = (long) (money * Math.pow(10d, 0));
        assertEquals(9007199254740992L, oldDoublePath); // the pre-existing corruption
        assertEquals(money, MoneyScale.rescale(money, 0)); // exact
        assertNotEquals(oldDoublePath, MoneyScale.rescale(money, 0));
    }

    // regression: the CSV importer used new BigDecimal(Math.pow(10, decimals))
    // as the scaling multiplier. That is the classic double-constructor pattern.

    @Test
    public void csvImporter_canonicalMatchesOldPathForSupportedDecimals() {
        // For the decimals the app actually supports (0..3), Math.pow(10, d) is an
        // exact double, so the fix does not change any stored value. Lock that in.
        String[] amounts = {"0", "12345.67", "-0.99", "1000000.5", "70.07"};
        for (int d = 0; d <= 3; d++) {
            for (String amount : amounts) {
                BigDecimal money = new BigDecimal(amount);
                long oldPath = money.multiply(new BigDecimal(Math.pow(10, d))).longValue();
                long canonical = MoneyScale.toMinorUnits(money, d);
                assertEquals("amount=" + amount + " decimals=" + d, oldPath, canonical);
            }
        }
        assertEquals(1234567L, MoneyScale.toMinorUnits(new BigDecimal("12345.67"), 2));
    }

    @Test
    public void csvImporter_doubleConstructorMultiplierIsCorruptWhereCanonicalIsExact() {
        // 10^23 is the first power of ten that is not an exact double. new
        // BigDecimal(Math.pow(10, 23)) captures the binary tail and yields
        // 99999999999999991611392 instead of 10^23, so the old multiply would
        // mangle the amount. The canonical movePointRight path is exact.
        BigDecimal amount = new BigDecimal("1");
        BigDecimal buggyMultiplier = new BigDecimal(Math.pow(10, 23));
        BigDecimal buggyResult = amount.multiply(buggyMultiplier);
        BigDecimal canonicalResult = amount.movePointRight(23);
        BigDecimal exact = BigDecimal.TEN.pow(23);

        assertNotEquals(exact, buggyResult);
        assertTrue(buggyResult.compareTo(new BigDecimal("99999999999999991611392")) == 0);
        assertTrue(canonicalResult.compareTo(exact) == 0);
    }

    private static void assertRoundTrip(long minorUnits, int decimals) {
        BigDecimal human = MoneyScale.toHumanAmount(minorUnits, decimals);
        assertEquals("round trip minorUnits=" + minorUnits + " decimals=" + decimals,
                minorUnits, MoneyScale.toMinorUnits(human, decimals));
    }
}

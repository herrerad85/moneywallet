package com.oriondev.moneywallet.storage.database.data.csv;

import com.oriondev.moneywallet.utils.DateUtils;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * The importer's row reading talks to a content resolver and to an Android static, so it cannot be
 * driven from a JVM test. The two pieces that need neither are here. What is not covered, and was
 * checked on an emulator instead: the line number the refusals are prefixed with.
 */
public class CSVDataImporterTest {

    @Test
    public void aDatetimeKeepsItsTime() {
        assertEquals(at(2026, 8, 12, 9, 30, 15), CSVDataImporter.parseDatetime("2026-08-12 09:30:15"));
    }

    /**
     * Midnight in the zone the device is in. On the handful of days a zone skips midnight for
     * daylight saving, the lenient parse answers 01:00 on the same day instead.
     */
    @Test
    public void aDateOnItsOwnIsMidnight() {
        assertEquals(at(2026, 8, 12, 0, 0, 0), CSVDataImporter.parseDatetime("2026-08-12"));
    }

    /**
     * A fact about DateUtils, not a check on the importer: nothing done to the importer can turn
     * this red. It is here because it is the reason parseDatetime cannot just read the date and
     * stop. The test that goes red if the comparison in parseDatetime is removed is
     * aDayThatDoesNotExistIsRefusedWhenTheRowHasNoTime below, checked by removing it.
     */
    @Test
    public void theDateParseDropsTheTimeFromAFullDatetime() {
        assertEquals(at(2026, 8, 12, 0, 0, 0),
                DateUtils.getDateFromSQLDateString("2026-08-12 09:30:15"));
    }

    /**
     * The date parse stops at the first character it cannot use, so the first three of these
     * would read as a date with whatever follows it dropped, and the fourth as August 1st. None
     * is ten characters, so in practice the length refuses all four before the comparison is
     * reached; take the length away and the comparison refuses all four on its own. All four were
     * refused outright before a date on its own was accepted.
     */
    @Test
    public void aDateWithAnythingElseOnItIsRefused() {
        refuses("2026-08-12T09:30:15");
        refuses("2026-08-12 09:30");
        refuses("2026-08-12garbage");
        refuses("2026-8-1");
    }

    /**
     * Only when the row carries no time. A day past the end of its month is still rolled when a
     * time is written after it, because that value goes through the datetime parse, which is
     * untouched here and behaves as it always has: "2026-02-30 00:00:00" imports as March 2nd.
     * Tightening that would stop files importing that import today.
     */
    @Test
    public void aDayThatDoesNotExistIsRefusedWhenTheRowHasNoTime() {
        refuses("2026-02-30");
        refuses("2026-13-45");
        refuses("2026-00-00");
    }

    /**
     * A year past 9999 is written back out at its own length, so it compares equal to itself and
     * the comparison alone lets it through. It is the one thing the length is there for. A short
     * year is not: it is padded out to four digits, so "926-08-12" fails the comparison and is
     * refused whether the length is checked or not.
     */
    @Test
    public void aYearPastFourDigitsIsRefused() {
        refuses("12026-08-12");
        refuses("926-08-12");
    }

    @Test
    public void aMissingColumnAndAnEmptyCellAreDifferentMessages() {
        Map<String, String> row = new HashMap<>();
        row.put("wallet", "Probe");
        row.put("currency", "  ");
        assertEquals("Probe", CSVDataImporter.required(row, "wallet"));
        assertEquals("no money column was found. Check the header line",
                messageFrom(row, "money"));
        assertEquals("the currency is empty, and every row needs one", messageFrom(row, "currency"));
    }

    private static String messageFrom(Map<String, String> row, String column) {
        try {
            CSVDataImporter.required(row, column);
            fail(column + " has to be refused");
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @Test
    public void theMessageNamesTheValueAndBothFormats() {
        try {
            CSVDataImporter.parseDatetime("12/08/2026");
            fail("a date in another format has to be refused");
        } catch (RuntimeException e) {
            assertEquals("this message is what the failure dialog is built around",
                    "the datetime \"12/08/2026\" is not a real date as yyyy-MM-dd HH:mm:ss "
                            + "or yyyy-MM-dd",
                    e.getMessage());
        }
    }

    /**
     * Checks the whole message, not just that something was thrown. Checking only that the message
     * names the value is not enough: the exception DateUtils throws names it too, so these tests
     * would stay green with the date reading taken out altogether.
     */
    private static void refuses(String value) {
        try {
            Date parsed = CSVDataImporter.parseDatetime(value);
            fail(value + " has to be refused, but it read as " + DateUtils.getSQLDateTimeString(parsed));
        } catch (RuntimeException expected) {
            assertEquals("refused, but not by the importer's own check",
                    "the datetime \"" + value + "\" is not a real date as yyyy-MM-dd HH:mm:ss "
                            + "or yyyy-MM-dd",
                    expected.getMessage());
        }
    }

    private static Date at(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}

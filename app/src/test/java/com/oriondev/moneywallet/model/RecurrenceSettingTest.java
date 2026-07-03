/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.moneywallet.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.oriondev.moneywallet.utils.DateUtils;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Host-side coverage for the pure occurrence computation extracted from
 * {@code RecurrenceHandlerIntentService}. {@link RecurrenceSetting#computeOccurrences(String, Date, Date)}
 * carries the exact rule that decides when a recurring transaction or transfer silently enters the
 * ledger, so every semantic the service relied on is pinned here: the seed is the inclusive first
 * instance, the window boundary is {@code !instance.after(now)}, an exhausted or invalid rule yields
 * a null next occurrence, and month-end rules skip absent days without crashing.
 */
public class RecurrenceSettingTest {

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    /** Builds a Date at the given calendar day (month is 0-based, as everywhere in this codebase). */
    private static Date day(int year, int month, int dayOfMonth) {
        return DateUtils.getDate(year, month, dayOfMonth);
    }

    /** Asserts a computed Date falls on the expected calendar day, ignoring time-of-day. */
    private static void assertDay(Date date, int year, int month, int dayOfMonth) {
        assertNotNull(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(dayOfMonth, calendar.get(Calendar.DAY_OF_MONTH));
    }

    // ---------------------------------------------------------------------------------------------
    // instantiability
    // ---------------------------------------------------------------------------------------------

    /**
     * The model class carries android.os.Parcelable plumbing, so first prove it loads and works in a
     * plain JVM test: construct it and exercise the legacy getNextOccurrence the importer depends on.
     */
    @Test
    public void recurrenceSettingIsInstantiableInPlainJvm() {
        RecurrenceSetting setting = new RecurrenceSetting(day(2020, 0, 1), "FREQ=DAILY");
        assertNotNull(setting.getRule());
        Date next = setting.getNextOccurrence(day(2020, 0, 1));
        assertDay(next, 2020, 0, 2);
    }

    // ---------------------------------------------------------------------------------------------
    // daily / weekly / monthly advancement between a seed and a now
    // ---------------------------------------------------------------------------------------------

    @Test
    public void dailyAdvancementCollectsEveryDayUpToNow() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=DAILY", day(2020, 0, 1), day(2020, 0, 5));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(5, dates.size());
        assertDay(dates.get(0), 2020, 0, 1);
        assertDay(dates.get(1), 2020, 0, 2);
        assertDay(dates.get(2), 2020, 0, 3);
        assertDay(dates.get(3), 2020, 0, 4);
        assertDay(dates.get(4), 2020, 0, 5);
        assertDay(update.getLastOccurrence(), 2020, 0, 5);
        assertDay(update.getNextOccurrence(), 2020, 0, 6);
    }

    @Test
    public void weeklyAdvancementSteppsSevenDays() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=WEEKLY", day(2020, 0, 1), day(2020, 0, 20));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(3, dates.size());
        assertDay(dates.get(0), 2020, 0, 1);
        assertDay(dates.get(1), 2020, 0, 8);
        assertDay(dates.get(2), 2020, 0, 15);
        assertDay(update.getLastOccurrence(), 2020, 0, 15);
        assertDay(update.getNextOccurrence(), 2020, 0, 22);
    }

    @Test
    public void monthlyAdvancementSteppsOneMonth() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=MONTHLY", day(2020, 0, 15), day(2020, 3, 20));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(4, dates.size());
        assertDay(dates.get(0), 2020, 0, 15);
        assertDay(dates.get(1), 2020, 1, 15);
        assertDay(dates.get(2), 2020, 2, 15);
        assertDay(dates.get(3), 2020, 3, 15);
        assertDay(update.getLastOccurrence(), 2020, 3, 15);
        assertDay(update.getNextOccurrence(), 2020, 4, 15);
    }

    // ---------------------------------------------------------------------------------------------
    // boundary exactness: an occurrence exactly at "now" is included, the day after is not
    // ---------------------------------------------------------------------------------------------

    @Test
    public void occurrenceExactlyAtNowIsIncludedNextDayIsNext() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=DAILY", day(2020, 5, 10), day(2020, 5, 10));
        List<Date> dates = update.getOccurrenceDates();
        // seed == now: !instance.after(now) is true, so the seed day is due
        assertEquals(1, dates.size());
        assertDay(dates.get(0), 2020, 5, 10);
        assertDay(update.getLastOccurrence(), 2020, 5, 10);
        // the first instance strictly after now stops the walk and becomes the next occurrence
        assertDay(update.getNextOccurrence(), 2020, 5, 11);
    }

    // ---------------------------------------------------------------------------------------------
    // month-end rule advances as rfc5545 dictates without skipping or crashing
    // ---------------------------------------------------------------------------------------------

    @Test
    public void monthEndRuleSkipsShortMonthsWithoutCrashing() {
        // BYMONTHDAY=31 has no instance in Feb/Apr/Jun; org.dmfs simply omits those months
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=MONTHLY;BYMONTHDAY=31", day(2020, 0, 31), day(2020, 6, 15));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(3, dates.size());
        assertDay(dates.get(0), 2020, 0, 31); // Jan 31
        assertDay(dates.get(1), 2020, 2, 31); // Mar 31 (Feb skipped)
        assertDay(dates.get(2), 2020, 4, 31); // May 31 (Apr skipped)
        assertDay(update.getLastOccurrence(), 2020, 4, 31);
        assertDay(update.getNextOccurrence(), 2020, 6, 31); // Jul 31 (Jun skipped)
    }

    // ---------------------------------------------------------------------------------------------
    // a rule with an end (COUNT / UNTIL) that exhausts inside the window -> null next occurrence
    // ---------------------------------------------------------------------------------------------

    @Test
    public void countRuleExhaustingInsideWindowLeavesNoNextOccurrence() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=DAILY;COUNT=3", day(2020, 1, 1), day(2020, 1, 28));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(3, dates.size());
        assertDay(dates.get(0), 2020, 1, 1);
        assertDay(dates.get(1), 2020, 1, 2);
        assertDay(dates.get(2), 2020, 1, 3);
        assertDay(update.getLastOccurrence(), 2020, 1, 3);
        assertNull(update.getNextOccurrence());
    }

    @Test
    public void untilRuleExhaustingInsideWindowLeavesNoNextOccurrence() {
        // UNTIL is inclusive per RFC 5545; the Jan 22 instance is kept, then the rule is done
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=WEEKLY;UNTIL=20200122", day(2020, 0, 1), day(2020, 11, 31));
        List<Date> dates = update.getOccurrenceDates();
        assertEquals(4, dates.size());
        assertDay(dates.get(0), 2020, 0, 1);
        assertDay(dates.get(1), 2020, 0, 8);
        assertDay(dates.get(2), 2020, 0, 15);
        assertDay(dates.get(3), 2020, 0, 22);
        assertDay(update.getLastOccurrence(), 2020, 0, 22);
        assertNull(update.getNextOccurrence());
    }

    // ---------------------------------------------------------------------------------------------
    // a window with nothing due returns empty and leaves the seed as the (unchanged) next occurrence
    // ---------------------------------------------------------------------------------------------

    @Test
    public void seedInTheFutureYieldsNoOccurrencesAndKeepsSeedAsNext() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "FREQ=MONTHLY", day(2020, 4, 10), day(2020, 4, 1));
        assertTrue(update.getOccurrenceDates().isEmpty());
        // nothing was inserted, so last occurrence stays at the seed and next stays at the seed too
        assertDay(update.getLastOccurrence(), 2020, 4, 10);
        assertDay(update.getNextOccurrence(), 2020, 4, 10);
    }

    // ---------------------------------------------------------------------------------------------
    // an unparseable rule yields no occurrences and a null next occurrence (no daily fallback)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void invalidRuleYieldsNoOccurrencesAndNullNext() {
        RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(
                "this is not a rule", day(2020, 2, 3), day(2020, 2, 30));
        assertTrue(update.getOccurrenceDates().isEmpty());
        assertDay(update.getLastOccurrence(), 2020, 2, 3);
        assertNull(update.getNextOccurrence());
    }
}

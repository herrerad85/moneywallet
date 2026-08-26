/*
 * Copyright (c) 2026.
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

package com.oriondev.moneywallet.ui.fragment.multipanel;

import com.oriondev.moneywallet.ui.view.calendar.TimelineView;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

/**
 * The calendar strip marks a day when its own key for that cell equals a key built from a date
 * SQLite returned. The two are built by different code from different material, the cell from a
 * {@link Calendar} and the mark from a string, so the month has to be counted the same way on
 * both sides or every mark lands eleven months from where it belongs.
 *
 * A run of this cannot see the strip. What it pins is that the two keys agree.
 */
public class CalendarDayKeyTest {

    @Test
    public void keyMatchesTheCellForTheSameDay() {
        assertEquals(TimelineView.dayKey(2026, Calendar.AUGUST, 3),
                CalendarMultiPanelFragment.dayKey("2026-08-03"));
    }

    @Test
    public void januaryAndDecemberAreTheMonthsAtTheEnds() {
        assertEquals(TimelineView.dayKey(2027, Calendar.JANUARY, 1),
                CalendarMultiPanelFragment.dayKey("2027-01-01"));
        assertEquals(TimelineView.dayKey(2026, Calendar.DECEMBER, 31),
                CalendarMultiPanelFragment.dayKey("2026-12-31"));
    }

    @Test
    public void twoDaysOfTheSameMonthGetDifferentKeys() {
        assertEquals(CalendarMultiPanelFragment.dayKey("2026-08-03") + 1,
                CalendarMultiPanelFragment.dayKey("2026-08-04"));
    }

    @Test
    public void aDateSqliteCouldNotReadIsNoDay() {
        assertEquals(CalendarMultiPanelFragment.NO_DAY, CalendarMultiPanelFragment.dayKey(null));
        assertEquals(CalendarMultiPanelFragment.NO_DAY, CalendarMultiPanelFragment.dayKey(""));
        assertEquals(CalendarMultiPanelFragment.NO_DAY,
                CalendarMultiPanelFragment.dayKey("2026-08-03 09:00:00"));
    }
}

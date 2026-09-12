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

package com.oriondev.moneywallet.ui.view.calendar;

import android.content.Context;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The numbers the two calendar strips draw, in the digits of the language the app is in. Both
 * strips used to build them without a locale, so a Persian screen drew Latin day numbers and a
 * Latin year beside a localized month name, above a list whose own dates and amounts were in
 * Persian digits.
 *
 * Persian is the language under test because it is the one bundled language whose digits are not
 * the Latin ones. It is not the only language that can reach these labels, since below Android 13
 * the app is handed the device locale list whole, so on an Arabic phone the numbers are Arabic
 * while every string around them is English.
 *
 * Four of these read the digits out of a label, so a number moving to another locale aware call
 * still passes. The other two read a text size and a label's width, and native graphics is what
 * makes those two mean anything, since the legacy shadow gives every character one unit of width
 * whatever its glyph.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CalendarStripDigitsTest {

    private static final int PERSIAN_ZERO = 0x06F0;
    private static final int PERSIAN_NINE = 0x06F9;

    private static final int WIDTH = 1080;
    private static final int HEIGHT = 400;

    private Context themed() {
        return new ContextThemeWrapper(ApplicationProvider.getApplicationContext(),
                R.style.MoneyWalletAppTheme);
    }

    private void laidOut(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.AT_MOST));
        view.layout(0, 0, WIDTH, view.getMeasuredHeight());
    }

    /**
     * The strip starts on a two digit day, because the cell read below is the first one and a
     * one digit day would never reach the two character limit the day label carries.
     */
    private TimelineView strip() {
        TimelineView strip = new TimelineView(themed());
        strip.setFirstDate(2026, 8, 12);
        strip.setLastDate(2026, 11, 31);
        laidOut(strip);
        return strip;
    }

    private MonthView monthRow() {
        MonthView row = new MonthView(themed());
        row.setLastDate(2026, 11);
        row.setSelectedMonth(2026, 8, false);
        laidOut(row);
        return row;
    }

    private MonthView monthRowFrom(int year) {
        MonthView row = new MonthView(themed());
        row.setFirstDate(year, 0);
        row.setLastDate(year, 11);
        laidOut(row);
        return row;
    }

    /**
     * Fails when the locale under test is not the Persian one. The digit assertions below would
     * fail on their own and say why, but the two about a text size and a width never look at a
     * digit and would pass quietly in any language.
     */
    private void requirePersianLocale() {
        assertEquals("the qualifier did not reach the default locale, so nothing below is a check",
                "fa", Locale.getDefault().getLanguage());
    }

    private static void assertPersianDigits(String where, CharSequence text) {
        assertTrue(where + " is empty, so it carries no digits to check", text.length() > 0);
        boolean found = false;
        for (int at = 0; at < text.length(); at++) {
            char character = text.charAt(at);
            if (Character.isDigit(character)) {
                found = true;
                assertTrue(where + " drew " + text + ", which carries the digit " + character
                                + " from outside the Persian set",
                        character >= PERSIAN_ZERO && character <= PERSIAN_NINE);
            }
        }
        assertTrue(where + " drew " + text + ", which carries no digit at all", found);
    }

    private static int digitCount(CharSequence text) {
        int found = 0;
        for (int at = 0; at < text.length(); at++) {
            if (Character.isDigit(text.charAt(at))) {
                found++;
            }
        }
        return found;
    }

    private static TextView label(ViewGroup strip, int cell, int id) {
        assertTrue("the strip laid out no cells, so there is no label to read",
                strip.getChildCount() > cell);
        return strip.getChildAt(cell).findViewById(id);
    }

    @Test
    @Config(qualifiers = "fa-rIR")
    public void theDayNumberIsWrittenInPersianDigits() {
        requirePersianLocale();
        CharSequence text = label(strip(), 0, R.id.mti_timeline_lbl_date).getText();
        assertPersianDigits("the day number", text);
        assertEquals("the day number " + text + " lost a digit, and the label carries a two "
                + "character limit", 2, digitCount(text));
    }

    @Test
    @Config(qualifiers = "fa-rIR")
    public void theMonthLabelYearIsWrittenInPersianDigits() {
        requirePersianLocale();
        assertPersianDigits("the month label",
                label(monthRow(), 0, R.id.mti_month_lbl).getText());
    }

    /**
     * The row takes the last two digits of the year, and a year ending in 00 through 09 leaves a
     * remainder of one digit, so an unpadded 2009 reads as the year 9. The strip runs from 1900,
     * which puts twenty such cells inside it.
     */
    @Test
    @Config(qualifiers = "fa-rIR")
    public void theMonthLabelKeepsBothYearDigits() {
        requirePersianLocale();
        CharSequence text = label(monthRowFrom(2009), 0, R.id.mti_month_lbl).getText();
        assertPersianDigits("the month label", text);
        assertEquals("the label " + text + " carries one year digit, so January 2009 names the "
                + "year 9", 2, digitCount(text));
    }

    /**
     * The row takes one text size for every label it can draw, stepped down until the widest of
     * them fits a cell. Persian labels fit at the size the layout asks for, and the Latin digits
     * are the wider glyphs, so a row sized from those steps down once and draws every Persian
     * label half a point small. Half a point is all that is at stake, and it is the only thing a
     * built view can be asked about which digits a size came from.
     *
     * It holds by that one step, so raising the declared text size, narrowing the cell, widening
     * its padding or running a font scale above one lands here too, with the digits innocent.
     * And it only catches a stand in that is too wide; one that came out too narrow, or empty,
     * passes this and is caught by the fit check below only if a label then overflows.
     */
    @Test
    @Config(qualifiers = "fa-rIR")
    public void theMonthLabelsKeepTheirDeclaredSizeInPersian() {
        requirePersianLocale();
        MonthView row = monthRow();
        float declared = row.getResources().getDimension(R.dimen.view_mti_month_lbl_text);
        assertEquals("the row stepped its text size down in Persian, which is what a size taken "
                        + "from the Latin digits does, and what a wider label or a narrower cell "
                        + "would do as well",
                declared, label(row, 0, R.id.mti_month_lbl).getTextSize(), 0.01f);
    }

    /**
     * Every month label fits inside its cell in Persian, which is what the shared text size is
     * derived for. This one is about the fit and not about the digits, so it passes on the commit
     * before the fix.
     */
    @Test
    @Config(qualifiers = "fa-rIR")
    public void everyMonthLabelFitsItsCellInPersian() {
        requirePersianLocale();
        MonthView row = monthRow();
        assertTrue("the row laid out no cells, so there is no label to fit",
                row.getChildCount() > 0);
        for (int cell = 0; cell < row.getChildCount(); cell++) {
            View item = row.getChildAt(cell);
            TextView lbl = item.findViewById(R.id.mti_month_lbl);
            int available = item.getWidth() - item.getPaddingLeft() - item.getPaddingRight();
            assertTrue("the cell has no width, so the fit below is not a check", available > 0);
            assertTrue("the label " + lbl.getText() + " asks for more width than its cell has",
                    Math.ceil(Layout.getDesiredWidth(lbl.getText(), lbl.getPaint())) <= available);
        }
    }

    @Test
    @Config(qualifiers = "en-rUS")
    public void theDayNumberStaysLatinInEnglish() {
        CharSequence text = label(strip(), 0, R.id.mti_timeline_lbl_date).getText();
        assertTrue("the day number is empty, so it carries no digits to check", text.length() > 0);
        for (int at = 0; at < text.length(); at++) {
            char character = text.charAt(at);
            assertTrue("the day number drew " + text + " in English", character >= '0'
                    && character <= '9');
        }
    }

}

package com.oriondev.moneywallet.ui.activity;

import com.oriondev.moneywallet.storage.database.Contract;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A period of a repeating budget is named after its chain and the day it begins on, and that name
 * is minted once and never rewritten. The dates on the row are editable. So the name and the dates
 * can be driven apart, and both things pinned here keep them together.
 *
 * The first is that a chain is ordered by those names. Ordering it by the start dates instead let
 * an old period be edited forward past the live one, after which the editor read the live period
 * as history and refused every save it was offered, saying the budget had moved on to a later
 * period when it had not.
 *
 * The second is that a repeating budget keeps its stored dates whenever its schedule is unchanged.
 * The start date field comes back on screen the moment repeat is unticked, and a day typed into it
 * there used to be honored on the save that followed even when repeat was ticked again and the
 * schedule never moved. That left the row named after a day it no longer began on. Move it back
 * far enough and the next roll's first period lands on the name the row is still holding, which
 * the unique index refuses with nothing reading the result and the rule already off the row.
 *
 * The block that holds the dates runs inside an Activity and reads instance fields, so it cannot
 * be driven from a JVM unit test: there is no Robolectric on the unit test classpath. It is pinned
 * by reading the source instead, with comments and string literals stripped first so that no
 * assertion here can be satisfied or broken by the wording of a comment, and with whitespace
 * collapsed so a line wrap does not fail it. That makes it a tripwire against a revert, not a
 * proof of behavior: a rewrite that keeps these tokens and still moves the dates goes through.
 */
public class BudgetPeriodIdentityTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/ui/activity/NewEditBudgetActivity.java";

    private static final String PERIOD_START = "private Date[] periodToSave(Mode mode) {";
    private static final String PERIOD_END = "Date periodStart =";

    private static final String SUPERSEDED_START = "private boolean isSupersededPeriod() {";
    private static final String SUPERSEDED_END = "Cursor cursor =";

    /** The chain filter is left alone; only what the rows are ordered by is asserted. */
    @Test
    public void aChainIsOrderedByItsUuids() {
        String selection = Contract.LATER_PERIOD_OF_CHAIN_SELECTION;
        assertTrue("a chain is ordered by its uuids: " + selection,
                selection.endsWith(Contract.BUDGET_UUID + " > ?"));
        assertFalse("a start date is editable and cannot order a chain: " + selection,
                selection.contains(Contract.Budget.START_DATE));
    }

    /** The budget asking is named to that selection by its whole uuid, not by its start date. */
    @Test
    public void theEditorAsksWithItsOwnUuid() throws IOException {
        String block = squash(region(SUPERSEDED_START, SUPERSEDED_END));
        assertTrue(block, block.contains("String uuid = storedColumn(Contract.BUDGET_UUID);"));
        assertTrue(block, block.contains(", uuid};"));
    }

    /** An unchanged schedule returns the stored dates, and nothing about the field is asked. */
    @Test
    public void anUnchangedScheduleHoldsTheStoredDates() throws IOException {
        String block = squash(region(PERIOD_START, PERIOD_END));
        assertTrue(block, block.contains("if (scheduleHeld) {"));
        assertFalse(block, block.contains("startHeld"));
    }

    private static String region(String start, String end) throws IOException {
        String text = stripCommentsAndStrings(readSource());
        int from = text.indexOf(start);
        int to = from < 0 ? -1 : text.indexOf(end, from + 1);
        if (from < 0 || to < 0) {
            fail("could not find the region between \"" + start + "\" and \"" + end + "\" in "
                    + SOURCE_PATH + ", so this test can no longer check it");
        }
        return text.substring(from, to);
    }

    private static String readSource() throws IOException {
        File source = new File(SOURCE_PATH);
        if (!source.exists()) {
            source = new File("app/" + SOURCE_PATH);
        }
        if (!source.exists()) {
            fail("could not find " + SOURCE_PATH + " from " + new File(".").getAbsolutePath());
        }
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * Removes comments and string literals, replacing each with a single space so that the tokens
     * on either side of a removed comment do not run together.
     */
    private static String stripCommentsAndStrings(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
                i = skipTo(text, i + 2, "\n");
                out.append(' ');
            } else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
                i = skipTo(text, i + 2, "*/") + 2;
                out.append(' ');
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < text.length() && text.charAt(i) != c) {
                    i += text.charAt(i) == '\\' ? 2 : 1;
                }
                i++;
                out.append(' ');
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Collapses every run of whitespace to one space. */
    private static String squash(String text) {
        return text.replaceAll("\\s+", " ");
    }

    private static int skipTo(String text, int from, String token) {
        int at = text.indexOf(token, from);
        return at < 0 ? text.length() : at;
    }
}

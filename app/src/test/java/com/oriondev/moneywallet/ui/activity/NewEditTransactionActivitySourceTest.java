package com.oriondev.moneywallet.ui.activity;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The block checked here runs inside an Activity, reads instance fields and talks to a content
 * resolver, so it cannot be driven from a JVM unit test: there is no Robolectric on the unit test
 * classpath. An automated check of what the editor actually opens with would take an instrumented
 * test on a device or emulator, and none was written. The invariants are pinned by reading the
 * source instead.
 *
 * Comments and string literals are stripped before anything is matched, so no assertion here can
 * be satisfied or broken by the wording of a comment. Text that goes through squash first also
 * tolerates any amount of whitespace between two tokens. Everything else is matched with its
 * spacing exactly as written, including the lines a region is found by, so a reformat or a line
 * wrap anywhere in that text fails the assertion with no defect present.
 *
 * What these assertions check is which token sequences the source does and does not carry:
 * that getItemId appears nowhere in the intent branch, that the saving uri is built from
 * mSavingId, that the statement assigning landedMoney reads START_MONEY and PROGRESS once each
 * and adds them, that the statement assigning projectedMoney does the same with
 * PROJECTED_PROGRESS, that all three columns are in the projection, that the prefill takes the
 * smaller of the two and sits in the withdraw everything case, that the case falls through, and
 * that the statement assigning wallet builds a Wallet from the saving's wallet columns. That
 * makes them a tripwire against a revert or a careless rewrite. It does not make them a proof of
 * behaviour, and they cannot show the value the keypad ends up with. A rewrite that keeps those
 * tokens and still breaks the editor goes through. One example, not a list: a second assignment
 * written with += or -= is invisible here, because "landedMoney -=" does not contain the
 * "landedMoney =" the statement is found by.
 *
 * Invariant one: inside the branch that fills a new transaction from its intent, getItemId is
 * always -1, the value NewEditItemActivity assigns whenever it was not launched to edit an
 * existing item, because that branch is the else of a getMode() == EDIT_ITEM test. A saving uri
 * built from -1 is savings/-1, which matches no route in the content provider, so the query
 * returned null without reaching the database. The editor opened with no wallet, the amount keypad
 * had no currency to scale against, and a typed 2000 was stored as 20.00.
 *
 * Invariant two: the prefill that WITHDRAW EVERYTHING opens on is the smaller of the saving's two
 * figures. START_MONEY plus PROGRESS is what it holds today, the sum SavingCursorAdapter draws
 * its current amount from. START_MONEY plus PROJECTED_PROGRESS is the lowest it can end up at
 * once everything already on it has happened, counting every withdrawal it has and only the
 * deposits that are confirmed. An unconfirmed deposit is in neither, so this is not what the
 * saving would hold if every row landed; it is never more than that, and it is less by exactly
 * the unconfirmed deposits, which on most savings is nothing at all. The row this prefill is
 * written into is dated now and confirmed, so both bound it, and offering more than the smaller
 * one offers an amount the same screen then refuses. END_MONEY is the target. Reading END_MONEY
 * there returns too little for any saving deposited past its target and leaves the overshoot
 * behind with the saving already marked complete. The prefill is held at zero when the smaller
 * figure is negative, since the amount written out would otherwise be a withdraw of a negative
 * amount.
 *
 * Invariant three: the same row builds the wallet the editor opens on, currency included. Without
 * that construction the wallet field is empty, the keypad has no currency to scale against, and a
 * typed 2000 is stored as 20.00.
 *
 * Invariant four: a withdraw carries two ceilings, one from each of the saving's two figures, and
 * every place that builds or uses them agrees on which is which. Both are plain longs of the same
 * kind, so swapping them compiles, inverts both ceilings and changes nothing the other assertions
 * here look at. Every place they can be swapped is pinned: the two call sites, the parameter list
 * they are received into, the two statements that assign them to their fields, the one that picks
 * which of the two the check starts from, and the bundle they are written to and read back from.
 * The same goes for which term is added back to which ceiling and for how the term that only one
 * of them gets is worked out, for taking the smaller of the two and then comparing against it,
 * for naming what it worked out in the dialog, and for the column the loader reads each figure
 * from.
 *
 * The early return this change deleted is asserted absent, by counting the date test it was
 * written around: that test belongs to the narrowing now and appears once inside the check. Like
 * everything else here it is token matching, and it is loose in both directions. A return written
 * around some other test is invisible to it, and a second reading of this one fails it whether or
 * not a return came with it.
 */
public class NewEditTransactionActivitySourceTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/ui/activity/NewEditTransactionActivity.java";

    private static final String INTENT_BRANCH_START = "mType = intent.getIntExtra(TYPE, TYPE_STANDARD);";
    private static final String INTENT_BRANCH_END = "datetime = new Date();";

    private static final String SAVING_BRANCH_START = "} else if (mType == TYPE_SAVING) {";
    private static final String SAVING_BRANCH_END = "} else if (mType == TYPE_MODEL) {";

    private static final String WITHDRAW_EVERYTHING_CASE = "case SAVING_WITHDRAW_EVERYTHING:";
    private static final String WITHDRAW_CASE = "case SAVING_WITHDRAW:";
    private static final String CATEGORY_QUERY_START = "uri = DataContentProvider.CONTENT_CATEGORIES;";

    private static final String PREFILL = "money = Math.max(Math.min(landedMoney, projectedMoney), 0L);";

    private static final String CHECK_START = "private boolean validateSavingWithdraw() {";
    private static final String CHECK_END = "ThemedDialog.buildMaterialDialog(this)";

    /** Each figure the prefill is built from, paired with the column it has to read. */
    private static final String[][] FIGURES = {
            {"landedMoney =", "Contract.Saving.PROGRESS"},
            {"projectedMoney =", "Contract.Saving.PROJECTED_PROGRESS"}
    };

    @Test
    public void newItemBranchDoesNotReadTheItemId() throws IOException {
        assertEquals("getItemId is read inside the branch that fills a new transaction from its "
                + "intent, where it is always -1", -1,
                region(INTENT_BRANCH_START, INTENT_BRANCH_END).indexOf("getItemId"));
    }

    @Test
    public void savingIsLoadedByTheIdTheIntentCarried() throws IOException {
        assertTrue("the saving must be loaded with mSavingId, the id the launching intent put in "
                + "SAVING_ID", region(SAVING_BRANCH_START, SAVING_BRANCH_END)
                .contains("CONTENT_SAVINGS, mSavingId"));
    }

    @Test
    public void withdrawEverythingPrefillsTheSmallerOfTheTwoFigures() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        assertTrue("the withdraw everything prefill must be the smaller of the two figures this "
                + "block computes, held at zero so a saving already over withdrawn cannot prefill "
                + "a negative amount", saving.contains(PREFILL));
        for (String[] figure : FIGURES) {
            String computed = statementAssigning(saving, figure[0]);
            assertTrue("the figure is START_MONEY plus " + figure[1] + ", so both have to be "
                    + "read, but the assignment is: " + computed,
                    computed.contains("Contract.Saving.START_MONEY")
                            && computed.contains(figure[1]));
            assertTrue("END_MONEY is the target, not what the saving holds, so it must not be "
                    + "part of the prefill, but the assignment is: " + computed,
                    !computed.contains("END_MONEY"));
            assertTrue("the two columns have to be added to each other and nothing subtracted "
                    + "from them, since each figure is the start money plus its own sum and the "
                    + "signs are already in the sum, but the assignment is: " + computed,
                    computed.contains("+") && !computed.contains("-"));
        }
    }

    @Test
    public void withdrawEverythingPrefillsOnlyItsOwnCase() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        String everything = saving.substring(saving.indexOf(WITHDRAW_EVERYTHING_CASE));
        int ownCaseEnd = everything.indexOf(WITHDRAW_CASE);
        assertTrue("the prefill has to sit in the withdraw everything case: moved into the deposit "
                + "case it fills in what the saving can give every time a deposit is opened",
                everything.substring(0, ownCaseEnd).contains(PREFILL));
    }

    @Test
    public void withdrawEverythingFallsThroughToTheWithdrawCategory() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        String everything = saving.substring(saving.indexOf(WITHDRAW_EVERYTHING_CASE));
        String ownCase = everything.substring(0, everything.indexOf(WITHDRAW_CASE));
        assertEquals("the withdraw everything case falls through on purpose to pick up the "
                + "withdraw category below it. A break here, which is what an IDE inspection "
                + "offers, leaves the category selection argument null, and the query below then "
                + "crashes the editor as it opens: the bind value at index 1 is null",
                -1, ownCase.indexOf("break"));
    }

    @Test
    public void everyColumnTheFiguresReadIsInTheProjection() throws IOException {
        // The saving branch builds two projections, one for the saving and one for its category,
        // so this looks only at the part before the category query starts.
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        String projection = statementAssigning(
                saving.substring(0, saving.indexOf(CATEGORY_QUERY_START)), "projection =");
        assertTrue("a column read through getColumnIndex but left out of the projection returns "
                + "index -1 and throws when it is read, so all three have to be listed: "
                + projection, projection.contains("Contract.Saving.START_MONEY")
                        && projection.contains("Contract.Saving.PROGRESS")
                        && projection.contains("Contract.Saving.PROJECTED_PROGRESS"));
    }

    @Test
    public void theSumsReadEachColumnOnce() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        for (String[] figure : FIGURES) {
            String computed = statementAssigning(saving, figure[0]);
            assertEquals("START_MONEY has to be read once, or the figure counts it twice: "
                    + computed, 1, occurrences(computed, "Contract.Saving.START_MONEY"));
            assertEquals(figure[1] + " has to be read once: " + computed,
                    1, occurrences(computed, figure[1]));
        }
    }

    @Test
    public void savingWalletIsBuiltFromTheSavingRow() throws IOException {
        String built = statementAssigning(region(SAVING_BRANCH_START, SAVING_BRANCH_END), "wallet =");
        assertTrue("the editor has to open on the saving's own wallet, built from the row just "
                + "read, but the assignment is: " + built, built.contains("new Wallet("));
        assertTrue("that wallet has to carry the saving's currency, or the amount keypad has "
                + "nothing to scale a typed amount against, but the assignment is: " + built,
                built.contains("Contract.Saving.WALLET_ID")
                        && built.contains("Contract.Saving.WALLET_CURRENCY"));
    }

    /**
     * Runs of whitespace are collapsed to one space before these are matched, so the amount of
     * space between two tokens does not matter. A break inserted where the pattern has no space
     * at all still fails them, which is a false alarm and not a defect in the code. Everything
     * else about them is literal: they are token matching, not behaviour, and a rewrite that
     * keeps these sequences and still gets the ceilings wrong goes through.
     */
    @Test
    public void theTwoCeilingsAreBuiltAndUsedInTheRightOrder() throws IOException {
        String source = squash(stripCommentsAndStrings(readSource()));
        assertTrue("the new withdraw path passes the projected figure first and the landed one "
                + "second, and swapping them inverts both ceilings",
                source.contains("setSavingWithdrawLimits(projectedMoney, landedMoney, 0L, 0L,"));
        // As far as the break that closes the case, not to the end of the saving branch: past
        // that break the same call runs for a deposit too, and the check would start refusing
        // deposits. This cannot see a condition wrapped around the call inside the case.
        String saving = squash(region(SAVING_BRANCH_START, SAVING_BRANCH_END));
        String fromWithdrawCase = saving.substring(saving.indexOf(WITHDRAW_CASE));
        String withdrawCase = fromWithdrawCase.substring(0, fromWithdrawCase.indexOf("break;"));
        assertTrue("and it sits in the withdraw case itself. Above the case label it lands in "
                + "withdraw everything, which falls through, so a plain withdraw from the savings "
                + "list loses its ceilings and the check returns on its first line; below the "
                + "break it runs for a deposit as well: " + withdrawCase,
                withdrawCase.contains("setSavingWithdrawLimits(projectedMoney, landedMoney, 0L, 0L,"));
        assertTrue("the loader reads PROJECTED_PROGRESS into the first slot and PROGRESS into the "
                + "second, and swapping them inverts both ceilings", source.contains(
                "startMoney + cursor.getLong(cursor.getColumnIndex(Contract.Saving.PROJECTED_PROGRESS)), "
                        + "startMoney + cursor.getLong(cursor.getColumnIndex(Contract.Saving.PROGRESS)), "
                        + "alreadyTaken, alreadyTakenLanded,"));
        assertTrue("the edit path adds the stored amount back to the projected ceiling always and "
                + "to the landed one only while the progress already counts that row",
                source.contains("loadSavingWithdrawLimits(contentResolver, mSavingId, money, "
                        + "storedRowHasLanded ? money : 0L);"));
        assertTrue("and the parameter list it arrives in takes them in that order. These two are "
                + "longs of the same kind as well, so swapping them here hands the landed ceiling "
                + "an amount its own sum never counted", source.contains(
                "private void loadSavingWithdrawLimits(ContentResolver contentResolver, "
                        + "long savingId, long alreadyTaken, long alreadyTakenLanded) {"));
        // Inside the check itself and in this order, because each of these read on its own is
        // satisfied by a body that computes the narrowing and then never uses it.
        String check = squash(region(CHECK_START, CHECK_END));
        int starts = check.indexOf("long limit = mSavingWithdrawLimit;");
        int narrows = check.indexOf("limit = Math.min(limit, mSavingWithdrawLandedLimit);");
        int compares = check.indexOf("if (mMoneyPicker.getCurrentMoney() <= limit) {");
        assertTrue("the check starts from the projected ceiling: " + check, starts >= 0);
        assertTrue("it narrows to the smaller for a row already counted in today's figure, which "
                + "is one confirmed and not dated later, read from the fields as they stand now: "
                + check, narrows > starts && check.indexOf("mConfirmedCheckBox.isChecked() && "
                + "!mDateTimePicker.getCurrentDateTime().after(new Date())") > starts);
        assertTrue("and it compares against what it worked out, after working it out. Comparing "
                + "first and narrowing afterwards leaves every literal here in place and the "
                + "landed ceiling binding nothing: " + check, compares > narrows);
        assertTrue("the dialog names what the check worked out. Naming a field instead tells the "
                + "user a figure this same screen has just refused",
                source.contains("mMoneyFormatter.getNotTintedString(currency, limit)"));
        assertEquals("inside this check the date test belongs to the narrowing and is read once. "
                + "Reading it twice is how the deleted early return comes back, and that return "
                + "lets a row saved unconfirmed or dated ahead skip the check, which is route one "
                + "of the reported bug. A second reading is not proof of one: splitting the "
                + "narrowing, or guarding something else on the same test, fails this too", 1,
                occurrences(check, "mDateTimePicker.getCurrentDateTime().after(new Date())"));
        assertTrue("the parameter list receives the projected figure first and the landed one "
                + "second, which is the third place the same swap inverts both ceilings",
                source.contains("private void setSavingWithdrawLimits(long projectedHeld, "
                        + "long landedHeld, long alreadyTaken, long alreadyTakenLanded, "
                        + "String currencyIso) {"));
        assertTrue("each field takes its own figure and its own added back term, and each is held "
                + "at the term it was given, so a row both sums already count can be kept or "
                + "reduced while a row only the projected sum counts can be refused at its own "
                + "amount once it is confirmed and dated today",
                source.contains("mSavingWithdrawLimit = Math.max(projectedHeld + alreadyTaken, "
                        + "alreadyTaken); mSavingWithdrawLandedLimit = Math.max(landedHeld + "
                        + "alreadyTakenLanded, alreadyTakenLanded);"));
        assertTrue("the stored row counts towards the landed ceiling only while the progress "
                + "counts it, which is while it is confirmed and not dated ahead", source.contains(
                "boolean storedRowHasLanded = cursor.getInt(cursor.getColumnIndex("
                        + "Contract.Transaction.CONFIRMED)) == 1 && !DateUtils."
                        + "getDateFromSQLDateTimeString(cursor.getString(cursor.getColumnIndex("
                        + "Contract.Transaction.DATE))).after(new Date());"));
        assertTrue("the loader reads each figure from its own column, and a column left out of "
                + "its projection returns index -1 and throws when it is read", source.contains(
                "String[] projection = new String[] { Contract.Saving.START_MONEY, "
                        + "Contract.Saving.PROGRESS, Contract.Saving.PROJECTED_PROGRESS, "
                        + "Contract.Saving.WALLET_CURRENCY };"));
        assertTrue("both ceilings are written to the bundle together. Nothing rebuilds either "
                + "one on the way back, and the check bails out entirely on a null projected "
                + "ceiling, so dropping the write is the check gone for that editor",
                source.contains("outState.putLong(SS_SAVING_WITHDRAW_LIMIT, "
                        + "mSavingWithdrawLimit); outState.putLong("
                        + "SS_SAVING_WITHDRAW_LANDED_LIMIT, mSavingWithdrawLandedLimit);"));
        assertTrue("and the landed one is read back with a plain assignment and an if, never a "
                + "second conditional. A conditional with a long on one arm and this Long field "
                + "on the other is a numeric one, so the field is unboxed, and it is null on "
                + "every editor that is not a saving withdraw: written that way this line crashed "
                + "the editor on rotation", source.contains(
                "mSavingWithdrawLandedLimit = mSavingWithdrawLimit; if (savedInstanceState"
                        + ".containsKey(SS_SAVING_WITHDRAW_LANDED_LIMIT)) { "
                        + "mSavingWithdrawLandedLimit = savedInstanceState.getLong("
                        + "SS_SAVING_WITHDRAW_LANDED_LIMIT); }"));
    }

    /**
     * Returns the statement that assigns to the given left hand side, skipping any long
     * declaration of the same name, which is how each figure is declared before the block fills
     * it. Fails when the literal text passed in matches more than once, so a second plain
     * assignment cannot slip past unread. Compound assignments do not contain that text:
     * "landedMoney -=" does not contain "landedMoney =", so a later += or -= is not seen.
     */
    private static String statementAssigning(String block, String assignment) {
        String found = null;
        for (int at = block.indexOf(assignment); at >= 0; at = block.indexOf(assignment, at + 1)) {
            if (block.lastIndexOf("long ", at) == at - "long ".length()) {
                continue;
            }
            int end = block.indexOf(';', at);
            if (end < 0) {
                fail("the assignment to " + assignment + " has no end");
            }
            if (found != null) {
                fail("more than one assignment to " + assignment + " in the saving branch, so "
                        + "this test can no longer tell which one feeds the prefill");
            }
            found = block.substring(at, end);
        }
        if (found == null) {
            fail("no assignment to " + assignment + " in the saving branch");
        }
        return found;
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
     * on either side of a removed comment do not run together. Without this the assertions above
     * could be satisfied, or broken, by prose that no compiler ever sees.
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

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + 1)) {
            count++;
        }
        return count;
    }

    private static int skipTo(String text, int from, String token) {
        int at = text.indexOf(token, from);
        return at < 0 ? text.length() : at;
    }
}

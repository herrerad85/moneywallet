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
 * What these assertions check is which token sequences the source does and does not carry: that
 * getItemId appears nowhere in the intent branch, that the saving uri is built from mSavingId,
 * that the saving's projection carries every column the block reads out of it, that the withdraw
 * everything prefill is the lowest the saving reaches from now onwards and sits in its own case,
 * that the case falls through, that the statement assigning wallet builds a Wallet from the
 * saving's wallet columns, that the walk over a saving's rows signs and skips them the way the
 * saving's own sums do, that every column the check's three queries read is in the projection
 * beside it, that the check counts from the date on screen over rows ordered by date,
 * that the saving case of the visibility switch hides the wallet field, and that between the debt
 * label and the saving label the source names no wallet field and does carry a break. That makes
 * them a tripwire against a revert or a careless rewrite. It does not make them a proof of
 * behavior, and they cannot show the value the keypad ends up with. A rewrite that keeps those
 * tokens and still breaks the editor goes through. One example, not a list: a second assignment
 * written with += or -= is invisible here, because "money -=" does not contain the "money =" a
 * statement is found by.
 *
 * Invariant one: inside the branch that fills a new transaction from its intent, getItemId is
 * always -1, the value NewEditItemActivity assigns whenever it was not launched to edit an
 * existing item, because that branch is the else of a getMode() == EDIT_ITEM test. A saving uri
 * built from -1 is savings/-1, which matches no route in the content provider, so the query
 * returned null without reaching the database. The editor opened with no wallet, the amount keypad
 * had no currency to scale against, and a typed 2000 was stored as 20.00.
 *
 * Invariant two: the prefill that WITHDRAW EVERYTHING opens on is the lowest the saving reaches
 * from now onwards, which is the same figure the check applies when the save is pressed, since
 * the row it writes is dated now. Offering more than that offers an amount the same screen then
 * refuses. END_MONEY is the target. Reading END_MONEY there returns too little for any saving
 * deposited past its target and leaves the overshoot behind with the saving already marked
 * complete. The prefill is held at zero when the figure is negative, since the amount written out
 * would otherwise be a withdrawal of a negative amount, and a saving whose rows do not come back
 * offers nothing for the same reason.
 *
 * Invariant three: the same row builds the wallet the editor opens on, currency included. Without
 * that construction the wallet field is empty, the keypad has no currency to scale against, and a
 * typed 2000 is stored as 20.00.
 *
 * Invariant four: the walk that feeds the ceiling treats each row the way the saving's own sums
 * do. A withdrawal is the income half of the pair, since it pays money into the wallet, and it is
 * the one that takes the saving down, so it is signed negative. Both of those are a single token
 * that compiles either way and inverts every row when it is wrong, so both are pinned. Only a
 * deposit is dropped for being unconfirmed. An unconfirmed deposit never lands, since landing
 * takes being confirmed, and an unconfirmed withdrawal left out would hand out a ceiling it can
 * then take the saving under.
 *
 * Invariant five: the ceiling is the lowest balance from the date on screen onwards, over the
 * saving's rows in date order. Four things are pinned, because each is invisible to the unit test
 * that covers the walk itself. The rows are ordered by date, which is the order the walk needs
 * and cannot tell is missing. The row being edited is left out of the walk by id, or it is held
 * against its own drain. The moment counted from is the date on screen and not today, which is
 * the whole defect this replaced. And a stored row kept or lowered on a date it does not move
 * back from is never refused, or two withdrawals that already have a saving under zero freeze
 * each other and deleting one is the only way out.
 *
 * Invariant six: a saving transaction does not offer its wallet field. A saving's progress is
 * summed over its transactions with no currency anywhere in that sum, so a row moved onto a wallet
 * held in another currency is added at face value, and 500 euros count as 500 dollars. The debt
 * label sits beside the saving one in the same switch and the two shared a body until the hide was
 * added. A debt payment is deliberately left offering its wallet field, since a debt's progress is
 * summed the same way and answering that is a separate change. Three things are read: the saving
 * case must hide the wallet field, and between the two labels the field's name must be absent and
 * a break must be present.
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

    /** Squashed, since it is two statements over three lines in the source. */
    private static final String PREFILL =
            "Long lowest = readLowestSavingBalanceFrom(contentResolver, savingUri, startMoney, "
                    + "DateUtils.getSQLDateTimeString(new Date()), -1L); "
                    + "money = lowest != null ? Math.max(lowest, 0L) : 0L;";

    private static final String CHECK_START = "private boolean validateSavingWithdraw() {";
    private static final String CHECK_END = "ThemedDialog.buildMaterialDialog(this)";

    private static final String DEBT_CASE = "case TYPE_DEBT:";
    private static final String SAVING_CASE = "case TYPE_SAVING:";
    private static final String CASE_BREAK = "break;";

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
    public void withdrawEverythingPrefillsWhatTheSavingCanGiveFromNow() throws IOException {
        String saving = squash(region(SAVING_BRANCH_START, SAVING_BRANCH_END));
        assertTrue("the withdraw everything prefill must be the lowest the saving reaches from "
                + "now onwards, which is the figure the check applies when the save is pressed, "
                + "held at zero so a saving already over withdrawn cannot prefill a negative "
                + "amount: " + saving, saving.contains(PREFILL));
        assertTrue("END_MONEY is the target, not what the saving holds, so it must not be read "
                + "in this block at all: " + saving, !saving.contains("Contract.Saving.END_MONEY"));
    }

    @Test
    public void withdrawEverythingPrefillsOnlyItsOwnCase() throws IOException {
        String saving = squash(region(SAVING_BRANCH_START, SAVING_BRANCH_END));
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
    public void everyColumnTheSavingRowReadsIsInTheProjection() throws IOException {
        // The saving branch builds two projections, one for the saving and one for its category,
        // so this looks only at the part before the category query starts.
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        String projection = statementAssigning(
                saving.substring(0, saving.indexOf(CATEGORY_QUERY_START)), "projection =");
        assertTrue("a column read through getColumnIndex but left out of the projection returns "
                + "index -1 and throws when it is read: " + projection,
                projection.contains("Contract.Saving.START_MONEY")
                        && projection.contains("Contract.Saving.WALLET_ID")
                        && projection.contains("Contract.Saving.WALLET_CURRENCY"));
    }

    @Test
    public void theWalkSignsAndSkipsRowsTheWayTheSavingsSumsDo() throws IOException {
        String source = squash(stripCommentsAndStrings(readSource()));
        assertTrue("a withdrawal is the income half of the pair, since it pays money into the "
                + "wallet. Reading the other constant here compiles and turns every row into its "
                + "opposite", source.contains("boolean withdrawal = cursor.getInt(cursor"
                        + ".getColumnIndex(Contract.Transaction.DIRECTION)) == Contract.Direction"
                        + ".INCOME;"));
        assertTrue("and a withdrawal takes the saving down while a deposit puts money in, so the "
                + "withdrawal is the negative one. Swapping the two arms compiles as well",
                source.contains("signedMoney[rows] = withdrawal ? -money : money;"));
        assertTrue("only a deposit is dropped for being unconfirmed, because landing takes being "
                + "confirmed and money that never arrives must not pay for a withdrawal that "
                + "does. Dropping an unconfirmed withdrawal too hands out a ceiling it can then "
                + "take the saving under", source.contains("if (!withdrawal && cursor.getInt("
                        + "cursor.getColumnIndex(Contract.Transaction.CONFIRMED)) != 1) {"));
    }

    @Test
    public void everyColumnTheCheckReadsIsInItsOwnProjection() throws IOException {
        // A column read through getColumnIndex but left out of the projection beside it returns
        // index -1 and throws when it is read. These three are the queries the check runs on the
        // way out, so a column dropped from any of them is a crash on saving a withdrawal.
        String source = squash(stripCommentsAndStrings(readSource()));
        assertTrue("the walk reads five columns off every row of the saving: " + source,
                source.contains("String[] projection = new String[] { Contract.Transaction.ID, "
                        + "Contract.Transaction.DATE, Contract.Transaction.MONEY, "
                        + "Contract.Transaction.DIRECTION, Contract.Transaction.CONFIRMED };"));
        assertTrue("the stored row is read for both the amount and the date, since it is kept or "
                + "lowered only when neither has moved the wrong way: " + source,
                source.contains("String[] projection = new String[] { Contract.Transaction.MONEY,"
                        + " Contract.Transaction.DATE };"));
        assertTrue("and the saving itself is read for what the walk starts from and the currency "
                + "the ceiling is in: " + source, source.contains("String[] projection = new "
                        + "String[] { Contract.Saving.START_MONEY, "
                        + "Contract.Saving.WALLET_CURRENCY };"));
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

    @Test
    public void aSavingTransactionDoesNotOfferItsWalletField() throws IOException {
        assertTrue("a saving's progress is summed with no currency in it, so the wallet a saving "
                + "transaction sits in must not be changeable from the editor",
                region(SAVING_CASE, CASE_BREAK)
                        .contains("mWalletEditText.setVisibility(View.GONE)"));
    }

    @Test
    public void theDebtCaseDoesNotNameTheWalletField() throws IOException {
        assertEquals("a debt payment is deliberately left offering its wallet field, so that field "
                + "must not be named between the debt label and the saving one", -1,
                region(DEBT_CASE, SAVING_CASE).indexOf("mWalletEditText"));
    }

    @Test
    public void theDebtCaseCarriesABreak() throws IOException {
        assertTrue("a debt payment is deliberately left offering its wallet field, and the two "
                + "labels shared a body until the wallet hide was added, so a break has to appear "
                + "between them", region(DEBT_CASE, SAVING_CASE).contains(CASE_BREAK));
    }

    /**
     * Runs of whitespace are collapsed to one space before these are matched, so the amount of
     * space between two tokens does not matter. A break inserted where the pattern has no space
     * at all still fails them, which is a false alarm and not a defect in the code. Everything
     * else about them is literal. They are token matching, not behavior, and a rewrite that keeps
     * these sequences and still gets the ceiling wrong goes through.
     */
    @Test
    public void theCheckCountsFromTheDateOnScreenOverRowsInDateOrder() throws IOException {
        String source = squash(stripCommentsAndStrings(readSource()));
        // The order clause is a string literal, so this one assertion reads a source with its
        // comments taken out and its literals left in. A comment carrying the same text would
        // satisfy it, which nothing else here can be.
        assertTrue("the rows come back ordered by date, which is the order the walk needs and "
                + "cannot tell is missing. In any other order the number it returns is not the "
                + "lowest balance at all", squash(stripComments(readSource())).contains(
                        "projection, selection, selectionArgs, Contract.Transaction.DATE + \" ASC\");"));
        assertTrue("the row being edited is left out of the walk, or its own drain is counted "
                + "against it and it can never be raised. A new item names -1, which no row "
                + "carries", source.contains("if (cursor.getLong(cursor.getColumnIndex("
                        + "Contract.Transaction.ID)) == excludedId) { continue; }"));
        assertTrue("and the id left out is the one being edited", source.contains(
                "readLowestSavingBalanceFrom(contentResolver, savingUri, startMoney, date, "
                        + "getItemId());"));
        assertTrue("the moment counted from is the date on screen. Counting from today is the "
                + "defect this replaced, since a row dated earlier drains every day from its own "
                + "date onwards", source.contains("String date = DateUtils.getSQLDateTimeString("
                        + "mDateTimePicker.getCurrentDateTime());"));
        assertTrue("a stored row kept or lowered on a date it does not move back from is never "
                + "refused. Without that, two withdrawals that already have a saving under zero "
                + "freeze each other and deleting one is the only way out", source.contains(
                "unchangedOrSmaller = money <= cursor.getLong(cursor.getColumnIndex("
                        + "Contract.Transaction.MONEY)) && date.compareTo(cursor.getString("
                        + "cursor.getColumnIndex(Contract.Transaction.DATE))) >= 0;"));
        // Inside the check itself and in this order, because each of these read on its own is
        // satisfied by a body that works the ceiling out and then never uses it.
        String check = squash(region(CHECK_START, CHECK_END));
        int keeps = check.indexOf("if (isStoredWithdrawalKeptOrLowered(contentResolver, money, date)) {");
        int works = check.indexOf("long limit = Math.max(lowest, 0L);");
        int compares = check.indexOf("if (money <= limit) {");
        assertTrue("the stored row is let through before anything is worked out: " + check,
                keeps >= 0);
        assertTrue("the ceiling is held at nothing, or a saving already under zero on some date "
                + "from here on refuses every save of every row it carries, an amount of nothing "
                + "included: " + check, works > keeps);
        assertTrue("and the amount is compared against what was worked out, after working it out. "
                + "Comparing first leaves every literal here in place and the ceiling binding "
                + "nothing: " + check, compares > works);
        assertTrue("the dialog names what the check worked out. Naming the raw figure instead "
                + "tells the user a negative number on a saving that is already under zero",
                source.contains("mMoneyFormatter.getNotTintedString(currency, limit)"));
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
        return strip(text, true);
    }

    /**
     * The same with the string literals left in, for the one assertion whose invariant is written
     * as a literal. Comments still go, so the text it looks for cannot come from prose.
     */
    private static String stripComments(String text) {
        return strip(text, false);
    }

    private static String strip(String text, boolean strings) {
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
            } else if (strings && (c == '"' || c == '\'')) {
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

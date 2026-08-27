package com.oriondev.moneywallet.ui.activity;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

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
 * beside it, that the check counts from the date on screen over rows ordered by date, that the
 * debt case and the saving case of the visibility switch each read as the hides they should be and
 * nothing more, and that the flag the debt case reads is worked out from the two paid categories
 * before the switch and is carried across a recreate. That makes them a tripwire against a revert
 * or a careless rewrite. It does not make them a proof of behavior, and they cannot show the value
 * the keypad ends up with. A rewrite that keeps those tokens and still breaks the editor goes
 * through. One example, not a list: a second assignment written with += or -= is invisible here,
 * because "money -=" does not contain the "money =" a statement is found by.
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
 * Invariant six: neither a saving transaction nor a debt payment offers its wallet field, and a
 * debt's master transaction still does. A saving's progress and a debt's progress are each summed
 * over the transactions filed against them with no currency anywhere in that sum, so a row moved
 * onto a wallet held in another currency is added at face value, and 500 euros count as 500
 * dollars. A debt's master transaction is not one of those rows. It carries the same TYPE_DEBT as
 * a payment and is told apart only by its category, and editing its wallet moves the debt itself
 * through syncDebtOfMasterTransaction, so hiding the field there would take that away and no test
 * on the database would see it, since SQLDatabaseTest drives the database and not this screen. So
 * both cases are read whole and compared, not searched. A second hide written above the guard, or a
 * break dropped so the debt case falls into the saving body below it, both hide the field on a
 * master transaction while a search for the guard still finds it, and the guard copied onto the
 * saving case is false on every saving row and gives that field back. The derivation is pinned
 * whole, its guard and the line the tag is read from included, and it carries the restore line
 * above it and the switch below it inside the same literal, so it is held against both of its
 * neighbors and nothing can be written on either side of it. That matters in three directions.
 * category is null everywhere above the loader, so a derivation hoisted there reads a guard that
 * is false on every path and the feature is gone with its own text unchanged. Moved below the
 * switch it leaves false to be read and every payment offers its wallet field again. Left where it
 * is with a single statement written under it, mDebtPayment = false on the edit path, it is
 * reverted just as completely. A guard flipped to run only on a recreate leaves false the same way
 * with the assignment still sitting there, and a tag read replaced by one of the two paid
 * constants makes every debt row a payment and takes the field off a master transaction, with both
 * case bodies untouched. The lines the tag is read from are pinned as well. The edit path's is
 * its own text; the two loads off the category table, the new payment one and the saving one, are
 * identical text, so those are counted and a swap on either drops the count. The derivation
 * compares whatever getTag returns, and a tag read off a name column, or a fifth argument dropped
 * for the four argument constructor the model branch already uses, is false on every row without
 * any of this text moving. Its
 * save and restore are pinned separately, because losing either one gives a payment its field back
 * on the next rotation and no assertion over the switch can see it.
 *
 * Comparing a case whole costs what searching it does not. A body that is behaviorally identical
 * and written differently fails, a guard without its braces or two statements swapped, and so does
 * any third statement a later change has a good reason to add. That is a false alarm to be read
 * and corrected, not a defect, and it is the price of catching a hide that a search finds in the
 * wrong place.
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
    private static final String WALLET_HIDE = "mWalletEditText.setVisibility(View.GONE)";
    private static final String CATEGORY_HIDE = "mCategoryEditText.setVisibility(View.GONE)";
    private static final String PAYMENT_GUARD = "if (mDebtPayment) { " + WALLET_HIDE + "; }";
    private static final String DEBT_CASE_BODY =
            DEBT_CASE + " " + CATEGORY_HIDE + "; " + PAYMENT_GUARD + " ";
    private static final String SAVING_CASE_BODY =
            SAVING_CASE + " " + CATEGORY_HIDE + "; " + WALLET_HIDE + "; ";
    private static final String SWITCH_ON_TYPE = "switch (mType) {";
    private static final String DEBT_PAYMENT_DERIVATION =
            "mDebtPayment = savedInstanceState.getBoolean(SS_DEBT_PAYMENT, false); }"
                    + " if (savedInstanceState == null && category != null) {"
                    + " String categoryTag = category.getTag();"
                    + " mDebtPayment = Contract.CategoryTag.PAID_DEBT.equals(categoryTag)"
                    + " || Contract.CategoryTag.PAID_CREDIT.equals(categoryTag); } "
                    + SWITCH_ON_TYPE;
    private static final String EDIT_PATH_TAG_READ =
            "cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG)) );";
    private static final String CATEGORY_TABLE_TAG_READ =
            "cursor.getString(cursor.getColumnIndex(Contract.Category.TAG)) );";

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
        assertEquals("a saving's progress is summed with no currency in it, so the wallet a saving "
                + "transaction sits in must not be changeable from the editor. The whole case is "
                + "compared, since the debt guard four lines above it is false on every saving row "
                + "and would give the field back if it were copied down here",
                SAVING_CASE_BODY, squash(region(SAVING_CASE, CASE_BREAK)));
    }

    @Test
    public void aDebtPaymentDoesNotOfferItsWalletField() throws IOException {
        assertEquals("a debt's progress is summed with no currency in it either, so the wallet a "
                + "debt payment sits in must not be changeable from the editor. The whole case is "
                + "compared, since a second hide above the guard, or a break dropped so this case "
                + "falls into the saving body, takes the field off a master transaction too",
                DEBT_CASE_BODY, squash(region(DEBT_CASE, CASE_BREAK)));
    }

    @Test
    public void aDebtsMasterTransactionKeepsItsWalletField() throws IOException {
        String source = squash(stripCommentsAndStrings(readSource()));
        int derived = source.indexOf(DEBT_PAYMENT_DERIVATION);
        int read = source.indexOf(SWITCH_ON_TYPE);
        assertTrue("only a payment loses the field, and the derivation is compared whole, its "
                + "guard and the line the tag is read from included, because a tag read replaced "
                + "by one of the two paid constants makes every debt row a payment: " + source,
                derived >= 0);
        assertEquals("this reads the first switch on the type and there has to be only one, or a "
                + "second one added anywhere above the derivation calls it late when it is not",
                -1, source.indexOf(SWITCH_ON_TYPE, read + 1));
        assertTrue("and the tag has to come off the category's own tag column on the edit path. "
                + "Every other column on that row is a name or an icon, and a tag read swapped for "
                + "one of them, or a fifth argument dropped for the four argument constructor the "
                + "model branch already uses, leaves the tag null or a display name, which never "
                + "equals either paid constant and hands every stored payment its wallet field "
                + "back", source.contains(EDIT_PATH_TAG_READ));
        assertEquals("and off the category table's tag column on both of the loads that read it, "
                + "the new payment one and the saving one. Those two are identical text, so they "
                + "are counted and not found. A swap on the new payment load is the half a check "
                + "on the edit path alone cannot see, and finding one of the two would have gone "
                + "on passing on the strength of the other", 2,
                source.split(Pattern.quote(CATEGORY_TABLE_TAG_READ), -1).length - 1);
    }

    @Test
    public void theDebtPaymentFlagIsCarriedAcrossARecreate() throws IOException {
        String source = squash(stripCommentsAndStrings(readSource()));
        assertTrue("the derivation is skipped on a recreate, since it runs only when there is no "
                + "saved state, so the flag has to come back out of the bundle or a rotation gives "
                + "a payment its wallet field back: " + source,
                source.contains("mDebtPayment = savedInstanceState.getBoolean(SS_DEBT_PAYMENT, "
                        + "false);"));
        assertTrue("and it has to be put in the bundle, which fails the same way and is the half a "
                + "rotation test on the restore alone still passes without",
                source.contains("outState.putBoolean(SS_DEBT_PAYMENT, mDebtPayment);"));
        assertTrue("and it starts false, so a debt row the flag was never worked out for keeps its "
                + "wallet field instead of losing it",
                source.contains("private boolean mDebtPayment = false;"));
        assertTrue("and its key has to be its own. The five saved state keys are near identical "
                + "lines and this one was written by copying its neighbor, so a key repeated from "
                + "one of them puts two values in one bundle entry and the one read back is "
                + "whichever was written last. This is the one assertion here that reads the "
                + "source with its string literals left in, since the key is a literal",
                squash(stripComments(readSource())).contains(
                        "SS_DEBT_PAYMENT = \"NewEditTransactionActivity::SavedState::DebtPayment\";"));
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
     * The same with the string literals left in, for the assertions whose invariant is written as
     * a literal. Comments still go, so the text they look for cannot come from prose.
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

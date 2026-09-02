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
 * What the editor DOES with the rules, which is neither a rule nor a value and so cannot be
 * driven from a JVM test. The rules themselves moved to {@link TransactionEditorRules} and are
 * covered by TransactionEditorRulesTest, which sees the arguments a rule is given and never the
 * call site that gives them. Reaching the call sites would take an Activity and a content
 * resolver, and there is no Robolectric on the unit test classpath.
 *
 * Every invariant here breaks silently. None throws, none is visible in the database, and each
 * ends with an amount stored at the wrong scale or a ceiling worked out from a number the saving
 * never held. Two assertions that used to live here were dropped on the opposite reasoning: a
 * column read but left out of its projection throws IllegalStateException from CursorWindow the
 * moment the branch opens, so it cannot hide, and the twelve device pairs this change was tested
 * against open every one of those branches.
 *
 * Each of the seven was checked by making the change it forbids and watching this file go red.
 * That is worth saying because the first version of this file kept three assertions and let
 * eleven mutants through, every one of them a live defect.
 *
 * Comments and string literals are stripped before anything is matched, so no assertion here can
 * be satisfied or broken by the wording of a comment. Runs of whitespace are collapsed, so the
 * space between two tokens does not matter. Everything else is matched literally, including the
 * lines a region is found by, so a rename or a line wrap in that text fails the assertion with no
 * defect present. These are token matching, not behavior. A rewrite that keeps the tokens and
 * still opens the editor on the wrong wallet goes through.
 *
 * What each one holds:
 *
 * getItemId is never read inside the branch that fills a new transaction from its intent, where
 * it is always -1, because that branch is the else of a getMode() == EDIT_ITEM test. A saving uri
 * built from -1 is savings/-1, which matches no route in the content provider, so the query
 * returns null without reaching the database, the editor opens with no wallet, the amount keypad
 * has no currency to scale against, and a typed 2000 is stored as 20.00. The saving is loaded by
 * the id the intent carried, which is the other half of that. The same row builds the wallet the
 * editor opens on, currency included, which is the third.
 *
 * The withdraw everything prefill is fed the lowest balance the saving reaches from now onwards
 * and never END_MONEY, which is the target and would strand the overshoot on a saving deposited
 * past it. The walk's rows are built from the cursor in the order SavingRow takes them, since its
 * id and its money are both long and swapping them compiles. Each hide answers for its own field.
 * And the ceiling counts from the date on screen, leaves out the row being edited, and is what
 * the dialog names.
 */
public class NewEditTransactionActivitySourceTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/ui/activity/NewEditTransactionActivity.java";

    private static final String INTENT_BRANCH_START =
            "mRules.setType(intent.getIntExtra(TYPE, TYPE_STANDARD));";
    private static final String INTENT_BRANCH_END = "datetime = new Date();";

    private static final String SAVING_BRANCH_START = "} else if (mRules.getType() == TYPE_SAVING) {";
    private static final String SAVING_BRANCH_END = "} else if (mRules.getType() == TYPE_MODEL) {";

    private static final String DERIVATION_AND_HIDES =
            "if (restored != null) { mRules = restored; } } if (savedInstanceState == null && "
                    + "category != null) { mRules.setDebtPayment(TransactionEditorRules"
                    + ".isDebtPaymentTag(category.getTag())); } if (mRules.hidesCategoryField()) "
                    + "{ mCategoryEditText.setVisibility(View.GONE); } if (mRules"
                    + ".hidesWalletField()) { mWalletEditText.setVisibility(View.GONE); }";

    private static final String EDIT_PATH_TAG_READ =
            "cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_TAG)) )";
    private static final String CATEGORY_TABLE_TAG_READ =
            "cursor.getString(cursor.getColumnIndex(Contract.Category.TAG)) )";

    private static final String SAVING_ACTION_READ =
            "int action = intent.getIntExtra(SAVING_ACTION, 0);";
    private static final String SAVING_ACTION_BLOCK = SAVING_ACTION_READ
            + " if (TransactionEditorRules.savingCategoryTag(action) == null) { finish(); return; }"
            + " if (TransactionEditorRules.completesTheSaving(action)) { Long lowest = "
            + "readLowestSavingBalanceFrom(contentResolver, savingUri, startMoney, DateUtils"
            + ".getSQLDateTimeString(new Date()), -1L); money = TransactionEditorRules"
            + ".withdrawEverythingPrefill(lowest); mRules.setSavingCompleted(true); } String[] "
            + "selectionArgs = new String[] { TransactionEditorRules.savingCategoryTag(action) };"
            + " cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);";

    private static final String CHECK_BLOCK =
            "if (isStoredWithdrawalKeptOrLowered(contentResolver, money, date)) { return true; } "
                    + "Long lowest = readLowestSavingBalanceFrom(contentResolver, savingUri, "
                    + "startMoney, date, getItemId()); if (lowest == null) { return true; } long "
                    + "limit = TransactionEditorRules.withdrawLimit(lowest); if "
                    + "(TransactionEditorRules.isWithinLimit(money, limit)) {";

    @Test
    public void newItemBranchDoesNotReadTheItemId() throws IOException {
        assertEquals("getItemId is read inside the branch that fills a new transaction from its "
                + "intent, where it is always -1", -1,
                region(INTENT_BRANCH_START, INTENT_BRANCH_END, 9000).indexOf("getItemId"));
    }

    @Test
    public void savingIsLoadedByTheIdTheIntentCarried() throws IOException {
        assertTrue("the saving must be loaded with the id the launching intent put in SAVING_ID, "
                + "not with the id of the item being edited, which a new item does not have",
                region(SAVING_BRANCH_START, SAVING_BRANCH_END, 1800)
                        .contains("CONTENT_SAVINGS, mRules.getSavingId()"));
    }

    @Test
    public void savingWalletIsBuiltFromTheSavingRow() throws IOException {
        String built = statementAssigning(
                region(SAVING_BRANCH_START, SAVING_BRANCH_END, 1800), "wallet =");
        assertEquals("the editor has to open on the saving's own wallet, built from the row just "
                + "read, and the whole statement is compared because Wallet takes its name and "
                + "its currency as neighboring Strings. Asking only that both columns appear "
                + "somewhere lets them be swapped, which opens the editor on a wallet named EUR "
                + "with no currency the keypad can scale against, and a typed 2000 is stored as "
                + "20.00",
                "wallet = new Wallet( cursor.getLong(cursor.getColumnIndex(Contract.Saving"
                + ".WALLET_ID)), cursor.getString(cursor.getColumnIndex(Contract.Saving"
                + ".WALLET_NAME)), IconLoader.parse(cursor.getString(cursor.getColumnIndex("
                + "Contract.Saving.WALLET_ICON))), CurrencyManager.getCurrency(cursor.getString("
                + "cursor.getColumnIndex(Contract.Saving.WALLET_CURRENCY))), 0L, 0L )", built);
    }

    @Test
    public void theSavingBranchIsComparedWhole() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END, 1800);
        int at = saving.indexOf(SAVING_ACTION_READ);
        assertTrue("the saving action is no longer read here, so this test can no longer check "
                + "what follows it", at >= 0);
        assertEquals("everything the action decides is compared whole and in order, because each "
                + "of these found on its own is satisfied while the defect it names is present. "
                + "The guard moved below the query puts back master's crash on a null bind value "
                + "with its own text untouched. The prefill hoisted out of its case fills in what "
                + "the saving holds every time a deposit is opened. The query handed a literal "
                + "array leaves the declaration sitting there unused and files every withdraw as "
                + "a deposit, which writes the row in the wrong direction and makes "
                + "validateSavingWithdraw step aside so the ceiling never runs",
                SAVING_ACTION_BLOCK, saving.substring(at, at + SAVING_ACTION_BLOCK.length()));
    }

    @Test
    public void theStateIsCarriedAcrossARecreate() throws IOException {
        String source = stripCommentsAndStrings(readSource());
        assertTrue("the rules object has to go into the bundle, or a rotation loses the debt "
                + "payment flag and the wallet field comes back on a payment",
                source.contains("outState.putSerializable(SS_RULES, mRules);"));
        assertTrue("and it has to be read back out, which fails the same way and is the half a "
                + "test on the put alone still passes without", source.contains(
                        "TransactionEditorRules restored = (TransactionEditorRules) "
                        + "savedInstanceState.getSerializable(SS_RULES); if (restored != null) { "
                        + "mRules = restored; }"));
        assertTrue("and the flag is worked out from the loaded category's own tag on first open. "
                + "A literal here makes every debt row a master transaction and hands every "
                + "stored payment its wallet field back", source.contains(
                        "mRules.setDebtPayment(TransactionEditorRules.isDebtPaymentTag("
                        + "category.getTag()));"));
    }

    @Test
    public void theWithdrawEverythingPrefillComesFromTheSavingsOwnBalance() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END, 1800);
        assertTrue("the prefill has to be fed the lowest the saving reaches from now onwards. "
                + "The rules class is handed a number and cannot see which number it is: "
                + saving, saving.contains("Long lowest = readLowestSavingBalanceFrom("
                        + "contentResolver, savingUri, startMoney, DateUtils."
                        + "getSQLDateTimeString(new Date()), -1L); money = TransactionEditorRules"
                        + ".withdrawEverythingPrefill(lowest);"));
        assertTrue("END_MONEY is the target, and a saving can be deposited past its target, so "
                + "reading it here strands the overshoot. It must not be read in this block at "
                + "all: " + saving, !saving.contains("Contract.Saving.END_MONEY"));
    }

    @Test
    public void theWalkIsBuiltFromTheColumnsInTheOrderItTakesThem() throws IOException {
        assertTrue("SavingRow takes five positional arguments and its id and its money are both "
                + "long, so swapping them compiles and turns every amount into a row id. The "
                + "rules test builds its own rows and cannot see this call",
                stripCommentsAndStrings(readSource()).contains(
                        "rows.add(new TransactionEditorRules.SavingRow( cursor.getLong(cursor"
                        + ".getColumnIndex(Contract.Transaction.ID)), cursor.getString(cursor"
                        + ".getColumnIndex(Contract.Transaction.DATE)), cursor.getLong(cursor"
                        + ".getColumnIndex(Contract.Transaction.MONEY)), cursor.getInt(cursor"
                        + ".getColumnIndex(Contract.Transaction.DIRECTION)), cursor.getInt(cursor"
                        + ".getColumnIndex(Contract.Transaction.CONFIRMED)) == 1 ));"));
    }

    @Test
    public void theRulesAreHandedTheirArgumentsInOrder() throws IOException {
        String source = stripCommentsAndStrings(readSource());
        assertTrue("isStoredWithdrawalKeptOrLowered takes the amount and date on screen and then "
                + "the stored pair. Both pairs are a long and a String, so writing the stored "
                + "pair first compiles, and raising a stored withdrawal is then waved through "
                + "before the ceiling is ever worked out", source.contains(
                        "unchangedOrSmaller = TransactionEditorRules"
                        + ".isStoredWithdrawalKeptOrLowered( money, date, cursor.getLong(cursor"
                        + ".getColumnIndex(Contract.Transaction.MONEY)), cursor.getString(cursor"
                        + ".getColumnIndex(Contract.Transaction.DATE)));"));
        assertTrue("and lowestSavingBalanceFrom takes the start money before the excluded id. "
                + "Both are long, so swapping them makes the row id the saving's opening balance, "
                + "and a new row names -1", source.contains(
                        "return TransactionEditorRules.lowestSavingBalanceFrom(startMoney, rows, "
                        + "excludedId, from);"));
    }

    @Test
    public void theDerivationAndTheHidesAreComparedWhole() throws IOException {
        String source = stripCommentsAndStrings(readSource());
        int at = source.indexOf("if (restored != null)");
        assertTrue("the restore block is gone, so this test can no longer find what it checks",
                at >= 0);
        String block = source.substring(at, at + DERIVATION_AND_HIDES.length());
        assertEquals("the flag a debt payment loses its wallet field by is worked out here, and "
                + "the block is compared whole, with the restore above it and both hides below "
                + "it, because nothing about this survives being found by a search. A statement "
                + "written inside the guard, the guard flipped to run only on a recreate, which "
                + "leaves it running on no path at all since category is null everywhere else, "
                + "and the derivation moved below the hides all leave this text sitting where it "
                + "is and hand every stored debt payment its wallet field back. Swapping which "
                + "field each hide answers for is the same kind of edit",
                DERIVATION_AND_HIDES, block);
    }

    @Test
    public void theCategoryTagIsReadFromTheTagColumn() throws IOException {
        String source = stripCommentsAndStrings(readSource());
        assertEquals("the tag has to come off the category's own tag column on the edit path. "
                + "Category has a four argument constructor that the model branch already uses, "
                + "so dropping the fifth argument compiles and leaves the tag null. A null tag "
                + "makes isDebtPaymentTag false for every row, and Category.getDirection falls "
                + "through to expense, so a stored saving withdrawal is rewritten as a deposit",
                1, count(source, EDIT_PATH_TAG_READ));
        assertEquals("and off the category table's tag column on both of the loads that read it, "
                + "the new debt payment one and the saving one. Those two are identical text, so "
                + "they are counted and not found, or finding one would go on passing on the "
                + "strength of the other. With a null tag there, validateSavingWithdraw steps "
                + "aside at its own tag check and the ceiling never runs at all",
                2, count(source, CATEGORY_TABLE_TAG_READ));
    }

    @Test
    public void theCheckIsComparedWhole() throws IOException {
        String source = stripCommentsAndStrings(readSource());
        int at = source.indexOf("if (isStoredWithdrawalKeptOrLowered");
        assertTrue("the stored row escape is gone from the check, and with it the only thing "
                + "that lets a withdrawal on a saving already under zero be lowered, saved "
                + "unchanged, or have its own note corrected", at >= 0);
        assertEquals("the check is compared whole and in order. Deleting the stored row escape "
                + "leaves the call inside its own helper, which another assertion here pins, so "
                + "the helper becomes unreachable and the coverage only looks present",
                CHECK_BLOCK, source.substring(at, at + CHECK_BLOCK.length()));
    }

    @Test
    public void theCheckCountsFromTheDateOnScreenAndLeavesOutTheRowBeingEdited()
            throws IOException {
        String source = stripCommentsAndStrings(readSource());
        assertTrue("the amount the check holds to the ceiling is the one on screen. It is the "
                + "fourth of the four values this check is built from, and the other three are "
                + "pinned on the lines below, so a literal here refuses nothing and leaves no "
                + "mark on the database", source.contains(
                        "long money = mMoneyPicker.getCurrentMoney();"));
        assertTrue("the moment counted from is the date on screen. Counting from today is the "
                + "defect this replaced, and the rules class is handed the date as a string",
                source.contains("String date = DateUtils.getSQLDateTimeString(mDateTimePicker"
                        + ".getCurrentDateTime());"));
        assertTrue("and the row left out of the walk is the one being edited, or its own drain "
                + "is counted against it and it can never be raised", source.contains(
                        "readLowestSavingBalanceFrom(contentResolver, savingUri, startMoney, "
                        + "date, getItemId());"));
        assertTrue("the ceiling is held at nothing before it is compared against. Without "
                + "that a saving already under zero on some later date refuses every save of "
                + "every row it carries, an amount of nothing included", source.contains(
                        "long limit = TransactionEditorRules.withdrawLimit(lowest);"));
        assertTrue("and the amount is the first argument and the ceiling the second. Both are "
                + "long, so the transposed call compiles and accepts everything over the ceiling "
                + "while refusing everything under it", source.contains(
                        "if (TransactionEditorRules.isWithinLimit(money, limit)) {"));
        assertTrue("the dialog names the ceiling that was worked out. Naming the raw figure "
                + "instead tells the user a negative number on a saving already under zero",
                source.contains("mMoneyFormatter.getNotTintedString(currency, limit)"));
    }

    /**
     * Returns the statement that assigns to the given left hand side. Fails when the literal text
     * passed in matches more than once, so a second plain assignment cannot slip past unread.
     * Compound assignments do not contain that text, so a later += or -= is not seen.
     */
    private static String statementAssigning(String block, String assignment) {
        String found = null;
        for (int at = block.indexOf(assignment); at >= 0; at = block.indexOf(assignment, at + 1)) {
            int end = block.indexOf(';', at);
            if (end < 0) {
                fail("the assignment to " + assignment + " has no end");
            }
            if (found != null) {
                fail("more than one assignment to " + assignment + " in the saving branch, so "
                        + "this test can no longer tell which one the editor opens on");
            }
            found = block.substring(at, end);
        }
        if (found == null) {
            fail("no assignment to " + assignment + " in the saving branch");
        }
        return found;
    }

    /**
     * The text between two anchors, taking the LAST end anchor and not the first, and refusing
     * a region that has collapsed.
     *
     * Both matter. INTENT_BRANCH_END is an ordinary statement that already appears twice in this
     * method, and moving it to the top of the branch it ends is behavior preserving; with the
     * first match it took the region shrinks to one line and every assertion over it passes over
     * whatever was left behind. The floor catches the same thing when the anchor is moved to a
     * spot that is still last.
     */
    private static String region(String start, String end, int atLeast) throws IOException {
        String text = stripCommentsAndStrings(readSource());
        int from = text.indexOf(start);
        int to = from < 0 ? -1 : text.lastIndexOf(end);
        if (from < 0 || to < from) {
            fail("could not find the region between \"" + start + "\" and \"" + end + "\" in "
                    + SOURCE_PATH + ", so this test can no longer check it");
        }
        String found = text.substring(from, to);
        if (found.length() < atLeast) {
            fail("the region between \"" + start + "\" and \"" + end + "\" is " + found.length()
                    + " characters, under the " + atLeast + " it should be. An anchor has moved, "
                    + "and every assertion over this region is passing over whatever is left");
        }
        return found;
    }

    /** How many times a literal occurs, so two identical loads cannot cover for each other. */
    private static int count(String text, String literal) {
        int n = 0;
        for (int at = text.indexOf(literal); at >= 0; at = text.indexOf(literal, at + 1)) {
            n++;
        }
        return n;
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
     * on either side of a removed comment do not run together, then collapses every run of
     * whitespace to one space.
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
        return out.toString().replaceAll("\\s+", " ");
    }

    private static int skipTo(String text, int from, String token) {
        int at = text.indexOf(token, from);
        return at < 0 ? text.length() : at;
    }

}

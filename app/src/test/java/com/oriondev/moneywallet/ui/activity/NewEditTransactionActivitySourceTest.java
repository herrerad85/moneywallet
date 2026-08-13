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
 * be satisfied or broken by the wording of a comment.
 *
 * What these assertions check is that specific token sequences are still present in the source:
 * that getItemId appears nowhere in the intent branch, that the saving uri is built from
 * mSavingId, that the statement assigning savingMoney reads START_MONEY and PROGRESS and adds
 * them, and that the statement assigning wallet builds a Wallet from the saving's wallet columns.
 * That makes them a tripwire against a revert or a careless rewrite. It does not make them a proof
 * of behaviour, and they cannot show the value the keypad ends up with. A rewrite that keeps those
 * tokens and still breaks the editor goes through: moving the prefill out of the withdraw
 * everything case, dropping a column from the projection while the getColumnIndex call that names
 * it stays, and adding a second assignment written with += or -= are all invisible here.
 *
 * Invariant one: inside the branch that fills a new transaction from its intent, getItemId is
 * always the NEW_ITEM placeholder of -1, because that branch is the else of a
 * getMode() == EDIT_ITEM test. Loading the saving with getItemId matched no row, so the editor
 * opened with no wallet, the amount keypad had no currency to scale against, and a typed 2000 was
 * stored as 20.00.
 *
 * Invariant two: the prefill that WITHDRAW EVERYTHING opens on is what the saving holds, which is
 * START_MONEY plus PROGRESS, the same sum SavingCursorAdapter draws its current amount from.
 * END_MONEY is the target. Reading END_MONEY there returns too little for any saving deposited
 * past its target and leaves the overshoot behind with the saving already marked complete.
 *
 * Invariant three: the same row builds the wallet the editor opens on, currency included. That
 * wallet is the whole point of the fix, so its construction has to stay: without it the wallet
 * field is empty, the keypad has no currency to scale against, and a typed 2000 is stored as
 * 20.00.
 */
public class NewEditTransactionActivitySourceTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/ui/activity/NewEditTransactionActivity.java";

    private static final String INTENT_BRANCH_START = "mType = intent.getIntExtra(TYPE, TYPE_STANDARD);";
    private static final String INTENT_BRANCH_END = "datetime = new Date();";

    private static final String SAVING_BRANCH_START = "} else if (mType == TYPE_SAVING) {";
    private static final String SAVING_BRANCH_END = "} else if (mType == TYPE_MODEL) {";

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
    public void withdrawEverythingPrefillsWhatTheSavingHolds() throws IOException {
        String saving = region(SAVING_BRANCH_START, SAVING_BRANCH_END);
        assertTrue("the withdraw everything prefill must be the savingMoney this block computes",
                saving.contains("money = savingMoney;"));
        String computed = statementAssigning(saving, "savingMoney =");
        assertTrue("what a saving holds is START_MONEY plus PROGRESS, so both have to be read to "
                + "compute the prefill, but the assignment is: " + computed,
                computed.contains("Contract.Saving.START_MONEY")
                        && computed.contains("Contract.Saving.PROGRESS"));
        assertTrue("END_MONEY is the target, not what the saving holds, so it must not be part of "
                + "the prefill, but the assignment is: " + computed,
                !computed.contains("END_MONEY"));
        assertTrue("the two columns have to be added, since a saving holds its start money plus "
                + "everything deposited into it, but the assignment is: " + computed,
                computed.contains("+") && !computed.contains("-"));
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
     * Returns the statement that assigns to the given left hand side, skipping the declaration
     * that only sets a default. Fails when the literal text passed in matches more than once, so a
     * second plain assignment cannot slip past unread. Compound assignments do not contain that
     * text: "savingMoney -=" does not contain "savingMoney =", so a later += or -= is not seen.
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
     * Removes comments and string literals, replacing each with a single space so that offsets
     * never merge two tokens into one. Without this the assertions above could be satisfied, or
     * broken, by prose that no compiler ever sees.
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

    private static int skipTo(String text, int from, String token) {
        int at = text.indexOf(token, from);
        return at < 0 ? text.length() : at;
    }
}

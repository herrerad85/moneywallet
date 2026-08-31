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

package com.oriondev.moneywallet.storage.database;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The instrumented cases in SQLDatabaseTest prove that inTransaction commits and rolls back. They
 * cannot see whether the provider still calls it, because they drive SQLDatabase directly and
 * never go through a provider at all. Dropping the wrapper from one of the three write methods
 * would leave every one of those cases green while every multi table write through that method
 * went back to being interruptible half way. That is what this pins.
 *
 * It pins two more things the same cases cannot see. That each wrapper resolves the shared helper
 * ONCE and hands that same object to its body, since a restore on another thread can swap the
 * shared helper at any moment and a body that re-resolved it per statement could open the
 * transaction on one helper and write through another. And that the bodies do not tell the
 * observers, since a notifyChange issued before the commit announces a change that the rollback
 * then undoes, and nothing takes the announcement back.
 *
 * Comments and string literals are both stripped before anything is matched. Comments, so that no
 * assertion here can be satisfied by a comment describing code the file no longer has. Literals,
 * because this file is full of them and one carries a comment marker: every CONTENT_ uri is built
 * from a content scheme whose two slashes read as the start of a line comment. A stripper that
 * took comments first would cut each of those lines there, so a line that both built a uri and
 * called notifyChange would survive as the uri alone and the check below would pass with the call
 * it exists to forbid sitting right there. That is not hypothetical, it was demonstrated against
 * an earlier version of this file. Literals are matched first for that reason.
 *
 * A failure means read the source and decide, not that the provider is broken.
 */
public class DataContentProviderTransactionSourceTest {

    /** A java string literal, then a line comment, then a block comment. Literal first. */
    private static final Pattern STRIPPED = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|//.*?$|/[*].*?[*]/",
            Pattern.DOTALL | Pattern.MULTILINE);

    private String readSource() {
        String path = "src/main/java/com/oriondev/moneywallet/storage/database/DataContentProvider.java";
        File file = new File(path);
        if (!file.exists()) {
            // a runner rooted at the repo root instead of the module, which the precedent
            // handles the same way
            file = new File("app/" + path);
        }
        if (!file.exists()) {
            fail("Cannot find DataContentProvider.java at " + file.getAbsolutePath());
        }
        try {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return STRIPPED.matcher(source).replaceAll(" ");
        } catch (IOException e) {
            fail("Cannot read DataContentProvider.java: " + e.getMessage());
            return null;
        }
    }

    private String bodyOf(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(signature + " is gone", start >= 0);
        int end = source.indexOf("\n    }", start);
        assertTrue("no end found for " + signature, end > start);
        return source.substring(start, end);
    }

    /** Each public write method, and the exact call its body must be reached by. */
    private static final String[][] WRITE_METHODS = {
            {"public Uri insert(", "database -> insertInTransaction(database,"},
            {"public int delete(", "database -> deleteInTransaction(database,"},
            {"public int update(", "database -> updateInTransaction(database,"},
    };

    private static final String[] TRANSACTION_BODIES = {
            "private Uri insertInTransaction(",
            "private int deleteInTransaction(",
            "private int updateInTransaction(",
    };

    @Test
    public void everyWriteMethodRunsItsBodyInsideATransaction() {
        String source = readSource();
        for (String[] method : WRITE_METHODS) {
            String body = bodyOf(source, method[0]);
            // inSharedTransaction and not inTransaction directly. It is the one that holds the
            // swap lock across resolving the helper AND running the transaction, so a restore
            // replacing the database cannot land between the two and leave the write running on
            // a helper that has just been closed
            assertTrue(method[0] + " no longer goes through SQLDatabase.inSharedTransaction, so "
                            + "a restore can swap the shared helper part way through its write",
                    body.contains("SQLDatabase.inSharedTransaction(getContext(),"));
            // the whole call and not its halves. Asserting only that inSharedTransaction appears
            // leaves every way of handing the body something else, such as passing db() into it
            // or resolving the static again inside it
            assertTrue(method[0] + " no longer hands its body the same helper the transaction was "
                            + "opened on. The exact call it needs is " + method[1],
                    body.contains(method[1]));
        }
    }

    @Test
    public void nothingInsideATransactionNotifiesTheObservers() {
        String source = readSource();
        for (String signature : TRANSACTION_BODIES) {
            assertFalse(signature + " now calls notifyChange, which would announce a change that "
                            + "a rollback then undoes",
                    bodyOf(source, signature).contains("notifyChange("));
        }
    }

    @Test
    public void noBodyResolvesTheSharedHelperForItself() {
        String source = readSource();
        for (String signature : TRANSACTION_BODIES) {
            String body = bodyOf(source, signature);
            // every way back to the shared helper, not just the one the provider happens to use.
            // A body that reached it again could be handed a different helper than the one its
            // transaction was opened on, which is the whole reason the caller resolves it once
            for (String route : new String[] {"db()", "getShared", "sShared"}) {
                assertFalse(signature + " reaches the shared helper through " + route
                                + " instead of using the one its caller resolved and passed in",
                        body.contains(route));
            }
        }
    }

    @Test
    public void thePreferenceWriteStaysOutsideTheTransaction() {
        String source = readSource();
        assertFalse("deleteInTransaction writes the current wallet preference again. That write "
                        + "and the broadcast behind it cannot be rolled back, so a commit that "
                        + "fails would leave the wallet in the ledger and the preference already "
                        + "moved off it",
                bodyOf(source, "private int deleteInTransaction(").contains("setCurrentWallet("));
        assertTrue("delete no longer resets the current wallet after the commit, so deleting the "
                        + "wallet in use leaves every query filtering on an id that is gone",
                bodyOf(source, "public int delete(").contains("setCurrentWallet("));
    }
}

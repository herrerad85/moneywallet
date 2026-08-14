package com.oriondev.moneywallet.storage.database.data;

import android.database.Cursor;

import com.oriondev.moneywallet.storage.database.Contract;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Two things are pinned here.
 *
 * The first is chooseCategory, run for real against a mocked cursor. It is the half of the lookup
 * that decides which matched category an imported row is filed on. An income or expense category
 * is its direction, but a system category is not: the transaction editor derives one from the
 * category and writes it over the row on every save, so a row given a system category whose tag
 * implies the other direction is flipped the first time it is opened and saved, and the wallet
 * total moves by twice the amount.
 *
 * The second is the two statements of the query that this change altered, the projection and the
 * arguments. The query cannot be run from a JVM test: entering getOrCreateCategory touches
 * DataContentProvider.CONTENT_CATEGORIES, a static Uri, and there is no Robolectric on the unit
 * test classpath, so a mocked resolver does not help. Those two are read out of the source, the
 * way NewEditTransactionActivitySourceTest reads its own, and so is the pair of statements that
 * carry what chooseCategory returns back to the caller. Text is a tripwire against a revert, not a
 * proof of behaviour: it cannot see whether the query returns the right rows. Comments are not
 * stripped, so a comment quoting the code being checked has to be reworded.
 *
 * Statements this change did not touch are left unpinned even where a mutation of them would be
 * serious, since pinning them would fail on a reformat of code this branch has no opinion about.
 */
public class AbstractDataImporterTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/storage/database/data/AbstractDataImporter.java";

    private static final int TYPE = 0;
    private static final int TAG = 1;
    private static final int ID = 2;

    /**
     * A cursor over the given rows, each an id, a type and a tag, in the order the query returns
     * them. Position starts before the first row, and the rewind chooseCategory does is stubbed, so
     * a test can hand over a cursor that has already been read.
     */
    private static Cursor cursorOver(Object[]... rows) {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndex(Contract.Category.TYPE)).thenReturn(TYPE);
        when(cursor.getColumnIndex(Contract.Category.TAG)).thenReturn(TAG);
        when(cursor.getColumnIndex(Contract.Category.ID)).thenReturn(ID);
        final int[] at = {-1};
        when(cursor.moveToNext()).thenAnswer(call -> ++at[0] < rows.length);
        when(cursor.moveToFirst()).thenAnswer(call -> {
            at[0] = 0;
            return rows.length > 0;
        });
        when(cursor.moveToPosition(-1)).thenAnswer(call -> {
            at[0] = -1;
            return false;
        });
        when(cursor.getLong(ID)).thenAnswer(call -> (Long) rows[at[0]][0]);
        when(cursor.getInt(TYPE)).thenAnswer(call -> ((Contract.CategoryType) rows[at[0]][1]).getValue());
        when(cursor.getString(TAG)).thenAnswer(call -> (String) rows[at[0]][2]);
        return cursor;
    }

    private static Object[] row(long id, Contract.CategoryType type, String tag) {
        return new Object[] {id, type, tag};
    }

    @Test
    public void anOrdinaryCategoryHoldsARowOfItsOwnDirection() {
        assertEquals(7L, AbstractDataImporter.chooseCategory(
                cursorOver(row(7L, Contract.CategoryType.EXPENSE, null)), Contract.Direction.EXPENSE));
        assertEquals(7L, AbstractDataImporter.chooseCategory(
                cursorOver(row(7L, Contract.CategoryType.INCOME, null)), Contract.Direction.INCOME));
        assertEquals("an expense category cannot hold an income row",
                AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                        cursorOver(row(7L, Contract.CategoryType.EXPENSE, null)), Contract.Direction.INCOME));
    }

    @Test
    public void aTagWithNoKnownDirectionIsRefusedForBoth() {
        // A transfer is written as two legs, one per wallet, so both directions appear
        // under the transfer tag and Category.getDirection has no case for it. What it returns
        // there is the same zero it returns for an expense, which is why these are asserted
        // rather than left to it.
        for (int direction : new int[] {Contract.Direction.EXPENSE, Contract.Direction.INCOME}) {
            assertEquals("the transfer tag has no direction of its own",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(1L, Contract.CategoryType.SYSTEM, Contract.CategoryTag.TRANSFER)),
                            direction));
            assertEquals("a system row with no tag is not one this build seeded",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(1L, Contract.CategoryType.SYSTEM, null)), direction));
            assertEquals("nor is one carrying a tag this build does not know, which a restore can "
                    + "write and a later version can introduce",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(1L, Contract.CategoryType.SYSTEM, "system::tenth")), direction));
            assertEquals("an empty tag is not null and must be refused the same way",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(1L, Contract.CategoryType.SYSTEM, "")), direction));
        }
    }

    @Test
    public void everyKnownTagIsOneTheDirectionLookupAnswersFor() {
        // Each tag is checked by being accepted for the direction it implies and refused for the
        // other. For an income tag that also catches its case going missing from
        // Category.getDirection, since the fall through zero is not the income the pair requires.
        // For an expense tag it does not: the fall through zero and a real expense case are the
        // same answer, and nothing here can tell them apart.
        String[] expense = {Contract.CategoryTag.CREDIT, Contract.CategoryTag.PAID_DEBT,
                Contract.CategoryTag.SAVING_DEPOSIT, Contract.CategoryTag.TAX,
                Contract.CategoryTag.TRANSFER_TAX};
        String[] income = {Contract.CategoryTag.DEBT, Contract.CategoryTag.PAID_CREDIT,
                Contract.CategoryTag.SAVING_WITHDRAW};
        for (String tag : expense) {
            assertEquals(tag + " is an expense", 3L, AbstractDataImporter.chooseCategory(
                    cursorOver(row(3L, Contract.CategoryType.SYSTEM, tag)), Contract.Direction.EXPENSE));
            assertEquals(tag + " must not hold an income row",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(3L, Contract.CategoryType.SYSTEM, tag)), Contract.Direction.INCOME));
        }
        for (String tag : income) {
            assertEquals(tag + " is an income", 3L, AbstractDataImporter.chooseCategory(
                    cursorOver(row(3L, Contract.CategoryType.SYSTEM, tag)), Contract.Direction.INCOME));
            assertEquals(tag + " must not hold an expense row",
                    AbstractDataImporter.NO_CATEGORY, AbstractDataImporter.chooseCategory(
                            cursorOver(row(3L, Contract.CategoryType.SYSTEM, tag)), Contract.Direction.EXPENSE));
        }
    }

    @Test
    public void aRefusedCandidateIsPassedOverRatherThanEndingTheSearch() {
        assertEquals("a system row the guard refuses must not hide a category the user made "
                + "behind it",
                40L, AbstractDataImporter.chooseCategory(cursorOver(
                        row(8L, Contract.CategoryType.SYSTEM, Contract.CategoryTag.SAVING_DEPOSIT),
                        row(40L, Contract.CategoryType.INCOME, null)), Contract.Direction.INCOME));
        assertEquals("the first candidate that holds the row wins, and the query orders them "
                + "newest first",
                40L, AbstractDataImporter.chooseCategory(cursorOver(
                        row(40L, Contract.CategoryType.EXPENSE, null),
                        row(8L, Contract.CategoryType.SYSTEM, Contract.CategoryTag.SAVING_DEPOSIT)),
                        Contract.Direction.EXPENSE));
        assertEquals("an empty cursor finds nothing",
                AbstractDataImporter.NO_CATEGORY,
                AbstractDataImporter.chooseCategory(cursorOver(), Contract.Direction.EXPENSE));
    }

    /**
     * Every other lookup in the importer opens with moveToFirst, so wrapping this one in the same
     * call is the likeliest edit anyone makes to it. The scan rewinds, which keeps that edit from
     * quietly dropping the first and best candidate.
     */
    @Test
    public void aCursorSomebodyElseAlreadyMovedStillFindsTheFirstRow() {
        Cursor cursor = cursorOver(row(40L, Contract.CategoryType.EXPENSE, null));
        cursor.moveToFirst();
        assertEquals(40L, AbstractDataImporter.chooseCategory(cursor, Contract.Direction.EXPENSE));
    }

    /**
     * The two type arguments of the lookup have to be the integer the column holds. A CategoryType
     * handed to String.valueOf becomes its constant name instead, which matches no row, and that
     * is how the system argument was written, so that arm of the OR was dead and a CSV row naming a
     * system category was matched or created as an ordinary category instead. The constant is named as well as the accessor,
     * because reading getValue() off the wrong constant passes any check that looks only at the
     * suffix and leaves the arm just as dead.
     */
    @Test
    public void bothTypeArgumentsOfTheLookupAreIntegers() throws IOException {
        String statement = statementIn("selectionArgs = ");
        for (int at = statement.indexOf("String.valueOf("); at >= 0;
             at = statement.indexOf("String.valueOf(", at + 1)) {
            int from = at + "String.valueOf(".length();
            assertTrue("every type argument has to read getValue(), or it is a CategoryType "
                    + "stringified to its constant name: " + statement,
                    statement.startsWith("type.getValue()", from)
                            || statement.startsWith("Contract.CategoryType.SYSTEM.getValue()", from));
        }
        assertEquals("the lookup takes a name and two type arguments: " + statement,
                2, occurrences(statement, "String.valueOf("));
        assertTrue("the system argument has to read the SYSTEM value, since any other constant "
                + "leaves that arm of the OR matching nothing: " + statement,
                statement.contains("Contract.CategoryType.SYSTEM.getValue()"));
        assertTrue("the name has to be bound first, or every argument binds to the wrong "
                + "placeholder: " + statement, statement.contains("{name,"));
    }

    /**
     * A column read through getColumnIndex but left out of the projection returns index -1 and
     * throws when it is read. The same invariant NewEditTransactionActivitySourceTest pins for its
     * own projection, and this change is what added the two columns the guard reads.
     */
    @Test
    public void everyColumnTheGuardReadsIsInTheProjection() throws IOException {
        String projection = statementIn("projection = ");
        String source = readSource().replaceAll("\\s+", "");
        for (int at = source.indexOf("getColumnIndex(Contract.Category."); at >= 0;
             at = source.indexOf("getColumnIndex(Contract.Category.", at + 1)) {
            int from = at + "getColumnIndex(".length();
            String column = source.substring(from, source.indexOf(')', from));
            assertTrue(column + " is read out of the cursor but is not in the projection, so its "
                    + "index is -1 and reading it throws: " + projection,
                    projection.contains(column));
        }
    }

    /**
     * The lines that carry the answer back. Reverting the first to take the row the query returned
     * puts the whole direction guard back to sleep, and the rest decide what an imported row is
     * filed on. None of them is visible to a test of chooseCategory, since none is inside it.
     */
    @Test
    public void theLookupTakesTheGuardsAnswerAndReturnsIt() throws IOException {
        String body = methodBody().replaceAll("\\s+", "");
        assertTrue("getOrCreateCategory has to pick its row through chooseCategory, or the guard "
                + "is dead code and an imported row can be filed on a category that rewrites its "
                + "direction", body.contains("longcategoryId=chooseCategory(cursor,direction);"));
        assertTrue("it has to return the id the guard chose, and only when the guard found one",
                body.contains("if(categoryId!=NO_CATEGORY){returncategoryId;}"));
    }

    /** The named statement of getOrCreateCategory, whitespace stripped, without its semicolon. */
    private static String statementIn(String assignment) throws IOException {
        String body = methodBody();
        int at = body.indexOf(assignment);
        int end = at < 0 ? -1 : body.indexOf(';', at);
        if (at < 0 || end < 0) {
            fail("no \"" + assignment + "\" statement in getOrCreateCategory");
        }
        return body.substring(at, end).replaceAll("\\s+", "");
    }

    /**
     * getOrCreateCategory, from its signature to its closing brace, found by counting braces so
     * that whatever is written after it stays out.
     */
    private static String methodBody() throws IOException {
        String source = readSource();
        int at = source.indexOf(" getOrCreateCategory(");
        if (at < 0) {
            fail("getOrCreateCategory is gone from " + SOURCE_PATH + ", so this test no longer "
                    + "checks anything. Point it at wherever the lookup moved to.");
        }
        int depth = 0;
        for (int i = source.indexOf('{', at); i < source.length(); i++) {
            if (source.charAt(i) == '{') {
                depth++;
            } else if (source.charAt(i) == '}' && --depth == 0) {
                return source.substring(at, i);
            }
        }
        fail("getOrCreateCategory has no closing brace");
        return null;
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + 1)) {
            count++;
        }
        return count;
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
}

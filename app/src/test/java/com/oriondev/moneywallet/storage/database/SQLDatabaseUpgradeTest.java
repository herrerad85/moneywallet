package com.oriondev.moneywallet.storage.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * What {@link SQLDatabase#onUpgrade} does to a database an older release left behind. Nothing else
 * runs it, so every case here is the only thing standing behind a step of it.
 *
 * Three cases are a database that has never held the join table, at each of the versions a release
 * stamped without it: 2, which upstream stamped up to 4.0.4.1, 3, which it stamped from then on and
 * so did every release of this app up to 1.5.0, and 4, which 1.6.0 stamped. A version 1 database
 * takes the same path as a version 2 one, the step between them having been withdrawn. Their tables
 * are written out here instead of taken from {@link Schema}, because Schema is what a fresh install
 * gets today and a migration reads what a shipped release wrote. The columns the later two arrive
 * with are the exception, added by the same statements the migration would have added them with.
 *
 * A fourth is a database this app wrote with the version put back to 3, which is what installing a
 * release older than the one that wrote it leaves, since onDowngrade does nothing and the helper
 * stamps the version anyway. That one uses Schema, the schema being the one it wrote. A fifth is a
 * database the migration cannot convert at all.
 *
 * No budget here is flagged deleted, because none can be. The delete is a real delete on every
 * build ever shipped, the soft delete beside it was gated on a constant that has read false since
 * the initial commit and came out in 06b9009, and a backup carries no deleted budget either.
 */
@RunWith(RobolectricTestRunner.class)
public class SQLDatabaseUpgradeTest {

    private static final String CREATE_TABLE_WALLET_BEFORE_3 = "CREATE TABLE wallets (" +
            "wallet_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "wallet_name TEXT NOT NULL, " +
            "wallet_icon TEXT, " +
            "wallet_currency TEXT NOT NULL, " +
            "wallet_start_money INTEGER NOT NULL DEFAULT 0, " +
            "wallet_count_in_total INTEGER NOT NULL DEFAULT 1, " +
            "wallet_note TEXT, " +
            "wallet_archived INTEGER NOT NULL DEFAULT 0, " +
            "wallet_tag TEXT, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "last_edit INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL DEFAULT 0)";

    private static final String CREATE_TABLE_CATEGORY_BEFORE_3 = "CREATE TABLE categories (" +
            "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "category_name TEXT NOT NULL, " +
            "category_icon TEXT NOT NULL, " +
            "category_type INTEGER NOT NULL, " +
            "category_parent INTEGER, " +
            "category_show_report INTEGER NOT NULL DEFAULT 1, " +
            "category_tag TEXT, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "last_edit INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (category_parent) REFERENCES categories(category_id) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE)";

    private static final String CREATE_TABLE_BUDGET_BEFORE_4 = "CREATE TABLE budgets (" +
            "budget_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "budget_type INTEGER NOT NULL, " +
            "budget_category INTEGER, " +
            "budget_start_date DATETIME NOT NULL, " +
            "budget_end_date DATETIME NOT NULL, " +
            "budget_money INTEGER NOT NULL, " +
            "budget_currency TEXT NOT NULL, " +
            "budget_tag TEXT, " +
            "uuid TEXT NOT NULL UNIQUE, " +
            "last_edit INTEGER NOT NULL, " +
            "deleted INTEGER NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (budget_category) REFERENCES categories(category_id) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE)";

    private static final String ICON = "{\"type\":\"color\",\"color\":\"#000000\",\"name\":\"T\"}";

    /** Every seeded row is stamped in 2019, so a row the migration writes or restamps is told apart by its own value. */
    private static final long EDIT = 1565000000000L;

    private static final String NAME = "upgrade.db";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * A database from before any of the three steps, carried forward by the release installed over
     * it. All three have to land in that one launch, because the second launch sees the version
     * already stamped and skips every one of them. A version 1 database arrives here too, taking
     * the same path.
     */
    @Test
    public void aDatabaseFromBeforeAllThreeStepsComesAllTheWayForwardInOneLaunch() {
        upgradeFromBeforeTheJoinTable(2);
    }

    /**
     * What a user of any release from upstream 4.0.5 to this app's 1.5.0 is holding. The columns
     * of the first step are there, because the release that stamped this version is the one that
     * added them, and the other two steps have still to run.
     */
    @Test
    public void aDatabaseWhereOnlyTheFirstStepHasEverRun() {
        upgradeFromBeforeTheJoinTable(3);
    }

    /**
     * What a 1.6.0 user is holding, and the upgrade this migration runs most often, since 1.6.0 is
     * the release before the join table. Only the last step has still to run, so this is the one
     * case where the table has to be created by a launch that skips both ALTER blocks.
     */
    @Test
    public void aDatabaseWhereEverythingButTheJoinTableHasRun() {
        upgradeFromBeforeTheJoinTable(4);
    }

    /**
     * A database no release has written the join table into, at each version one of them stamped.
     * They differ by the columns their own upgrade had already added, and by nothing else the
     * migration reads.
     */
    private void upgradeFromBeforeTheJoinTable(int version) {
        SQLiteDatabase old = mContext.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        old.execSQL(CREATE_TABLE_WALLET_BEFORE_3);
        old.execSQL(CREATE_TABLE_CATEGORY_BEFORE_3);
        old.execSQL(CREATE_TABLE_BUDGET_BEFORE_4);
        // each release added its own columns by its own upgrade, so they are added here the same
        // way instead of being declared, and the fixture stays the tables that release shipped
        if (version >= 3) {
            old.execSQL(Schema.CREATE_WALLET_INDEX_COLUMN);
            old.execSQL(Schema.CREATE_CATEGORY_INDEX_COLUMN);
        }
        if (version >= 4) {
            old.execSQL(Schema.CREATE_BUDGET_RULE_COLUMN);
            old.execSQL(Schema.CREATE_BUDGET_RULE_START_COLUMN);
        }
        insertWallet(old, 1, "Cash");
        insertCategory(old, 1, "Food");
        insertCategory(old, 2, "Rent");
        insertCategory(old, 3, "Fun");
        insertBudget(old, 1, Schema.BudgetType.CATEGORY, 1L);
        // two budgets can cover one category, and each needs its own row
        insertBudget(old, 5, Schema.BudgetType.CATEGORY, 1L);
        // the ordinary budget, the one most databases are full of. It carries no category at all,
        // and the fill has to leave it alone, since the join table refuses a row without one
        insertBudget(old, 4, Schema.BudgetType.EXPENSES, null);
        // no release has ever written these two, since every editor clears the category when the
        // type is changed to one that does not cover categories. They are here because the two
        // clearing steps of migration 5 are written to defend against exactly this, and nothing
        // else in the file reaches either one. One of each type, since both steps are written
        // against the type that does cover categories and not against these
        insertBudget(old, 2, Schema.BudgetType.EXPENSES, 2L);
        insertBudget(old, 3, Schema.BudgetType.INCOMES, 3L);
        old.setVersion(version);
        old.close();

        SQLDatabase helper = new SQLDatabase(mContext, NAME);
        SQLiteDatabase upgraded = helper.getWritableDatabase();

        // written out so that the next version bump has to come through here
        assertEquals(5, upgraded.getVersion());
        assertEquals("ok", text(upgraded, "PRAGMA integrity_check"));
        assertEquals(0L, count(upgraded, "pragma_foreign_key_check", null));
        assertSchemaMatchesAFreshInstall(upgraded);

        // the rows that were already there are carried over, the index columns come out on them at
        // the default the app sorts and reads by, and the two budget rule columns come out empty,
        // whichever of those this run added and whichever the fixture arrived with
        assertEquals(1L, count(upgraded, Schema.Wallet.TABLE, null));
        assertEquals("the upgrade neither seeds nor drops categories",
                3L, count(upgraded, Schema.Category.TABLE, null));
        assertEquals(5L, count(upgraded, Schema.Budget.TABLE, null));
        assertEquals(Long.valueOf(0L),
                number(upgraded, "SELECT wallet_index FROM wallets WHERE wallet_id = 1"));
        assertEquals(Long.valueOf(0L),
                number(upgraded, "SELECT category_index FROM categories WHERE category_id = 1"));
        assertNull(number(upgraded, "SELECT budget_rule FROM budgets WHERE budget_id = 1"));
        assertNull(number(upgraded, "SELECT budget_rule_start FROM budgets WHERE budget_id = 1"));

        // both budgets that covered a category come out covering it through the table, each with
        // its own row, stamped in milliseconds like every other row this app writes
        assertEquals(4L, count(upgraded, Schema.BudgetCategory.TABLE, null));
        assertEquals(2L, count(upgraded, Schema.BudgetCategory.TABLE, "_category = 1 AND deleted = 0"));
        assertEquals(Long.valueOf(1L), categoryColumnOf(upgraded, 1));
        assertStampedNow(upgraded, "SELECT last_edit FROM budget_categories WHERE _budget = 1");

        // the budget with no category is left with none, and neither of those is asserted, since
        // a fill that tried to give it one throws on the join table and takes the whole upgrade
        // with it before anything here is read

        // the two of other types keep neither. Their rows are flagged and not removed, which is
        // what an ordinary edit does to the same rows
        assertEquals(1L, count(upgraded, Schema.BudgetCategory.TABLE, "_budget = 2 AND deleted = 1"));
        assertEquals(1L, count(upgraded, Schema.BudgetCategory.TABLE, "_budget = 3 AND deleted = 1"));
        assertNull(categoryColumnOf(upgraded, 2));
        assertNull(categoryColumnOf(upgraded, 3));
        // and none of them is restamped, whether the migration rewrote its column or not. That
        // value is the age a backup carries for the row
        assertEquals(Long.valueOf(EDIT),
                number(upgraded, "SELECT last_edit FROM budgets WHERE budget_id = 2"));
        assertEquals(Long.valueOf(EDIT),
                number(upgraded, "SELECT last_edit FROM budgets WHERE budget_id = 1"));

        // the table the upgrade just built has to take its rows with whichever end of them goes
        // first. Category 2 is what proves the category end, since the budget holding that row
        // came out of the upgrade with a null column, so no other key can carry the row away
        upgraded.execSQL("DELETE FROM categories WHERE category_id = 2");
        assertEquals(0L, count(upgraded, Schema.BudgetCategory.TABLE, "_category = 2"));
        upgraded.execSQL("DELETE FROM budgets WHERE budget_id = 1");
        assertEquals(0L, count(upgraded, Schema.BudgetCategory.TABLE, "_budget = 1"));
        helper.close();
    }

    /**
     * A database the migration cannot convert. The launch has to fail and leave the version where
     * it was, because SQLiteOpenHelper stamps the new one the moment onUpgrade returns without
     * throwing, and a database stamped as converted is never handed to the migration again. So a
     * failure that is caught and carried on from is worse than the crash it replaces, it is
     * permanent.
     */
    @Test
    public void aFailedUpgradeLeavesTheVersionWhereItWas() {
        SQLiteDatabase old = mContext.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        old.execSQL(CREATE_TABLE_WALLET_BEFORE_3);
        old.execSQL(CREATE_TABLE_CATEGORY_BEFORE_3);
        old.execSQL(CREATE_TABLE_BUDGET_BEFORE_4);
        // a budget naming a category that is not there, which the fill cannot write a row for
        // while onConfigure has foreign keys on. This connection is not the one it configures
        insertBudget(old, 1, Schema.BudgetType.CATEGORY, 99L);
        old.setVersion(2);
        old.close();

        SQLDatabase helper = new SQLDatabase(mContext, NAME);
        try {
            helper.getWritableDatabase();
            fail("a database the migration cannot convert came back converted, so either the fill "
                    + "no longer refuses an orphan row or the failure was caught and carried on from");
        } catch (SQLiteConstraintException expected) {
            helper.close();
        }

        SQLiteDatabase after = mContext.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        assertEquals(2, after.getVersion());
        // and nothing the run did before it failed is left behind either
        assertEquals(0L, count(after, "pragma_table_list",
                "name = '" + Schema.BudgetCategory.TABLE + "'"));
        after.close();
    }

    /**
     * A database this app wrote, put back to the version 1.5.0 and everything before it stamps.
     * The columns the budget rule step adds are already there, so its guards have to skip it, or
     * the statement throws and takes every read of the database with it.
     */
    @Test
    public void aDatabasePutBackToTheVersionBeforeTheJoinTable() {
        upgradeAgainOverRowsThatAreAlreadyThere(3);
    }

    /**
     * The join table is already there and already holds rows, so the create has to leave it alone
     * and the fill has to leave the categories a budget covers as they are. The column an older
     * release wrote names at most one of them.
     */
    private void upgradeAgainOverRowsThatAreAlreadyThere(int version) {
        SQLiteDatabase old = mContext.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        old.execSQL(Schema.CREATE_TABLE_CATEGORY);
        old.execSQL(Schema.CREATE_TABLE_BUDGET);
        old.execSQL(Schema.CREATE_TABLE_BUDGET_CATEGORY);
        insertCategory(old, 1, "Food");
        insertCategory(old, 5, "Rent");
        insertCategory(old, 7, "Fun");
        insertCategory(old, 9, "Travel");
        // budgets are numbered a hundred up so that no budget id is also a category id here. The
        // join table carries one of each, and a predicate naming the wrong one of the two reads
        // the same either way while the two sets overlap

        // covers two categories and used to cover a third, which is what an edit leaves behind. The
        // column names a fourth, which is all a release that predates the table could write
        insertBudget(old, 101, Schema.BudgetType.CATEGORY, 9L);
        insertBudgetCategory(old, 101, 5, false);
        insertBudgetCategory(old, 101, 7, false);
        insertBudgetCategory(old, 101, 1, true);
        // covers nothing live, and the category its column names is the one it holds flagged
        insertBudget(old, 102, Schema.BudgetType.CATEGORY, 5L);
        insertBudgetCategory(old, 102, 5, true);
        // changed to a type that does not cover categories by a release that could not see the
        // table, so the column is cleared as that release always clears it and the row it never
        // saw is still live, holding that category against deletion
        insertBudget(old, 103, Schema.BudgetType.EXPENSES, null);
        insertBudgetCategory(old, 103, 7, false);
        // and one it had already dropped before the type was changed. Flagging what is flagged
        // again would restamp it, and a stale deletion carrying today's time beats a newer edit
        // wherever these rows are compared by it
        insertBudgetCategory(old, 103, 1, true);
        // a budget covering the lowest category anything here covers, so the column of a budget
        // that does not cover it has to be read from that budget's own rows and not from the table
        insertBudget(old, 104, Schema.BudgetType.CATEGORY, 1L);
        insertBudgetCategory(old, 104, 1, false);
        old.setVersion(version);
        old.close();

        SQLDatabase helper = new SQLDatabase(mContext, NAME);
        SQLiteDatabase upgraded = helper.getWritableDatabase();

        assertEquals(5, upgraded.getVersion());
        assertEquals("ok", text(upgraded, "PRAGMA integrity_check"));
        assertEquals(0L, count(upgraded, "pragma_foreign_key_check", null));
        assertEquals(7L, count(upgraded, Schema.BudgetCategory.TABLE, null));

        // the two it covers survive, the one it dropped stays dropped, and the category the older
        // release put in the column is not added to any of them
        assertEquals(2L, count(upgraded, Schema.BudgetCategory.TABLE, "_budget = 101 AND deleted = 0"));
        assertEquals(1L, count(upgraded, Schema.BudgetCategory.TABLE,
                "_budget = 101 AND _category = 1 AND deleted = 1"));
        assertEquals(0L, count(upgraded, Schema.BudgetCategory.TABLE, "_category = 9"));
        // the column is put back on the lowest category the budget actually covers, so nothing
        // reading it alone shows a category no screen has the budget covering. Lower ids are
        // covered here by another budget, and this one keeps its own
        assertEquals(Long.valueOf(5L), categoryColumnOf(upgraded, 101));
        assertEquals(Long.valueOf(1L), categoryColumnOf(upgraded, 104));

        // the budget holding only a flagged row for the category its column names comes out
        // covering that category, and not carrying a column no live row backs
        assertEquals(1L, count(upgraded, Schema.BudgetCategory.TABLE,
                "_budget = 102 AND _category = 5 AND deleted = 0"));
        assertEquals(Long.valueOf(5L), categoryColumnOf(upgraded, 102));

        // the one whose type was changed loses the row it was left holding, flagged and restamped
        // the way an ordinary edit flags it, while the row it had already dropped is left as it is
        assertEquals(2L, count(upgraded, Schema.BudgetCategory.TABLE, "_budget = 103 AND deleted = 1"));
        assertStampedNow(upgraded,
                "SELECT last_edit FROM budget_categories WHERE _budget = 103 AND _category = 7");
        assertEquals(Long.valueOf(EDIT), number(upgraded,
                "SELECT last_edit FROM budget_categories WHERE _budget = 103 AND _category = 1"));
        helper.close();
    }

    /**
     * The tables an ALTER step touches have to come out of an upgrade with the columns and the
     * foreign keys a fresh install gets, or an upgraded database and a new one are two different
     * databases running the same queries. Compared by name and not in order, since a column an
     * ALTER adds lands at the end while the same column is declared in the middle.
     *
     * Only those three. The join table is created from the same statement on both paths, so
     * comparing it here would be comparing a statement against itself. So is any table on a run
     * where the fixture arrived with the columns already added and no ALTER had anything to do.
     */
    private void assertSchemaMatchesAFreshInstall(SQLiteDatabase upgraded) {
        SQLDatabase helper = new SQLDatabase(mContext, "fresh.db");
        SQLiteDatabase fresh = helper.getWritableDatabase();
        for (String table : new String[] {Schema.Wallet.TABLE, Schema.Category.TABLE,
                Schema.Budget.TABLE}) {
            String columns = declaration(upgraded, "table_info", table);
            // a pragma against a table that is not there answers nothing at all, so without this
            // a name that stopped matching a table would compare two empty strings and pass
            assertFalse(table + " is not in the upgraded database", columns.isEmpty());
            assertEquals(table + " columns", declaration(fresh, "table_info", table), columns);
            assertEquals(table + " foreign keys", declaration(fresh, "foreign_key_list", table),
                    declaration(upgraded, "foreign_key_list", table));
        }
        helper.close();
    }

    /** One line per row of the pragma, sorted, with the position columns left out. */
    private String declaration(SQLiteDatabase db, String pragma, String table) {
        Cursor cursor = db.rawQuery("PRAGMA " + pragma + "(" + table + ")", null);
        List<String> rows = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                StringBuilder row = new StringBuilder();
                for (int column = 0; column < cursor.getColumnCount(); column++) {
                    String name = cursor.getColumnName(column);
                    if ("cid".equals(name) || "id".equals(name) || "seq".equals(name)) {
                        continue;
                    }
                    row.append(name).append('=').append(cursor.getString(column)).append(' ');
                }
                rows.add(row.toString());
            }
        } finally {
            cursor.close();
        }
        Collections.sort(rows);
        StringBuilder joined = new StringBuilder();
        for (String row : rows) {
            joined.append(row).append('\n');
        }
        return joined.toString();
    }

    private void insertWallet(SQLiteDatabase db, long id, String name) {
        db.execSQL("INSERT INTO wallets (wallet_id, wallet_name, wallet_currency, uuid, " +
                        "last_edit, deleted) VALUES (?, ?, 'EUR', ?, ?, 0)",
                new Object[] {id, name, "wallet-" + id, EDIT});
    }

    private void insertCategory(SQLiteDatabase db, long id, String name) {
        db.execSQL("INSERT INTO categories (category_id, category_name, category_icon, " +
                        "category_type, uuid, last_edit, deleted) VALUES (?, ?, ?, ?, ?, ?, 0)",
                new Object[] {id, name, ICON, Schema.CategoryType.EXPENSE, "category-" + id, EDIT});
    }

    private void insertBudget(SQLiteDatabase db, long id, int type, Long category) {
        db.execSQL("INSERT INTO budgets (budget_id, budget_type, budget_category, " +
                        "budget_start_date, budget_end_date, budget_money, budget_currency, uuid, " +
                        "last_edit, deleted) VALUES (?, ?, ?, '2026-09-01', '2026-09-30', 30000, " +
                        "'EUR', ?, ?, 0)",
                new Object[] {id, type, category, "budget-" + id, EDIT});
    }

    /**
     * A row of the join table as the app writes one. The uuid is deliberately not the one the fill
     * derives, because the app writes a random uuid here and a row carrying the derived one would
     * let the fill collide on the uuid instead of on the pair of keys.
     */
    private void insertBudgetCategory(SQLiteDatabase db, long budget, long category, boolean deleted) {
        db.execSQL("INSERT INTO budget_categories (_budget, _category, uuid, last_edit, deleted) " +
                        "VALUES (?, ?, ?, ?, ?)",
                new Object[] {budget, category, "3f7a1c9e-" + budget + "-" + category, EDIT,
                        deleted ? 1 : 0});
    }

    /**
     * A row the migration wrote or restamped carries the moment of the upgrade. Read as a window
     * and not as a floor, since a floor says nothing about a stamp in the future and nothing about
     * one shifted by a timezone. The shifted one is caught only where the machine running this is
     * not itself on UTC, and every offset in use is wider than the window.
     */
    private void assertStampedNow(SQLiteDatabase db, String sql) {
        long stamped = number(db, sql);
        long since = System.currentTimeMillis() - stamped;
        assertTrue("stamped " + stamped + ", which is " + since + "ms ago",
                since >= 0 && since < 300000L);
    }

    private Long categoryColumnOf(SQLiteDatabase db, long budget) {
        return number(db, "SELECT budget_category FROM budgets WHERE budget_id = " + budget);
    }

    private Long number(SQLiteDatabase db, String sql) {
        Cursor cursor = db.rawQuery(sql, null);
        try {
            assertTrue("no row for " + sql, cursor.moveToFirst());
            return cursor.isNull(0) ? null : cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    private long count(SQLiteDatabase db, String table, String selection) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table +
                (selection != null ? " WHERE " + selection : ""), null);
        try {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    private String text(SQLiteDatabase db, String sql) {
        Cursor cursor = db.rawQuery(sql, null);
        try {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        } finally {
            cursor.close();
        }
    }
}

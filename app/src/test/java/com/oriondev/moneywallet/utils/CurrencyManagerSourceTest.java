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

package com.oriondev.moneywallet.utils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The deadlock this class was rewritten to remove needed two things at once: a reader of the cache
 * that could block on a lock, and a reload that held that same lock while it read the database.
 * An importer runs a whole import inside one transaction, which holds the single SQLite connection
 * the app shares, and asks this class for a currency on every row. So the importer waited on the
 * lock while the reload waited on the connection.
 *
 * Only one of those two has to go for the cycle to be impossible, and it is the readers that went.
 * If nothing a thread inside a transaction can call acquires anything, that thread can never be
 * made to wait inside this class, whatever invalidateCache does. READERS below is every such
 * method, and it has to stay that way if a new one is added.
 *
 * invalidateCache does still hold a lock across the database, because two reloads have to be kept
 * from interleaving and nothing cheaper orders them correctly, and so does initialize, which does
 * the same load. That is safe only while the two things the second and third cases pin stay true:
 * nothing but those two takes that lock, and neither is called from inside a transaction. Both
 * refuse at runtime if anything does.
 *
 * What this is and is not. It reads the source and matches text, so it is a tripwire against the
 * old code coming back, not a proof that the new code is correct. Someone determined to reintroduce
 * a lock can pick a spelling none of these look for. The behavior that can be run instead is in
 * CurrencyManagerConcurrencyTest. Comments and string literals are stripped first, so nothing here
 * can be satisfied by a comment describing code the file no longer has, and whitespace is collapsed
 * so a statement may be wrapped, following ReportLoaderSourceTest.
 *
 * A failure means read the source and decide, not that the cache is broken.
 */
public class CurrencyManagerSourceTest {

    /** A java string literal, then a line comment, then a block comment. Literal first. */
    private static final Pattern STRIPPED = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|//.*?$|/[*].*?[*]/",
            Pattern.DOTALL | Pattern.MULTILINE);

    /** Every way of taking a lock that this file could plausibly be rewritten to use. */
    private static final String[] WAYS_TO_TAKE_A_LOCK = {
            "synchronized", ".lock()", ".lockInterruptibly()", ".tryLock(", "Lock ",
    };

    private static String readSource() {
        String path = "src/main/java/com/oriondev/moneywallet/utils/CurrencyManager.java";
        File file = new File(path);
        if (!file.exists()) {
            // a runner rooted at the repo root instead of the module, which the precedent
            // handles the same way
            file = new File("app/" + path);
        }
        if (!file.exists()) {
            fail("Cannot find CurrencyManager.java at " + file.getAbsolutePath());
        }
        try {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return STRIPPED.matcher(source).replaceAll(" ").replaceAll("\\s+", " ");
        } catch (IOException e) {
            fail("Cannot read CurrencyManager.java: " + e.getMessage());
            return null;
        }
    }

    /**
     * The signature and everything to its matching closing brace. Literals and comments are gone
     * by the time this runs and the file has no character literal, so every brace left is real.
     */
    private static String bodyOf(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(signature + " is gone", start >= 0);
        int depth = 0;
        for (int i = source.indexOf('{', start); i < source.length(); i++) {
            char character = source.charAt(i);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return source.substring(start, i + 1);
            }
        }
        fail("no closing brace found for " + signature);
        return null;
    }

    /** How many blocks deep a position sits inside a body. 1 is a statement of the method. */
    private static int depthOf(String body, int position) {
        int depth = 0;
        for (int i = 0; i < position; i++) {
            char character = body.charAt(i);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
            }
        }
        return depth;
    }

    /** Every way out of this class that a thread inside a database transaction can reach. */
    private static final String[] READERS = {
            "public static CurrencyUnit getCurrency(String iso) {",
            "public static Collection<CurrencyUnit> getCurrencies() {",
            "public static CurrencyUnit getDefaultCurrency() {",
            "public static ExchangeRate getExchangeRate(CurrencyUnit currency1, CurrencyUnit currency2) {",
            "public static ExchangeRateCache getExchangeRateCache() {",
            "public static int getDecimals(CurrencyUnit currency) {",
    };

    @Test
    public void noReaderOfTheCacheTakesALock() {
        String source = readSource();
        for (String reader : READERS) {
            String body = bodyOf(source, reader);
            for (String lock : WAYS_TO_TAKE_A_LOCK) {
                assertFalse(reader + " takes a lock again through " + lock.trim() + ". An importer "
                                + "calls in here while holding the one database connection, so a "
                                + "reader that can block is half of the deadlock this removed",
                        body.contains(lock));
            }
        }
    }

    /**
     * The reader cases above read one method body each, so a reader that took its lock inside a
     * helper it calls would pass every one of them. These close that from the other side, over the
     * whole file: every lock taken any way at all has to be RELOAD_LOCK, only invalidateCache and
     * initialize may name it, and the reload has to both load and publish inside it.
     */
    @Test
    public void theOnlyLockIsReloadLockAndOnlyTheReloadTakesIt() {
        String source = readSource();
        // over the WHOLE file and not just the reader bodies, because a reader that calls a
        // helper which locks passes every case that reads one body at a time
        for (String lock : WAYS_TO_TAKE_A_LOCK) {
            int taken = count(source, lock);
            int onReloadLock = "synchronized".equals(lock)
                    ? count(source, "synchronized (RELOAD_LOCK)") : 0;
            assertTrue("CurrencyManager takes a lock through " + lock.trim() + " that is not "
                            + "RELOAD_LOCK. Whichever method holds it, an importer holding the one "
                            + "database connection can be made to wait on it, which is half of the "
                            + "deadlock this removed",
                    taken == onReloadLock);
        }
        String reload = bodyOf(source, "public static void invalidateCache(Context context) {");
        String setUp = bodyOf(source, "public static void initialize(Context context) {");
        // the declaration, and every other mention inside invalidateCache. A helper that took
        // RELOAD_LOCK for a reader would raise the first count and not the second
        assertTrue("something other than invalidateCache and initialize names RELOAD_LOCK. It is "
                        + "held across a database read, which is only safe while the two methods "
                        + "that take it both refuse to run inside a transaction",
                count(source, "RELOAD_LOCK")
                        == count(reload, "RELOAD_LOCK") + count(setUp, "RELOAD_LOCK") + 1);
        // and the load has to be INSIDE it. Lifted out, the lock guards the assignment alone,
        // which nothing needs, and two reloads interleave their read and their publish again. The
        // cache then keeps whichever finished last, which is not whichever read last
        assertTrue("invalidateCache reads the currencies outside RELOAD_LOCK and only publishes "
                        + "under it. That is the interleaving this lock exists to stop: two "
                        + "reloads both read, and the cache keeps the one that finished last "
                        + "instead of the one that read last",
                depthOf(reload, reload.indexOf("loadCurrencies(context)")) == 2);
        // and so does the assignment. Reading under the lock and publishing outside it orders the
        // reads and nothing else, so the cache still keeps whichever reload finished last
        assertTrue("invalidateCache publishes outside RELOAD_LOCK. Two reloads then read one at a "
                        + "time and still publish in whatever order they finish, so a reload that "
                        + "read an older table can overwrite one that read a newer one",
                depthOf(reload, reload.indexOf("mInstance.mCurrencyCache =")) == 2);
        // initialize loads the same way and takes the same lock, so its load has to be inside it
        // too. Built outside and only assigned under the lock, a first launch can seed the table
        // while a reload on another thread is reading it
        // initialize is pinned by position and not by depth alone. Depth 2 is reached just as
        // well by the null check with no lock around it at all, which is the very shape the
        // message below names, so the construction has to sit AFTER the lock is entered
        for (String loader : new String[] {"invalidateCache", "initialize"}) {
            String body = "invalidateCache".equals(loader) ? reload : setUp;
            int locked = body.indexOf("synchronized (RELOAD_LOCK)");
            assertTrue(loader + " no longer takes RELOAD_LOCK at all, so the two loaders can run "
                    + "their read and their publish at the same time", locked >= 0);
            // before the lock, by position. At depth 1 but below the block it is useless, and a
            // caller already inside a transaction walks straight into the old deadlock
            int refuses = body.indexOf("refuseIfInsideATransaction()");
            assertTrue(loader + " no longer refuses before it takes RELOAD_LOCK. A caller that "
                            + "already holds a database connection then takes the lock across a "
                            + "read that needs it, which is the deadlock this removed",
                    refuses >= 0 && refuses < locked && depthOf(body, refuses) == 1);
        }
        assertTrue("initialize builds the instance outside RELOAD_LOCK and only assigns it under "
                        + "the lock, so its load and a reload can run at the same time. That is "
                        + "the interleaving the lock exists to stop",
                setUp.indexOf("new CurrencyManager(context)")
                        > setUp.indexOf("synchronized (RELOAD_LOCK)")
                        && depthOf(setUp, setUp.indexOf("new CurrencyManager(context)")) >= 2);
        // the map the readers walk without a lock is only safe while nothing can write to it
        assertTrue("loadCurrencies no longer wraps the map it publishes. A later write to a "
                        + "published map is what the volatile stops carrying, and the walk in "
                        + "CurrencyManagerConcurrencyTest is what breaks first",
                source.contains("return Collections.unmodifiableMap(currencies);"));
    }

    /**
     * The other half of that argument, and the half this file cannot see: the lock is held across
     * a database read, so it is only safe while no thread that already holds the one database
     * connection can reach invalidateCache. These are the three callers that exist, all of which
     * call it with no transaction open. A new one is not necessarily wrong, it just has to be
     * checked, which is what this is for.
     */
    @Test
    public void nothingNewCallsTheReloadWithoutBeingChecked() {
        String[] allowed = {
                "service/BackupHandlerIntentService.java",
                "service/UpgradeLegacyEditionIntentService.java",
                "ui/activity/NewEditCurrencyActivity.java",
                "App.java",
        };
        List<String> callers = new ArrayList<>();
        List<File> files = new ArrayList<>();
        for (File root : productionSourceRoots()) {
            files.addAll(sourceFiles(root));
        }
        for (File file : files) {
            String text = read(file);
            if (text.contains("CurrencyManager.invalidateCache(")
                    || text.contains("CurrencyManager.initialize(")) {
                String path = file.getPath().replace(File.separatorChar, '/');
                boolean known = false;
                for (String one : allowed) {
                    known = known || path.endsWith(one);
                }
                if (!known) {
                    callers.add(path);
                }
            }
        }
        assertTrue("a new caller of CurrencyManager.invalidateCache or initialize: " + callers
                        + ". Both hold a "
                        + "lock across a database read, so read the new caller and make sure it is "
                        + "not inside a transaction, then add it here",
                callers.isEmpty());
    }

    /**
     * Every production source set, not just main. floss, gmap, osm and proprietary are flavor
     * source sets and are compiled into the app exactly like main is, so a caller added in one of
     * them ships.
     */
    private static List<File> productionSourceRoots() {
        File app = new File("src");
        if (!app.isDirectory()) {
            app = new File("app/src");
        }
        assertTrue("cannot find the source root at " + app.getAbsolutePath(), app.isDirectory());
        List<File> roots = new ArrayList<>();
        File[] sets = app.listFiles();
        assertTrue("no source sets under " + app.getAbsolutePath(), sets != null);
        for (File set : sets) {
            String name = set.getName();
            if (set.isDirectory() && !name.equals("test") && !name.equals("androidTest")) {
                File java = new File(set, "java");
                if (java.isDirectory()) {
                    roots.add(java);
                }
            }
        }
        assertTrue("no production source set has a java directory", !roots.isEmpty());
        return roots;
    }

    private static List<File> sourceFiles(File directory) {
        List<File> found = new ArrayList<>();
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    found.addAll(sourceFiles(child));
                } else if (child.getName().endsWith(".java")) {
                    found.add(child);
                }
            }
        }
        return found;
    }

    private static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("cannot read " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + 1)) {
            found++;
        }
        return found;
    }

    @Test
    public void bothStaticsThatCarryTheCacheAreVolatile() {
        String source = readSource();
        // the cache itself, so a reader on another thread sees the map that was published and not
        // a stale one
        assertTrue("the currency cache field is no longer volatile",
                source.contains("private volatile Map<String, CurrencyUnit> mCurrencyCache;"));
        // and the holder, because the cache field is no longer final. A final field publishes the
        // object it points at even when its holder is reached through a race, and volatile does
        // not, so a racing reader could otherwise see mInstance set and its cache still null
        assertTrue("the singleton field is no longer volatile, and the cache field it carries is "
                        + "not final. getCurrency, getCurrencies, getExchangeRate and "
                        + "getExchangeRateCache all read mInstance with no lock, so without it a "
                        + "reader can see mInstance set and the map it carries still null",
                source.contains("private static volatile CurrencyManager mInstance;"));
    }
}

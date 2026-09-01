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

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.storage.database.DataContentProvider;

import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * getCurrencies hands out a view of the cache, and the rate download service walks that view
 * across a run of database writes. A currency edit or a restore reloads the cache in the middle of
 * that walk, and the reload used to empty the very map being walked, so the download thread got a
 * ConcurrentModificationException part way down the list of currencies it was storing rates for.
 *
 * The cache is now replaced instead of emptied, which leaves a walk already under way reading a
 * map nothing is touching. This drives the two against each other on purpose.
 */
public class CurrencyManagerConcurrencyTest {

    private static final long TIMEOUT_SECONDS = 30;

    /**
     * The lock invalidateCache holds is only safe while no caller already holds the one database
     * connection, so it refuses instead of deadlocking. This is the probe for that refusal, and
     * it is the half of the safety argument no source reading can settle.
     */
    @Test
    public void aReloadRefusesToRunInsideATransaction() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CurrencyManager.initialize(context);
        try {
            DataContentProvider.runInOneTransaction(context, () -> {
                CurrencyManager.invalidateCache(context);
                return null;
            });
            fail("invalidateCache ran inside a transaction instead of refusing");
        } catch (IllegalStateException expected) {
            assertTrue("refused for the wrong reason: " + expected.getMessage(),
                    expected.getMessage().contains("inside a transaction"));
        }
    }

    @Test
    public void aReloadDoesNotDisturbAWalkOfTheCurrencies() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CurrencyManager.initialize(context);
        Collection<CurrencyUnit> beforeTheReload = CurrencyManager.getCurrencies();
        int expected = beforeTheReload.size();
        // the walk has to reach a second element after the reload for anything to go wrong, so a
        // cache of one would pass this whichever way the code behaved
        assertTrue("only " + expected + " currencies installed, too few to walk", expected > 1);
        // a currency the cache holds now, so the reload can be shown to have kept it
        String knownIso = beforeTheReload.iterator().next().getIso();

        CountDownLatch walkStarted = new CountDownLatch(1);
        CountDownLatch reloadFinished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger walked = new AtomicInteger();

        Thread walker = new Thread(() -> {
            try {
                for (CurrencyUnit unit : CurrencyManager.getCurrencies()) {
                    assertNotNull("a null currency in the cache", unit);
                    if (walked.incrementAndGet() == 1) {
                        // hold the walk open on its first element until the reload has been and
                        // gone, so the two cannot pass each other by luck of the scheduler
                        walkStarted.countDown();
                        reloadFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    }
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        walker.start();
        assertTrue("the walk never started", walkStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        CurrencyManager.invalidateCache(context);
        reloadFinished.countDown();
        walker.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
        // join returns whether or not the walk ended, and reading its counters after a timeout
        // reads them with no ordering against the writes, so the failures below would describe
        // the wrong thing
        assertFalse("the walk was still running after " + TIMEOUT_SECONDS + " seconds",
                walker.isAlive());

        assertNull("a reload landing in the middle of a walk threw " + failure.get(),
                failure.get());
        assertEquals("the walk saw a different number of currencies than it started with",
                expected, walked.get());
        // without these three the case passes against an invalidateCache that publishes nothing,
        // and against one that publishes an empty map, and then it says nothing about a reload
        Collection<CurrencyUnit> afterTheReload = CurrencyManager.getCurrencies();
        assertNotSame("invalidateCache published no new map, so nothing was driven against the walk",
                beforeTheReload, afterTheReload);
        assertEquals("the reload published a different number of currencies than it read",
                expected, afterTheReload.size());
        assertNotNull("the reload published a map without " + knownIso + " in it, which the cache "
                + "held before it ran", CurrencyManager.getCurrency(knownIso));
    }
}

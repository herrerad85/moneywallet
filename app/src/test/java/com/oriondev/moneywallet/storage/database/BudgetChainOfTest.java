package com.oriondev.moneywallet.storage.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A roll names each period it opens after the chain, a colon, and the day that period begins on,
 * and {@link Contract#budgetChainOf(String)} is the one place that reads such a name back. It has
 * to be the exact inverse of that, because the answer decides which chain a period is asked about
 * and, through it, which budgets of that chain come after the one asking.
 *
 * Everything this app mints is safe whichever way the name is cut, since a minted uuid is plain
 * hexadecimal and carries no colon of its own. A uuid restored from a backup file is whatever that
 * file held, and that is what these pin: a chain root holding a colon must stay the whole root,
 * because reading part of it as the chain put the root after its own periods in the byte order the
 * selection uses, and every period of that chain then read as history and could not be saved while
 * it repeated.
 */
public class BudgetChainOfTest {

    private static final String ROOT = "3f7a1c9e2b4d5a6f";

    @Test
    public void aBudgetThatStartedItsOwnChainIsItsOwnAnswer() {
        assertEquals(ROOT, Contract.budgetChainOf(ROOT));
    }

    @Test
    public void aPeriodGivesTheChainItWasOpenedFor() {
        assertEquals(ROOT, Contract.budgetChainOf(ROOT + ":2026-08-15"));
    }

    @Test
    public void aRootHoldingAColonKeepsAllOfIt() {
        assertEquals("weird:root", Contract.budgetChainOf("weird:root"));
        assertEquals("weird:root", Contract.budgetChainOf("weird:root:2026-08-15"));
    }

    /**
     * The whole point of keeping it: a root and its periods have to sort the way the roll opened
     * them, which holds when the root is a prefix of every period of its chain and fails when a
     * shorter reading of it is used instead.
     */
    @Test
    public void aRootHoldingAColonStillSortsBeforeItsOwnPeriods() {
        String root = "weird:root";
        String august = Contract.budgetChainOf(root) + ":2026-08-15";
        String september = Contract.budgetChainOf(root) + ":2026-09-15";
        assertTrue(root.compareTo(august) < 0);
        assertTrue(august.compareTo(september) < 0);
    }

    @Test
    public void onlyARealDayIsTakenOff() {
        assertEquals("a:notaday", Contract.budgetChainOf("a:notaday"));
        assertEquals("a:2026-8-15", Contract.budgetChainOf("a:2026-8-15"));
        assertEquals("a:2026-08-15x", Contract.budgetChainOf("a:2026-08-15x"));
        assertEquals("a:", Contract.budgetChainOf("a:"));
    }

    /** One day is appended per period, so only one comes off. */
    @Test
    public void onlyOneDayIsTakenOff() {
        assertEquals(ROOT + ":2026-07-15", Contract.budgetChainOf(ROOT + ":2026-07-15:2026-08-15"));
    }
}

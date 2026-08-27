package com.oriondev.moneywallet.storage.database;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * What a saving withdrawal dated D may take is the lowest the saving's balance reaches from D
 * onwards, and {@link Contract#lowestSavingBalanceFrom(long, String[], long[], String)} is the one
 * place that works that out. The transaction editor holds every withdrawal to it.
 *
 * The two sums a saving carries cannot answer it. Both are totals over the whole set of rows and
 * neither holds any date order, so a withdrawal dated before the deposit that funds it cleared
 * both of them and left the saving under zero for the days between the two. That is the reported
 * bug, and the first two cases here are the numbers it was reported with.
 *
 * A row dated at or before the moment asked about is part of the balance at that moment, however
 * far under zero the saving went on the way there, because a withdrawal written today cannot take
 * back a day that has already passed.
 */
public class LowestSavingBalanceFromTest {

    private static final String[] NO_DATES = new String[0];
    private static final long[] NO_MONEY = new long[0];

    private static final String SEPTEMBER_10 = "2026-09-10 12:00:00";
    private static final String SEPTEMBER_30 = "2026-09-30 12:00:00";
    private static final String OCTOBER_01 = "2026-10-01 12:00:00";

    @Test
    public void aSavingWithNoRowsIsWorthItsStartMoney() {
        assertEquals(150000L, Contract.lowestSavingBalanceFrom(150000L, NO_DATES, NO_MONEY,
                SEPTEMBER_10));
    }

    @Test
    public void aDepositDatedAheadDoesNotPayForAWithdrawalDatedBeforeIt() {
        assertEquals(0L, Contract.lowestSavingBalanceFrom(0L,
                new String[] {SEPTEMBER_30}, new long[] {300000L}, SEPTEMBER_10));
    }

    @Test
    public void theSameDepositPaysForAWithdrawalDatedAfterIt() {
        assertEquals(300000L, Contract.lowestSavingBalanceFrom(0L,
                new String[] {SEPTEMBER_30}, new long[] {300000L}, OCTOBER_01));
    }

    @Test
    public void aSavingWithMoneyInItOffersOnlyWhatItHoldsOnTheDayAsked() {
        assertEquals(150000L, Contract.lowestSavingBalanceFrom(150000L,
                new String[] {SEPTEMBER_30}, new long[] {300000L}, SEPTEMBER_10));
    }

    @Test
    public void aRowDatedExactlyTheMomentAskedAboutIsAlreadyCounted() {
        assertEquals(300000L, Contract.lowestSavingBalanceFrom(0L,
                new String[] {SEPTEMBER_30}, new long[] {300000L}, SEPTEMBER_30));
    }

    @Test
    public void aDipAfterTheMomentAskedAboutIsTheAnswer() {
        // 3,000.00 in on 10 September and 2,500.00 out on 30 September, asked from 10 September:
        // the saving is worth 3,000.00 on the day and 500.00 twenty days later, and a withdrawal
        // written now takes from both.
        assertEquals(50000L, Contract.lowestSavingBalanceFrom(0L,
                new String[] {SEPTEMBER_10, SEPTEMBER_30},
                new long[] {300000L, -250000L}, SEPTEMBER_10));
    }

    @Test
    public void aDipBeforeTheMomentAskedAboutIsNotTheAnswer() {
        // 2,500.00 out on 10 September against a start money of 3,000.00, then 1,000.00 back in
        // on 30 September. Asked from 1 October the saving is worth 1,500.00, and the 500.00 it
        // was worth in between is a day nothing written now can reach.
        assertEquals(150000L, Contract.lowestSavingBalanceFrom(300000L,
                new String[] {SEPTEMBER_10, SEPTEMBER_30},
                new long[] {-250000L, 100000L}, OCTOBER_01));
    }

    @Test
    public void aDipBetweenTwoLaterRowsIsTheAnswer() {
        // 2,000.00 out on 30 September and 5,000.00 back in on 1 October, against a start money
        // of 3,000.00, asked from 10 September. The saving is worth 3,000.00 on the day, 1,000.00
        // for one day, and 6,000.00 after that, so a withdrawal written now may take 1,000.00.
        // Every other case here has at most one row after the moment asked about, so a walk that
        // compares only the first and the last balance passes all of them and answers 3,000.00
        // here, which is three times what the saving can give.
        assertEquals(100000L, Contract.lowestSavingBalanceFrom(300000L,
                new String[] {SEPTEMBER_30, OCTOBER_01},
                new long[] {-200000L, 500000L}, SEPTEMBER_10));
    }

    @Test
    public void rowsSharingADateAreCountedTogether() {
        // A 1,500.00 withdrawal and a 2,000.00 deposit both dated 30 September, against a start
        // money of 1,000.00, asked from 10 September. Two rows can carry the same date string,
        // and a database imported from the old app carries 00:00:00 on every row, so a whole
        // day of them ties. The saving is worth 1,000.00 until 30 September and 1,500.00 after,
        // and it is never worth the 500.00 negative the withdrawal on its own would make it.
        // Which of the two comes back first is not something the query decides, so both orders
        // have to give the same answer.
        assertEquals(100000L, Contract.lowestSavingBalanceFrom(100000L,
                new String[] {SEPTEMBER_30, SEPTEMBER_30},
                new long[] {-150000L, 200000L}, SEPTEMBER_10));
        assertEquals(100000L, Contract.lowestSavingBalanceFrom(100000L,
                new String[] {SEPTEMBER_30, SEPTEMBER_30},
                new long[] {200000L, -150000L}, SEPTEMBER_10));
        // And where the pair nets downward, the end of the run is the answer, so it has to be
        // read. The two above are both cleared by a walk that skips a run of more than one row
        // and never looks at where it ends, because the saving is worth more after that run
        // than the 1,000.00 it started from. This one is not: 1,500.00 and 1,000.00 both out on
        // 30 September leave the saving 1,500.00 in the red from that date, and a walk that
        // skips the run answers 1,000.00 and offers a withdrawal the saving cannot cover.
        assertEquals(-150000L, Contract.lowestSavingBalanceFrom(100000L,
                new String[] {SEPTEMBER_30, SEPTEMBER_30},
                new long[] {-150000L, -100000L}, SEPTEMBER_10));
    }

    @Test
    public void aSavingAlreadyUnderZeroAnswersBelowZero() {
        assertEquals(-100000L, Contract.lowestSavingBalanceFrom(0L,
                new String[] {SEPTEMBER_10}, new long[] {-100000L}, SEPTEMBER_10));
    }
}

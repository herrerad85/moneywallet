/*
 * Copyright (c) 2026.
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

package com.oriondev.moneywallet.storage.wrapper;

import com.oriondev.moneywallet.model.Group;
import com.oriondev.moneywallet.storage.database.Contract;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * A group header on the transactions list carries three figures now, what came in, what went out
 * and the difference. The first two are what a reader compares, so neither may cancel against the
 * other, and the third has to stay the difference of them or the header contradicts itself.
 *
 * Group.DAILY is used throughout because the other groupings ask PreferenceManager for the first
 * day of the week or the month, which needs a running app. The arithmetic under test does not
 * depend on which grouping built the header.
 */
public class TransactionHeaderMoneyTest {

    private static final String USD = "USD";

    private TransactionHeaderCursor.Header header() {
        Date date = new Date(0);
        return new TransactionHeaderCursor.Header(Group.DAILY, null, null, date);
    }

    @Test
    public void incomeAndExpenseAreBothPositiveAndTheTotalIsTheDifference() {
        TransactionHeaderCursor.Header header = header();
        header.add(USD, 120000, Contract.Direction.INCOME);
        header.add(USD, 45000, Contract.Direction.INCOME);
        header.add(USD, 60000, Contract.Direction.EXPENSE);
        header.add(USD, 4000, Contract.Direction.EXPENSE);
        assertEquals(165000, header.getIncome().getMoney(USD));
        assertEquals(64000, header.getExpense().getMoney(USD));
        assertEquals(101000, header.getMoney().getMoney(USD));
    }

    @Test
    public void spendingMoreThanCameInLeavesTheTotalNegativeAndTheOtherTwoUntouched() {
        TransactionHeaderCursor.Header header = header();
        header.add(USD, 1000, Contract.Direction.INCOME);
        header.add(USD, 3000, Contract.Direction.EXPENSE);
        assertEquals(1000, header.getIncome().getMoney(USD));
        assertEquals(3000, header.getExpense().getMoney(USD));
        assertEquals(-2000, header.getMoney().getMoney(USD));
    }

    /**
     * A figure counts only the rows going its own way, so the other one is left holding no
     * currency at all. What the screen does with that is TransactionCursorAdapter.orZero, and
     * this is the half of it the header owes: a currency here would be one nobody spent or
     * earned, and on a header counting two currencies it would be a second amount to read.
     */
    @Test
    public void aHeaderOfExpensesOnlyLeavesTheIncomeHoldingNothing() {
        TransactionHeaderCursor.Header header = header();
        header.add(USD, 4500, Contract.Direction.EXPENSE);
        assertEquals(0, header.getIncome().getNumberOfCurrencies());
        assertEquals(4500, header.getExpense().getMoney(USD));
        assertEquals(-4500, header.getMoney().getMoney(USD));
    }

    @Test
    public void aHeaderOfIncomeOnlyLeavesTheExpenseHoldingNothing() {
        TransactionHeaderCursor.Header header = header();
        header.add(USD, 4500, Contract.Direction.INCOME);
        assertEquals(0, header.getExpense().getNumberOfCurrencies());
        assertEquals(4500, header.getIncome().getMoney(USD));
        assertEquals(4500, header.getMoney().getMoney(USD));
    }

    @Test
    public void currenciesAreKeptApart() {
        TransactionHeaderCursor.Header header = header();
        header.add(USD, 1000, Contract.Direction.INCOME);
        header.add("EUR", 700, Contract.Direction.EXPENSE);
        assertEquals(1000, header.getIncome().getMoney(USD));
        assertEquals(0, header.getIncome().getMoney("EUR"));
        assertEquals(700, header.getExpense().getMoney("EUR"));
        assertEquals(1000, header.getMoney().getMoney(USD));
        assertEquals(-700, header.getMoney().getMoney("EUR"));
    }
}

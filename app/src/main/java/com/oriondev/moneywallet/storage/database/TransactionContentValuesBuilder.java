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

import android.content.ContentValues;

/**
 * Single home, outside {@link SQLDatabase}, for the set of {@code Contract.Transaction}
 * columns that make up a transaction row on the write path (inserts and updates through
 * the content provider). Each column name is written here rather than repeated at every
 * call site.
 *
 * Every setter is optional: a caller sets only the columns it actually provides, so the
 * per-caller column set is preserved. A nullable column keeps its caller's null convention,
 * call the setter (even with a null argument) to write the column, or omit the call to
 * leave it unset.
 */
public class TransactionContentValuesBuilder {

    private final ContentValues mValues = new ContentValues();

    public TransactionContentValuesBuilder money(Long value) {
        mValues.put(Contract.Transaction.MONEY, value);
        return this;
    }

    public TransactionContentValuesBuilder date(String value) {
        mValues.put(Contract.Transaction.DATE, value);
        return this;
    }

    public TransactionContentValuesBuilder description(String value) {
        mValues.put(Contract.Transaction.DESCRIPTION, value);
        return this;
    }

    public TransactionContentValuesBuilder categoryId(long value) {
        mValues.put(Contract.Transaction.CATEGORY_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder direction(int value) {
        mValues.put(Contract.Transaction.DIRECTION, value);
        return this;
    }

    public TransactionContentValuesBuilder type(int value) {
        mValues.put(Contract.Transaction.TYPE, value);
        return this;
    }

    public TransactionContentValuesBuilder walletId(long value) {
        mValues.put(Contract.Transaction.WALLET_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder placeId(Long value) {
        mValues.put(Contract.Transaction.PLACE_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder note(String value) {
        mValues.put(Contract.Transaction.NOTE, value);
        return this;
    }

    public TransactionContentValuesBuilder eventId(Long value) {
        mValues.put(Contract.Transaction.EVENT_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder savingId(Long value) {
        mValues.put(Contract.Transaction.SAVING_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder debtId(Long value) {
        mValues.put(Contract.Transaction.DEBT_ID, value);
        return this;
    }

    public TransactionContentValuesBuilder recurrenceId(long value) {
        mValues.put(Contract.Transaction.RECURRENCE_ID, value);
        return this;
    }

    // Two confirmed()/countInTotal() overloads exist on purpose: most call sites store a
    // boolean, but the model-to-transaction mapping stores the raw cursor int. Keeping both
    // signatures preserves each site's exact ContentValues value type with no conversion.

    public TransactionContentValuesBuilder confirmed(boolean value) {
        mValues.put(Contract.Transaction.CONFIRMED, value);
        return this;
    }

    public TransactionContentValuesBuilder confirmed(int value) {
        mValues.put(Contract.Transaction.CONFIRMED, value);
        return this;
    }

    public TransactionContentValuesBuilder countInTotal(boolean value) {
        mValues.put(Contract.Transaction.COUNT_IN_TOTAL, value);
        return this;
    }

    public TransactionContentValuesBuilder countInTotal(int value) {
        mValues.put(Contract.Transaction.COUNT_IN_TOTAL, value);
        return this;
    }

    public TransactionContentValuesBuilder peopleIds(String value) {
        mValues.put(Contract.Transaction.PEOPLE_IDS, value);
        return this;
    }

    public TransactionContentValuesBuilder attachmentIds(String value) {
        mValues.put(Contract.Transaction.ATTACHMENT_IDS, value);
        return this;
    }

    public ContentValues build() {
        return mValues;
    }
}

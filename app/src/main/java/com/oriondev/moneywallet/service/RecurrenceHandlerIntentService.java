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

package com.oriondev.moneywallet.service;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.oriondev.moneywallet.broadcast.RecurrenceBroadcastReceiver;
import com.oriondev.moneywallet.model.RecurrenceSetting;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.TransactionContentValuesBuilder;
import com.oriondev.moneywallet.storage.database.TransferContentValuesBuilder;
import com.oriondev.moneywallet.utils.DateUtils;

import java.util.Date;

/**
 * Created by andrea on 11/11/18.
 */
public class RecurrenceHandlerIntentService extends JobIntentService {

    private static final int JOB_ID = 3564;

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, RecurrenceHandlerIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        addMissingRecurrentTransactionOccurrences();
        addMissingRecurrentTransferOccurrences();
        RecurrenceBroadcastReceiver.scheduleRecurrenceTask(this);
    }

    private void addMissingRecurrentTransactionOccurrences() {
        Uri uri = DataContentProvider.CONTENT_RECURRENT_TRANSACTIONS;
        String selection = Contract.RecurrentTransaction.NEXT_OCCURRENCE + " IS NOT NULL AND DATE(" + Contract.RecurrentTransaction.NEXT_OCCURRENCE + ") <= DATE('now', 'localtime')";
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                // get basic information about the recurrence entity
                long transactionId = cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.ID));
                String firstOccurrenceDateString = cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransaction.NEXT_OCCURRENCE));
                Date firstOccurrenceDate = DateUtils.getDateFromSQLDateString(firstOccurrenceDateString);
                String rule = cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransaction.RULE));
                RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(rule, firstOccurrenceDate, new Date());
                for (Date occurrenceDate : update.getOccurrenceDates()) {
                    TransactionContentValuesBuilder builder = new TransactionContentValuesBuilder()
                            .money(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.MONEY)))
                            .date(DateUtils.getSQLDateTimeString(occurrenceDate))
                            .description(cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransaction.DESCRIPTION)))
                            .categoryId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.CATEGORY_ID)))
                            .direction(cursor.getInt(cursor.getColumnIndex(Contract.RecurrentTransaction.DIRECTION)))
                            .type(Contract.TransactionType.STANDARD)
                            .walletId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.WALLET_ID)))
                            .note(cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransaction.NOTE)));
                    if (!cursor.isNull(cursor.getColumnIndex(Contract.RecurrentTransaction.PLACE_ID))) {
                        builder.placeId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.PLACE_ID)));
                    }
                    if (!cursor.isNull(cursor.getColumnIndex(Contract.RecurrentTransaction.EVENT_ID))) {
                        builder.eventId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransaction.EVENT_ID)));
                    }
                    builder.recurrenceId(transactionId)
                            .confirmed(cursor.getInt(cursor.getColumnIndex(Contract.RecurrentTransaction.CONFIRMED)) == 1)
                            .countInTotal(cursor.getInt(cursor.getColumnIndex(Contract.RecurrentTransaction.COUNT_IN_TOTAL)) == 1);
                    ContentValues contentValues = builder.build();
                    getContentResolver().insert(DataContentProvider.CONTENT_TRANSACTIONS, contentValues);
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(Contract.RecurrentTransaction.LAST_OCCURRENCE, DateUtils.getSQLDateString(update.getLastOccurrence()));
                contentValues.put(Contract.RecurrentTransaction.NEXT_OCCURRENCE, update.getNextOccurrence() != null ? DateUtils.getSQLDateString(update.getNextOccurrence()) : null);
                Uri contentUri = ContentUris.withAppendedId(DataContentProvider.CONTENT_RECURRENT_TRANSACTIONS, transactionId);
                getContentResolver().update(contentUri, contentValues, null, null);
            }
            cursor.close();
        }
    }

    private void addMissingRecurrentTransferOccurrences() {
        Uri uri = DataContentProvider.CONTENT_RECURRENT_TRANSFERS;
        String selection = Contract.RecurrentTransfer.NEXT_OCCURRENCE + " IS NOT NULL AND DATE(" + Contract.RecurrentTransfer.NEXT_OCCURRENCE + ") <= DATE('now', 'localtime')";
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                // get basic information about the recurrence entity
                long recurrenceId = cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.ID));
                String firstOccurrenceDateString = cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransfer.NEXT_OCCURRENCE));
                Date firstOccurrenceDate = DateUtils.getDateFromSQLDateString(firstOccurrenceDateString);
                String rule = cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransfer.RULE));
                RecurrenceSetting.OccurrenceUpdate update = RecurrenceSetting.computeOccurrences(rule, firstOccurrenceDate, new Date());
                for (Date occurrenceDate : update.getOccurrenceDates()) {
                    TransferContentValuesBuilder builder = new TransferContentValuesBuilder()
                            .description(cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransfer.DESCRIPTION)))
                            .date(DateUtils.getSQLDateTimeString(occurrenceDate))
                            .fromWalletId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.WALLET_FROM_ID)))
                            .toWalletId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.WALLET_TO_ID)))
                            .taxWalletId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.WALLET_FROM_ID)))
                            .fromMoney(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.MONEY_FROM)))
                            .toMoney(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.MONEY_TO)))
                            .taxMoney(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.MONEY_TAX)))
                            .note(cursor.getString(cursor.getColumnIndex(Contract.RecurrentTransfer.NOTE)));
                    if (!cursor.isNull(cursor.getColumnIndex(Contract.RecurrentTransfer.PLACE_ID))) {
                        builder.placeId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.PLACE_ID)));
                    }
                    if (!cursor.isNull(cursor.getColumnIndex(Contract.RecurrentTransfer.EVENT_ID))) {
                        builder.eventId(cursor.getLong(cursor.getColumnIndex(Contract.RecurrentTransfer.EVENT_ID)));
                    }
                    builder.recurrenceId(recurrenceId)
                            .confirmed(cursor.getInt(cursor.getColumnIndex(Contract.RecurrentTransfer.CONFIRMED)) == 1)
                            .countInTotal(cursor.getInt(cursor.getColumnIndex(Contract.RecurrentTransfer.COUNT_IN_TOTAL)) == 1);
                    ContentValues contentValues = builder.build();
                    getContentResolver().insert(DataContentProvider.CONTENT_TRANSFERS, contentValues);
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(Contract.RecurrentTransfer.LAST_OCCURRENCE, DateUtils.getSQLDateString(update.getLastOccurrence()));
                contentValues.put(Contract.RecurrentTransfer.NEXT_OCCURRENCE, update.getNextOccurrence() != null ? DateUtils.getSQLDateString(update.getNextOccurrence()) : null);
                Uri contentUri = ContentUris.withAppendedId(DataContentProvider.CONTENT_RECURRENT_TRANSFERS, recurrenceId);
                getContentResolver().update(contentUri, contentValues, null, null);
            }
            cursor.close();
        }
    }
}
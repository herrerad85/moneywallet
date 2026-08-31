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

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oriondev.moneywallet.BuildConfig;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;
import com.oriondev.moneywallet.ui.widget.WalletWidgetProvider;

import java.util.List;

/**
 * Created by andrea on 17/01/18.
 */
public class DataContentProvider extends ContentProvider {

    /*package-local*/ static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".storage.data";

    public static final Uri CONTENT_CURRENCIES = Uri.parse("content://" + AUTHORITY + "/currencies");
    public static final Uri CONTENT_WALLETS = Uri.parse("content://" + AUTHORITY + "/wallets");
    public static final Uri CONTENT_TRANSACTIONS = Uri.parse("content://" + AUTHORITY + "/transactions");
    public static final Uri CONTENT_TRANSFERS = Uri.parse("content://" + AUTHORITY + "/transfers");
    public static final Uri CONTENT_CATEGORIES = Uri.parse("content://" + AUTHORITY + "/categories");
    public static final Uri CONTENT_DEBTS = Uri.parse("content://" + AUTHORITY + "/debts");
    public static final Uri CONTENT_BUDGETS = Uri.parse("content://" + AUTHORITY + "/budgets");
    public static final Uri CONTENT_SAVINGS = Uri.parse("content://" + AUTHORITY + "/savings");
    public static final Uri CONTENT_EVENTS = Uri.parse("content://" + AUTHORITY + "/events");
    public static final Uri CONTENT_RECURRENT_TRANSACTIONS = Uri.parse("content://" + AUTHORITY + "/recurrences/transactions");
    public static final Uri CONTENT_RECURRENT_TRANSFERS = Uri.parse("content://" + AUTHORITY + "/recurrences/transfers");
    public static final Uri CONTENT_TRANSACTION_MODELS = Uri.parse("content://" + AUTHORITY + "/models/transactions");
    public static final Uri CONTENT_TRANSFER_MODELS = Uri.parse("content://" + AUTHORITY + "/models/transfers");
    public static final Uri CONTENT_PLACES = Uri.parse("content://" + AUTHORITY + "/places");
    public static final Uri CONTENT_PEOPLE = Uri.parse("content://" + AUTHORITY + "/people");
    public static final Uri CONTENT_ATTACHMENTS = Uri.parse("content://" + AUTHORITY + "/attachments");

    private static final int CURRENCY_LIST = 1;
    private static final int WALLET_LIST = 2;
    private static final int TRANSACTION_LIST = 3;
    private static final int TRANSFER_LIST = 4;
    private static final int CATEGORY_LIST = 5;
    private static final int DEBT_LIST = 6;
    private static final int BUDGET_LIST = 7;
    private static final int SAVING_LIST = 8;
    private static final int EVENT_LIST = 9;
    private static final int RECURRENT_TRANSACTION_LIST = 10;
    private static final int RECURRENT_TRANSFER_LIST = 11;
    private static final int TRANSACTION_MODEL_LIST = 12;
    private static final int TRANSFER_MODEL_LIST = 13;
    private static final int PLACE_LIST = 14;
    private static final int PERSON_LIST = 15;
    private static final int ATTACHMENT_LIST = 16;
    private static final int ATTACHMENT_ITEM = 17;

    private static final int CURRENCY_ITEM = 18;
    private static final int WALLET_ITEM = 19;
    private static final int TRANSACTION_ITEM = 20;
    private static final int TRANSFER_ITEM = 21;
    private static final int CATEGORY_ITEM = 22;
    private static final int DEBT_ITEM = 23;
    private static final int BUDGET_ITEM = 24;
    private static final int SAVING_ITEM = 25;
    private static final int EVENT_ITEM = 26;
    private static final int RECURRENT_TRANSACTION_ITEM = 27;
    private static final int RECURRENT_TRANSFER_ITEM = 28;
    private static final int TRANSACTION_MODEL_ITEM = 29;
    private static final int TRANSFER_MODEL_ITEM = 30;
    private static final int PLACE_ITEM = 31;
    private static final int PERSON_ITEM = 32;

    private static final int TRANSACTION_ATTACHMENTS = 33;
    private static final int TRANSACTION_PEOPLE = 34;
    private static final int TRANSFER_ATTACHMENTS = 35;
    private static final int TRANSFER_PEOPLE = 36;
    private static final int DEBT_PEOPLE = 37;
    private static final int BUDGET_WALLETS = 38;
    private static final int BUDGET_CATEGORIES = 46;

    private static final int CATEGORY_TRANSACTION_LIST = 39;
    private static final int DEBT_TRANSACTION_LIST = 40;
    private static final int BUDGET_TRANSACTION_LIST = 41;
    private static final int SAVING_TRANSACTION_LIST = 42;
    private static final int EVENT_TRANSACTION_LIST = 43;
    private static final int PLACE_TRANSACTION_LIST = 44;
    private static final int PERSON_TRANSACTION_LIST = 45;

    private static final UriMatcher mUriMatcher = createUriMatcher();

    private static UriMatcher createUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, "currencies", CURRENCY_LIST);
        matcher.addURI(AUTHORITY, "currencies/*", CURRENCY_ITEM);
        matcher.addURI(AUTHORITY, "wallets", WALLET_LIST);
        matcher.addURI(AUTHORITY, "wallets/#", WALLET_ITEM);
        matcher.addURI(AUTHORITY, "transactions", TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "transactions/#", TRANSACTION_ITEM);
        matcher.addURI(AUTHORITY, "transactions/#/attachments", TRANSACTION_ATTACHMENTS);
        matcher.addURI(AUTHORITY, "transactions/#/people", TRANSACTION_PEOPLE);
        matcher.addURI(AUTHORITY, "transfers", TRANSFER_LIST);
        matcher.addURI(AUTHORITY, "transfers/#", TRANSFER_ITEM);
        matcher.addURI(AUTHORITY, "transfers/#/attachments", TRANSFER_ATTACHMENTS);
        matcher.addURI(AUTHORITY, "transfers/#/people", TRANSFER_PEOPLE);
        matcher.addURI(AUTHORITY, "categories", CATEGORY_LIST);
        matcher.addURI(AUTHORITY, "categories/#", CATEGORY_ITEM);
        matcher.addURI(AUTHORITY, "categories/#/transactions", CATEGORY_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "debts", DEBT_LIST);
        matcher.addURI(AUTHORITY, "debts/#", DEBT_ITEM);
        matcher.addURI(AUTHORITY, "debts/#/people", DEBT_PEOPLE);
        matcher.addURI(AUTHORITY, "debts/#/transactions", DEBT_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "budgets", BUDGET_LIST);
        matcher.addURI(AUTHORITY, "budgets/#", BUDGET_ITEM);
        matcher.addURI(AUTHORITY, "budgets/#/wallets", BUDGET_WALLETS);
        matcher.addURI(AUTHORITY, "budgets/#/categories", BUDGET_CATEGORIES);
        matcher.addURI(AUTHORITY, "budgets/#/transactions", BUDGET_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "savings", SAVING_LIST);
        matcher.addURI(AUTHORITY, "savings/#", SAVING_ITEM);
        matcher.addURI(AUTHORITY, "savings/#/transactions", SAVING_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "events", EVENT_LIST);
        matcher.addURI(AUTHORITY, "events/#", EVENT_ITEM);
        matcher.addURI(AUTHORITY, "events/#/transactions", EVENT_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "recurrences/transactions", RECURRENT_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "recurrences/transactions/#", RECURRENT_TRANSACTION_ITEM);
        matcher.addURI(AUTHORITY, "recurrences/transfers", RECURRENT_TRANSFER_LIST);
        matcher.addURI(AUTHORITY, "recurrences/transfers/#", RECURRENT_TRANSFER_ITEM);
        matcher.addURI(AUTHORITY, "models/transactions", TRANSACTION_MODEL_LIST);
        matcher.addURI(AUTHORITY, "models/transactions/#", TRANSACTION_MODEL_ITEM);
        matcher.addURI(AUTHORITY, "models/transfers", TRANSFER_MODEL_LIST);
        matcher.addURI(AUTHORITY, "models/transfers/#", TRANSFER_MODEL_ITEM);
        matcher.addURI(AUTHORITY, "places", PLACE_LIST);
        matcher.addURI(AUTHORITY, "places/#", PLACE_ITEM);
        matcher.addURI(AUTHORITY, "places/#/transactions", PLACE_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "people", PERSON_LIST);
        matcher.addURI(AUTHORITY, "people/#", PERSON_ITEM);
        matcher.addURI(AUTHORITY, "people/#/transactions", PERSON_TRANSACTION_LIST);
        matcher.addURI(AUTHORITY, "attachments", ATTACHMENT_LIST);
        matcher.addURI(AUTHORITY, "attachments/#", ATTACHMENT_ITEM);
        return matcher;
    }

    /**
     * Read on every use instead of held in a field. A restore swaps the file underneath both
     * providers and closes the old helper, and a field would then be a closed handle until the
     * next process start.
     */
    private SQLDatabase db() {
        return SQLDatabase.getShared(getContext());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        switch (mUriMatcher.match(uri)) {
            case CURRENCY_LIST:
                cursor = new MultiUriCursorWrapper(db().getCurrencies(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_CURRENCIES);
                break;
            case CURRENCY_ITEM:
                cursor = new MultiUriCursorWrapper(db().getCurrency(uri.getLastPathSegment(), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                break;
            case WALLET_LIST:
                cursor = new MultiUriCursorWrapper(db().getWallets(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                break;
            case WALLET_ITEM:
                cursor = new MultiUriCursorWrapper(db().getWallet(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                break;
            case TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getTransactions(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSACTION_ITEM:
                cursor = new MultiUriCursorWrapper(db().getTransaction(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSACTION_ATTACHMENTS:
                cursor = new MultiUriCursorWrapper(db().getTransactionAttachments(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSACTION_PEOPLE:
                cursor = new MultiUriCursorWrapper(db().getTransactionPeople(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                break;
            case TRANSFER_LIST:
                cursor = new MultiUriCursorWrapper(db().getTransfers(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSFER_ITEM:
                cursor = new MultiUriCursorWrapper(db().getTransfer(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSFER_ATTACHMENTS:
                cursor = new MultiUriCursorWrapper(db().getTransferAttachments(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case TRANSFER_PEOPLE:
                cursor = new MultiUriCursorWrapper(db().getTransferPeople(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                break;
            case CATEGORY_LIST:
                cursor = new MultiUriCursorWrapper(db().getCategories(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                break;
            case CATEGORY_ITEM:
                cursor = new MultiUriCursorWrapper(db().getCategory(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                break;
            case CATEGORY_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getCategoryTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                break;
            case DEBT_LIST:
                cursor = new MultiUriCursorWrapper(db().getDebts(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case DEBT_ITEM:
                cursor = new MultiUriCursorWrapper(db().getDebt(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case DEBT_PEOPLE:
                cursor = new MultiUriCursorWrapper(db().getDebtPeople(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                // Saving a debt's master transaction now writes the debt's people, so a write
                // to a transaction can change what this cursor holds.
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case DEBT_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getDebtTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case BUDGET_LIST:
                cursor = new MultiUriCursorWrapper(db().getBudgets(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_BUDGETS);
                break;
            case BUDGET_ITEM:
                cursor = new MultiUriCursorWrapper(db().getBudget(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case BUDGET_WALLETS:
                cursor = new MultiUriCursorWrapper(db().getBudgetWallets(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_BUDGETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                break;
            case BUDGET_CATEGORIES:
                cursor = new MultiUriCursorWrapper(db().getBudgetCategories(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_BUDGETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                break;
            case BUDGET_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getBudgetTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_BUDGETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case SAVING_LIST:
                cursor = new MultiUriCursorWrapper(db().getSavings(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_SAVINGS);
                break;
            case SAVING_ITEM:
                cursor = new MultiUriCursorWrapper(db().getSaving(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case SAVING_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getSavingTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_SAVINGS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case EVENT_LIST:
                cursor = new MultiUriCursorWrapper(db().getEvents(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                break;
            case EVENT_ITEM:
                cursor = new MultiUriCursorWrapper(db().getEvent(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case EVENT_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getEventTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case RECURRENT_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getRecurrentTransactions(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTION_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_RECURRENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case RECURRENT_TRANSACTION_ITEM:
                cursor = new MultiUriCursorWrapper(db().getRecurrentTransaction(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTION_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_RECURRENT_TRANSACTIONS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case RECURRENT_TRANSFER_LIST:
                cursor = new MultiUriCursorWrapper(db().getRecurrentTransfers(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFER_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_RECURRENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case RECURRENT_TRANSFER_ITEM:
                cursor = new MultiUriCursorWrapper(db().getRecurrentTransfer(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFER_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_RECURRENT_TRANSFERS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case TRANSACTION_MODEL_LIST:
                cursor = new MultiUriCursorWrapper(db().getTransactionModels(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTION_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case TRANSACTION_MODEL_ITEM:
                cursor = new MultiUriCursorWrapper(db().getTransactionModel(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case TRANSFER_MODEL_LIST:
                cursor = new MultiUriCursorWrapper(db().getTransferModels(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSFER_MODELS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case TRANSFER_MODEL_ITEM:
                cursor = new MultiUriCursorWrapper(db().getTransferModel(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                cursor.setNotificationUri(getContentResolver(), CONTENT_WALLETS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_CATEGORIES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_DEBTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_EVENTS);
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case PLACE_LIST:
                cursor = new MultiUriCursorWrapper(db().getPlaces(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                break;
            case PLACE_ITEM:
                cursor = new MultiUriCursorWrapper(db().getPlace(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                break;
            case PLACE_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getPlaceTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_PLACES);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case PERSON_LIST:
                cursor = new MultiUriCursorWrapper(db().getPeople(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                break;
            case PERSON_ITEM:
                cursor = new MultiUriCursorWrapper(db().getPerson(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                break;
            case PERSON_TRANSACTION_LIST:
                cursor = new MultiUriCursorWrapper(db().getPeopleTransactions(parseIdAtIndex(uri, 1), projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_PEOPLE);
                cursor.setNotificationUri(getContentResolver(), CONTENT_TRANSACTIONS);
                break;
            case ATTACHMENT_LIST:
                cursor = new MultiUriCursorWrapper(db().getAttachments(projection, selection, selectionArgs, sortOrder));
                cursor.setNotificationUri(getContentResolver(), CONTENT_ATTACHMENTS);
                break;
            case ATTACHMENT_ITEM:
                cursor = new MultiUriCursorWrapper(db().getAttachment(ContentUris.parseId(uri), projection));
                cursor.setNotificationUri(getContentResolver(), uri);
                break;
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case CURRENCY_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.currency";
            case CURRENCY_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.currency";
            case WALLET_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.wallet";
            case WALLET_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.wallet";
            case TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case TRANSACTION_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.transaction";
            case TRANSACTION_ATTACHMENTS:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.attachments";
            case TRANSACTION_PEOPLE:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.person";
            case TRANSFER_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transfer";
            case TRANSFER_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.transfer";
            case TRANSFER_ATTACHMENTS:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.attachments";
            case TRANSFER_PEOPLE:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.person";
            case CATEGORY_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.category";
            case CATEGORY_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.category";
            case CATEGORY_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case DEBT_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.debt";
            case DEBT_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.debt";
            case DEBT_PEOPLE:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.person";
            case DEBT_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case BUDGET_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.budget";
            case BUDGET_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.budget";
            case BUDGET_WALLETS:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.wallet";
            case BUDGET_CATEGORIES:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.category";
            case BUDGET_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case SAVING_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.saving";
            case SAVING_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.saving";
            case SAVING_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case EVENT_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.event";
            case EVENT_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.event";
            case EVENT_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case RECURRENT_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.recurrence.transaction";
            case RECURRENT_TRANSACTION_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.recurrence.transaction";
            case RECURRENT_TRANSFER_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.recurrence.transfer";
            case RECURRENT_TRANSFER_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.recurrence.transfer";
            case TRANSACTION_MODEL_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.model.transaction";
            case TRANSACTION_MODEL_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.model.transaction";
            case TRANSFER_MODEL_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.model.transfer";
            case TRANSFER_MODEL_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.model.transfer";
            case PLACE_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.place";
            case PLACE_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.place";
            case PLACE_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case PERSON_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.person";
            case PERSON_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.person";
            case PERSON_TRANSACTION_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.transaction";
            case ATTACHMENT_LIST:
                return "vnd.android.cursor.dir/vnd.com.oriondev.moneywallet.storage.attachment";
            case ATTACHMENT_ITEM:
                return "vnd.android.cursor.item/vnd.com.oriondev.moneywallet.storage.attachment";
        }
        return null;
    }

    /**
     * Every write this provider makes runs inside one transaction, so a method that touches more
     * than one table cannot leave the database part way through. The observers are told only once
     * the commit has gone through, since a transaction that rolls back would otherwise have
     * already announced a change that never happened.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        Uri objectUri = SQLDatabase.inSharedTransaction(getContext(),
                database -> insertInTransaction(database, uri, contentValues));
        if (objectUri != null) {
            PreferenceManager.setLastTimeDataIsChanged(System.currentTimeMillis());
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null) {
                contentResolver.notifyChange(objectUri, null);
            }
        }
        return objectUri;
    }

    private Uri insertInTransaction(SQLDatabase database, Uri uri, ContentValues contentValues) {
        String currencyIso = null;
        long objectId = 0L;
        switch (mUriMatcher.match(uri)) {
            case CURRENCY_LIST:
                currencyIso = database.insertCurrency(contentValues);
                break;
            case WALLET_LIST:
                objectId = database.insertWallet(contentValues);
                break;
            case TRANSACTION_LIST:
                objectId = database.insertTransaction(contentValues);
                break;
            case TRANSFER_LIST:
                objectId = database.insertTransfer(contentValues);
                break;
            case CATEGORY_LIST:
                objectId = database.insertCategory(contentValues);
                break;
            case DEBT_LIST:
                objectId = database.insertDebt(contentValues);
                break;
            case BUDGET_LIST:
                objectId = database.insertBudget(contentValues);
                break;
            case SAVING_LIST:
                objectId = database.insertSaving(contentValues);
                break;
            case EVENT_LIST:
                objectId = database.insertEvent(contentValues);
                break;
            case RECURRENT_TRANSACTION_LIST:
                objectId = database.insertRecurrentTransaction(contentValues);
                break;
            case RECURRENT_TRANSFER_LIST:
                objectId = database.insertRecurrentTransfer(contentValues);
                break;
            case TRANSACTION_MODEL_LIST:
                objectId = database.insertTransactionModel(contentValues);
                break;
            case TRANSFER_MODEL_LIST:
                objectId = database.insertTransferModel(contentValues);
                break;
            case PLACE_LIST:
                objectId = database.insertPlace(contentValues);
                break;
            case PERSON_LIST:
                objectId = database.insertPerson(contentValues);
                break;
            case ATTACHMENT_LIST:
                objectId = database.insertAttachment(contentValues);
                break;
        }
        if (currencyIso != null) {
            return Uri.withAppendedPath(uri, currencyIso);
        }
        if (objectId > 0L) {
            return ContentUris.withAppendedId(uri, objectId);
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // one element so the body running inside the transaction can hand the uri back out. A
        // lambda cannot assign a local, and deriving it a second time out here would be the same
        // sixteen cases written twice
        Uri[] notifyUri = new Uri[1];
        int result = SQLDatabase.inSharedTransaction(getContext(),
                database -> deleteInTransaction(database, uri, notifyUri));
        // out here with the notify, and not in the WALLET_ITEM case where it used to sit, because
        // it writes a preference and broadcasts to every open screen and neither of those can be
        // rolled back. Reading the preference after the delete is the same read, nothing in the
        // database is what it answers from. Every wallet delete still passes through this point,
        // which is the reason it lives in the provider and not in the screen that starts one
        Context context = getContext();
        if (result > 0 && context != null && mUriMatcher.match(uri) == WALLET_ITEM
                && ContentUris.parseId(uri) == PreferenceManager.getCurrentWallet()) {
            // the deleted wallet still has its id stored as the current one, and every query the
            // filter reaches keeps filtering on it, so all of them come back with nothing
            PreferenceManager.setCurrentWallet(context, PreferenceManager.TOTAL_WALLET_ID);
        }
        ContentResolver contentResolver = getContentResolver();
        if (contentResolver != null && notifyUri[0] != null) {
            PreferenceManager.setLastTimeDataIsChanged(System.currentTimeMillis());
            contentResolver.notifyChange(notifyUri[0], null);
        }
        return result;
    }

    private int deleteInTransaction(SQLDatabase database, Uri uri, Uri[] notifyUri) {
        int result = 0;
        switch (mUriMatcher.match(uri)) {
            case CURRENCY_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_CURRENCIES;
                result = database.deleteCurrency(uri.getLastPathSegment());
                break;
            case WALLET_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_WALLETS;
                result = database.deleteWallet(ContentUris.parseId(uri));
                break;
            case TRANSACTION_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_TRANSACTIONS;
                result = database.deleteTransaction(ContentUris.parseId(uri));
                break;
            case TRANSFER_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_TRANSFERS;
                result = database.deleteTransfer(ContentUris.parseId(uri));
                break;
            case CATEGORY_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_CATEGORIES;
                result = database.deleteCategory(ContentUris.parseId(uri));
                break;
            case DEBT_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_DEBTS;
                result = database.deleteDebt(ContentUris.parseId(uri));
                break;
            case BUDGET_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_BUDGETS;
                result = database.deleteBudget(ContentUris.parseId(uri));
                break;
            case SAVING_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_SAVINGS;
                result = database.deleteSaving(ContentUris.parseId(uri));
                break;
            case EVENT_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_EVENTS;
                result = database.deleteEvent(ContentUris.parseId(uri));
                break;
            case RECURRENT_TRANSACTION_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_RECURRENT_TRANSACTIONS;
                result = database.deleteRecurrentTransaction(ContentUris.parseId(uri));
                break;
            case RECURRENT_TRANSFER_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_RECURRENT_TRANSFERS;
                result = database.deleteRecurrentTransfer(ContentUris.parseId(uri));
                break;
            case TRANSACTION_MODEL_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_TRANSACTION_MODELS;
                result = database.deleteTransactionModel(ContentUris.parseId(uri));
                break;
            case TRANSFER_MODEL_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_TRANSFER_MODELS;
                result = database.deleteTransferModel(ContentUris.parseId(uri));
                break;
            case PLACE_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_PLACES;
                result = database.deletePlace(ContentUris.parseId(uri));
                break;
            case PERSON_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_PEOPLE;
                result = database.deletePerson(ContentUris.parseId(uri));
                break;
            case ATTACHMENT_ITEM:
                notifyUri[0] = DataContentProvider.CONTENT_ATTACHMENTS;
                result = database.deleteAttachment(ContentUris.parseId(uri));
                break;
        }
        return result;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int result = SQLDatabase.inSharedTransaction(getContext(),
                database -> updateInTransaction(database, uri, values));
        if (result > 0) {
            PreferenceManager.setLastTimeDataIsChanged(System.currentTimeMillis());
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null) {
                contentResolver.notifyChange(uri, null);
            }
        }
        return result;
    }

    private int updateInTransaction(SQLDatabase database, Uri uri, ContentValues values) {
        int result = 0;
        switch (mUriMatcher.match(uri)) {
            case CURRENCY_ITEM:
                result = database.updateCurrency(uri.getLastPathSegment(), values);
                break;
            case WALLET_ITEM:
                result = database.updateWallet(ContentUris.parseId(uri), values);
                break;
            case TRANSACTION_ITEM:
                result = database.updateTransaction(ContentUris.parseId(uri), values);
                break;
            case TRANSFER_ITEM:
                result = database.updateTransfer(ContentUris.parseId(uri), values);
                break;
            case CATEGORY_ITEM:
                result = database.updateCategory(ContentUris.parseId(uri), values);
                break;
            case DEBT_ITEM:
                result = database.updateDebt(ContentUris.parseId(uri), values);
                break;
            case BUDGET_ITEM:
                result = database.updateBudget(ContentUris.parseId(uri), values);
                break;
            case SAVING_ITEM:
                result = database.updateSaving(ContentUris.parseId(uri), values);
                break;
            case EVENT_ITEM:
                result = database.updateEvent(ContentUris.parseId(uri), values);
                break;
            case RECURRENT_TRANSACTION_ITEM:
                result = database.updateRecurrentTransaction(ContentUris.parseId(uri), values);
                break;
            case RECURRENT_TRANSFER_ITEM:
                result = database.updateRecurrentTransfer(ContentUris.parseId(uri), values);
                break;
            case TRANSACTION_MODEL_ITEM:
                result = database.updateTransactionModel(ContentUris.parseId(uri), values);
                break;
            case TRANSFER_MODEL_ITEM:
                result = database.updateTransferModel(ContentUris.parseId(uri), values);
                break;
            case PLACE_ITEM:
                result = database.updatePlace(ContentUris.parseId(uri), values);
                break;
            case PERSON_ITEM:
                result = database.updatePerson(ContentUris.parseId(uri), values);
                break;
        }
        return result;
    }

    private ContentResolver getContentResolver() {
        Context context = getContext();
        return context != null ? context.getContentResolver() : null;
    }

    /**
     * Parse an id from a uri starting from the end of the string.
     * @param uri to parse from.
     * @param index of the id starting from the end.
     * @return the parsed id.
     */
    private long parseIdAtIndex(Uri uri, int index) {
        List<String> segments = uri.getPathSegments();
        int fixedIndex = segments.size() - index;
        return Long.parseLong(segments.get(fixedIndex - 1));
    }

    /**
     * Runs a replacement of the database file with the shared helper closed and no write of this
     * provider's in flight. The runnable does the file work and nothing else.
     *
     * Public, and here, because SQLDatabase is package local and the importers that replace the
     * database file sit in a sub package, so they cannot name it.
     */
    public static void replaceDatabaseFile(Context context, Runnable swap) {
        SQLDatabase.resetShared(context, swap);
    }

    @SuppressLint("Recycle")
    public static void notifyDatabaseIsChanged(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentProviderClient client = contentResolver.acquireContentProviderClient(AUTHORITY);
        if (client != null) {
            ContentProvider contentProvider = client.getLocalContentProvider();
            if (contentProvider instanceof DataContentProvider) {
                SQLDatabase.resetShared(context);
                PreferenceManager.setCurrentWallet(context, PreferenceManager.NO_CURRENT_WALLET);
                // Same reason the current wallet is cleared just above. Every wallet is inserted
                // fresh by a restore, so the ids come back naming other wallets, and a widget is
                // pointed at one by its id. Left alone it would show a wallet nobody chose and
                // file new transactions into it. Nothing on this path calls notifyChange either,
                // which is why the redraw is asked for here.
                WalletWidgetProvider.forgetConfiguredWallets(context);
            }
            client.close();
        }
    }
}
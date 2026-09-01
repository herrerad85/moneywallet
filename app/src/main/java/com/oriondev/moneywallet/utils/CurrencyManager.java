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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.LocaleList;

import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.ExchangeRate;
import com.oriondev.moneywallet.storage.cache.ExchangeRateCache;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.DataContentProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class act as a proxy on the top of the content provider.
 * It is responsible to cache the currency list for a faster access at runtime and manage all
 * the related operations (like handle currency rates).
 */
public class CurrencyManager {

    /**
     * Volatile so that a thread which reads this field and finds it set also sees the map the
     * constructor built. The field it holds used to be final, and a final field carries that
     * guarantee on its own even when the reference to its holder is published in a race. A
     * volatile field does not, so the guarantee has to come from here instead.
     */
    private static volatile CurrencyManager mInstance;

    /**
     * Held for the whole of one load, so two of them cannot interleave their read and their
     * publish and leave the cache holding the older of the two. Both loaders take it, initialize
     * and invalidateCache.
     *
     * This is a lock held across a database read, which is what the monitor it replaced did and
     * what deadlocked. It is safe here for one reason, and that reason has to stay true: only
     * those two take it, and neither is called from inside a database transaction, so no thread
     * holding the one connection the app shares can be made to wait on it. The readers below take
     * nothing at all. CurrencyManagerSourceTest pins the first half and refuseIfInsideATransaction
     * refuses the second at runtime.
     */
    private static final Object RELOAD_LOCK = new Object();

    /**
     * A thread inside a transaction holds the one database connection until it returns, and both
     * loaders below take RELOAD_LOCK across a read that needs it, so one calling either of them
     * is the deadlock this class was rewritten to remove. Refused here and not deadlocked later.
     *
     * The refusal is raised inside the caller's transaction, so it rolls that whole transaction
     * back. For an importer that would mean losing the import, which is the right outcome for a
     * call that could not have completed anyway.
     */
    private static void refuseIfInsideATransaction() {
        if (DataContentProvider.isInsideOneTransaction()) {
            throw new IllegalStateException("CurrencyManager loaded from inside a transaction");
        }
    }

    /**
     * Under the same lock as a reload, because building the instance reads the currency table and
     * writes the default set into an empty one, which is the work a reload does. That makes the
     * check and the assignment one step, and it stops this and a reload both loading at once.
     *
     * App.onCreate is the only caller. That is Application.onCreate, which Android runs after
     * every content provider's onCreate, so the providers are already serving by the time this
     * runs and the lock is not decoration.
     *
     * @throws IllegalStateException if called from inside a database transaction. See
     *         {@link #refuseIfInsideATransaction()}.
     */
    public static void initialize(Context context) {
        refuseIfInsideATransaction();
        synchronized (RELOAD_LOCK) {
            if (mInstance == null) {
                mInstance = new CurrencyManager(context);
            }
        }
    }

    private final ExchangeRateCache mExchangeRateCache;

    /**
     * Built whole and never written to again once it is published, so a reader takes no lock and
     * a reload is never seen half done. The map is wrapped unmodifiable to keep it that way. That
     * is a guard and not a requirement of the memory model, since the volatile write already
     * carries everything the loading thread put in the map, but a later write to a published map
     * would take that away and the wrapper is what stops one being added.
     */
    private volatile Map<String, CurrencyUnit> mCurrencyCache;

    private CurrencyManager(Context context) {
        mExchangeRateCache = new ExchangeRateCache(context);
        mCurrencyCache = loadCurrencies(context);
    }

    private static Map<String, CurrencyUnit> loadCurrencies(Context context) {
        Map<String, CurrencyUnit> currencies = loadUserCurrencies(context);
        if (currencies.isEmpty()) {
            System.out.println("[CurrencyManager] No currency found. Loading default currencies from the assets...");
            currencies = loadDefaultCurrencies(context);
        }
        return Collections.unmodifiableMap(currencies);
    }

    /**
     * Reads every currency again and swaps the new map in.
     *
     * The load holds nothing that a reader of the cache can take, and it has to. An importer runs
     * a whole import inside one transaction, and the database is opened without write ahead
     * logging, so that transaction holds the single connection the whole app shares. The importer
     * asks this class for the currency named on every row it reads. A lock held here across the
     * load and taken again by {@link #getCurrency(String)} put those two in opposite orders, the
     * importer waiting on the lock and this method waiting on the connection, which is what this
     * method used to do. The readers take nothing now, which is what makes the lock below safe.
     *
     * The load is usually a read, and on a currency table that is empty it also inserts the whole
     * default set, which a restore from a legacy backup leaves behind since that format carries no
     * currencies.
     *
     * The whole reload runs under RELOAD_LOCK so that two of them cannot interleave. Ordering them
     * by something cheaper than a lock was tried and does not work, because a reload reads over an
     * interval and not at an instant, so no single number taken at its start or at its end says
     * whose read saw more. A reload that seeds the table writes rows another may or may not have
     * seen, and a reload whose own caller has just inserted one currency reads a table that is not
     * empty and so seeds nothing and holds only that one. Running them one at a time is what makes
     * all of those come out right.
     *
     * The cost is that one reload waits for another, and the currency editor calls this on the
     * main thread. What it waits for is one read of the currency table, or on an empty table the
     * write of the default set.
     *
     * @param context of the application.
     * @throws IllegalStateException if called from inside a database transaction. See
     *         {@link #refuseIfInsideATransaction()}.
     */
    public static void invalidateCache(Context context) {
        refuseIfInsideATransaction();
        System.out.println("[CurrencyManager] Invalidating cache...");
        synchronized (RELOAD_LOCK) {
            mInstance.mCurrencyCache = loadCurrencies(context);
        }
    }

    /**
     * Reads the currencies this installation has into a new map. A call to this method is very
     * expensive because it is an I/O operation, and invalidateCache reaches it from the main
     * thread.
     * @param context of the application.
     * @return what the currency table holds, which is empty on a first run.
     */
    private static Map<String, CurrencyUnit> loadUserCurrencies(Context context) {
        Map<String, CurrencyUnit> currencies = new HashMap<>();
        ContentResolver contentResolver = context.getContentResolver();
        String[] projections = new String[] {
                Contract.Currency.ISO,
                Contract.Currency.NAME,
                Contract.Currency.SYMBOL,
                Contract.Currency.DECIMALS
        };
        Cursor cursor = contentResolver.query(DataContentProvider.CONTENT_CURRENCIES, projections, null, null, null);
        if (cursor != null) {
            int indexIso = cursor.getColumnIndex(Contract.Currency.ISO);
            int indexName = cursor.getColumnIndex(Contract.Currency.NAME);
            int indexSymbol = cursor.getColumnIndex(Contract.Currency.SYMBOL);
            int indexDecimals = cursor.getColumnIndex(Contract.Currency.DECIMALS);
            while (cursor.moveToNext()) {
                CurrencyUnit currencyUnit = new CurrencyUnit(
                        cursor.getString(indexIso),
                        cursor.getString(indexName),
                        cursor.getString(indexSymbol),
                        cursor.getInt(indexDecimals));
                currencies.put(currencyUnit.getIso(), currencyUnit);
            }
            cursor.close();
        }
        return currencies;
    }

    /**
     * Seeds the currency table from the asset file and answers what the table holds
     * afterwards, which is not always what was written. An insert that collides on the ISO
     * primary key is refused, and a refused insert returns a null uri and raises nothing, so
     * a table holding every currency already but with all of them deleted reads empty here and
     * used to publish the whole file anyway, naming currencies the provider answers nothing for.
     * insertCurrency brings such a row back now, and this answers with what the table holds
     * afterwards so the two cannot disagree. It costs one more query every time a seed runs,
     * which invalidateCache above reaches from the main thread.
     */
    private static Map<String, CurrencyUnit> loadDefaultCurrencies(Context context) {
        final List<ContentValues> rows = new ArrayList<>();
        try {
            // open assets file and load all the default currencies into a JSONArray
            StringBuilder jsonBuilder = new StringBuilder();
            InputStream inputStream = context.getAssets().open("resources/currencies.json");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            JSONArray array = new JSONArray(jsonBuilder.toString());
            // every row is read and turned into a currency out here, so the transaction below
            // holds the one database connection for the writes and for nothing else
            for (int i = 0; i < array.length(); i++) {
                JSONObject currency = array.getJSONObject(i);
                ContentValues contentValues = new ContentValues();
                contentValues.put(Contract.Currency.ISO, currency.getString("code"));
                contentValues.put(Contract.Currency.NAME, currency.getString("name"));
                contentValues.put(Contract.Currency.SYMBOL, currency.optString("symbol", null));
                contentValues.put(Contract.Currency.DECIMALS, currency.optInt("decimals", 2));
                // bring back a row this installation holds and does not serve, which is the state
                // that made this seed land nothing at all. The currency editor inserts without
                // this and a collision stays a refusal for it
                contentValues.put(Contract.Currency.REVIVE_IF_DELETED, true);
                rows.add(contentValues);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Exception while reading currencies file from assets: " + e.getMessage(), e);
        }
        try {
            // one transaction for the whole set, so the table is never seen part way through it.
            // RELOAD_LOCK already keeps the other loader out, so the reader this protects is
            // everything else that queries the currency table, the currency list loader among
            // them. It also turns one announcement per currency into one for the set
            DataContentProvider.runInOneTransaction(context, () -> {
                ContentResolver contentResolver = context.getContentResolver();
                for (ContentValues row : rows) {
                    contentResolver.insert(DataContentProvider.CONTENT_CURRENCIES, row);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Exception while storing the default currencies: " + e.getMessage(), e);
        }
        // read back rather than answering with the file, so the cache holds what the
        // provider will serve and never more than it
        return loadUserCurrencies(context);
    }

    /**
     * Obtain the currency object from the iso code.
     * @param iso of the currency to obtain.
     * @return the currency object if the iso code is found.
     */
    public static CurrencyUnit getCurrency(String iso) {
        return mInstance.mCurrencyCache.get(iso);
    }

    /**
     * @return every currency this installation has. The collection is a view of a map that is
     *         never written to, so a reload replaces the map the next caller reads and leaves a
     *         walk already under way to finish over the whole of the old one.
     */
    public static Collection<CurrencyUnit> getCurrencies() {
        return mInstance.mCurrencyCache.values();
    }

    /**
     * @return the number of decimals of the currency, or 2 when the currency is null because
     *         {@link #getCurrency(String)} did not know the iso code stored on the row. Two is
     *         what MoneyFormatter divides the same row by when it cannot resolve the currency
     *         either, and a chart drawn beside a figure that formatter wrote has to agree with
     *         it. It used to answer 0, from a caller that refuses to save a row it cannot
     *         resolve and so never rendered the answer.
     */
    public static int getDecimals(CurrencyUnit currency) {
        return currency != null ? currency.getDecimals() : 2;
    }

    /**
     * @return the rate between the two currencies, or null if either is absent or no rate is
     *         known for the pair.
     */
    public static ExchangeRate getExchangeRate(CurrencyUnit currency1, CurrencyUnit currency2) {
        if (currency1 == null || currency2 == null) {
            return null;
        }
        return mInstance.mExchangeRateCache.getExchangeRate(currency1.getIso(), currency2.getIso());
    }

    /**
     * Obtain the currency the user's language settings imply.
     * If for example the user is using the it-IT locale, the EUR currency will be returned.
     * A locale that carries no country implies no currency at all, so those are skipped and
     * the first preferred locale that does imply one decides. Whether this installation still
     * has that currency is a separate question, and a deleted one still answers null rather
     * than moving on to the next locale.
     * @return the current currency, or null if no preferred locale implies one, or the user
     *         has deleted the one it implies.
     */
    public static CurrencyUnit getDefaultCurrency() {
        LocaleList preferredLocales = LocaleList.getAdjustedDefault();
        for (int index = 0; index < preferredLocales.size(); index++) {
            Currency currency = currencyOf(preferredLocales.get(index));
            if (currency != null) {
                CurrencyUnit currencyUnit = getCurrency(currency.getCurrencyCode());
                if (currencyUnit == null) {
                    System.out.println("[CurrencyManager] The locale implies "
                            + currency.getCurrencyCode() + ", which is not installed");
                }
                return currencyUnit;
            }
        }
        System.out.println("[CurrencyManager] No preferred locale implies a currency: "
                + preferredLocales.toLanguageTags());
        return null;
    }

    /**
     * @return the currency the locale implies, or null if it implies none. getInstance throws
     *         when the locale has no ISO 3166 country and returns null for a territory that has
     *         no currency of its own, and here those mean the same thing.
     */
    private static Currency currencyOf(Locale locale) {
        try {
            return Currency.getInstance(locale);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ExchangeRateCache getExchangeRateCache() {
        return mInstance.mExchangeRateCache;
    }
}
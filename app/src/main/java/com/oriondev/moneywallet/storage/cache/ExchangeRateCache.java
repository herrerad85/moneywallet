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

package com.oriondev.moneywallet.storage.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.oriondev.moneywallet.model.ExchangeRate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andre on 25/03/2018.
 */
public class ExchangeRateCache {

    /**
     * Written by the rate download service on its own thread and read from the main thread on
     * every keystroke in the currency converter, so it cannot be a plain map. It used to be one:
     * a get landing during a resize can answer null for a rate the cache holds, and a get that
     * reaches a CacheObj the writer has only just allocated can read the three fields at their
     * defaults, divide by a rate of zero, and hand the converter an infinite rate it cannot parse.
     *
     * A reader takes no lock here, and that is a requirement rather than a preference.
     * CurrencyManagerSourceTest lists CurrencyManager.getExchangeRate among the methods a thread
     * inside a database transaction can reach and asserts none of them takes a lock; that method
     * hands straight to this class. ConcurrentHashMap.get blocks on nothing, and its put is what
     * carries a CacheObj's fields to the thread that reads them.
     */
    private final Map<String, CacheObj> mCacheMemory;
    private final SQLCache mCacheStorage;

    public ExchangeRateCache(Context context) {
        mCacheMemory = new ConcurrentHashMap<>();
        mCacheStorage = new SQLCache(context);
        loadCacheInMemory();
    }

    private void loadCacheInMemory() {
        String[] projection = new String[] {
                SQLCache.ExchangeRateT.CURRENCY_ISO,
                SQLCache.ExchangeRateT.RATE,
                SQLCache.ExchangeRateT.TIMESTAMP
        };
        Cursor cursor = mCacheStorage.getExchangeRates(projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()){
                String iso = cursor.getString(cursor.getColumnIndex(SQLCache.ExchangeRateT.CURRENCY_ISO));
                // exchange_currency_iso is a TEXT PRIMARY KEY with no NOT NULL, so a row can carry
                // no iso at all. It names nothing any reader can ask for, and putting it would
                // throw here and take the app down on the next launch instead of on that row.
                if (iso == null) {
                    continue;
                }
                CacheObj cacheObj = new CacheObj();
                cacheObj.mCurrency = iso;
                cacheObj.mRate = cursor.getDouble(cursor.getColumnIndex(SQLCache.ExchangeRateT.RATE));
                cacheObj.mTimestamp = cursor.getLong(cursor.getColumnIndex(SQLCache.ExchangeRateT.TIMESTAMP));
                mCacheMemory.put(cacheObj.mCurrency, cacheObj);
            }
            cursor.close();
        }
    }

    public void setExchangeRate(String currency, double rate, long timestamp) {
        // The same null key getExchangeRate refuses, refused on the way in as well. A rate filed
        // under no iso is one nothing can look up. The only caller today cannot reach this, since
        // it asks the rates it downloaded whether they name this currency and a null names
        // nothing, but the guard belongs with the map that needs it and not in a service in
        // another package.
        if (currency == null) {
            return;
        }
        CacheObj cacheObj = new CacheObj();
        cacheObj.mCurrency = currency;
        cacheObj.mRate = rate;
        cacheObj.mTimestamp = timestamp;
        mCacheMemory.put(currency, cacheObj);
        ContentValues contentValues = new ContentValues();
        contentValues.put(SQLCache.ExchangeRateT.CURRENCY_ISO, currency);
        contentValues.put(SQLCache.ExchangeRateT.RATE, rate);
        contentValues.put(SQLCache.ExchangeRateT.TIMESTAMP, timestamp);
        mCacheStorage.insertOrUpdateExchangeRate(contentValues);
    }

    public ExchangeRate getExchangeRate(String currency1, String currency2) {
        if (TextUtils.equals(currency1, currency2)) {
            return new ExchangeRate(currency1, currency2, 1, System.currentTimeMillis());
        }
        // A currency row can carry a null iso, since Schema declares it TEXT PRIMARY KEY with no
        // NOT NULL and SQLite allows a null there, and CurrencyManager.getExchangeRate checks the
        // two currencies for null without checking their isos. A map that refuses a null key
        // throws on one, where the map this replaced answered null and the pair below returned
        // null. This keeps that answer. Two null isos are equal and never reach here.
        if (currency1 == null || currency2 == null) {
            return null;
        }
        CacheObj rate1 = mCacheMemory.get(currency1);
        CacheObj rate2 = mCacheMemory.get(currency2);
        if (rate1 != null && rate2 != null) {
            double rate = (1d / rate1.mRate) * rate2.mRate;
            long timestamp = Math.min(rate1.mTimestamp, rate2.mTimestamp);
            return new ExchangeRate(currency1, currency2, rate, timestamp);
        }
        return null;
    }

    private class CacheObj {

        private String mCurrency;
        private double mRate;
        private long mTimestamp;
    }
}
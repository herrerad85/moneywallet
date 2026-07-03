package com.oriondev.moneywallet.storage.database.data;

/**
 * Created by andrea on 21/12/18.
 */

/**
 * Contract column keys shared by every data exporter. The value of each key is the string that
 * ends up in a CSV header cell, so the literals must not change. Only the CSV exporter uses
 * {@link #COLUMN_CURRENCY}; the other formats simply never add it to their column list.
 */
public class Constants {

    public static final String COLUMN_WALLET = "wallet";
    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_DATETIME = "datetime";
    public static final String COLUMN_MONEY = "money";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_EVENT = "event";
    public static final String COLUMN_PEOPLE = "people";
    public static final String COLUMN_PLACE = "place";
    public static final String COLUMN_NOTE = "note";
}

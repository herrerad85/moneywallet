package com.oriondev.moneywallet.storage.database.data;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import androidx.collection.LongSparseArray;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.Wallet;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateUtils;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Created by andrea on 21/12/18.
 */

public abstract class AbstractDataExporter {

    public static final String COLUMN_EVENT = "column_event";
    public static final String COLUMN_PEOPLE = "column_people";
    public static final String COLUMN_PLACE = "column_place";
    public static final String COLUMN_NOTE = "column_note";

    private final Context mContext;
    private final LongSparseArray<String> mPeopleCache;

    private boolean mShouldLoadPeople = false;

    public AbstractDataExporter(Context context, File folder) throws IOException {
        mContext = context;
        mPeopleCache = new LongSparseArray<>();
    }

    protected String getDefaultFileName(String extension) {
        String dateTimeString = DateUtils.getFilenameDateTimeString(new Date());
        return "Tallybook_export_" + dateTimeString + extension;
    }

    public abstract boolean isMultiWalletSupported();

    public abstract String[] getColumns(boolean uniqueWallet, String[] optionalColumns);

    /**
     * Append the user-selected optional columns to a format specific base column list. The
     * mapping from selector token to contract column key is identical for every exporter, as is
     * the side effect of flagging that the people cache must be loaded, so it lives here once.
     */
    protected void appendOptionalColumns(List<String> columns, String[] optionalColumns) {
        if (optionalColumns != null) {
            for (String column : optionalColumns) {
                switch (column) {
                    case COLUMN_EVENT:
                        columns.add(Constants.COLUMN_EVENT);
                        break;
                    case COLUMN_PEOPLE:
                        columns.add(Constants.COLUMN_PEOPLE);
                        mShouldLoadPeople = true;
                        break;
                    case COLUMN_PLACE:
                        columns.add(Constants.COLUMN_PLACE);
                        break;
                    case COLUMN_NOTE:
                        columns.add(Constants.COLUMN_NOTE);
                        break;
                }
            }
        }
    }

    public boolean shouldLoadPeople() {
        return mShouldLoadPeople;
    }

    /**
     * Resolve the cell value for a single contract column from the current cursor row. This is the
     * one place that knows how each column maps onto the cursor, so the three formats no longer
     * duplicate it. Two things stay parametrized because they are format specific:
     * <ul>
     *     <li>{@code formatter} is the exporter's own {@link MoneyFormatter} (the CSV formatter is
     *     configured differently from the XLS/PDF one), and</li>
     *     <li>{@code peopleSeparator} joins multiple people, ", " for XLS/PDF but "," for CSV
     *     because exported CSVs are parsed back by the CSV importer.</li>
     * </ul>
     * Returns {@code null} for an empty people list or an unknown column, matching the previous
     * per-format behaviour exactly.
     */
    protected String getColumnValue(Cursor cursor, String column, MoneyFormatter formatter, String peopleSeparator) {
        switch (column) {
            case Constants.COLUMN_WALLET:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_NAME));
            case Constants.COLUMN_CURRENCY:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY));
            case Constants.COLUMN_CATEGORY:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.CATEGORY_NAME));
            case Constants.COLUMN_DATETIME:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.DATE));
            case Constants.COLUMN_MONEY:
                CurrencyUnit currencyUnit = CurrencyManager.getCurrency(cursor.getString(cursor.getColumnIndex(Contract.Transaction.WALLET_CURRENCY)));
                long money = cursor.getLong(cursor.getColumnIndex(Contract.Transaction.MONEY));
                int direction = cursor.getInt(cursor.getColumnIndex(Contract.Transaction.DIRECTION));
                if (direction == Contract.Direction.EXPENSE) {
                    money *= -1;
                }
                return formatter.getNotTintedString(currencyUnit, money);
            case Constants.COLUMN_DESCRIPTION:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.DESCRIPTION));
            case Constants.COLUMN_EVENT:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.EVENT_NAME));
            case Constants.COLUMN_PEOPLE:
                return getPeopleValue(cursor, peopleSeparator);
            case Constants.COLUMN_PLACE:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.PLACE_NAME));
            case Constants.COLUMN_NOTE:
                return cursor.getString(cursor.getColumnIndex(Contract.Transaction.NOTE));
            default:
                return null;
        }
    }

    private String getPeopleValue(Cursor cursor, String separator) {
        List<Long> peopleIds = Contract.parseObjectIds(cursor.getString(cursor.getColumnIndex(Contract.Transaction.PEOPLE_IDS)));
        if (peopleIds != null && !peopleIds.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Long personId : peopleIds) {
                String name = getPersonName(personId);
                if (!TextUtils.isEmpty(name)) {
                    if (builder.length() > 0) {
                        builder.append(separator);
                    }
                    builder.append(name);
                }
            }
            return builder.toString();
        } else {
            return null;
        }
    }

    /**
     * Localized header label for a contract column. Used by the XLS and PDF formats, which write a
     * translated header row; the CSV format writes the raw column keys instead and never calls
     * this. Returns {@code null} for a column without a header string, matching the old behaviour.
     */
    protected String getColumnHeader(String column) {
        Context context = getContext();
        switch (column) {
            case Constants.COLUMN_DATETIME:
                return context.getString(R.string.hint_date);
            case Constants.COLUMN_CATEGORY:
                return context.getString(R.string.hint_category);
            case Constants.COLUMN_MONEY:
                return context.getString(R.string.hint_money);
            case Constants.COLUMN_WALLET:
                return context.getString(R.string.hint_wallet);
            case Constants.COLUMN_DESCRIPTION:
                return context.getString(R.string.hint_description);
            case Constants.COLUMN_EVENT:
                return context.getString(R.string.hint_event);
            case Constants.COLUMN_PEOPLE:
                return context.getString(R.string.hint_people);
            case Constants.COLUMN_PLACE:
                return context.getString(R.string.hint_place);
            case Constants.COLUMN_NOTE:
                return context.getString(R.string.hint_note);
            default:
                return null;
        }
    }

    public void cachePeople(Cursor cursor) {
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(Contract.Person.ID));
            String name = cursor.getString(cursor.getColumnIndex(Contract.Person.NAME));
            mPeopleCache.put(id, name);
        }
    }

    protected String getPersonName(long id) {
        return mPeopleCache.get(id);
    }

    protected Context getContext() {
        return mContext;
    }

    public abstract void exportData(Cursor cursor, String[] columns, Wallet... wallets) throws IOException;

    public abstract void close() throws IOException;

    public abstract File getOutputFile();

    public abstract String getResultType();
}

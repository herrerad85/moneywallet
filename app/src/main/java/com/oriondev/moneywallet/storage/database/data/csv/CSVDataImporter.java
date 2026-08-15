package com.oriondev.moneywallet.storage.database.data.csv;

import android.content.Context;

import com.opencsv.CSVReaderHeaderAware;
import com.oriondev.moneywallet.model.CurrencyUnit;
import com.oriondev.moneywallet.model.MoneyScale;
import com.oriondev.moneywallet.storage.database.Contract;
import com.oriondev.moneywallet.storage.database.data.AbstractDataImporter;
import com.oriondev.moneywallet.storage.database.data.Constants;
import com.oriondev.moneywallet.utils.CurrencyManager;
import com.oriondev.moneywallet.utils.DateUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * Created by andrea on 23/12/18.
 */
public class CSVDataImporter extends AbstractDataImporter {

    /** The length of yyyy-MM-dd, which the date parse itself does not bound. */
    private static final int SQL_DATE_LENGTH = 10;

    private final File mFile;

    private final CSVReaderHeaderAware mReader;

    private int mRoundedAmounts;

    public CSVDataImporter(Context context, File file) throws IOException {
        super(context, file);
        mFile = file;
        mReader = new CSVReaderHeaderAware(new FileReader(file));
    }

    /**
     * Reads the whole file once without saving anything, then reads it again to save it. Saving
     * each row as it was read left every row before a bad one in the ledger, under a message
     * telling the user the import had failed. Reading twice parses every row twice. That is the
     * cost of not keeping the parsed rows: an import file can be any size, and a list of them
     * would grow with it.
     */
    @Override
    public void importData() throws IOException {
        try (CSVReaderHeaderAware check = new CSVReaderHeaderAware(new FileReader(mFile))) {
            readRows(check, false);
        }
        readRows(mReader, true);
    }

    /**
     * Reads every row and throws on the first bad one. Saves a row only when write is true.
     * {@link #importData} calls this twice, with write off and then on, so that a bad file is
     * caught before anything is saved. Saving itself can still fail part way through, and whatever
     * {@link #insertTransaction} already saved stays.
     */
    private void readRows(CSVReaderHeaderAware reader, boolean write) throws IOException {
        Map<String, String> lineMap = reader.readMap();
        while (lineMap != null) {
            try {
                readRow(lineMap, write);
            } catch (RuntimeException e) {
                // Only the checking pass blames a line. The saving pass is where
                // insertTransaction runs, and "Line 37: Failed to create the new wallet" would
                // send the user to look at a row that is fine.
                if (write) {
                    throw e;
                }
                throw new RuntimeException("Line " + reader.getLinesRead() + ": " + e.getMessage(), e);
            }
            lineMap = reader.readMap();
        }
    }

    private void readRow(Map<String, String> lineMap, boolean write) {
        // extract required information from the csv file
        String wallet = required(lineMap, Constants.COLUMN_WALLET);
        String currency = required(lineMap, Constants.COLUMN_CURRENCY);
        String category = required(lineMap, Constants.COLUMN_CATEGORY);
        String datetimeString = required(lineMap, Constants.COLUMN_DATETIME);
        String moneyString = required(lineMap, Constants.COLUMN_MONEY);
        // extract the optional information from the csv file
        String description = getTrimmedString(lineMap.get(Constants.COLUMN_DESCRIPTION));
        String event = getTrimmedString(lineMap.get(Constants.COLUMN_EVENT));
        String people = getTrimmedString(lineMap.get(Constants.COLUMN_PEOPLE));
        String place = getTrimmedString(lineMap.get(Constants.COLUMN_PLACE));
        String note = getTrimmedString(lineMap.get(Constants.COLUMN_NOTE));
        // try to build the internal transaction state starting from strings
        CurrencyUnit currencyUnit = CurrencyManager.getCurrency(currency);
        if (currencyUnit == null) {
            throw new RuntimeException("Unknown currency unit (" + currency + ")");
        }
        BigDecimal moneyDecimal;
        try {
            moneyDecimal = new BigDecimal(moneyString.replaceAll(",", "."));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid money amount (" + e.getMessage() + ")");
        }
        long money = toMinorUnitsCounting(moneyDecimal, moneyString, currencyUnit.getDecimals(), write);
        int direction = money < 0 ? Contract.Direction.EXPENSE : Contract.Direction.INCOME;
        Date datetime = parseDatetime(datetimeString);
        if (write) {
            insertTransaction(wallet, currencyUnit, category, datetime, Math.abs(money), direction, description, event, place, people, note);
        }
    }

    @Override
    public int getRoundedAmounts() {
        return mRoundedAmounts;
    }

    /**
     * The amount of a row in minor units. Rounded rather than cut off, because a file written
     * elsewhere carries whatever precision that place kept, and nothing here shows the amount
     * for review before it is saved. The row's own currency column decides the scale.
     *
     * Rows the currency could not hold exactly are counted, so the screen that reports the
     * import can say so. The test is the stored amount read back against what the row said, not
     * the rounded amount against the cut off one: a row rounded down lands where cutting off
     * would have left it, and its value moved just the same. Counted only on the pass that
     * writes, since every row is read twice.
     *
     * @param cell  the money column as the row wrote it, which is what a refusal quotes.
     * @param write true on the pass that saves, which is the one that counts.
     */
    private long toMinorUnitsCounting(BigDecimal amount, String cell, int decimals, boolean write) {
        long money;
        try {
            money = MoneyScale.toMinorUnitsRounded(amount, decimals);
        } catch (ArithmeticException e) {
            throw new RuntimeException("Money amount is out of range (" + cell + ")");
        }
        // The one value abs cannot turn positive, which would otherwise reach the ledger as a
        // negative amount sitting on an expense.
        if (money == Long.MIN_VALUE) {
            throw new RuntimeException("Money amount is out of range (" + cell + ")");
        }
        if (write && MoneyScale.toHumanAmount(money, decimals).compareTo(amount) != 0) {
            mRoundedAmounts++;
        }
        return money;
    }

    /**
     * The value of a column every row needs. A column the header does not have and a cell left
     * empty are mistakes in different places, so they do not get the same message.
     */
    /*package-local*/ static String required(Map<String, String> lineMap, String column) {
        String value = lineMap.get(column);
        if (value == null) {
            throw new RuntimeException("no " + column + " column was found. Check the header line");
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new RuntimeException("the " + column + " is empty, and every row needs one");
        }
        return value;
    }

    /**
     * A datetime, or a date on its own. A date with no time is the obvious thing to write by
     * hand, and it used to end the whole import.
     *
     * The datetime is tried first because that is what this app's own export writes. The order
     * is not what makes this safe; the two checks below are, and they hold in either order.
     */
    /*package-local*/ static Date parseDatetime(String value) {
        try {
            return DateUtils.getDateFromSQLDateTimeString(value);
        } catch (RuntimeException notADatetime) {
            // fall through and read it as a date on its own
        }
        Date date;
        try {
            date = DateUtils.getDateFromSQLDateString(value);
        } catch (RuntimeException notADate) {
            throw notADate(value);
        }
        // What this parse does not throw for, it can still read as a different day than the one
        // written down, so the answer is written back out and compared. That is what refuses
        // anything still carrying a time, since the parse stops at the first character it cannot
        // use and "2026-08-12T09:30:15" would come back as the 12th with the time gone, and
        // anything naming a day past the end of its month, since it rolls those forward and
        // "2026-02-30" would come back as March 2nd. The length covers the one thing the
        // comparison cannot see. A year is padded out to four digits and never cut short, so a
        // short year fails the comparison anyway, but a year past 9999 is written back at its own
        // length: "12026-08-12" compares equal to itself and would land in the year 12026. Every
        // value refused here was already refused before a date on its own was accepted at all.
        // The length is tested first, so for most of these it is the check that fires.
        if (value.length() != SQL_DATE_LENGTH || !DateUtils.getSQLDateString(date).equals(value)) {
            throw notADate(value);
        }
        return date;
    }

    private static RuntimeException notADate(String value) {
        return new RuntimeException("the datetime \"" + value + "\" is not a real date as "
                + "yyyy-MM-dd HH:mm:ss or yyyy-MM-dd");
    }

    private String getTrimmedString(String source) {
        if (source != null) {
            return source.trim();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        mReader.close();
    }
}

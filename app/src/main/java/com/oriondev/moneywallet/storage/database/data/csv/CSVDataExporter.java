package com.oriondev.moneywallet.storage.database.data.csv;

import android.content.Context;
import android.database.Cursor;

import com.opencsv.CSVWriter;
import com.oriondev.moneywallet.model.Wallet;
import com.oriondev.moneywallet.storage.database.data.AbstractDataExporter;
import com.oriondev.moneywallet.storage.database.data.Constants;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 21/12/18.
 */
public class CSVDataExporter extends AbstractDataExporter {

    // multiple people are joined with a bare comma (not ", ") because an exported CSV is parsed
    // back by CSVDataImporter, which splits the people cell on this exact separator.
    private static final String PEOPLE_SEPARATOR = ",";

    private final File mOutputFile;
    private final CSVWriter mWriter;
    private final MoneyFormatter mMoneyFormatter;

    public CSVDataExporter(Context context, File folder) throws IOException {
        super(context, folder);
        mOutputFile = new File(folder, getDefaultFileName(".csv"));
        mWriter = new CSVWriter(new FileWriter(mOutputFile));
        mMoneyFormatter = MoneyFormatter.getInstance();
        mMoneyFormatter.setCurrencyEnabled(false);
        mMoneyFormatter.setRoundDecimalsEnabled(false);
        mMoneyFormatter.setGroupDigitEnabled(false);
    }

    @Override
    public boolean isMultiWalletSupported() {
        // in a csv file we cannot create different sections for the transactions of
        // each wallet so we have to list all the transactions inside the same file
        return false;
    }

    @Override
    public String[] getColumns(boolean uniqueWallet, String[] optionalColumns) {
        List<String> contractColumns = new ArrayList<>();
        contractColumns.add(Constants.COLUMN_WALLET);
        contractColumns.add(Constants.COLUMN_CURRENCY);
        contractColumns.add(Constants.COLUMN_CATEGORY);
        contractColumns.add(Constants.COLUMN_DATETIME);
        contractColumns.add(Constants.COLUMN_MONEY);
        contractColumns.add(Constants.COLUMN_DESCRIPTION);
        appendOptionalColumns(contractColumns, optionalColumns);
        return contractColumns.toArray(new String[contractColumns.size()]);
    }

    @Override
    public void exportData(Cursor cursor, String[] columns, Wallet... wallets) throws IOException {
        // the header line is the raw column keys, not localized labels: an exported CSV is
        // re-imported, so these keys are the machine readable contract with CSVDataImporter.
        mWriter.writeNext(columns);
        // export all the rows
        while (cursor.moveToNext()) {
            // for each row, we need to export all the fields as string
            String[] csvRow = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                csvRow[i] = getColumnValue(cursor, columns[i], mMoneyFormatter, PEOPLE_SEPARATOR);
            }
            mWriter.writeNext(csvRow);
        }
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
    }

    @Override
    public File getOutputFile() {
        return mOutputFile;
    }

    @Override
    public String getResultType() {
        return "application/vnd.ms-excel";
    }
}

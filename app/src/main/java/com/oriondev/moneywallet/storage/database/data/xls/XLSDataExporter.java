package com.oriondev.moneywallet.storage.database.data.xls;

import android.content.Context;
import android.database.Cursor;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.Wallet;
import com.oriondev.moneywallet.storage.database.data.AbstractDataExporter;
import com.oriondev.moneywallet.storage.database.data.Constants;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.CellView;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Created by andrea on 22/12/18.
 */
public class XLSDataExporter extends AbstractDataExporter {

    private static final String PEOPLE_SEPARATOR = ", ";

    private final File mOutputFile;
    private final WritableWorkbook mWorkbook;
    private final MoneyFormatter mMoneyFormatter;

    public XLSDataExporter(Context context, File folder) throws IOException {
        super(context, folder);
        mOutputFile = new File(folder, getDefaultFileName(".xls"));
        mWorkbook = Workbook.createWorkbook(mOutputFile);
        mMoneyFormatter = MoneyFormatter.getInstance();
    }

    @Override
    public boolean isMultiWalletSupported() {
        return true;
    }

    @Override
    public String[] getColumns(boolean uniqueWallet, String[] optionalColumns) {
        List<String> contractColumns = new ArrayList<>();
        contractColumns.add(Constants.COLUMN_DATETIME);
        contractColumns.add(Constants.COLUMN_CATEGORY);
        contractColumns.add(Constants.COLUMN_MONEY);
        if (uniqueWallet) {
            contractColumns.add(Constants.COLUMN_WALLET);
        }
        contractColumns.add(Constants.COLUMN_DESCRIPTION);
        appendOptionalColumns(contractColumns, optionalColumns);
        return contractColumns.toArray(new String[contractColumns.size()]);
    }

    @Override
    public void exportData(Cursor cursor, String[] columns, Wallet... wallets) throws IOException {
        WritableSheet sheet = mWorkbook.createSheet(getSheetName(wallets), getSheetIndex());
        try {
            // write the header of each column
            writeSheetHeader(sheet, columns);
            // write the body of the wallet
            for (int r = 1; r <= cursor.getCount(); r++) {
                // move the cursor to the fixed position
                cursor.moveToPosition(r - 1);
                // for each line of the cursor, write a line in the sheet
                for (int i = 0; i < columns.length; i++) {
                    String label = getColumnValue(cursor, columns[i], mMoneyFormatter, PEOPLE_SEPARATOR);
                    sheet.addCell(new Label(i, r, label));
                }
            }
            // calculate the width of each column to fit the values
            for (int i = 0; i < columns.length; i++) {
                CellView cellView = sheet.getColumnView(i);
                cellView.setAutosize(true);
                sheet.setColumnView(i, cellView);
            }
        } catch (WriteException e) {
            throw new IOException(e);
        }
    }

    private String getSheetName(Wallet... wallets) {
        if (wallets != null && wallets.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (Wallet wallet : wallets) {
                if (builder.length() != 0) {
                    builder.append(", ");
                }
                builder.append(wallet.getName());
            }
            return builder.toString();
        }
        return getContext().getString(R.string.hint_unknown);
    }

    private int getSheetIndex() {
        return mWorkbook.getNumberOfSheets();
    }

    private void writeSheetHeader(WritableSheet sheet, String[] columns) throws WriteException {
        for (int i = 0; i < columns.length; i++) {
            String label = getColumnHeader(columns[i]);
            WritableFont cellFont = new WritableFont(WritableFont.TAHOMA, 10);
            cellFont.setBoldStyle(WritableFont.BOLD);
            WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
            sheet.addCell(new Label(i, 0, label, cellFormat));
        }
    }

    @Override
    public void close() throws IOException {
        try {
            mWorkbook.write();
            mWorkbook.close();
        } catch (WriteException e) {
            throw new IOException(e);
        }
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

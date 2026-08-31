package com.oriondev.moneywallet.storage.database.data.pdf;

import android.content.Context;
import android.database.Cursor;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.model.Wallet;
import com.oriondev.moneywallet.storage.database.data.AbstractDataExporter;
import com.oriondev.moneywallet.storage.database.data.Constants;
import com.oriondev.moneywallet.utils.MoneyFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 22/12/18.
 */
public class PDFDataExporter extends AbstractDataExporter {

    private static final String PEOPLE_SEPARATOR = ", ";

    private final File mOutputFile;
    private final Document mDocument;
    private final MoneyFormatter mMoneyFormatter;

    private int mChapterCount = 0;

    public PDFDataExporter(Context context, File folder) throws IOException {
        super(context, folder);
        mOutputFile = new File(folder, getDefaultFileName(".pdf"));
        mMoneyFormatter = MoneyFormatter.getInstance();
        mDocument = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(mDocument, new FileOutputStream(mOutputFile));
        } catch (DocumentException e) {
            throw new IOException(e);
        }
        mDocument.addAuthor("Cash Ledger - Expense Manager");
        mDocument.open();
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
        try {
            Chapter chapter = createChapter(wallets);
            chapter.add(createTable(cursor, columns));
            mDocument.add(chapter);
        } catch (DocumentException e) {
            throw new IOException(e);
        }
    }

    private Chapter createChapter(Wallet... wallets) throws DocumentException {
        StringBuilder chapterTitleBuilder = new StringBuilder();
        if (wallets != null && wallets.length > 0) {
            for (Wallet wallet : wallets) {
                if (chapterTitleBuilder.length() != 0) {
                    chapterTitleBuilder.append(", ");
                }
                chapterTitleBuilder.append(wallet.getName());
            }
        } else {
            chapterTitleBuilder.append(getContext().getString(R.string.hint_unknown));
        }
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLDITALIC);
        Chunk chunk = new Chunk(chapterTitleBuilder.toString(), font);
        Paragraph paragraph = new Paragraph(chunk);
        paragraph.setSpacingAfter(30);
        return new Chapter(paragraph, ++mChapterCount);
    }

    private PdfPTable createTable(Cursor cursor, String[] columns) throws DocumentException {
        PdfPTable table = createTable(columns);
        for (int i = 0; i < cursor.getCount(); i++) {
            // move the cursor to the fixed position
            cursor.moveToPosition(i);
            // for each line of the cursor, write a line in the sheet
            for (String column : columns) {
                table.addCell(getColumnValue(cursor, column, mMoneyFormatter, PEOPLE_SEPARATOR));
            }
        }
        return table;
    }

    private PdfPTable createTable(String[] columns) throws DocumentException {
        // create the table with a fixed number of columns
        PdfPTable table = new PdfPTable(columns.length);
        table.setWidthPercentage(100f);
        table.getDefaultCell().setBackgroundColor(BaseColor.YELLOW);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        // initialize the table creating the header line
        for (String column : columns) {
            table.addCell(getColumnHeader(column));
        }
        table.getDefaultCell().setBackgroundColor(BaseColor.WHITE);
        return table;
    }

    @Override
    public void close() throws IOException {
        mDocument.close();
    }

    @Override
    public File getOutputFile() {
        return mOutputFile;
    }

    @Override
    public String getResultType() {
        return "application/pdf";
    }
}

package com.oriondev.moneywallet;

import android.database.Cursor;

public class LintProbe {

    public static String read(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex("probe"));
    }
}

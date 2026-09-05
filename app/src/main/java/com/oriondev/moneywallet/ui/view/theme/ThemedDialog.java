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

package com.oriondev.moneywallet.ui.view.theme;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.oriondev.moneywallet.R;
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog;
import com.philliphsu.bottomsheetpickers.time.BottomSheetTimePickerDialog;
import com.philliphsu.bottomsheetpickers.time.numberpad.NumberPadTimePickerDialog;

/**
 * Created by andrea on 20/08/18.
 */
public class ThemedDialog {

    // What material-dialogs faded a disabled action to.
    private static final float DISABLED_BUTTON_ALPHA = 0.4f;

    public static MaterialAlertDialogBuilder buildMaterialDialog(Context context) {
        return new ThemedAlertDialogBuilder(context);
    }

    /**
     * The accent as it lands on a dialog, contrast checked against the dialog surface so it falls
     * back to readable text when the user picks an accent that disappears there. Public because a
     * dialog outside this package tints a widget of its own with it.
     */
    public static int getAccentColor() {
        ITheme theme = ThemeEngine.getTheme();
        int background = theme.getColorCardBackground();
        return Util.visibleOr(theme.getColorAccent(), background, theme.getBestTextColor(background));
    }

    /**
     * The faded foreground for a control that is idle or disabled on a dialog, the same hint color
     * the theme engine hands a hint on that surface. It is read off the dialog surface and not off
     * the accent, so it stays visible whatever accent the user picks.
     */
    public static int getIdleColor() {
        ITheme theme = ThemeEngine.getTheme();
        return theme.getBestHintColor(theme.getColorCardBackground());
    }

    /**
     * The one definition of which dialog style the app draws a dialog with. The builder and the
     * scrollable custom view both read it here so the two cannot drift apart.
     */
    @StyleRes
    private static int getDialogTheme() {
        return ThemeEngine.getTheme().isDark() ? R.style.MoneyWalletAlertDialogDark : R.style.MoneyWalletAlertDialogLight;
    }

    /**
     * AlertDialog.setView does not scroll what it is given, so the layouts that used to ask
     * material-dialogs to wrap them in a scroller carry their own.
     */
    public static View inflateScrollableView(Context context, @LayoutRes int layoutRes) {
        // Every caller passes an activity, whose theme is light in every mode, so a plain widget
        // in the layout would resolve its text colors light and draw dark on a dark dialog. The
        // wrapper puts the layout on the same dialog style the builder picks.
        Context themedContext = new ContextThemeWrapper(context, getDialogTheme());
        NestedScrollView scrollView = new NestedScrollView(themedContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The framework paints selectableItemBackground over the scroller's whole bounds while
            // the scroller holds focus itself, which it takes outside touch mode once an arrow key
            // scrolls the focused child off screen.
            scrollView.setDefaultFocusHighlightEnabled(false);
        }
        View content = LayoutInflater.from(themedContext).inflate(layoutRes, scrollView, false);
        Resources resources = context.getResources();
        int sidePadding = resources.getDimensionPixelSize(R.dimen.material_dialog_custom_view_start_end_padding);
        int verticalPadding = resources.getDimensionPixelSize(R.dimen.material_dialog_custom_view_top_bottom_padding);
        // The side padding replaces what the layout declared instead of adding to it, which is
        // what material-dialogs did with a wrapped custom view, so a layout that already carries
        // its own 24dp keeps 24dp. The scroller does not clip it so the content can scroll
        // through the vertical padding.
        content.setPaddingRelative(sidePadding, 0, sidePadding, 0);
        scrollView.setPadding(0, verticalPadding, 0, verticalPadding);
        scrollView.setClipToPadding(false);
        scrollView.addView(content);
        return scrollView;
    }

    /**
     * Raises the soft keyboard with the dialog and keeps the positive button disabled while the
     * field is empty, which is what material-dialogs did for an input declared without allowing an
     * empty value. The button only exists once the dialog has been shown, so this runs after show.
     */
    public static void showWithInput(AlertDialog dialog, final EditText editText, final boolean allowEmpty) {
        tintInput(editText);
        // A caller that prefills the field would otherwise leave the caret in front of that text
        // and have the first keystroke land at the front of the stored value.
        editText.setSelection(editText.length());
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        dialog.show();
        if (allowEmpty) {
            return;
        }
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(editText.length() > 0);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                positiveButton.setEnabled(text.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable text) {}
        });
    }

    /**
     * Paints the underline with the accent while the field is live and leaves it faded while the
     * field is disabled or idle, and paints the typed text and the hint. All of it comes from the
     * theme engine, not from colorControlNormal or textColorPrimary on the inflating context,
     * because that context is the activity and its theme is light in every mode.
     */
    public static void tintInput(EditText editText) {
        int accent = getAccentColor();
        int idle = getIdleColor();
        ColorStateList colors = new ColorStateList(
                new int[][] {
                        new int[] {-android.R.attr.state_enabled},
                        new int[] {-android.R.attr.state_pressed, -android.R.attr.state_focused},
                        new int[] {}
                },
                new int[] {idle, idle, accent}
        );
        ViewCompat.setBackgroundTintList(editText, colors);
        TintHelper.setCursorTint(editText, accent);
        ITheme theme = ThemeEngine.getTheme();
        editText.setTextColor(theme.getBestTextColor(theme.getColorCardBackground()));
        editText.setHintTextColor(idle);
    }

    /**
     * The single alert dialog factory for the app. It picks the light or dark dialog theme,
     * because ThemeEngine paints views by hand and never sets an XML theme, and puts the accent on
     * the buttons, which only exist once the dialog is shown.
     */
    private static class ThemedAlertDialogBuilder extends MaterialAlertDialogBuilder {

        private final ColorStateList mButtonColors;

        private ThemedAlertDialogBuilder(Context context) {
            super(context, getDialogTheme());
            int accent = getAccentColor();
            mButtonColors = new ColorStateList(
                    new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                    new int[] {Util.adjustAlpha(accent, DISABLED_BUTTON_ALPHA), accent}
            );
        }

        @Override
        public AlertDialog create() {
            final AlertDialog dialog = super.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface unused) {
                    tintButton(dialog, AlertDialog.BUTTON_POSITIVE);
                    tintButton(dialog, AlertDialog.BUTTON_NEGATIVE);
                    tintButton(dialog, AlertDialog.BUTTON_NEUTRAL);
                }
            });
            return dialog;
        }

        private void tintButton(AlertDialog dialog, int which) {
            Button button = dialog.getButton(which);
            if (button != null) {
                // A state list, not a single color, so a disabled button still reads as disabled.
                button.setTextColor(mButtonColors);
            }
        }
    }

    public static DatePickerDialog.Builder buildDatePickerDialog(DatePickerDialog.OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth) {
        DatePickerDialog.Builder builder = new DatePickerDialog.Builder(listener, year, monthOfYear, dayOfMonth);
        ITheme theme = ThemeEngine.getTheme();
        builder.setThemeDark(theme.isDark());
        builder.setAccentColor(theme.getColorAccent());
        return builder;
    }

    public static NumberPadTimePickerDialog.Builder buildNumberPadTimePickerDialog(BottomSheetTimePickerDialog.OnTimeSetListener listener, boolean is24HourMode) {
        NumberPadTimePickerDialog.Builder builder = new NumberPadTimePickerDialog.Builder(listener, is24HourMode);
        ITheme theme = ThemeEngine.getTheme();
        builder.setThemeDark(theme.isDark());
        builder.setAccentColor(theme.getColorAccent());
        return builder;
    }

    public static BottomSheetBuilder buildBottomSheet(Context context) {
        BottomSheetBuilder builder = new BottomSheetBuilder(context);
        ITheme theme = ThemeEngine.getTheme();
        builder.setBackgroundColor(theme.getColorCardBackground());
        builder.setTitleTextColor(theme.getTextColorPrimary());
        builder.setItemTextColor(theme.getTextColorPrimary());
        builder.setIconTintColor(theme.getIconColor());
        return builder;
    }
}
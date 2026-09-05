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

package com.oriondev.moneywallet.ui.fragment.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.ui.view.ColorSwatchView;
import com.oriondev.moneywallet.ui.view.theme.ThemedDialog;

import java.util.Locale;

/**
 * The app's color chooser, replacing the one material-dialogs provided. The palette is the same
 * one, in ColorPalette; the two levels and the drill in are the same behavior. What it drops is
 * the alpha channel, the RGB sliders and the Custom/Presets toggle, and in their place the hex
 * field is on both levels and is the only way to reach a color the palette does not carry.
 */
public class ColorChooserDialog extends DialogFragment {

    private static final String ARG_TITLE = "ColorChooserDialog::Arguments::Title";
    private static final String ARG_ACCENT_PALETTE = "ColorChooserDialog::Arguments::AccentPalette";
    private static final String ARG_PRESELECT_COLOR = "ColorChooserDialog::Arguments::PreselectColor";

    private static final String SS_IN_SHADE_LEVEL = "ColorChooserDialog::SavedState::InShadeLevel";
    private static final String SS_HUE_INDEX = "ColorChooserDialog::SavedState::HueIndex";
    private static final String SS_SHADE_INDEX = "ColorChooserDialog::SavedState::ShadeIndex";
    private static final String SS_SELECTED_COLOR = "ColorChooserDialog::SavedState::SelectedColor";

    private static final int HEX_DIGITS = 6;

    public static ColorChooserDialog newInstance(@StringRes int title, boolean accentPalette, @ColorInt int preselectColor) {
        ColorChooserDialog dialog = new ColorChooserDialog();
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_TITLE, title);
        arguments.putBoolean(ARG_ACCENT_PALETTE, accentPalette);
        arguments.putInt(ARG_PRESELECT_COLOR, preselectColor);
        dialog.setArguments(arguments);
        return dialog;
    }

    private Callback mCallback;

    private int[] mHues;
    private int[][] mShades;

    private boolean mInShadeLevel;
    private int mHueIndex;
    private int mShadeIndex;
    private int mSelectedColor;

    private GridView mGridView;
    private EditText mHexEditText;
    private ColorSwatchView mPreviewView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if (!(parent instanceof Callback)) {
            throw new IllegalStateException("ColorChooserDialog must be shown from a parent fragment implementing Callback");
        }
        mCallback = (Callback) parent;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean accentPalette = arguments != null && arguments.getBoolean(ARG_ACCENT_PALETTE);
        mHues = accentPalette ? ColorPalette.ACCENT_COLORS : ColorPalette.PRIMARY_COLORS;
        mShades = accentPalette ? ColorPalette.ACCENT_COLORS_SUB : ColorPalette.PRIMARY_COLORS_SUB;
        if (savedInstanceState != null) {
            mInShadeLevel = savedInstanceState.getBoolean(SS_IN_SHADE_LEVEL);
            mHueIndex = savedInstanceState.getInt(SS_HUE_INDEX);
            mShadeIndex = savedInstanceState.getInt(SS_SHADE_INDEX);
            mSelectedColor = savedInstanceState.getInt(SS_SELECTED_COLOR);
        } else {
            mSelectedColor = 0xFF000000 | (arguments != null ? arguments.getInt(ARG_PRESELECT_COLOR) : Color.BLACK);
            findPreselect(mSelectedColor);
        }
        AlertDialog dialog = ThemedDialog.buildMaterialDialog(requireActivity())
                .setTitle(arguments != null ? arguments.getInt(ARG_TITLE) : R.string.dialog_color_picker_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface unused, int which) {
                        mCallback.onColorSelection(ColorChooserDialog.this, mSelectedColor);
                        dismiss();
                    }

                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        // The dialog's own context carries the light or dark dialog theme the factory picked, so
        // the layout's theme attributes have to resolve against it and not against the activity.
        // This is the same clone DialogFragment makes for a dialog it inflates a content view for,
        // and it has to be made by hand here because getLayoutInflater() hands back the plain
        // activity inflater while onCreateDialog is still running.
        View view = getLayoutInflater().cloneInContext(dialog.getContext())
                .inflate(R.layout.dialog_color_chooser, null);
        mGridView = view.findViewById(R.id.color_chooser_grid_view);
        mHexEditText = view.findViewById(R.id.color_chooser_hex_edit_text);
        mPreviewView = view.findViewById(R.id.color_chooser_preview_view);
        mGridView.setAdapter(new SwatchAdapter());
        mHexEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(HEX_DIGITS)});
        ThemedDialog.tintInput(mHexEditText);
        mHexEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                onHexTyped(text.toString());
            }

            @Override
            public void afterTextChanged(Editable text) {}

        });
        writeHex(mSelectedColor);
        mPreviewView.setColor(mSelectedColor);
        dialog.setView(view);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return;
        }
        // Negative is Back on the shade level, and back has to leave the dialog up, so the click
        // that comes with the button is replaced. The button only exists once the dialog has been
        // shown. Tapping outside and the back button still cancel.
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mInShadeLevel) {
                    mInShadeLevel = false;
                    findPreselect(mSelectedColor);
                    invalidate();
                } else {
                    dialog.cancel();
                }
            }

        });
        invalidateNegativeButton();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SS_IN_SHADE_LEVEL, mInShadeLevel);
        outState.putInt(SS_HUE_INDEX, mHueIndex);
        outState.putInt(SS_SHADE_INDEX, mShadeIndex);
        outState.putInt(SS_SELECTED_COLOR, mSelectedColor);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mCallback != null) {
            mCallback.onColorChooserDismissed(this);
        }
    }

    /**
     * Points the two indices at the preselect if the palette carries it, as a hue or as one of a
     * hue's shades, and clears them if it does not. Either way the dialog opens on the hue level,
     * which is what the material-dialogs one did; the shade index only decides where drilling in
     * lands.
     */
    private void findPreselect(@ColorInt int color) {
        for (int hue = 0; hue < mHues.length; hue++) {
            if (mHues[hue] == color) {
                mHueIndex = hue;
                mShadeIndex = findShade(hue, color);
                return;
            }
            int shade = findShade(hue, color);
            if (shade >= 0) {
                mHueIndex = hue;
                mShadeIndex = shade;
                return;
            }
        }
        mHueIndex = -1;
        mShadeIndex = -1;
    }

    private int findShade(int hue, @ColorInt int color) {
        for (int shade = 0; shade < mShades[hue].length; shade++) {
            if (mShades[hue][shade] == color) {
                return shade;
            }
        }
        return -1;
    }

    private void onHexTyped(String text) {
        if (text.length() != HEX_DIGITS) {
            return;
        }
        try {
            mSelectedColor = Color.parseColor("#" + text);
        } catch (IllegalArgumentException e) {
            return;
        }
        mPreviewView.setColor(mSelectedColor);
        if (mInShadeLevel) {
            mShadeIndex = mHueIndex >= 0 ? findShade(mHueIndex, mSelectedColor) : -1;
        } else {
            findPreselect(mSelectedColor);
        }
        ((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
    }

    private void writeHex(@ColorInt int color) {
        mHexEditText.setText(String.format(Locale.US, "%06X", 0xFFFFFF & color));
    }

    private void onSwatchClicked(int position) {
        if (mInShadeLevel) {
            mShadeIndex = position;
            mSelectedColor = mShades[mHueIndex][position];
        } else {
            // The ring on the hue level marks the family of the selected color, which may be one of
            // that family's shades, so tapping the ringed hue drills in and keeps the color.
            if (mHueIndex != position) {
                mSelectedColor = mHues[position];
                mShadeIndex = findShade(position, mSelectedColor);
            }
            mHueIndex = position;
            mInShadeLevel = true;
        }
        writeHex(mSelectedColor);
        mPreviewView.setColor(mSelectedColor);
        invalidate();
    }

    private void invalidate() {
        ((BaseAdapter) mGridView.getAdapter()).notifyDataSetChanged();
        invalidateNegativeButton();
    }

    private void invalidateNegativeButton() {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(mInShadeLevel ? R.string.action_back : android.R.string.cancel);
        }
    }

    public interface Callback {

        void onColorSelection(ColorChooserDialog dialog, @ColorInt int color);

        void onColorChooserDismissed(ColorChooserDialog dialog);
    }

    private class SwatchAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mInShadeLevel ? mShades[mHueIndex].length : mHues.length;
        }

        @Override
        public Object getItem(int position) {
            return colorAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @ColorInt
        private int colorAt(int position) {
            return mInShadeLevel ? mShades[mHueIndex][position] : mHues[position];
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                int size = getResources().getDimensionPixelSize(R.dimen.color_chooser_swatch_size);
                convertView = new ColorSwatchView(parent.getContext());
                convertView.setLayoutParams(new GridView.LayoutParams(size, size));
            }
            ColorSwatchView swatch = (ColorSwatchView) convertView;
            swatch.setColor(colorAt(position));
            swatch.setSwatchSelected(position == (mInShadeLevel ? mShadeIndex : mHueIndex));
            swatch.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onSwatchClicked(position);
                }

            });
            return convertView;
        }
    }
}

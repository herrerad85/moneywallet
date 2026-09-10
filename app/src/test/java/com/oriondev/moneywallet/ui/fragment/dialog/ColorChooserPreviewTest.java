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
import android.view.WindowManager;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.oriondev.moneywallet.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A preview repaints every screen behind the chooser, so it happens once, and only for a color the
 * user picked.
 * <p>
 * The hex field's watcher is what makes a typed color take effect, and the dialog writes that field
 * itself when it opens and on every tap, so both of those reach the watcher too.
 */
@RunWith(RobolectricTestRunner.class)
public class ColorChooserPreviewTest {

    private static final int PRESELECT = 0xFF3F51B5;

    @Test
    public void openingTheChooserPreviewsNothingAndOneTapPreviewsOnce() {
        ActivityController<AppCompatActivity> controller =
                Robolectric.buildActivity(AppCompatActivity.class).setup();
        try {
            CountingHost host = new CountingHost();
            controller.get().getSupportFragmentManager()
                    .beginTransaction().add(host, "host").commitNow();
            ColorChooserDialog chooser =
                    ColorChooserDialog.newInstance(R.string.dialog_color_picker_title, false, PRESELECT);
            chooser.showNow(host.getChildFragmentManager(), "chooser");

            assertEquals("the color the chooser opened on was previewed before anyone picked it",
                    0, host.mPreviews);

            firstSwatchOf(chooser).performClick();
            assertEquals("one tap, one repaint of every screen", 1, host.mPreviews);
        } finally {
            controller.close();
        }
    }

    /**
     * The field's own watcher is what puts a preview back when the framework restores the field,
     * and it ignores anything that is not six digits of a real color, which is what half typing a
     * custom value leaves behind.
     */
    @Test
    public void aPreviewOutlivesTheScreenBeingRebuiltUnderAHalfTypedHexValue() {
        ActivityController<AppCompatActivity> controller =
                Robolectric.buildActivity(AppCompatActivity.class).setup();
        try {
            AppCompatActivity activity = controller.get();
            CountingHost host = new CountingHost();
            activity.getSupportFragmentManager()
                    .beginTransaction().add(host, "host").commitNow();
            ColorChooserDialog chooser =
                    ColorChooserDialog.newInstance(R.string.dialog_color_picker_title, false, PRESELECT);
            chooser.showNow(host.getChildFragmentManager(), "chooser");
            firstSwatchOf(chooser).performClick();
            hexFieldOf(chooser).setText("F4433");

            controller.recreate();

            CountingHost restored = (CountingHost) controller.get()
                    .getSupportFragmentManager().findFragmentByTag("host");
            assertNotNull(restored);
            assertEquals("the rebuilt screen was left showing the stored color while the chooser"
                    + " still held the picked one", 1, restored.mPreviews);
        } finally {
            controller.close();
        }
    }

    /**
     * The dim is over the surface being previewed, so it goes while one is up. Walking back to
     * the color that is stored ends the preview and has to look like it did on opening.
     */
    @Test
    public void theDimFollowsWhatIsBeingPreviewed() {
        ActivityController<AppCompatActivity> controller =
                Robolectric.buildActivity(AppCompatActivity.class).setup();
        try {
            CountingHost host = new CountingHost();
            controller.get().getSupportFragmentManager()
                    .beginTransaction().add(host, "host").commitNow();
            ColorChooserDialog chooser =
                    ColorChooserDialog.newInstance(R.string.dialog_color_picker_title, false, PRESELECT);
            chooser.showNow(host.getChildFragmentManager(), "chooser");
            assertTrue("the chooser opened over an undimmed screen", isDimmed(chooser));

            firstSwatchOf(chooser).performClick();
            assertFalse("the dim stayed over the color being previewed", isDimmed(chooser));

            hexFieldOf(chooser).setText("3F51B5");
            assertTrue("nothing is previewed and the dim never came back", isDimmed(chooser));
        } finally {
            controller.close();
        }
    }

    private boolean isDimmed(ColorChooserDialog chooser) {
        Dialog dialog = chooser.getDialog();
        assertNotNull(dialog);
        assertNotNull(dialog.getWindow());
        return (dialog.getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_DIM_BEHIND) != 0;
    }

    private EditText hexFieldOf(ColorChooserDialog chooser) {
        Dialog dialog = chooser.getDialog();
        assertNotNull(dialog);
        return dialog.findViewById(R.id.color_chooser_hex_edit_text);
    }

    private View firstSwatchOf(ColorChooserDialog chooser) {
        Dialog dialog = chooser.getDialog();
        assertNotNull(dialog);
        GridView grid = dialog.findViewById(R.id.color_chooser_grid_view);
        assertNotNull(grid);
        return grid.getAdapter().getView(0, null, grid);
    }

    public static class CountingHost extends Fragment implements ColorChooserDialog.Callback {

        private int mPreviews;

        @Override
        public void onColorSelection(ColorChooserDialog dialog, int color) {}

        @Override
        public void onColorChooserDismissed(ColorChooserDialog dialog) {}

        /** As the engine does, which refuses to override the color that is stored. */
        @Override
        public boolean onColorPreview(ColorChooserDialog dialog, int color) {
            mPreviews++;
            return color != PRESELECT;
        }
    }
}

package com.oriondev.moneywallet.ui.fragment.dialog;

import android.graphics.Color;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.ui.view.ColorSwatchView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

/**
 * Drives the color chooser on the JVM under a host fragment that plays the caller: which swatch it
 * opens on, what the hex field reads, and which color OK hands back. The swatches it reads are the
 * grid's own children, so what it sees is what the grid draws.
 */
@RunWith(RobolectricTestRunner.class)
public class ColorChooserDialogTest {

    private static final String TAG_HOST = "ColorChooserDialogTest::Host";
    private static final String TAG_DIALOG = "ColorChooserDialogTest::Dialog";

    /** The caller the dialog resolves its callback from, keeping what it was handed. */
    public static class HostFragment extends Fragment implements ColorChooserDialog.Callback {

        Integer selected;
        boolean dismissed;

        @Override
        public void onColorSelection(ColorChooserDialog dialog, int color) {
            selected = color;
        }

        @Override
        public void onColorChooserDismissed(ColorChooserDialog dialog) {
            dismissed = true;
        }
    }

    @Test
    public void aPreselectThatThePaletteCarriesOpensOnItsSwatch() {
        run(Color.parseColor("#3F51B5"), false, (host, dialog) -> {
            assertTrue(swatch(dialog, 4).isSwatchSelected());
            assertFalse(swatch(dialog, 0).isSwatchSelected());
            assertEquals("3F51B5", hex(dialog).getText().toString());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFF3F51B5), host.selected);
        });
    }

    @Test
    public void aPreselectOutsideThePaletteOpensOnNoSwatchAtAll() {
        run(Color.parseColor("#FF0000"), false, (host, dialog) -> {
            GridView grid = grid(dialog);
            assertEquals(grid.getAdapter().getCount(), grid.getChildCount());
            for (int index = 0; index < grid.getChildCount(); index++) {
                assertFalse(swatch(dialog, index).isSwatchSelected());
            }
            assertEquals("FF0000", hex(dialog).getText().toString());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFFFF0000), host.selected);
        });
    }

    @Test
    public void aColorTypedIntoTheHexFieldSelectsItsSwatch() {
        run(Color.parseColor("#FF0000"), false, (host, dialog) -> {
            hex(dialog).setText("4CAF50");
            layout(dialog);
            assertEquals("#4CAF50", preview(dialog).getContentDescription().toString());
            assertTrue(swatch(dialog, 9).isSwatchSelected());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFF4CAF50), host.selected);
        });
    }

    @Test
    public void aColorTypedWithAnExtraDigitIsCutToTheSixTheFieldHolds() {
        run(Color.parseColor("#FF0000"), false, (host, dialog) -> {
            hex(dialog).setText("4CAF50A");
            layout(dialog);
            assertEquals("4CAF50", hex(dialog).getText().toString());
            assertTrue(swatch(dialog, 9).isSwatchSelected());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFF4CAF50), host.selected);
        });
    }

    @Test
    public void aTapOnAHueOtherThanTheRingedOneTakesItsColorAndShade() {
        run(Color.parseColor("#FF0000"), false, (host, dialog) -> {
            swatch(dialog, 4).performClick();
            layout(dialog);
            assertEquals(10, grid(dialog).getChildCount());
            // Indigo 500 is the hue itself and the sixth of the shades this level is showing.
            assertTrue(swatch(dialog, 5).isSwatchSelected());
            assertEquals("3F51B5", hex(dialog).getText().toString());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFF3F51B5), host.selected);
        });
    }

    @Test
    public void backFromAShadeLevelRingsTheFamilyOfTheTypedColorAndDrillingBackInKeepsIt() {
        run(Color.parseColor("#3F51B5"), false, (host, dialog) -> {
            swatch(dialog, 4).performClick();
            layout(dialog);
            assertEquals(dialog.getString(R.string.action_back), negative(dialog).getText().toString());
            assertEquals(10, grid(dialog).getChildCount());
            assertTrue(swatch(dialog, 5).isSwatchSelected());
            // Red 300, a shade of a hue other than the one this level is showing.
            hex(dialog).setText("E57373");
            layout(dialog);
            GridView shades = grid(dialog);
            assertEquals(shades.getAdapter().getCount(), shades.getChildCount());
            for (int index = 0; index < shades.getChildCount(); index++) {
                assertFalse(swatch(dialog, index).isSwatchSelected());
            }
            back(dialog);
            layout(dialog);
            assertEquals(19, grid(dialog).getChildCount());
            assertTrue(swatch(dialog, 0).isSwatchSelected());
            assertFalse(swatch(dialog, 4).isSwatchSelected());
            assertEquals("E57373", hex(dialog).getText().toString());
            swatch(dialog, 0).performClick();
            layout(dialog);
            assertEquals(dialog.getString(R.string.action_back), negative(dialog).getText().toString());
            assertEquals(10, grid(dialog).getChildCount());
            assertTrue(swatch(dialog, 3).isSwatchSelected());
            assertEquals("E57373", hex(dialog).getText().toString());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFFE57373), host.selected);
        });
    }

    @Test
    public void aPreselectCarryingAnAlphaChannelIsTakenAsItsOpaqueColor() {
        run(0x803F51B5, false, (host, dialog) -> {
            assertTrue(swatch(dialog, 4).isSwatchSelected());
            assertEquals("3F51B5", hex(dialog).getText().toString());
            ok(dialog);
            assertEquals(Integer.valueOf(0xFF3F51B5), host.selected);
        });
    }

    @Test
    public void rotatingOnTheShadeLevelKeepsTheLevelTheRingAndTheField() {
        run(Color.parseColor("#3F51B5"), false, (host, dialog) -> {
            swatch(dialog, 4).performClick();
            layout(dialog);
            // Indigo 50, the first of the shades this level is showing.
            hex(dialog).setText("E8EAF6");
            layout(dialog);
            assertTrue(swatch(dialog, 0).isSwatchSelected());

            mController.recreate();
            shadowOf(Looper.getMainLooper()).idle();
            HostFragment restoredHost = (HostFragment) mController.get()
                    .getSupportFragmentManager().findFragmentByTag(TAG_HOST);
            assertNotNull(restoredHost);
            ColorChooserDialog restored = (ColorChooserDialog) restoredHost
                    .getChildFragmentManager().findFragmentByTag(TAG_DIALOG);
            assertNotNull(restored);
            layout(restored);
            assertEquals(10, grid(restored).getChildCount());
            assertTrue(swatch(restored, 0).isSwatchSelected());
            assertEquals("E8EAF6", hex(restored).getText().toString());
            assertEquals(restored.getString(R.string.action_back),
                    negative(restored).getText().toString());

            back(restored);
            layout(restored);
            assertEquals(19, grid(restored).getChildCount());
            assertTrue(swatch(restored, 4).isSwatchSelected());
            ok(restored);
            assertEquals(Integer.valueOf(0xFFE8EAF6), restoredHost.selected);
        });
    }

    private interface Case {

        void run(HostFragment host, ColorChooserDialog dialog);
    }

    private ActivityController<AppCompatActivity> mController;

    private void run(int preselect, boolean accentPalette, Case body) {
        // The host is a bare AppCompatActivity, which the manifest does not declare, so it is
        // driven through a controller instead of through ActivityScenario. The controller is a
        // field so that a case which recreates the activity can reach it.
        ActivityController<AppCompatActivity> controller =
                Robolectric.buildActivity(AppCompatActivity.class).setup();
        mController = controller;
        try {
            AppCompatActivity activity = controller.get();
            HostFragment host = new HostFragment();
            activity.getSupportFragmentManager().beginTransaction().add(host, TAG_HOST).commitNow();
            ColorChooserDialog dialog = ColorChooserDialog.newInstance(
                    R.string.dialog_color_picker_title, accentPalette, preselect);
            dialog.show(host.getChildFragmentManager(), TAG_DIALOG);
            host.getChildFragmentManager().executePendingTransactions();
            assertNull(host.selected);
            layout(dialog);
            body.run(host, dialog);
        } finally {
            controller.close();
        }
    }

    /**
     * Measures and lays out the dialog's decor, which is what attaches the grid's children. The
     * adapter rebuilds them on the next layout, so this runs again after anything that changes the
     * grid.
     */
    private void layout(ColorChooserDialog dialog) {
        View decor = dialog.getDialog().getWindow().getDecorView();
        DisplayMetrics metrics = decor.getResources().getDisplayMetrics();
        decor.measure(View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.AT_MOST));
        decor.layout(0, 0, decor.getMeasuredWidth(), decor.getMeasuredHeight());
    }

    private GridView grid(ColorChooserDialog dialog) {
        return dialog.getDialog().findViewById(R.id.color_chooser_grid_view);
    }

    private ColorSwatchView swatch(ColorChooserDialog dialog, int index) {
        return (ColorSwatchView) grid(dialog).getChildAt(index);
    }

    private EditText hex(ColorChooserDialog dialog) {
        return dialog.getDialog().findViewById(R.id.color_chooser_hex_edit_text);
    }

    private ColorSwatchView preview(ColorChooserDialog dialog) {
        return dialog.getDialog().findViewById(R.id.color_chooser_preview_view);
    }

    private Button negative(ColorChooserDialog dialog) {
        return ((AlertDialog) dialog.getDialog()).getButton(AlertDialog.BUTTON_NEGATIVE);
    }

    private void back(ColorChooserDialog dialog) {
        negative(dialog).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void ok(ColorChooserDialog dialog) {
        ((AlertDialog) dialog.getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        // AlertController hands the button click to a Handler, and the looper is paused here.
        shadowOf(Looper.getMainLooper()).idle();
    }
}

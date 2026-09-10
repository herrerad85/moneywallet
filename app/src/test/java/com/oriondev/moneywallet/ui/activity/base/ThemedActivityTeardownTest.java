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

package com.oriondev.moneywallet.ui.activity.base;

import androidx.fragment.app.Fragment;

import com.oriondev.moneywallet.ui.view.theme.ITheme;
import com.oriondev.moneywallet.ui.view.theme.ThemeEngine;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertEquals;

/**
 * A color chooser ends its preview as it is torn down, and an activity being destroyed tears its
 * fragments down, so the theme can change inside the destruction of a screen that is in no state to
 * be repainted.
 */
@RunWith(RobolectricTestRunner.class)
public class ThemedActivityTeardownTest {

    @After
    public void clearThePreview() {
        ThemeEngine.clearPreview();
    }

    @Test
    public void anActivityIsNotRepaintedByItsOwnFragmentsBeingTornDown() {
        ActivityController<CountingActivity> controller =
                Robolectric.buildActivity(CountingActivity.class).setup();
        CountingActivity activity = controller.get();
        activity.getSupportFragmentManager()
                .beginTransaction().add(new PreviewEndingFragment(), "ending").commitNow();
        ThemeEngine.previewColorPrimary(ThemeEngine.getTheme().getColorPrimary() ^ 0x00FFFFFF);

        controller.pause().stop().destroy();

        assertEquals("a dying activity was repainted", 0, activity.mRepaintsWhileDying);
    }

    /** Stands in for the color chooser, which ends its preview from the same point. */
    public static class PreviewEndingFragment extends Fragment {

        @Override
        public void onDestroy() {
            super.onDestroy();
            ThemeEngine.clearPreview();
        }
    }

    public static class CountingActivity extends ThemedActivity {

        private boolean mDying;
        private int mRepaintsWhileDying;

        @Override
        protected void onDestroy() {
            mDying = true;
            super.onDestroy();
        }

        @Override
        public void onThemeChanged(ITheme theme) {
            if (mDying) {
                mRepaintsWhileDying++;
                return;
            }
            super.onThemeChanged(theme);
        }
    }
}

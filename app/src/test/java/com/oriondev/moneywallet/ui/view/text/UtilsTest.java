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

package com.oriondev.moneywallet.ui.view.text;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;

import org.junit.Test;

/**
 * Regression coverage for issue #174. {@link Utils#isRtl} used to compare the value returned by
 * Configuration.getLayoutDirection, which is 0 or 1, against Configuration.SCREENLAYOUT_LAYOUTDIR_RTL,
 * which is the value 128 of a mask over the screenLayout field. Those can never be equal, so every
 * right to left branch in MaterialEditText and MaterialTextView was unreachable and nothing either
 * view draws itself has ever mirrored. The right to left case below fails on the old comparison.
 */
public class UtilsTest {

    private Resources resourcesWithLayoutDirection(int layoutDirection) {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getLayoutDirection()).thenReturn(layoutDirection);
        Resources resources = mock(Resources.class);
        when(resources.getConfiguration()).thenReturn(configuration);
        return resources;
    }

    @Test
    public void rightToLeftConfigurationIsRightToLeft() {
        assertTrue(Utils.isRtl(resourcesWithLayoutDirection(View.LAYOUT_DIRECTION_RTL)));
    }

    @Test
    public void leftToRightConfigurationIsNotRightToLeft() {
        assertFalse(Utils.isRtl(resourcesWithLayoutDirection(View.LAYOUT_DIRECTION_LTR)));
    }
}

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

import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.graphics.Insets;
import androidx.core.view.LayoutInflaterCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.ui.view.theme.ITheme;
import com.oriondev.moneywallet.ui.view.theme.ThemeEngine;
import com.oriondev.moneywallet.ui.view.theme.ThemedLayoutInflater;
import com.oriondev.moneywallet.utils.Utils;

import java.lang.reflect.Field;

/**
 * This activity is used as base activity for all the application activities.
 * It will automatically apply the current theme to all the views that are subscribed
 * to the ThemeEngine.
 * The first step is done during the inflation of the layout: here the theme properties are
 * automatically set to the view that is subscribed just after the creation.
 * The activity will than register itself as an observer for the current theme changes.
 * Whenever a property of the current theme changes, the observer will be notified.
 * Before the destruction the activity MUST un subscribe as observer to avoid memory leaks.
 */
public abstract class ThemedActivity extends AppCompatActivity implements ThemeEngine.ThemeObserver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.registerObserver(this);
        applyLayoutInflaterWrapper();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onThemeSetup(ThemeEngine.getTheme());
    }

    // From Android 15 (API 35) edge to edge is enforced for apps targeting SDK 35: the system bars
    // no longer reserve space, so without this the toolbar would sit under the status bar and the
    // bottom controls (first run buttons, keypad, FABs) under the navigation bar. We opt the content
    // back into the safe area by padding it with the system bar and display cutout insets. The bars
    // themselves show through to the themed window background, and onThemeStatusBarIcons keeps the
    // bar icons legible against it. Centralised here so every activity that goes through this base
    // class inherits the fix.

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applySystemBarInsets();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applySystemBarInsets();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applySystemBarInsets();
    }

    private void applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }
        final View content = findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        content.requestApplyInsets();
    }

    /**
     * This method is used to set the layout inflater factory even if it is already set.
     * Use reflection to access the private boolean field and set it as false.
     * The AppCompat library uses it's own factory to correct the xml layouts.
     * In this way both the factories will coexist without problems.
     */
    private void applyLayoutInflaterWrapper() {
        LayoutInflater inflater = getLayoutInflater();
        final LayoutInflater.Factory2 baseFactory = inflater.getFactory2();
        try {
            Field field = LayoutInflater.class.getDeclaredField("mFactorySet");
            field.setAccessible(true);
            field.setBoolean(inflater, false);
            LayoutInflaterCompat.setFactory2(getLayoutInflater(), new ThemedLayoutInflater(baseFactory));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ThemeEngine.unregisterObserver(this);
    }

    @Override
    public void onThemeChanged(ITheme theme) {
        ThemeEngine.applyTheme(getWindow().peekDecorView(), true);
        onThemeSetup(theme);
    }

    /**
     * This method is called by the activity when the activity has been created and
     * dynamically when the theme engine detects a change of a value of the theme.
     * @param theme current theme to apply
     */
    @CallSuper
    protected void onThemeSetup(ITheme theme) {
        setupActivityBaseTheme(theme);
    }

    private void setupActivityBaseTheme(ITheme theme) {
        onThemeStatusBar(theme);
        onThemeStatusBarIcons(theme);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onThemeTaskDescription(theme);
        }
        onThemeWindowBackground(theme);
    }

    protected void onThemeStatusBar(ITheme theme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(theme.getColorPrimaryDark());
        }
    }

    protected void onThemeStatusBarIcons(ITheme theme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Under enforced edge to edge the system bars sit over the window background rather than
            // the status bar color, so the icon contrast must follow that background instead.
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                boolean lightBackground = Utils.isColorLight(theme.getColorWindowBackground());
                controller.setSystemBarsAppearance(lightBackground ? mask : 0, mask);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getWindow().getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            int statusBarColor = theme.getColorPrimaryDark();
            boolean isStatusBarLight = Utils.isColorLight(statusBarColor);
            if (isStatusBarLight) {
                decorView.setSystemUiVisibility(systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onThemeTaskDescription(ITheme theme) {
        String name = getString(R.string.app_name);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        setTaskDescription(new ActivityManager.TaskDescription(name, icon, theme.getColorPrimary()));
    }

    protected void onThemeWindowBackground(ITheme theme) {
        View view = getWindow().getDecorView();
        view.setBackgroundColor(theme.getColorWindowBackground());
    }
}
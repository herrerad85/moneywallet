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
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.graphics.Insets;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.ui.view.theme.ITheme;
import com.oriondev.moneywallet.ui.view.theme.ThemeEngine;
import com.oriondev.moneywallet.utils.Utils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

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

    private static final String THEMED_VIEW_PACKAGE = "com.oriondev.moneywallet.ui.view.theme.Themed";

    private static final Map<String, Constructor<?>> sThemedViewConstructors = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeEngine.registerObserver(this);
    }

    /**
     * Themed views are handed the current theme as they are inflated. AppCompatActivity is itself
     * the layout inflater factory and routes through here every view it does not create itself, so
     * this is the hook for it.
     * <p>
     * This used to be a separate factory installed over AppCompat's by clearing the private
     * LayoutInflater.mFactorySet field through reflection. That field has not been reachable for
     * several Android releases, and the failure was caught and printed rather than raised, so the
     * factory was silently never installed. Nothing else themes the hierarchy at startup, so
     * onApplyTheme was running on nothing at all until the user changed a theme setting, which is
     * the one thing that walks the tree.
     */
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        View themed = onCreateThemedView(name, context, attrs);
        if (themed != null) {
            return themed;
        }
        return super.onCreateView(parent, name, context, attrs);
    }

    private View onCreateThemedView(String name, Context context, AttributeSet attrs) {
        if (!name.startsWith(THEMED_VIEW_PACKAGE)) {
            return null;
        }
        View view;
        try {
            view = (View) getThemedViewConstructor(name).newInstance(context, attrs);
        } catch (Exception e) {
            // let the normal inflation path build it: it uses the same constructor and will report
            // a genuinely missing class or constructor far better than this can
            e.printStackTrace();
            return null;
        }
        // deliberately outside the catch above. A view that was built correctly is worth keeping
        // even if theming it fails, and rebuilding it would run its constructor a second time
        ThemeEngine.applyTheme(view, false);
        return view;
    }

    /**
     * Cached because this runs for every themed view of every inflation, and a list row can carry a
     * dozen of them. LayoutInflater and AppCompat both keep the same kind of map for the same
     * reason. Bounded by the number of distinct themed tags in the layouts, currently about forty,
     * and it holds only names and constructors, so nothing with a lifecycle is retained.
     */
    private static Constructor<?> getThemedViewConstructor(String name) throws Exception {
        Constructor<?> constructor = sThemedViewConstructors.get(name);
        if (constructor == null) {
            constructor = Class.forName(name).getConstructor(Context.class, AttributeSet.class);
            sThemedViewConstructors.put(name, constructor);
        }
        return constructor;
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
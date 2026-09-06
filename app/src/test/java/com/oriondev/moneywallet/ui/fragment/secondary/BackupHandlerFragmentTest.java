package com.oriondev.moneywallet.ui.fragment.secondary;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.api.BackendServiceFactory;
import com.oriondev.moneywallet.storage.database.TestDatabases;
import com.oriondev.moneywallet.ui.activity.BackupListActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

/**
 * The cover button is tapped while the activity is resumed, which is where a backend used to
 * register its launcher and where AndroidX refuses a registration.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupHandlerFragmentTest {

    @Before
    public void setUp() {
        TestDatabases.useFreshDatabase(ApplicationProvider.<Context>getApplicationContext());
    }

    @Test
    public void theExternalMemoryCoverButtonAsksForStoragePermission() {
        try (ActivityScenario<BackupListActivity> scenario = ActivityScenario.launch(BackupListActivity.class)) {
            scenario.onActivity(activity -> {
                clickTheCoverButton(activity, BackendServiceFactory.SERVICE_ID_EXTERNAL_MEMORY);
                ShadowActivity.PermissionsRequest request = shadowOf(activity).getLastRequestedPermission();
                assertNotNull(request);
                assertEquals(Manifest.permission.WRITE_EXTERNAL_STORAGE, request.requestedPermissions[0]);
            });
        }
    }

    @Test
    public void theLocalFolderCoverButtonOpensTheFolderPicker() {
        try (ActivityScenario<BackupListActivity> scenario = ActivityScenario.launch(BackupListActivity.class)) {
            scenario.onActivity(activity -> {
                clickTheCoverButton(activity, BackendServiceFactory.SERVICE_ID_SAF);
                Intent picker = shadowOf(activity).getNextStartedActivity();
                assertNotNull(picker);
                assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, picker.getAction());
            });
        }
    }

    private void clickTheCoverButton(FragmentActivity activity, String backendId) {
        Fragment fragment = BackupHandlerFragment.newInstance(backendId, true, true);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "BackupHandlerFragmentTest")
                .commitNow();
        View coverActionButton = fragment.requireView().findViewById(R.id.cover_action_button);
        assertNotNull(coverActionButton);
        coverActionButton.performClick();
    }
}

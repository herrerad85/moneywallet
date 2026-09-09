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
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

/**
 * Every test drives the backup screen inside a resumed activity, which is where a backend used
 * to register its launcher and where AndroidX refuses a registration.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupHandlerFragmentTest {

    @Before
    public void setUp() {
        TestDatabases.useFreshDatabase(ApplicationProvider.<Context>getApplicationContext());
    }

    @Test
    @Config(sdk = 32)
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
    @Config(sdk = {33, 36})
    public void theExternalMemoryScreenOpensFromAndroid13WithoutTheStoragePermission() {
        assertCoverVisibility(BackendServiceFactory.SERVICE_ID_EXTERNAL_MEMORY, View.GONE, View.VISIBLE);
    }

    @Test
    @Config(sdk = 32)
    public void theExternalMemoryScreenStaysCoveredWithoutTheStoragePermissionBelowAndroid13() {
        assertCoverVisibility(BackendServiceFactory.SERVICE_ID_EXTERNAL_MEMORY, View.VISIBLE, View.GONE);
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
        Fragment fragment = showBackupHandler(activity, backendId);
        View coverActionButton = fragment.requireView().findViewById(R.id.cover_action_button);
        assertNotNull(coverActionButton);
        coverActionButton.performClick();
    }

    private Fragment showBackupHandler(FragmentActivity activity, String backendId) {
        Fragment fragment = BackupHandlerFragment.newInstance(backendId, true, true);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, fragment, "BackupHandlerFragmentTest")
                .commitNow();
        return fragment;
    }

    private void assertCoverVisibility(String backendId, int expectedCover, int expectedPrimary) {
        try (ActivityScenario<BackupListActivity> scenario = ActivityScenario.launch(BackupListActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment fragment = showBackupHandler(activity, backendId);
                View cover = fragment.requireView().findViewById(R.id.cover_layout);
                View primary = fragment.requireView().findViewById(R.id.primary_layout);
                assertEquals(expectedCover, cover.getVisibility());
                assertEquals(expectedPrimary, primary.getVisibility());
            });
        }
    }
}

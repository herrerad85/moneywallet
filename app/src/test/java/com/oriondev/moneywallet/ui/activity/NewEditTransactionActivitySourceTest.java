package com.oriondev.moneywallet.ui.activity;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The block checked here runs inside an Activity, reads instance fields and talks to a content
 * resolver, so it cannot be driven from a JVM unit test: there is no Robolectric on the unit test
 * classpath, and the instrumented suite is configured with clearPackageData, which empties the
 * device it runs on. The invariant is pinned by reading the source instead.
 *
 * The invariant: inside the branch that fills a new transaction from its intent, getItemId is
 * always the NEW_ITEM placeholder of -1, because that branch is the else of a
 * getMode() == EDIT_ITEM test. Any row loaded there has to be keyed by an id the intent carried.
 * Loading the saving with getItemId matched no row, so the editor opened with no wallet, the amount
 * keypad had no currency to scale against, and a typed 2000 was stored as 20.00.
 */
public class NewEditTransactionActivitySourceTest {

    private static final String SOURCE_PATH =
            "src/main/java/com/oriondev/moneywallet/ui/activity/NewEditTransactionActivity.java";

    private static final String REGION_START = "mType = intent.getIntExtra(TYPE, TYPE_STANDARD);";
    private static final String REGION_END = "datetime = new Date();";

    @Test
    public void newItemBranchDoesNotReadTheItemId() throws IOException {
        assertEquals("getItemId() is read inside the branch that fills a new transaction from its "
                + "intent, where it is always -1", -1, readIntentBranch().indexOf("getItemId()"));
    }

    @Test
    public void savingIsLoadedByTheIdTheIntentCarried() throws IOException {
        assertTrue("the saving must be loaded with mSavingId, the id the launching intent put in "
                + "SAVING_ID", readIntentBranch().contains("CONTENT_SAVINGS, mSavingId"));
    }

    private static String readIntentBranch() throws IOException {
        File source = new File(SOURCE_PATH);
        if (!source.exists()) {
            source = new File("app/" + SOURCE_PATH);
        }
        String text = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        int start = text.indexOf(REGION_START);
        int end = text.indexOf(REGION_END, start + 1);
        if (start < 0 || end < 0) {
            fail("could not find the branch that fills a new transaction from its intent in "
                    + source.getAbsolutePath() + ", so this test can no longer check it");
        }
        return text.substring(start, end);
    }
}

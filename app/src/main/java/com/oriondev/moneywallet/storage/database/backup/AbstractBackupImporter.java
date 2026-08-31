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

package com.oriondev.moneywallet.storage.database.backup;

import android.content.ContentResolver;
import android.content.Context;
import androidx.annotation.NonNull;

import com.oriondev.moneywallet.storage.database.DataContentProvider;
import com.oriondev.moneywallet.storage.database.ImportException;
import com.oriondev.moneywallet.storage.database.SQLDatabaseImporter;
import com.oriondev.moneywallet.storage.database.SyncContentProvider;
import com.oriondev.moneywallet.storage.preference.PreferenceManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Created by andrea on 25/10/18.
 */
public abstract class AbstractBackupImporter {

    private static final String TEMP_BACKUP_FILE = "database.bk";

    private final Context mContext;
    private final File mBackupFile;

    /*package-local*/ AbstractBackupImporter(Context context, File backupFile) {
        mContext = context;
        mBackupFile = backupFile;
    }

    /**
     * This method imports all the data included in the backup file inside the main database.
     * The current database is backed-up before starting the import procedure and restored if
     * something goes wrong. The old data is then permanently removed if success.
     * @param temporaryFolder folder where temporary data can be stored.
     * @param databaseFolder folder where the database is located.
     * @throws ImportException if an error occur while importing the backup file.
     *
     * What this does NOT do, because a reader will ask and part of the answer took a device run.
     * Nothing serializes another thread's DataContentProvider writes against this import. The
     * recurrence job is the one that needs no user present, it runs off an alarm, and the lock in
     * BackupHandlerIntentService that serializes backup against restore is not taken anywhere in
     * that job.
     *
     * Such a write is not refused and not held, so it lands wherever the path points at the time.
     * Between the rename below and the import's first insert that is a database this class has
     * just emptied. After it, it is the half imported database itself, carrying ids that named
     * different rows before the restore, and foreign keys are on.
     *
     * Refusing them was built and reverted. The refusal reaches the caller as an exception, no
     * caller on any of these write paths catches it, and injecting that throw into the recurrence
     * job on an emulator ended the run with FATAL EXCEPTION on the AsyncTask that JobIntentService
     * dispatches on, with the process gone; the same trigger without it left the process running.
     * Every component here shares that one process, so the restore dies with it, half written.
     * Holding the writes instead blocks whichever thread they arrived on, for as long as the
     * backup is large.
     *
     * Closing this properly means never pointing the live path at a database an import is still
     * filling, which is an import that builds its database elsewhere and swaps it in at the end.
     */
    public void importDatabase(@NonNull File temporaryFolder, @NonNull File databaseFolder) throws ImportException {
        File temporary = createBackupCopyOfCurrentDatabase(databaseFolder);
        try {
            importDatabase(temporaryFolder);
            // The categories the user had hidden are remembered by id, and the database that
            // knew those ids has just been replaced. The rows that went into the new one were
            // given fresh ids in the order they were read, so the remembered ones now name other
            // categories. Forgetting them leaves every category showing, where the list starts.
            PreferenceManager.setCollapsedCategories(Collections.<String>emptySet());
        } catch (ImportException | RuntimeException e) {
            // RuntimeException as well. JSONDatabaseImporter converts IOException and
            // JSONException and catches nothing else, so anything unchecked the provider raises
            // comes straight through, a full disk being the one to expect. Every one of those
            // used to skip the rollback and leave the half written import live
            restoreBackupCopyOfDatabase(databaseFolder, temporary);
            throw e;
        }
        // deliberately not in a finally. The rollback above deletes the half imported database
        // before it renames the backup back, so a rollback that throws has already destroyed one
        // copy, and a finally here would then delete the only one left. Reached only when the
        // import succeeded, where the copy is the replaced database and is not wanted, or when
        // the rollback succeeded, where the rename has already consumed it
        FileUtils.deleteQuietly(temporary);
    }

    protected abstract void importDatabase(@NonNull File temporaryFolder) throws ImportException;

    public void importAttachments(@NonNull File attachmentFolder) throws ImportException {
        try {
            if (attachmentFolder.exists()) {
                FileUtils.cleanDirectory(attachmentFolder);
            } else {
                FileUtils.forceMkdir(attachmentFolder);
            }
            importAttachmentFiles(attachmentFolder);
        } catch (IOException e) {
            throw new ImportException(e.getMessage());
        }
    }

    protected abstract void importAttachmentFiles(File attachmentFolder) throws IOException, ImportException;

    /*package-local*/ File getBackupFile() {
        return mBackupFile;
    }

    protected ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }

    /*package-local*/ void notifyImportStarted() {
        // Before starting to write the database file, it is necessary to notify both the providers
        // to ensure that they point to the new file (and not the old reference)
        DataContentProvider.notifyDatabaseIsChanged(mContext);
        SyncContentProvider.notifyDatabaseIsChanged(mContext);
    }

    /**
     * Both renames happen with the shared helper closed and no DataContentProvider write in
     * flight. Moving the database while a connection is open on it strands that connection's
     * journal at the old path and refuses its next write, and moving it while a transaction is
     * open is worse, the transaction commits into the file this method is putting aside.
     */
    private File createBackupCopyOfCurrentDatabase(@NonNull File databaseFolder) throws ImportException {
        File temporary = new File(databaseFolder, TEMP_BACKUP_FILE);
        if (temporary.exists()) {
            FileUtils.deleteQuietly(temporary);
        }
        File database = new File(databaseFolder, SQLDatabaseImporter.DATABASE_NAME);
        if (!database.exists()) {
            return temporary;
        }
        boolean[] renamed = new boolean[1];
        DataContentProvider.replaceDatabaseFile(mContext, () -> renamed[0] = database.renameTo(temporary));
        if (!renamed[0]) {
            throw new ImportException("Cannot backup the old database file");
        }
        return temporary;
    }

    private void restoreBackupCopyOfDatabase(@NonNull File databaseFolder, @NonNull File backup) throws ImportException {
        File database = new File(databaseFolder, SQLDatabaseImporter.DATABASE_NAME);
        boolean[] restored = new boolean[1];
        DataContentProvider.replaceDatabaseFile(mContext, () -> {
            if (database.exists()) {
                FileUtils.deleteQuietly(database);
            }
            restored[0] = !backup.exists() || backup.renameTo(database);
        });
        if (!restored[0]) {
            throw new ImportException("Rollback failed, all data is lost");
        }
    }
}
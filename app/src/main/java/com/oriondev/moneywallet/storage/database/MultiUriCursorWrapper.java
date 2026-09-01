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

package com.oriondev.moneywallet.storage.database;

import android.content.ContentResolver;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * This class works as wrapper on top of {@link Cursor}.
 * This allow to register multiple notification uri instead of a single one.
 *
 * Nothing registers more than one on it now. What it is still here for is the replay in
 * registerContentObserver, which hands a newly registered observer the changes this cursor was
 * already told about. AbstractCursor drops those, and a loader registers after the query it is
 * loading has returned, so that window is a real one.
 *
 * Credits to: https://gist.github.com/chalup/4201307da02b9cfe4f40
 */
public class MultiUriCursorWrapper extends CursorWrapper {

    public MultiUriCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    protected boolean mClosed;
    protected ContentResolver mContentResolver;

    private final LinkedHashSet<Uri> mNotifyUris = new LinkedHashSet<>();

    private final Object mSelfObserverLock = new Object();
    private ContentObserver mSelfObserver;
    private boolean mSelfObserverRegistered;

    private final LinkedHashSet<Uri> mChangedByUris = new LinkedHashSet<>();

    private final ContentObservable mContentObservable = new ContentObservable();

    @Override
    public void deactivate() {
        onDeactivateOrClose();
        super.deactivate();
    }

    protected void onDeactivateOrClose() {
        if (mSelfObserver != null) {
            mContentResolver.unregisterContentObserver(mSelfObserver);
            mSelfObserverRegistered = false;
        }
    }

    @Override
    public boolean requery() {
        if (mSelfObserver != null && !mSelfObserverRegistered) {
            for (Uri notifyUri : mNotifyUris) {
                mContentResolver.registerContentObserver(notifyUri, true, mSelfObserver);
            }
            mSelfObserverRegistered = true;
        }

        boolean success = super.requery();
        if (success) {
            mChangedByUris.clear();
        }
        return success;
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() && mClosed;
    }

    @Override
    public void close() {
        super.close();
        mClosed = true;
        mContentObservable.unregisterAll();
        onDeactivateOrClose();
    }

    /**
     * Under the same lock onChange takes, because this walks the set that method adds to and it
     * adds from whichever binder thread the notification arrived on. Without the lock a loader
     * registering here can walk the set while it is being written, and a change that lands
     * between onChange's dispatch and its add reaches neither the dispatch nor this replay.
     */
    @Override
    public void registerContentObserver(ContentObserver observer) {
        synchronized (mSelfObserverLock) {
            mContentObservable.registerObserver(observer);
            for (Uri changedByUri : mChangedByUris) {
                observer.dispatchChange(false, changedByUri);
            }
        }
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        // cursor will unregister all observers when it close
        if (!mClosed) {
            mContentObservable.unregisterObserver(observer);
        }
    }

    private void onChange(boolean selfChange, Uri uri) {
        synchronized (mSelfObserverLock) {
            mContentObservable.dispatchChange(selfChange, uri);

            if (!selfChange) {
                mChangedByUris.add(uri);
            }
        }
    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri notifyUri) {
        synchronized (mSelfObserverLock) {
            mNotifyUris.add(notifyUri);
            mContentResolver = contentResolver;
            if (mSelfObserver == null) {
                mSelfObserver = new SelfContentObserver(this);
            }
            mContentResolver.registerContentObserver(notifyUri, true, mSelfObserver);
            mSelfObserverRegistered = true;
        }
    }

    @Override
    public Uri getNotificationUri() {
        synchronized (mSelfObserverLock) {
            return getFirstNotificationUri();
        }
    }

    private Uri getFirstNotificationUri() {
        if (mNotifyUris != null) {
            Iterator<Uri> iterator = mNotifyUris.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }

    @Override
    protected void finalize() {
        try {
            super.finalize();

            if (mSelfObserver != null && mSelfObserverRegistered) {
                mContentResolver.unregisterContentObserver(mSelfObserver);
            }

            if (!mClosed) close();
        } catch (Throwable e) {
            // ignored
        }
    }

    private static class SelfContentObserver extends ContentObserver {

        WeakReference<MultiUriCursorWrapper> mCursor;

        public SelfContentObserver(MultiUriCursorWrapper cursor) {
            super(null);
            mCursor = new WeakReference<>(cursor);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            MultiUriCursorWrapper cursor = mCursor.get();
            if (cursor != null) {
                cursor.onChange(false, uri);
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
    }
}

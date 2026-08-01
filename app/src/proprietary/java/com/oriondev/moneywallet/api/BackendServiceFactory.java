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

package com.oriondev.moneywallet.api;

import android.content.Context;
import android.os.Build;

import com.oriondev.moneywallet.R;
import com.oriondev.moneywallet.api.disk.DiskBackendService;
import com.oriondev.moneywallet.api.disk.DiskBackendServiceAPI;
import com.oriondev.moneywallet.api.dropbox.DropboxBackendService;
import com.oriondev.moneywallet.api.dropbox.DropboxBackendServiceAPI;
import com.oriondev.moneywallet.api.google.GoogleDriveBackendService;
import com.oriondev.moneywallet.api.google.GoogleDriveBackendServiceAPI;
import com.oriondev.moneywallet.api.saf.SAFBackendService;
import com.oriondev.moneywallet.api.saf.SAFBackendServiceAPI;
import com.oriondev.moneywallet.api.webdav.WebDAVBackendService;
import com.oriondev.moneywallet.api.webdav.WebDAVBackendServiceAPI;
import com.oriondev.moneywallet.model.BackupService;
import com.oriondev.moneywallet.model.DropBoxFile;
import com.oriondev.moneywallet.model.GoogleDriveFile;
import com.oriondev.moneywallet.model.IFile;
import com.oriondev.moneywallet.model.LocalFile;
import com.oriondev.moneywallet.model.SAFFile;
import com.oriondev.moneywallet.model.WebDAVFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 21/11/18.
 */
public class BackendServiceFactory {

    public static final String SERVICE_ID_DROPBOX = "dropbox";
    public static final String SERVICE_ID_GOOGLE_DRIVE = "google_drive";
    public static final String SERVICE_ID_EXTERNAL_MEMORY = "external_memory";
    public static final String SERVICE_ID_SAF = "storage_access_framework";
    public static final String SERVICE_ID_WEBDAV = "webdav";

    private static final List<BackendDescriptor> DESCRIPTORS = buildDescriptors();

    private static List<BackendDescriptor> buildDescriptors() {
        List<BackendDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new BackendDescriptor(SERVICE_ID_DROPBOX, R.drawable.ic_dropbox_24dp) {
            @Override
            public AbstractBackendServiceDelegate createDelegate(AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
                return new DropboxBackendService(listener);
            }

            @Override
            public IBackendServiceAPI createServiceApi(Context context) throws BackendException {
                return new DropboxBackendServiceAPI(context);
            }

            @Override
            public IFile createFile(String encoded) {
                return new DropBoxFile(encoded);
            }
        });
        descriptors.add(new BackendDescriptor(SERVICE_ID_GOOGLE_DRIVE, R.drawable.ic_google_drive_24dp) {
            @Override
            public AbstractBackendServiceDelegate createDelegate(AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
                return new GoogleDriveBackendService(listener);
            }

            @Override
            public IBackendServiceAPI createServiceApi(Context context) throws BackendException {
                return new GoogleDriveBackendServiceAPI(context);
            }

            @Override
            public IFile createFile(String encoded) {
                return new GoogleDriveFile(encoded);
            }
        });
        descriptors.add(new BackendDescriptor(SERVICE_ID_EXTERNAL_MEMORY, R.drawable.ic_sd_24dp) {
            @Override
            public AbstractBackendServiceDelegate createDelegate(AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
                return new DiskBackendService(listener);
            }

            @Override
            public IBackendServiceAPI createServiceApi(Context context) throws BackendException {
                return new DiskBackendServiceAPI();
            }

            @Override
            public IFile createFile(String encoded) {
                return new LocalFile(encoded);
            }
        });
        descriptors.add(new BackendDescriptor(SERVICE_ID_SAF, R.drawable.ic_storage_black_24dp) {
            @Override
            public boolean isAvailable() {
                return Build.VERSION.SDK_INT >= 21;
            }

            @Override
            public AbstractBackendServiceDelegate createDelegate(AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
                return new SAFBackendService(listener);
            }

            @Override
            public IBackendServiceAPI createServiceApi(Context context) throws BackendException {
                return new SAFBackendServiceAPI(context);
            }

            @Override
            public IFile createFile(String encoded) {
                return SAFFile.decode(encoded);
            }
        });
        descriptors.add(new BackendDescriptor(SERVICE_ID_WEBDAV, R.drawable.ic_webdav_24dp) {
            @Override
            public AbstractBackendServiceDelegate createDelegate(AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
                return new WebDAVBackendService(listener);
            }

            @Override
            public IBackendServiceAPI createServiceApi(Context context) throws BackendException {
                return new WebDAVBackendServiceAPI(context);
            }

            @Override
            public IFile createFile(String encoded) {
                return WebDAVFile.decode(encoded);
            }
        });
        return descriptors;
    }

    private static BackendDescriptor findDescriptor(String backendId) {
        for (BackendDescriptor descriptor : DESCRIPTORS) {
            if (descriptor.getId().equals(backendId)) {
                return descriptor;
            }
        }
        return null;
    }

    public static AbstractBackendServiceDelegate getServiceById(String backendId, AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
        BackendDescriptor descriptor = findDescriptor(backendId);
        return descriptor != null ? descriptor.createDelegate(listener) : null;
    }

    public static IBackendServiceAPI getServiceAPIById(Context context, String backendId) throws BackendException {
        BackendDescriptor descriptor = findDescriptor(backendId);
        if (descriptor == null) {
            throw new BackendException("Invalid backend");
        }
        return descriptor.createServiceApi(context);
    }

    public static List<BackupService> getBackupServices() {
        List<BackupService> services = new ArrayList<>();
        for (BackendDescriptor descriptor : DESCRIPTORS) {
            if (descriptor.isAvailable()) {
                services.add(new BackupService(descriptor.getId(), descriptor.getIconRes(), descriptor.getNameRes()));
            }
        }
        return services;
    }

    public static IFile getFile(String backendId, String encoded) {
        if (encoded != null) {
            BackendDescriptor descriptor = findDescriptor(backendId);
            if (descriptor != null) {
                return descriptor.createFile(encoded);
            }
        }
        return null;
    }
}

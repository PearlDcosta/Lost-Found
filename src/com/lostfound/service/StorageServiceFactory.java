package com.lostfound.service;

import com.lostfound.util.ConfigLoader;

/**
 * Single point of control for which StorageService implementation the
 * app uses. Reads storage.provider from config.properties: "LOCAL"
 * (default, no cloud credentials needed) or "CLOUDINARY" (real cloud
 * object storage). Every caller (ReportItemFrame) asks this factory
 * for a StorageService and never instantiates LocalStorageService or
 * CloudStorageService directly — that's what makes swapping providers
 * a one-line config change instead of a code change.
 */
public final class StorageServiceFactory {

    private static StorageService instance;

    private StorageServiceFactory() {
        // static utility class, no instances
    }

    public static synchronized StorageService getInstance() {
        if (instance == null) {
            String provider = ConfigLoader.get("storage.provider", "LOCAL").toUpperCase();
            instance = "CLOUDINARY".equals(provider) ? new CloudStorageService() : new LocalStorageService();
            System.out.println("[StorageServiceFactory] using " + instance.getClass().getSimpleName());
        }
        return instance;
    }
}

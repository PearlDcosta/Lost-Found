package com.lostfound.service;

import com.lostfound.util.ConfigLoader;










public final class StorageServiceFactory {

    private static StorageService instance;

    private StorageServiceFactory() {
        
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

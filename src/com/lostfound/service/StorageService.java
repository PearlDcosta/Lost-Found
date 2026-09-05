package com.lostfound.service;

import com.lostfound.exception.StorageException;

import java.io.File;

/**
 * Abstraction over "where item images live". The rest of the
 * application (ReportItemFrame, ItemServiceImpl) only ever depends on
 * this interface, never on a concrete implementation — swapping
 * LocalStorageService for CloudStorageService (or a different cloud
 * provider entirely) requires touching exactly one line
 * (StorageServiceFactory), nowhere else in the codebase.
 *
 * Implementations:
 *   LocalStorageService  — saves to a local folder; used for offline
 *                          development/testing without needing real
 *                          cloud credentials.
 *   CloudStorageService  — uploads to a real cloud object storage
 *                          provider over HTTP (Storage as a Service).
 */
public interface StorageService {

    /**
     * Uploads {@code file} and returns a URL/reference where it can
     * later be retrieved. That reference — never raw image bytes — is
     * what gets stored in ITEMS.image_url.
     */
    String uploadImage(File file, String desiredFileName) throws StorageException;

    /** Quick reachability check, e.g. for a startup diagnostic. */
    boolean isAvailable();
}

package com.lostfound.service;

import com.lostfound.exception.StorageException;
import com.lostfound.util.ConfigLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores images on the local filesystem (the project's "images/"
 * folder by default). Used for offline development/testing without
 * needing real cloud credentials, per the project brief's explicit
 * request for a LocalStorageService fallback. The returned reference
 * is a "file:" URI, which ImageIcon/Toolkit can load exactly the same
 * way it loads an "http(s):" URI from CloudStorageService — the rest
 * of the app doesn't need to know or care which one produced it.
 */
public class LocalStorageService implements StorageService {

    private final Path storageDir;

    public LocalStorageService() {
        this(Paths.get(ConfigLoader.get("storage.local.dir", "images")));
    }

    public LocalStorageService(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create local storage directory: " + storageDir, e);
        }
    }

    @Override
    public String uploadImage(File file, String desiredFileName) throws StorageException {
        if (file == null || !file.exists()) {
            throw new StorageException("Source image file does not exist.");
        }

        String extension = getExtension(desiredFileName != null ? desiredFileName : file.getName());
        String uniqueName = UUID.randomUUID() + extension;
        Path target = storageDir.resolve(uniqueName);

        try {
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store image locally: " + e.getMessage(), e);
        }

        return target.toUri().toString(); // e.g. file:/home/.../images/3f2a-....jpg
    }

    @Override
    public boolean isAvailable() {
        return Files.isDirectory(storageDir) && Files.isWritable(storageDir);
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }
}

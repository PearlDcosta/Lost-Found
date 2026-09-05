package com.lostfound.exception;

/**
 * Thrown when an image upload/retrieval operation fails: the file is
 * missing/invalid, the local storage directory can't be written to,
 * or the cloud storage provider's HTTP request fails.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

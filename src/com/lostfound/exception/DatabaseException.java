package com.lostfound.exception;

/**
 * Thrown whenever a database operation fails (connection failure,
 * SQL error, constraint violation, etc). All DAO methods declare
 * this instead of leaking raw SQLException up to the service/GUI
 * layers, so callers deal with one consistent, application-specific
 * exception type.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

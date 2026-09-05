package com.lostfound.exception;

/**
 * Thrown when user-supplied input fails validation (empty required
 * field, bad email format, password too short, invalid status
 * transition requested, etc). Carries a message safe to show
 * directly to the user in a JOptionPane.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

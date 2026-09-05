package com.lostfound.exception;

/**
 * Thrown for any login/registration failure: invalid credentials,
 * duplicate email on registration, etc. Kept deliberately generic
 * for login failures ("Invalid email or password") so the system
 * never reveals whether a given email is registered.
 */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

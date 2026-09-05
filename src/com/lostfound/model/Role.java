package com.lostfound.model;

/**
 * Distinguishes the two account types in the system.
 * Stored in USERS.role as a MySQL ENUM('STUDENT','ADMIN').
 */
public enum Role {
    STUDENT,
    ADMIN;

    /**
     * Safely converts a database/string value into a Role.
     * Throws IllegalArgumentException for anything unexpected,
     * which calling code should catch and wrap as needed.
     */
    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value cannot be null");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}

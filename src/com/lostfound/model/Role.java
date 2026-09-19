package com.lostfound.model;

public enum Role {
    STUDENT,
    ADMIN;

    public static Role fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role value cannot be null");
        }
        return Role.valueOf(value.trim().toUpperCase());
    }
}

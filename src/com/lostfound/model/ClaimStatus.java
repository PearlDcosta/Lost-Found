package com.lostfound.model;

public enum ClaimStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static ClaimStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ClaimStatus value cannot be null");
        }
        return ClaimStatus.valueOf(value.trim().toUpperCase());
    }
}

package com.lostfound.model;

/**
 * Status of an individual claim submitted by a student against
 * a found item. Independent of ItemStatus, but drives it:
 * approving a claim pushes the related item from CLAIM_REQUESTED
 * to VERIFIED (see ClaimServiceImpl in Phase 11).
 */
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

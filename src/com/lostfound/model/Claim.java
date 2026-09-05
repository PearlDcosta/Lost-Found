package com.lostfound.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a row in the CLAIMS table — a student's claim on a
 * (usually FOUND) item. Passed across RMI by ClaimServiceRemote
 * (Phase 6), so it must be Serializable.
 */
public class Claim implements Serializable {

    private static final long serialVersionUID = 1L;

    private int claimId;
    private int itemId;
    private int userId;              // claimant's user_id
    private String claimDescription;
    private LocalDateTime claimDate;
    private ClaimStatus status;

    /** Full constructor — used when reading a complete row from the DB. */
    public Claim(int claimId, int itemId, int userId, String claimDescription,
                 LocalDateTime claimDate, ClaimStatus status) {
        this.claimId = claimId;
        this.itemId = itemId;
        this.userId = userId;
        this.claimDescription = claimDescription;
        this.claimDate = claimDate;
        this.status = status;
    }

    /** Constructor used when a student submits a new claim. */
    public Claim(int itemId, int userId, String claimDescription) {
        this.itemId = itemId;
        this.userId = userId;
        this.claimDescription = claimDescription;
        this.status = ClaimStatus.PENDING;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        this.claimId = claimId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public void setClaimDescription(String claimDescription) {
        this.claimDescription = claimDescription;
    }

    public LocalDateTime getClaimDate() {
        return claimDate;
    }

    public void setClaimDate(LocalDateTime claimDate) {
        this.claimDate = claimDate;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim)) return false;
        Claim claim = (Claim) o;
        return claimId == claim.claimId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimId);
    }

    @Override
    public String toString() {
        return "Claim{" +
                "claimId=" + claimId +
                ", itemId=" + itemId +
                ", userId=" + userId +
                ", status=" + status +
                ", claimDate=" + claimDate +
                '}';
    }
}

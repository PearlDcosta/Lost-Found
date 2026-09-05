package com.lostfound.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a row in the STATUS_HISTORY table — an audit trail entry
 * for one item status transition. oldStatus is null for the very
 * first entry (when an item is first reported, there is no prior
 * status). changedBy is the user who triggered the change: the
 * reporter for the initial entry, or the admin for every subsequent
 * transition driven through VerifyClaimFrame/UpdateStatusFrame.
 */
public class StatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private int historyId;
    private int itemId;
    private ItemStatus oldStatus; // null for the initial entry
    private ItemStatus newStatus;
    private int changedBy;
    private LocalDateTime changedAt;

    public StatusHistory(int historyId, int itemId, ItemStatus oldStatus, ItemStatus newStatus,
                          int changedBy, LocalDateTime changedAt) {
        this.historyId = historyId;
        this.itemId = itemId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public StatusHistory(int itemId, ItemStatus oldStatus, ItemStatus newStatus, int changedBy) {
        this.itemId = itemId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public ItemStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(ItemStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public ItemStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ItemStatus newStatus) {
        this.newStatus = newStatus;
    }

    public int getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(int changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatusHistory)) return false;
        StatusHistory that = (StatusHistory) o;
        return historyId == that.historyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(historyId);
    }

    @Override
    public String toString() {
        return "StatusHistory{" +
                "itemId=" + itemId +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", changedBy=" + changedBy +
                ", changedAt=" + changedAt +
                '}';
    }
}

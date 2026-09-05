package com.lostfound.model;

/**
 * Declares whether a report was filed as a lost item or a found item.
 * This is separate from ItemStatus: type never changes after creation,
 * while status evolves through the lifecycle.
 */
public enum ItemType {
    LOST,
    FOUND;

    public static ItemType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ItemType value cannot be null");
        }
        return ItemType.valueOf(value.trim().toUpperCase());
    }
}

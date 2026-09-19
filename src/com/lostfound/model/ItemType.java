package com.lostfound.model;

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

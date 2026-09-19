package com.lostfound.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public enum ItemStatus {
    LOST,
    FOUND,
    CLAIM_REQUESTED,
    VERIFIED,
    RETURNED;

    private static final Map<ItemStatus, Set<ItemStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ItemStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(LOST, EnumSet.of(FOUND));
        ALLOWED_TRANSITIONS.put(FOUND, EnumSet.of(CLAIM_REQUESTED));
        ALLOWED_TRANSITIONS.put(CLAIM_REQUESTED, EnumSet.of(VERIFIED, FOUND)); 
        ALLOWED_TRANSITIONS.put(VERIFIED, EnumSet.of(RETURNED));
        ALLOWED_TRANSITIONS.put(RETURNED, EnumSet.noneOf(ItemStatus.class)); 
    }

    



    public boolean canTransitionTo(ItemStatus target) {
        if (target == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
    public static boolean isValidTransition(ItemStatus from, ItemStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return from.canTransitionTo(to);
    }

    public static ItemStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ItemStatus value cannot be null");
        }
        return ItemStatus.valueOf(value.trim().toUpperCase());
    }
}

package com.lostfound.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the lifecycle stage of an item report:
 *
 *   LOST -> FOUND -> CLAIM_REQUESTED -> VERIFIED -> RETURNED
 *
 * The allowed-transitions map below is the single source of truth
 * for which status changes are legal. Every service method that
 * changes an item's status (ItemServiceImpl, ClaimServiceImpl) must
 * call ItemStatus.isValidTransition(...) before committing a change,
 * so an item can never skip a stage or move backwards by mistake.
 */
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
        ALLOWED_TRANSITIONS.put(CLAIM_REQUESTED, EnumSet.of(VERIFIED, FOUND)); // FOUND = claim rejected, revert
        ALLOWED_TRANSITIONS.put(VERIFIED, EnumSet.of(RETURNED));
        ALLOWED_TRANSITIONS.put(RETURNED, EnumSet.noneOf(ItemStatus.class)); // terminal state
    }

    /**
     * @return true if moving from this status to {@code target} is a
     *         legal lifecycle transition.
     */
    public boolean canTransitionTo(ItemStatus target) {
        if (target == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * Static convenience form, symmetrical with isValidTransition(from, to)
     * used throughout the service layer.
     */
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

package com.lostfound.remote;

import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * RMI remote interface for item reporting, search, and lifecycle
 * management. Item objects returned here are Serializable model
 * objects (Phase 3) that travel across the network as remote-object
 * communication (lab Module 3) — the client works with a local copy
 * of the data, not a live remote reference to a database row.
 */
public interface ItemServiceRemote extends Remote {

    Item reportItem(int userId, String itemName, String category, ItemType type,
                     String description, String location, LocalDate itemDate)
            throws RemoteException, ValidationException, DatabaseException;

    Item getItem(int itemId) throws RemoteException, DatabaseException;

    List<Item> getAllItems() throws RemoteException, DatabaseException;

    List<Item> getItemsByUser(int userId) throws RemoteException, DatabaseException;

    List<Item> searchItems(String itemName, String category, String location,
                            ItemType type, ItemStatus status, LocalDate itemDate)
            throws RemoteException, DatabaseException;

    void updateItemImage(int itemId, String imageUrl) throws RemoteException, DatabaseException;

    /**
     * Attempts to move an item to {@code newStatus}. Throws
     * ValidationException if the transition is not legal per
     * ItemStatus.isValidTransition(...). {@code changedByUserId} must
     * belong to an ADMIN account (verified server-side) and is
     * recorded in STATUS_HISTORY as the actor responsible for the change.
     */
    void updateItemStatus(int itemId, ItemStatus newStatus, int changedByUserId)
            throws RemoteException, DatabaseException, ValidationException, AuthenticationException;

    /** Admin-only: requires adminUserId to belong to an ADMIN account (verified server-side). */
    void deleteItem(int itemId, int adminUserId) throws RemoteException, DatabaseException, AuthenticationException;

    int countByStatus(ItemStatus status) throws RemoteException, DatabaseException;

    int countAllItems() throws RemoteException, DatabaseException;
}

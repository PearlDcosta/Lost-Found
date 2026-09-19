package com.lostfound.remote;

import com.lostfound.dao.ItemDAO;
import com.lostfound.dao.StatusHistoryDAO;
import com.lostfound.dao.UserDAO;
import com.lostfound.exception.AuthenticationException;
import com.lostfound.exception.DatabaseException;
import com.lostfound.exception.RPCException;
import com.lostfound.exception.ValidationException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.model.StatusHistory;
import com.lostfound.rpc.ServerTimeRPCClient;
import com.lostfound.util.AuthorizationUtil;
import com.lostfound.util.ValidationUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.List;

public class ItemServiceImpl extends UnicastRemoteObject implements ItemServiceRemote {

    private final ItemDAO itemDAO = new ItemDAO();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();
    private final UserDAO userDAO = new UserDAO();

    public ItemServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public Item reportItem(int userId, String itemName, String category, ItemType type,
                            String description, String location, LocalDate itemDate)
            throws RemoteException, ValidationException, DatabaseException {

        ValidationUtil.requireNonBlank(itemName, "Item name");
        ValidationUtil.requireNonBlank(category, "Category");
        ValidationUtil.requireNonBlank(location, "Location");
        if (type == null) {
            throw new ValidationException("Item type (LOST or FOUND) is required.");
        }
        if (itemDate == null) {
            throw new ValidationException("Item date is required.");
        }

        
        
        
        
        
        
        
        
        LocalDate serverToday;
        try {
            serverToday = ServerTimeRPCClient.getServerDateTime().toLocalDate();
        } catch (RPCException e) {
            System.err.println("[ItemServiceImpl] RPC time service unavailable, " +
                    "falling back to local clock: " + e.getMessage());
            serverToday = LocalDate.now();
        }

        if (itemDate.isAfter(serverToday)) {
            throw new ValidationException("Item date cannot be in the future (server date: " + serverToday + ").");
        }

        Item item = new Item(userId, itemName.trim(), category.trim(), type,
                description == null ? "" : description.trim(), location.trim(), itemDate);
        Item created = itemDAO.insert(item);

        
        
        statusHistoryDAO.insert(new StatusHistory(created.getItemId(), null, created.getStatus(), userId));

        return created;
    }

    @Override
    public Item getItem(int itemId) throws RemoteException, DatabaseException {
        return itemDAO.findById(itemId)
                .orElseThrow(() -> new DatabaseException("No item found with id " + itemId));
    }

    @Override
    public List<Item> getAllItems() throws RemoteException, DatabaseException {
        return itemDAO.findAll();
    }

    @Override
    public List<Item> getItemsByUser(int userId) throws RemoteException, DatabaseException {
        return itemDAO.findByUserId(userId);
    }

    @Override
    public List<Item> searchItems(String itemName, String category, String location,
                                   ItemType type, ItemStatus status, LocalDate itemDate)
            throws RemoteException, DatabaseException {
        return itemDAO.search(itemName, category, location, type, status, itemDate);
    }

    @Override
    public void updateItemImage(int itemId, String imageUrl) throws RemoteException, DatabaseException {
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new DatabaseException("No item found with id " + itemId));
        item.setImageUrl(imageUrl);
        itemDAO.update(item);
    }

    @Override
    public void updateItemStatus(int itemId, ItemStatus newStatus, int changedByUserId)
            throws RemoteException, DatabaseException, ValidationException, AuthenticationException {

        AuthorizationUtil.requireAdmin(userDAO, changedByUserId);

        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new DatabaseException("No item found with id " + itemId));

        if (!ItemStatus.isValidTransition(item.getStatus(), newStatus)) {
            throw new ValidationException("Cannot change status from " + item.getStatus() +
                    " to " + newStatus + " — this transition is not allowed.");
        }
        ItemStatus oldStatus = item.getStatus();
        itemDAO.updateStatus(itemId, newStatus);
        statusHistoryDAO.insert(new StatusHistory(itemId, oldStatus, newStatus, changedByUserId));
    }

    @Override
    public void deleteItem(int itemId, int adminUserId) throws RemoteException, DatabaseException, AuthenticationException {
        AuthorizationUtil.requireAdmin(userDAO, adminUserId);
        itemDAO.delete(itemId);
    }

    @Override
    public int countByStatus(ItemStatus status) throws RemoteException, DatabaseException {
        return itemDAO.countByStatus(status);
    }

    @Override
    public int countAllItems() throws RemoteException, DatabaseException {
        return itemDAO.countAll();
    }
}

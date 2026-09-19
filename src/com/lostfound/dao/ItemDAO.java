package com.lostfound.dao;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Item;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.ItemType;
import com.lostfound.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {

    public Item insert(Item item) throws DatabaseException {
        String sql = "INSERT INTO ITEMS (user_id, item_name, category, type, description, " +
                "location, item_date, image_url, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, item.getUserId());
            stmt.setString(2, item.getItemName());
            stmt.setString(3, item.getCategory());
            stmt.setString(4, item.getType().name());
            stmt.setString(5, item.getDescription());
            stmt.setString(6, item.getLocation());
            stmt.setDate(7, Date.valueOf(item.getItemDate()));
            stmt.setString(8, item.getImageUrl());
            stmt.setString(9, item.getStatus().name());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Creating item failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    return findById(newId).orElseThrow(() ->
                            new DatabaseException("Item was inserted but could not be re-read (id=" + newId + ")"));
                } else {
                    throw new DatabaseException("Creating item failed, no generated ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert item: " + e.getMessage(), e);
        }
    }

    public Optional<Item> findById(int itemId) throws DatabaseException {
        String sql = "SELECT * FROM ITEMS WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch item by id " + itemId + ": " + e.getMessage(), e);
        }
    }

    public List<Item> findAll() throws DatabaseException {
        String sql = "SELECT * FROM ITEMS ORDER BY created_at DESC";
        List<Item> items = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }
            return items;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all items: " + e.getMessage(), e);
        }
    }

    public List<Item> findByUserId(int userId) throws DatabaseException {
        String sql = "SELECT * FROM ITEMS WHERE user_id = ? ORDER BY created_at DESC";
        List<Item> items = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
            return items;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch items for user " + userId + ": " + e.getMessage(), e);
        }
    }
    public List<Item> search(String itemName, String category, String location,
                              ItemType type, ItemStatus status, LocalDate itemDate) throws DatabaseException {

        StringBuilder sql = new StringBuilder("SELECT * FROM ITEMS WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (itemName != null && !itemName.isBlank()) {
            sql.append(" AND item_name LIKE ?");
            params.add("%" + itemName.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category LIKE ?");
            params.add("%" + category.trim() + "%");
        }
        if (location != null && !location.isBlank()) {
            sql.append(" AND location LIKE ?");
            params.add("%" + location.trim() + "%");
        }
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (itemDate != null) {
            sql.append(" AND item_date = ?");
            params.add(Date.valueOf(itemDate));
        }
        sql.append(" ORDER BY created_at DESC");

        List<Item> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to search items: " + e.getMessage(), e);
        }
    }

    public void update(Item item) throws DatabaseException {
        String sql = "UPDATE ITEMS SET item_name = ?, category = ?, description = ?, " +
                "location = ?, item_date = ?, image_url = ?, status = ? WHERE item_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getItemName());
            stmt.setString(2, item.getCategory());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getLocation());
            stmt.setDate(5, Date.valueOf(item.getItemDate()));
            stmt.setString(6, item.getImageUrl());
            stmt.setString(7, item.getStatus().name());
            stmt.setInt(8, item.getItemId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Update failed: no item found with id " + item.getItemId());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update item: " + e.getMessage(), e);
        }
    }
    public void updateStatus(int itemId, ItemStatus newStatus) throws DatabaseException {
        String sql = "UPDATE ITEMS SET status = ? WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setInt(2, itemId);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Status update failed: no item found with id " + itemId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update item status: " + e.getMessage(), e);
        }
    }

    public void delete(int itemId) throws DatabaseException {
        String sql = "DELETE FROM ITEMS WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Delete failed: no item found with id " + itemId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete item: " + e.getMessage(), e);
        }
    }
    public int countByStatus(ItemStatus status) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM ITEMS WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count items by status: " + e.getMessage(), e);
        }
    }

    public int countAll() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM ITEMS";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count items: " + e.getMessage(), e);
        }
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        Date itemDate = rs.getDate("item_date");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        return new Item(
                rs.getInt("item_id"),
                rs.getInt("user_id"),
                rs.getString("item_name"),
                rs.getString("category"),
                ItemType.fromString(rs.getString("type")),
                rs.getString("description"),
                rs.getString("location"),
                itemDate != null ? itemDate.toLocalDate() : null,
                rs.getString("image_url"),
                ItemStatus.fromString(rs.getString("status")),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }
}

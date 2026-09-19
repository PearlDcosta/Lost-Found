package com.lostfound.dao;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.ItemStatus;
import com.lostfound.model.StatusHistory;
import com.lostfound.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class StatusHistoryDAO {

    public StatusHistory insert(StatusHistory entry) throws DatabaseException {
        String sql = "INSERT INTO STATUS_HISTORY (item_id, old_status, new_status, changed_by) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, entry.getItemId());
            if (entry.getOldStatus() != null) {
                stmt.setString(2, entry.getOldStatus().name());
            } else {
                stmt.setNull(2, java.sql.Types.VARCHAR);
            }
            stmt.setString(3, entry.getNewStatus().name());
            stmt.setInt(4, entry.getChangedBy());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Creating status history entry failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    entry.setHistoryId(keys.getInt(1));
                }
            }
            return entry;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert status history entry: " + e.getMessage(), e);
        }
    }

    public List<StatusHistory> findByItemId(int itemId) throws DatabaseException {
        String sql = "SELECT * FROM STATUS_HISTORY WHERE item_id = ? ORDER BY changed_at ASC";
        List<StatusHistory> history = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapRow(rs));
                }
            }
            return history;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch status history for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    private StatusHistory mapRow(ResultSet rs) throws SQLException {
        String oldStatusStr = rs.getString("old_status");
        Timestamp changedAtTs = rs.getTimestamp("changed_at");
        return new StatusHistory(
                rs.getInt("history_id"),
                rs.getInt("item_id"),
                oldStatusStr != null ? ItemStatus.fromString(oldStatusStr) : null,
                ItemStatus.fromString(rs.getString("new_status")),
                rs.getInt("changed_by"),
                changedAtTs != null ? changedAtTs.toLocalDateTime() : null
        );
    }
}

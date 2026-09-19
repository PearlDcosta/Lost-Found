package com.lostfound.dao;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Claim;
import com.lostfound.model.ClaimStatus;
import com.lostfound.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;




public class ClaimDAO {

    public Claim insert(Claim claim) throws DatabaseException {
        String sql = "INSERT INTO CLAIMS (item_id, user_id, claim_description, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, claim.getItemId());
            stmt.setInt(2, claim.getUserId());
            stmt.setString(3, claim.getClaimDescription());
            stmt.setString(4, claim.getStatus().name());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Creating claim failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    return findById(newId).orElseThrow(() ->
                            new DatabaseException("Claim was inserted but could not be re-read (id=" + newId + ")"));
                } else {
                    throw new DatabaseException("Creating claim failed, no generated ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert claim: " + e.getMessage(), e);
        }
    }

    public Optional<Claim> findById(int claimId) throws DatabaseException {
        String sql = "SELECT * FROM CLAIMS WHERE claim_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, claimId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch claim by id " + claimId + ": " + e.getMessage(), e);
        }
    }

    public List<Claim> findAll() throws DatabaseException {
        String sql = "SELECT * FROM CLAIMS ORDER BY claim_date DESC";
        List<Claim> claims = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                claims.add(mapRow(rs));
            }
            return claims;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all claims: " + e.getMessage(), e);
        }
    }

    public List<Claim> findByItemId(int itemId) throws DatabaseException {
        String sql = "SELECT * FROM CLAIMS WHERE item_id = ? ORDER BY claim_date DESC";
        List<Claim> claims = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
            return claims;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch claims for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    public List<Claim> findByUserId(int userId) throws DatabaseException {
        String sql = "SELECT * FROM CLAIMS WHERE user_id = ? ORDER BY claim_date DESC";
        List<Claim> claims = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
            return claims;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch claims for user " + userId + ": " + e.getMessage(), e);
        }
    }

    public List<Claim> findByStatus(ClaimStatus status) throws DatabaseException {
        String sql = "SELECT * FROM CLAIMS WHERE status = ? ORDER BY claim_date DESC";
        List<Claim> claims = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
            return claims;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch claims by status: " + e.getMessage(), e);
        }
    }

    public void updateStatus(int claimId, ClaimStatus status) throws DatabaseException {
        String sql = "UPDATE CLAIMS SET status = ? WHERE claim_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, claimId);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Status update failed: no claim found with id " + claimId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update claim status: " + e.getMessage(), e);
        }
    }

    public void delete(int claimId) throws DatabaseException {
        String sql = "DELETE FROM CLAIMS WHERE claim_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, claimId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Delete failed: no claim found with id " + claimId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete claim: " + e.getMessage(), e);
        }
    }

    
    public int countByStatus(ClaimStatus status) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM CLAIMS WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to count claims by status: " + e.getMessage(), e);
        }
    }

    private Claim mapRow(ResultSet rs) throws SQLException {
        Timestamp claimDateTs = rs.getTimestamp("claim_date");
        return new Claim(
                rs.getInt("claim_id"),
                rs.getInt("item_id"),
                rs.getInt("user_id"),
                rs.getString("claim_description"),
                claimDateTs != null ? claimDateTs.toLocalDateTime() : null,
                ClaimStatus.fromString(rs.getString("status"))
        );
    }
}

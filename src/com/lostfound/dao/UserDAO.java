package com.lostfound.dao;

import com.lostfound.exception.DatabaseException;
import com.lostfound.model.Role;
import com.lostfound.model.User;
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

/**
 * Data-access object for the USERS table. Every method uses
 * PreparedStatement exclusively (never string-concatenated SQL),
 * which prevents SQL injection by construction.
 *
 * This class is called by AuthServiceImpl (Phase 6/7) — it has no
 * knowledge of RMI, Swing, or business rules; it only knows how to
 * turn User objects into rows and back.
 */
public class UserDAO {

    /**
     * Inserts a new user and returns a copy of it with the
     * database-generated user_id and created_at populated.
     */
    public User insert(User user) throws DatabaseException {
        String sql = "INSERT INTO USERS (name, email, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole().name());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    return findById(newId).orElseThrow(() ->
                            new DatabaseException("User was inserted but could not be re-read (id=" + newId + ")"));
                } else {
                    throw new DatabaseException("Creating user failed, no generated ID obtained.");
                }
            }

        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                // integrity constraint violation, e.g. duplicate email (UNIQUE)
                throw new DatabaseException("A user with email '" + user.getEmail() + "' already exists.", e);
            }
            throw new DatabaseException("Failed to insert user: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(int userId) throws DatabaseException {
        String sql = "SELECT * FROM USERS WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch user by id " + userId + ": " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) throws DatabaseException {
        String sql = "SELECT * FROM USERS WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch user by email: " + e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) throws DatabaseException {
        String sql = "SELECT 1 FROM USERS WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to check if email exists: " + e.getMessage(), e);
        }
    }

    public List<User> findAll() throws DatabaseException {
        String sql = "SELECT * FROM USERS ORDER BY user_id";
        List<User> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;

        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all users: " + e.getMessage(), e);
        }
    }

    public void update(User user) throws DatabaseException {
        String sql = "UPDATE USERS SET name = ?, email = ?, role = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getRole().name());
            stmt.setInt(4, user.getUserId());

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Update failed: no user found with id " + user.getUserId());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + e.getMessage(), e);
        }
    }

    public void updatePassword(int userId, String newHashedPassword) throws DatabaseException {
        String sql = "UPDATE USERS SET password = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newHashedPassword);
            stmt.setInt(2, userId);

            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Password update failed: no user found with id " + userId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update password: " + e.getMessage(), e);
        }
    }

    public void delete(int userId) throws DatabaseException {
        String sql = "DELETE FROM USERS WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DatabaseException("Delete failed: no user found with id " + userId);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /** Maps the current row of a ResultSet to a User object. */
    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                Role.fromString(rs.getString("role")),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }
}

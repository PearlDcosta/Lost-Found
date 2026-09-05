package com.lostfound.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a row in the USERS table.
 *
 * Instances of this class travel over RMI (e.g. AuthServiceRemote.login
 * returns a User), so it implements Serializable — required for any
 * object passed as an argument or return value across a remote method
 * call. The password field is intentionally never sent back to the
 * client after authentication (see PasswordUtil / AuthServiceImpl).
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String name;
    private String email;
    private String password; // salted hash, format: salt$hash
    private Role role;
    private LocalDateTime createdAt;

    /** Full constructor - used when reading a complete row from the DB. */
    public User(int userId, String name, String email, String password,
                Role role, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }

    /** Constructor used before insertion, when userId/createdAt are not yet known. */
    public User(String name, String email, String password, Role role) {
        this(0, name, email, password, role, null);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /**
     * Returns a copy of this user with the password field cleared,
     * safe to send to the client after a successful login so the
     * hash never leaves the server unnecessarily.
     */
    public User withoutPassword() {
        return new User(userId, name, email, null, role, createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}

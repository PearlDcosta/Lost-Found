package com.lostfound.util;

import com.lostfound.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection factory. Every DAO (Phase 5) calls
 * DBConnection.getConnection() to obtain a fresh connection to the
 * cloud-hosted MySQL instance — nowhere else in the codebase calls
 * DriverManager directly.
 *
 * Credentials come exclusively from ConfigLoader (resources/config.properties),
 * never from a literal String in this file.
 */
public final class DBConnection {

    private static final String DB_URL;
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    static {
        DB_URL = ConfigLoader.get("db.url");
        DB_USERNAME = ConfigLoader.get("db.username");
        DB_PASSWORD = ConfigLoader.get("db.password");

        try {
            // Explicitly loading the driver class is not strictly required
            // on modern JDBC 4+ drivers (they self-register via
            // META-INF/services), but doing it explicitly gives a clear,
            // early error message if the connector jar is missing from
            // the classpath, instead of a confusing failure later.
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            // ExceptionInInitializerError's own cause-tracking is unreliable
            // across JDK versions when built from the String constructor, so
            // the driver class name and root cause message are folded
            // directly into the error text instead of relying on getCause().
            throw new ExceptionInInitializerError(
                    "MySQL JDBC driver (" + DRIVER_CLASS + ") not found on the classpath. " +
                    "Download 'mysql-connector-j-*.jar' and place it in the 'lib' folder, " +
                    "then include it on the classpath when running the app. " +
                    "Root cause: " + e);
        }
    }

    private DBConnection() {
        // static utility class, no instances
    }

    /**
     * Opens a new connection to the cloud MySQL database.
     * Callers are responsible for closing it (try-with-resources is
     * recommended in every DAO method).
     */
    public static Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to connect to the database. Check that db.url/db.username/db.password " +
                    "in config.properties are correct and that the cloud MySQL instance is reachable. " +
                    "Underlying error: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience method for safely closing a connection without forcing
     * every caller to write its own try/catch around close().
     */
    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Warning: failed to close database connection cleanly: " + e.getMessage());
            }
        }
    }

    /**
     * Quick connectivity check, useful at application startup or from
     * an admin "test connection" button.
     */
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (DatabaseException | SQLException e) {
            System.err.println("Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }
}

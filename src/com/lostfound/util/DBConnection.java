package com.lostfound.util;

import com.lostfound.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;










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
            
            
            
            
            
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            
            
            
            
            throw new ExceptionInInitializerError(
                    "MySQL JDBC driver (" + DRIVER_CLASS + ") not found on the classpath. " +
                    "Download 'mysql-connector-j-*.jar' and place it in the 'lib' folder, " +
                    "then include it on the classpath when running the app. " +
                    "Root cause: " + e);
        }
    }

    private DBConnection() {
        
    }

    




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

    



    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Warning: failed to close database connection cleanly: " + e.getMessage());
            }
        }
    }

    



    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (DatabaseException | SQLException e) {
            System.err.println("Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }
}

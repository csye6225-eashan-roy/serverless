package com.cloud.serverless;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Logger;

public class DatabaseService {

    private static final Logger logger = Logger.getLogger(DatabaseService.class.getName());
    private final String dbUser = System.getenv("DB_USER");
    private final String dbPass = System.getenv("DB_PASS");
    private final String dbName = System.getenv("DB_NAME");
    private final String dbHost = System.getenv("DB_HOST"); //private ip of cloudsql

    public DatabaseService() {
        try {
            // Ensure the JDBC driver is available
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.severe("PostgreSQL JDBC Driver is not found. Include it in your library path ");
            e.printStackTrace();
            return;
        }
    }

    public void updateUserVerificationToken(String username, String token, Timestamp expiryTime) {
        // Build the JDBC URL
        String jdbcUrl = String.format("jdbc:postgresql://%s/%s", dbHost, dbName);

        // SQL statement to update the user
        String sql = "UPDATE users SET email_verification_token = ?, email_verification_token_expiry = ? WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.setTimestamp(2, expiryTime);
            stmt.setString(3, username);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("User verification token updated successfully for " + username);
            } else {
                logger.warning("No user found with the username: " + username);
            }
        } catch (SQLException e) {
            logger.severe("Database update failed for " + username + ": " + e.getMessage());
        }
    }
}

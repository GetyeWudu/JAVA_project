package com.mycompany.bankms;

import java.sql.*;

/**
 * One-time migration script to reset all client passwords to default "Nigus@123"
 * and set mustChangePassword flag to TRUE.
 * 
 * Run this script once after deploying the password management feature.
 * 
 * Usage: java -cp target/classes:path/to/mysql-connector.jar com.mycompany.bankms.PasswordMigrationScript
 */
public class PasswordMigrationScript {
    
    private static final String DEFAULT_PASSWORD = "Nigus@123";
    
    public static void main(String[] args) {
        System.out.println("Starting password migration...");
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            // Step 1: Add mustChangePassword column if it doesn't exist
            try {
                String alterSql = "ALTER TABLE users ADD COLUMN mustChangePassword BOOLEAN DEFAULT FALSE";
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(alterSql);
                System.out.println("Added mustChangePassword column to users table");
            } catch (SQLException e) {
                // Column might already exist, that's okay
                System.out.println("mustChangePassword column already exists or error: " + e.getMessage());
            }
            
            // Step 2: Count clients before migration
            String countSql = "SELECT COUNT(*) FROM users WHERE role = 'client'";
            PreparedStatement countStmt = conn.prepareStatement(countSql);
            ResultSet countRs = countStmt.executeQuery();
            int clientCount = 0;
            if (countRs.next()) {
                clientCount = countRs.getInt(1);
            }
            System.out.println("Found " + clientCount + " client accounts");
            
            // Step 3: Reset passwords
            String updateSql = "UPDATE users SET password_hash = ?, mustChangePassword = TRUE WHERE role = 'client'";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, DEFAULT_PASSWORD);
            int rowsUpdated = updateStmt.executeUpdate();
            System.out.println("Updated " + rowsUpdated + " client passwords to default");
            
            // Step 4: Create audit log entries
            String auditSql = "INSERT INTO transactions (account_id, transaction_type, amount, description, date) " +
                            "SELECT a.account_id, 'Admin Action', 0, 'Password reset to default via migration', NOW() " +
                            "FROM accounts a " +
                            "JOIN customers c ON a.customer_id = c.customer_id " +
                            "JOIN users u ON c.user_id = u.user_id " +
                            "WHERE u.role = 'client'";
            Statement auditStmt = conn.createStatement();
            int auditRows = auditStmt.executeUpdate(auditSql);
            System.out.println("Created " + auditRows + " audit log entries");
            
            // Commit changes
            conn.commit();
            System.out.println("\nMigration completed successfully!");
            System.out.println("All client passwords have been reset to: " + DEFAULT_PASSWORD);
            System.out.println("Clients will be required to change their password on next login.");
            
            // Display sample of updated users
            String verifySql = "SELECT user_id, username, mustChangePassword FROM users WHERE role = 'client' LIMIT 5";
            PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
            ResultSet verifyRs = verifyStmt.executeQuery();
            
            System.out.println("\nSample of updated users:");
            System.out.println("UserID | Username | MustChangePassword");
            System.out.println("-------+----------+-------------------");
            while (verifyRs.next()) {
                System.out.printf("%6d | %-8s | %s%n", 
                    verifyRs.getInt("user_id"),
                    verifyRs.getString("username"),
                    verifyRs.getBoolean("mustChangePassword") ? "YES" : "NO"
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Migration failed with error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

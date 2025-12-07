package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.sql.*;
import java.util.Random;

public class RegisterController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegister() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = "Nigus@123"; // Default password for new clients
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || 
            email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration Error", "Please fill in all required fields.");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Create User
            String userSql = "INSERT INTO users (username, password_hash, role, status) VALUES (?, ?, 'client', 'active')";
            PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            userStmt.setString(1, username);
            userStmt.setString(2, password); // In real app, hash this!
            userStmt.executeUpdate();

            ResultSet userKeys = userStmt.getGeneratedKeys();
            int userId = 0;
            if (userKeys.next()) {
                userId = userKeys.getInt(1);
            } else {
                throw new SQLException("Creating user failed, no ID obtained.");
            }

            // 2. Create Customer Profile
            String customerSql = "INSERT INTO customers (user_id, first_name, last_name, email, phone, address) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement custStmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS);
            custStmt.setInt(1, userId);
            custStmt.setString(2, firstName);
            custStmt.setString(3, lastName);
            custStmt.setString(4, email);
            custStmt.setString(5, phone);
            custStmt.setString(6, address);
            custStmt.executeUpdate();

            ResultSet custKeys = custStmt.getGeneratedKeys();
            int customerId = 0;
            if (custKeys.next()) {
                customerId = custKeys.getInt(1);
            } else {
                throw new SQLException("Creating customer failed.");
            }

            // 3. Create Default Savings Account
            // We need a valid branch_id. Assuming '1' exists or we create a dummy one.
            // For now, we will fetch the first available branch or default to 1.
            int branchId = getOrCreateDefaultBranch(conn);
            
            String accountNum = generateAccountNumber();
            String accountSql = "INSERT INTO accounts (customer_id, branch_id, account_number, account_type, balance, status) VALUES (?, ?, ?, 'savings', 0.00, 'active')";
            PreparedStatement accStmt = conn.prepareStatement(accountSql);
            accStmt.setInt(1, customerId);
            accStmt.setInt(2, branchId);
            accStmt.setString(3, accountNum);
            accStmt.executeUpdate();

            conn.commit(); // Commit Transaction
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!\nYour Account Number: " + accountNum + "\nDefault Password: Nigus@123");
            goToLogin();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            statusLabel.setText("Registration failed. Username or Email might exist.");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private String generateAccountNumber() {
        Random rand = new Random();
        int number = rand.nextInt(90000000) + 10000000; // Generates 8 digit number
        return String.valueOf(number);
    }

    private int getOrCreateDefaultBranch(Connection conn) throws SQLException {
        // Try to find existing branch
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT branch_id FROM branches LIMIT 1");
        if (rs.next()) {
            return rs.getInt("branch_id");
        }
        // If no branch, create Main Branch
        stmt.executeUpdate("INSERT INTO branches (name, address, city, state, zip_code) VALUES ('Main Branch', '123 Bank St', 'New York', 'NY', '10001')");
        rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
        if (rs.next()) return rs.getInt(1);
        return 1;
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/bankms/login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
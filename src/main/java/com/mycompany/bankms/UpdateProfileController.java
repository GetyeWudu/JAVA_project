package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.util.regex.Pattern;

public class UpdateProfileController {

    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private Label statusLabel;
    
    // Password change fields
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordStatusLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
        loadCurrentInfo();
    }

    private void loadCurrentInfo() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT email, phone_number, address FROM customers WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                emailField.setText(rs.getString("email"));
                phoneField.setText(rs.getString("phone_number"));
                addressField.setText(rs.getString("address"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleUpdate() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE customers SET email=?, phone_number=?, address=? WHERE user_id=?")) {
            
            stmt.setString(1, emailField.getText().trim());
            stmt.setString(2, phoneField.getText().trim());
            stmt.setString(3, addressField.getText().trim());
            stmt.setInt(4, userId);
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Profile updated successfully!");
            } else {
                statusLabel.setText("Update failed.");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleChangePassword() {
        String currentPass = currentPasswordField.getText().trim();
        String newPass = newPasswordField.getText().trim();
        String confirmPass = confirmPasswordField.getText().trim();
        
        // Clear previous messages
        passwordStatusLabel.setStyle("-fx-text-fill: red;");
        
        // Validation
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passwordStatusLabel.setText("All password fields are required.");
            return;
        }
        
        if (!newPass.equals(confirmPass)) {
            passwordStatusLabel.setText("New passwords do not match.");
            return;
        }
        
        // Password strength validation
        String validationError = validatePassword(newPass);
        if (validationError != null) {
            passwordStatusLabel.setText(validationError);
            return;
        }
        
        try (Connection conn = DBConnection.getConnection()) {
            // Check current password
            String checkSql = "SELECT password_hash FROM users WHERE user_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String dbPassword = rs.getString("password_hash");
                if (!currentPass.equals(dbPassword)) {
                    passwordStatusLabel.setText("Current password is incorrect.");
                    return;
                }
                
                // Update password and clear mustChangePassword flag
                String updateSql = "UPDATE users SET password_hash = ?, mustChangePassword = FALSE WHERE user_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, newPass);
                updateStmt.setInt(2, userId);
                int rows = updateStmt.executeUpdate();
                
                if (rows > 0) {
                    passwordStatusLabel.setStyle("-fx-text-fill: green;");
                    passwordStatusLabel.setText("Password changed successfully!");
                    // Clear fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                } else {
                    passwordStatusLabel.setText("Password update failed.");
                }
            } else {
                passwordStatusLabel.setText("User not found.");
            }
        } catch (SQLException e) {
            passwordStatusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        
        // Check for uppercase letter
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            return "Password must contain at least one uppercase letter.";
        }
        
        // Check for lowercase letter
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            return "Password must contain at least one lowercase letter.";
        }
        
        // Check for digit
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            return "Password must contain at least one digit.";
        }
        
        // Check for special character
        if (!Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find()) {
            return "Password must contain at least one special character.";
        }
        
        return null; // Password is valid
    }
}
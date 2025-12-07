package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

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

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passwordStatusLabel.setStyle("-fx-text-fill: red;");
            passwordStatusLabel.setText("All password fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            passwordStatusLabel.setStyle("-fx-text-fill: red;");
            passwordStatusLabel.setText("New passwords do not match.");
            return;
        }

        String checkSql = "SELECT password_hash FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getString("password_hash").equals(currentPass)) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, newPass);
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    
                    passwordStatusLabel.setStyle("-fx-text-fill: green;");
                    passwordStatusLabel.setText("Password changed successfully!");
                    
                    // Clear fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                }
            } else {
                passwordStatusLabel.setStyle("-fx-text-fill: red;");
                passwordStatusLabel.setText("Current password is incorrect.");
            }

        } catch (SQLException e) {
            passwordStatusLabel.setStyle("-fx-text-fill: red;");
            passwordStatusLabel.setText("Error: " + e.getMessage());
        }
    }
}
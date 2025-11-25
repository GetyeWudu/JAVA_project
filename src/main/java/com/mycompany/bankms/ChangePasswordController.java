package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class ChangePasswordController {

    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
    }

    @FXML
    private void handleChangePassword() {
        String oldPass = oldPasswordField.getText().trim();
        String newPass = newPasswordField.getText().trim();
        String confirmPass = confirmPasswordField.getText().trim();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            statusLabel.setText("New passwords do not match.");
            return;
        }

        String checkSql = "SELECT password_hash FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getString("password_hash").equals(oldPass)) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, newPass); // Hash in production
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    statusLabel.setText("Password changed successfully.");
                }
            } else {
                statusLabel.setText("Incorrect current password.");
            }

        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}

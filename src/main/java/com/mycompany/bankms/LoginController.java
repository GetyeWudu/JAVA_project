package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try (Connection conn = DBConnection.getConnection()) {
            // Use password_hash based on your DB
            String checkSql = "SELECT user_id, password_hash, role, status, mustChangePassword FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String dbPass = rs.getString("password_hash");
                String role = rs.getString("role");
                String status = rs.getString("status");
                boolean mustChange = rs.getBoolean("mustChangePassword");

                if ("inactive".equalsIgnoreCase(status) || "frozen".equalsIgnoreCase(status)) {
                    messageLabel.setText("Account Locked. Contact Admin.");
                    return;
                }

                if (password.equals(dbPass)) {
                    if (role.equals("admin")) {
                        loadAdminDashboard(userId, username);
                    } else {
                        // Check if client must change password
                        if (mustChange) {
                            loadPasswordChangePage(userId, username);
                        } else {
                            loadClientDashboard(userId, username);
                        }
                    }
                } else {
                    messageLabel.setText("Invalid Credentials.");
                }
            } else {
                messageLabel.setText("User not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Database Connection Error");
        }
    }
    
    private void loadPasswordChangePage(int userId, String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/update_profile.fxml"));
        Parent root = loader.load();
        
        UpdateProfileController controller = loader.getController();
        controller.setUserSession(userId);
        
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        
        // Show alert to inform user
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Change Required");
            alert.setHeaderText("You must change your password");
            alert.setContentText("Please scroll down to the 'Change Password' section and set a new password before accessing your account.");
            alert.showAndWait();
        });
    }

    private void loadAdminDashboard(int adminId, String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/dashboard.fxml"));
        Parent root = loader.load();
        
        DashboardController controller = loader.getController();
        controller.setAdminSession(adminId, username);
        
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true); // KEEP FULL SCREEN
    }

    private void loadClientDashboard(int userId, String username) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/client_dashboard.fxml"));
        Parent root = loader.load();
        
        ClientDashboardController controller = loader.getController();
        controller.setUserSession(userId, username);
        
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setMaximized(true); // KEEP FULL SCREEN
    }
}
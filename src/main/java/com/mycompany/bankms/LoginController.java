package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }

        String sql = "SELECT user_id FROM users WHERE username = ? AND password_hash = ? AND role = 'client' AND status = 'active'";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Hash in production

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("user_id");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/client_dashboard.fxml"));
                Parent root = loader.load();
                ClientDashboardController controller = loader.getController();
                controller.setUserSession(userId, username);

                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(new Scene(root));
            } else {
                statusLabel.setText("Invalid credentials.");
            }

        } catch (Exception e) {
            statusLabel.setText("Login error.");
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/bankms/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            statusLabel.setText("Failed to load registration screen.");
            e.printStackTrace();
        }
    }
}

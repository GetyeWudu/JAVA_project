package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class UpdateProfileController {

    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressField;
    @FXML private Label statusLabel;

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
}
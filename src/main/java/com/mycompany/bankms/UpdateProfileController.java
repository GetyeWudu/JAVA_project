package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class UpdateProfileController {

    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private Label statusLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
    }

    @FXML
    private void handleUpdate() {
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (phone.isEmpty() && address.isEmpty()) {
            statusLabel.setText("Please enter phone or address to update.");
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE customers SET ");
        if (!phone.isEmpty()) sql.append("phone = ?, ");
        if (!address.isEmpty()) sql.append("address = ?, ");
        sql.setLength(sql.length() - 2); // remove last comma
        sql.append(" WHERE user_id = ?");

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int index = 1;
            if (!phone.isEmpty()) stmt.setString(index++, phone);
            if (!address.isEmpty()) stmt.setString(index++, address);
            stmt.setInt(index, userId);

            int rows = stmt.executeUpdate();
            statusLabel.setText(rows > 0 ? "Profile updated successfully." : "No changes made.");

        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}

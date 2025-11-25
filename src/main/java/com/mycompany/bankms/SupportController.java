package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class SupportController {

    @FXML private TextField subjectField;
    @FXML private TextArea messageArea;
    @FXML private Label statusLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
    }

    @FXML
    private void handleSendMessage() {
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (subject.isEmpty() || message.isEmpty()) {
            statusLabel.setText("Please fill in both subject and message.");
            return;
        }

        String sql = "INSERT INTO messages (sender_id, receiver_id, subject, message_body) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, 1); // Assuming admin has user_id = 1
            stmt.setString(3, subject);
            stmt.setString(4, message);

            stmt.executeUpdate();
            statusLabel.setText("Message sent successfully.");

        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}

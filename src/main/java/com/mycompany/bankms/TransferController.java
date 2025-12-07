package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.sql.*;

public class TransferController {

    @FXML private Label balanceLabel;
    @FXML private TextField recipientField;
    @FXML private TextField amountField;
    @FXML private Label accountNumberLabel;

    private int accountId;
    private int userId;
    private String username;
    private double currentBalance;

    public void setSession(int userId, String username, int accountId, String accountNumber, double currentBalance) {
        this.userId = userId;
        this.username = username;
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        
        if (accountNumberLabel != null) accountNumberLabel.setText(accountNumber);
        if (balanceLabel != null) balanceLabel.setText(String.format("%.2f ETB", currentBalance));
    }

    @FXML
    private void handleTransfer() {
        String targetAccNum = recipientField.getText().trim();
        String amountText = amountField.getText().trim();

        if (targetAccNum.isEmpty() || amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill all fields.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Invalid amount.");
            return;
        }

        if (amount > currentBalance) {
            showAlert(Alert.AlertType.ERROR, "Insufficient Funds", "You do not have enough balance.");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // 1. Verify Recipient Exists & Get ID
            int targetAccountId = -1;
            PreparedStatement checkTarget = conn.prepareStatement("SELECT account_id FROM accounts WHERE account_number = ?");
            checkTarget.setString(1, targetAccNum);
            ResultSet rsTarget = checkTarget.executeQuery();
            
            if (rsTarget.next()) {
                targetAccountId = rsTarget.getInt("account_id");
            } else {
                throw new SQLException("Recipient account not found.");
            }

            if (targetAccountId == accountId) {
                throw new SQLException("Cannot transfer money to yourself.");
            }

            // 2. Deduct from Sender (Me)
            PreparedStatement deduct = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_id = ?");
            deduct.setDouble(1, amount);
            deduct.setInt(2, accountId);
            deduct.executeUpdate();

            // 3. Add to Recipient
            PreparedStatement add = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_id = ?");
            add.setDouble(1, amount);
            add.setInt(2, targetAccountId);
            add.executeUpdate();

            // 4. Log Sender Transaction (Debit)
            String logSender = "INSERT INTO transactions (account_id, transaction_type, amount, description, date) VALUES (?, 'Transfer Out', ?, ?, NOW())";
            PreparedStatement psSender = conn.prepareStatement(logSender);
            psSender.setInt(1, accountId);
            psSender.setDouble(2, amount);
            psSender.setString(3, "To Account: " + targetAccNum);
            psSender.executeUpdate();

            // 5. Log Recipient Transaction (Credit)
            String logRecipient = "INSERT INTO transactions (account_id, transaction_type, amount, description, date) VALUES (?, 'Transfer In', ?, ?, NOW())";
            PreparedStatement psRecipient = conn.prepareStatement(logRecipient);
            psRecipient.setInt(1, targetAccountId);
            psRecipient.setDouble(2, amount);
            psRecipient.setString(3, "From Account ID: " + accountId);
            psRecipient.executeUpdate();

            conn.commit(); // COMMIT
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Transfer Successful!");
            goBackToDashboard();

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) {} }
            showAlert(Alert.AlertType.ERROR, "Transfer Failed", e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) {} }
        }
    }

    @FXML
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/client_dashboard.fxml"));
            Parent root = loader.load();
            
            ClientDashboardController controller = loader.getController();
            controller.setUserSession(userId, username);

            Stage stage = (Stage) amountField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
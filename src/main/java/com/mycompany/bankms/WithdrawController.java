package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.sql.*;

public class WithdrawController {

    @FXML private Label balanceLabel;
    @FXML private TextField amountField;
    @FXML private Label accountNumberLabel;

    private int accountId;
    private int userId;
    private String username;
    private double currentBalance; // To check sufficient funds

    // This method matches the signature used in ClientDashboardController
    public void setSession(int userId, String username, int accountId, String accountNumber, double currentBalance) {
        this.userId = userId;
        this.username = username;
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        
        if (accountNumberLabel != null) {
            accountNumberLabel.setText(accountNumber);
        }
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("ETB %.2f", currentBalance));
        }
    }

    @FXML
    private void handleWithdraw() {
        String amountText = amountField.getText();

        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please enter an amount.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Invalid amount. Please enter a positive number.");
            return;
        }

        if (amount > currentBalance) {
            showAlert(Alert.AlertType.ERROR, "Insufficient Funds", "You do not have enough balance for this withdrawal.");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // 1. Update Balance (Subtract)
            String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setDouble(1, amount);
            updateStmt.setInt(2, accountId);
            int rows = updateStmt.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Account update failed.");
            }

            // 2. Log Transaction
            // Note: Using 'transaction_type' as per your fixed database schema
            String logSql = "INSERT INTO transactions (account_id, transaction_type, amount, description, date) VALUES (?, 'Withdraw', ?, 'Cash Withdrawal', NOW())";
            PreparedStatement logStmt = conn.prepareStatement(logSql);
            logStmt.setInt(1, accountId);
            logStmt.setDouble(2, amount);
            logStmt.executeUpdate();

            conn.commit(); // COMMIT TRANSACTION
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Withdrawal successful!");
            goBackToDashboard();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            showAlert(Alert.AlertType.ERROR, "System Error", "Withdrawal failed. " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void goBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/client_dashboard.fxml"));
            Parent root = loader.load();
            
            ClientDashboardController controller = loader.getController();
            controller.setUserSession(userId, username); // Reload dashboard to show new balance

            Stage stage = (Stage) amountField.getScene().getWindow();
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
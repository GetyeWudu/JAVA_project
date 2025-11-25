package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class DepositController {

    @FXML private TextField amountField;
    @FXML private Label statusLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
    }

    @FXML
    private void handleDeposit() {
        String amountText = amountField.getText().trim();
        double amount;

        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid amount.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "")) {
            conn.setAutoCommit(false);

            // Get account ID
            String getAccountSql = "SELECT account_id, balance FROM accounts WHERE customer_id = (SELECT customer_id FROM customers WHERE user_id = ?)";
            try (PreparedStatement accStmt = conn.prepareStatement(getAccountSql)) {
                accStmt.setInt(1, userId);
                ResultSet rs = accStmt.executeQuery();

                if (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    double currentBalance = rs.getDouble("balance");
                    double newBalance = currentBalance + amount;

                    // Update balance
                    String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, newBalance);
                        updateStmt.setInt(2, accountId);
                        updateStmt.executeUpdate();
                    }

                    // Insert transaction
                    String txnSql = "INSERT INTO transactions (account_id, type, amount, description) VALUES (?, 'deposit', ?, ?)";
                    try (PreparedStatement txnStmt = conn.prepareStatement(txnSql)) {
                        txnStmt.setInt(1, accountId);
                        txnStmt.setDouble(2, amount);
                        txnStmt.setString(3, "Client deposit");
                        txnStmt.executeUpdate();
                    }

                    conn.commit();
                    statusLabel.setText("Deposit successful!");
                } else {
                    statusLabel.setText("Account not found.");
                    conn.rollback();
                }
            } catch (SQLException e) {
                conn.rollback();
                statusLabel.setText("Error: " + e.getMessage());
            }
        } catch (SQLException e) {
            statusLabel.setText("Connection error: " + e.getMessage());
        }
    }
}

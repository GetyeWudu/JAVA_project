package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class WithdrawController {
    @FXML private TextField amountField;
    @FXML private Label statusLabel;
    private int userId;

    public void setUserSession(int userId) { this.userId = userId; }

    @FXML
    private void handleWithdraw() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            statusLabel.setText("Invalid amount.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "")) {
            conn.setAutoCommit(false);

            String getAcc = "SELECT account_id, balance FROM accounts WHERE customer_id = (SELECT customer_id FROM customers WHERE user_id = ?)";
            try (PreparedStatement accStmt = conn.prepareStatement(getAcc)) {
                accStmt.setInt(1, userId);
                ResultSet rs = accStmt.executeQuery();
                if (rs.next()) {
                    int accId = rs.getInt("account_id");
                    double currentBal = rs.getDouble("balance");

                    if (currentBal < amount) {
                        statusLabel.setText("Insufficient balance.");
                        return;
                    }

                    String update = "UPDATE accounts SET balance = ? WHERE account_id = ?";
                    try (PreparedStatement upd = conn.prepareStatement(update)) {
                        upd.setDouble(1, currentBal - amount);
                        upd.setInt(2, accId);
                        upd.executeUpdate();
                    }

                    String txn = "INSERT INTO transactions (account_id, type, amount, description) VALUES (?, 'withdrawal', ?, 'Client withdrawal')";
                    try (PreparedStatement txnStmt = conn.prepareStatement(txn)) {
                        txnStmt.setInt(1, accId);
                        txnStmt.setDouble(2, amount);
                        txnStmt.executeUpdate();
                    }

                    conn.commit();
                    statusLabel.setText("Withdrawal successful.");
                } else {
                    conn.rollback();
                    statusLabel.setText("Account not found.");
                }
            }
        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}

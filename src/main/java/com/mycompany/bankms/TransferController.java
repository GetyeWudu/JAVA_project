package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class TransferController {
    @FXML private TextField targetAccountField;
    @FXML private TextField amountField;
    @FXML private Label statusLabel;
    private int userId;

    public void setUserSession(int userId) { this.userId = userId; }

    @FXML
    private void handleTransfer() {
        String targetText = targetAccountField.getText().trim();
        String amountText = amountField.getText().trim();
        double amount;

        try {
            int targetAccount = Integer.parseInt(targetText);
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();

            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "")) {
                conn.setAutoCommit(false);

                String getSender = "SELECT account_id, balance FROM accounts WHERE customer_id = (SELECT customer_id FROM customers WHERE user_id = ?)";
                try (PreparedStatement senderStmt = conn.prepareStatement(getSender)) {
                    senderStmt.setInt(1, userId);
                    ResultSet senderRs = senderStmt.executeQuery();

                    if (senderRs.next()) {
                        int senderId = senderRs.getInt("account_id");
                        double senderBal = senderRs.getDouble("balance");

                        if (senderBal < amount) {
                            statusLabel.setText("Insufficient balance.");
                            return;
                        }

                        String getReceiver = "SELECT account_id FROM accounts WHERE account_id = ?";
                        try (PreparedStatement receiverStmt = conn.prepareStatement(getReceiver)) {
                            receiverStmt.setInt(1, targetAccount);
                            ResultSet receiverRs = receiverStmt.executeQuery();

                            if (receiverRs.next()) {
                                int receiverId = receiverRs.getInt("account_id");

                                // Update balances
                                String updateSender = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                                String updateReceiver = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
                                try (
                                    PreparedStatement updSender = conn.prepareStatement(updateSender);
                                    PreparedStatement updReceiver = conn.prepareStatement(updateReceiver)
                                ) {
                                    updSender.setDouble(1, amount);
                                    updSender.setInt(2, senderId);
                                    updSender.executeUpdate();

                                    updReceiver.setDouble(1, amount);
                                    updReceiver.setInt(2, receiverId);
                                    updReceiver.executeUpdate();
                                }

                                // Log transactions
                                String txn = "INSERT INTO transactions (account_id, type, amount, description) VALUES (?, ?, ?, ?)";
                                try (PreparedStatement txnStmt = conn.prepareStatement(txn)) {
                                    txnStmt.setInt(1, senderId);
                                    txnStmt.setString(2, "transfer");
                                    txnStmt.setDouble(3, amount);
                                    txnStmt.setString(4, "Transfer to account " + receiverId);
                                    txnStmt.executeUpdate();

                                    txnStmt.setInt(1, receiverId);
                                    txnStmt.setString(2, "receive");
                                    txnStmt.setDouble(3, amount);
                                    txnStmt.setString(4, "Received from account " + senderId);
                                    txnStmt.executeUpdate();
                                }

                                conn.commit();
                                statusLabel.setText("Transfer successful.");
                            } else {
                                conn.rollback();
                                statusLabel.setText("Target account not found.");
                            }
                        }
                    } else {
                        conn.rollback();
                        statusLabel.setText("Sender account not found.");
                    }
                }
            } catch (SQLException e) {
                statusLabel.setText("Error: " + e.getMessage());
            }

        } catch (Exception e) {
            statusLabel.setText("Invalid input.");
        }
    }
}

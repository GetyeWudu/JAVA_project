package com.mycompany.bankms;

import java.io.IOException;
import java.sql.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class ClientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentPane;

    private int userId;
    private String username;
    
    // Cached Account Info to pass to sub-screens
    private int accountId;
    private String accountNumber;
    private double currentBalance;

    public void setUserSession(int userId, String username) {
        this.userId = userId;
        this.username = username;
        welcomeLabel.setText("Welcome, " + username);
        
        loadAccountData();  // 1. Get Data from DB
        showAccountDetails(); // 2. Show Default Screen
    }

    private void loadAccountData() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT a.account_id, a.account_number, a.balance " +
                 "FROM accounts a " +
                 "JOIN customers c ON a.customer_id = c.customer_id " +
                 "WHERE c.user_id = ?")) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                this.accountId = rs.getInt("account_id");
                this.accountNumber = rs.getString("account_number");
                this.currentBalance = rs.getDouble("balance");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/bankms/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {}
    }

    @FXML private void showDeposit() { loadView("deposit.fxml"); }
    @FXML private void showWithdraw() { loadView("withdraw.fxml"); }
    @FXML private void showTransfer() { loadView("transfer.fxml"); }
    @FXML private void showHistory() { loadView("transaction_history.fxml"); }
    @FXML private void showUpdateProfile() { loadView("update_profile.fxml"); }
    @FXML private void showAccountDetails() { loadView("account_details.fxml"); }
    
    // NEW: Button for Loans
    @FXML private void showLoans() { loadView("loans.fxml"); }

    private void loadView(String fxmlFile) {
        try {
            // Refresh balance every time we switch screens
            loadAccountData(); 
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/" + fxmlFile));
            Parent view = loader.load();
            Object controller = loader.getController();

            // Pass data to sub-controllers
            if (controller instanceof DepositController) {
                ((DepositController) controller).setSession(userId, username, accountId, accountNumber, currentBalance);
            } 
            else if (controller instanceof WithdrawController) {
                ((WithdrawController) controller).setSession(userId, username, accountId, accountNumber, currentBalance);
            } 
            else if (controller instanceof TransferController) {
                ((TransferController) controller).setSession(userId, username, accountId, accountNumber, currentBalance);
            } 
            else if (controller instanceof TransactionHistoryController) {
                ((TransactionHistoryController) controller).setUserSession(userId);
            } 
            else if (controller instanceof UpdateProfileController) {
                ((UpdateProfileController) controller).setUserSession(userId);
            } 
            else if (controller instanceof AccountDetailsController) {
                 ((AccountDetailsController) controller).setSession(userId, username, accountId, accountNumber, currentBalance);
            }
            // NEW: Handle Loan Controller
            else if (controller instanceof LoanController) {
                ((LoanController) controller).setSession(userId, username, accountId, accountNumber, currentBalance);
            }

            contentPane.getChildren().setAll(view);
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }
}
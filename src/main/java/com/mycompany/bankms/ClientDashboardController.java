package com.mycompany.bankms;

import java.io.IOException;
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

    public void setUserSession(int userId, String username) {
        this.userId = userId;
        this.username = username;
        welcomeLabel.setText("Welcome, " + username);
        showDashboard();
    }

    @FXML
    private void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/bankms/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
        }
    }

    @FXML private void showDashboard() { loadView("dashboard.fxml"); }
    @FXML private void showDeposit() { loadView("deposit.fxml"); }
    @FXML private void showWithdraw() { loadView("withdraw.fxml"); }
    @FXML private void showTransfer() { loadView("transfer.fxml"); }
    @FXML private void showHistory() { loadView("transaction_history.fxml"); }
    @FXML private void showAccountDetails() { loadView("account_details.fxml"); }
    @FXML private void showUpdateProfile() { loadView("update_profile.fxml"); }
    @FXML private void showChangePassword() { loadView("change_password.fxml"); }
    @FXML private void showSupport() { loadView("support.fxml"); }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/" + fxmlFile));
            Parent view = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardController) {
                DashboardController dashboard = (DashboardController) controller;
                dashboard.setUserSession(userId);
            } else if (controller instanceof DepositController) {
                DepositController deposit = (DepositController) controller;
                deposit.setUserSession(userId);
            } else if (controller instanceof WithdrawController) {
                WithdrawController withdraw = (WithdrawController) controller;
                withdraw.setUserSession(userId);
            } else if (controller instanceof TransferController) {
                TransferController transfer = (TransferController) controller;
                transfer.setUserSession(userId);
            } else if (controller instanceof TransactionHistoryController) {
                TransactionHistoryController history = (TransactionHistoryController) controller;
                history.setUserSession(userId);
            } else if (controller instanceof AccountDetailsController) {
                AccountDetailsController details = (AccountDetailsController) controller;
                details.setUserSession(userId);
            } else if (controller instanceof UpdateProfileController) {
                UpdateProfileController update = (UpdateProfileController) controller;
                update.setUserSession(userId);
            } else if (controller instanceof ChangePasswordController) {
                ChangePasswordController change = (ChangePasswordController) controller;
                change.setUserSession(userId);
            } else if (controller instanceof SupportController) {
                SupportController support = (SupportController) controller;
                support.setUserSession(userId);
            }

            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
        }
    }
}

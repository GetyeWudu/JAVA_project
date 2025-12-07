package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.application.Platform;

public class AccountDetailsController {
    @FXML private Label accNumLabel;
    @FXML private Label balanceLabel;
    @FXML private Label statusLabel;
    @FXML private Label userLabel; // Optional, if you have a user greeting

    public void setSession(int userId, String username, int accountId, String accountNumber, double balance) {
        Platform.runLater(() -> {
            if (accNumLabel != null) accNumLabel.setText(accountNumber != null ? accountNumber : "----");
            if (balanceLabel != null) balanceLabel.setText(String.format("$ %.2f", balance));
            if (statusLabel != null) statusLabel.setText("Active");
            if (userLabel != null) userLabel.setText(username);
        });
    }
}
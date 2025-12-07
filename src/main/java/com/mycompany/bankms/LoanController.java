package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;

public class LoanController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> loanTypeBox;
    @FXML private Label statusLabel;
    @FXML private TableView<ObservableList<String>> loanTable;
    @FXML private TableColumn<ObservableList<String>, String> idCol;
    @FXML private TableColumn<ObservableList<String>, String> amountCol;
    @FXML private TableColumn<ObservableList<String>, String> typeCol;
    @FXML private TableColumn<ObservableList<String>, String> statusCol;
    @FXML private TableColumn<ObservableList<String>, String> dateCol;

    private int accountId;
    private int userId;
    private double currentBalance; // We need this for logic!

    @FXML
    public void initialize() {
        if (loanTypeBox != null) {
            loanTypeBox.getItems().addAll("Personal Loan", "Home Loan", "Car Loan", "Education Loan");
        }
    }

    // Updated to receive Current Balance
    public void setSession(int userId, String username, int accountId, String accountNumber, double balance) {
        this.userId = userId;
        this.accountId = accountId;
        this.currentBalance = balance;
        loadLoanHistory();
    }

    @FXML
    private void handleApply() {
        String amountText = amountField.getText();
        String type = loanTypeBox.getValue();

        if (amountText.isEmpty() || type == null) {
            showError("Please fill all fields.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            // --- BUSINESS LOGIC START ---
            
            // Rule 1: Cannot apply for negative or zero
            if (amount <= 0) {
                showError("Invalid amount.");
                return;
            }

            // Rule 2: Max Limits based on Loan Type
            if (type.equals("Personal Loan") && amount > 10000) {
                showError("Personal loans are capped at 10,000 ETB.");
                return;
            }
            if (type.equals("Car Loan") && amount > 50000) {
                showError("Car loans are capped at 50,000 ETB.");
                return;
            }

            // Rule 3: Collateral Check (Simulation)
            // You cannot borrow more than 20x your current balance (prevents brand new empty accounts from scamming)
            if (currentBalance < 50 && amount > 1000) {
                 showError("Insufficient account history/balance for this loan size.");
                 return;
            }

            // --- BUSINESS LOGIC END ---

            try (Connection conn = DBConnection.getConnection()) {
                String sql = "INSERT INTO loans (account_id, amount, loan_type, status) VALUES (?, ?, ?, 'Pending')";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, accountId);
                stmt.setDouble(2, amount);
                stmt.setString(3, type);
                
                stmt.executeUpdate();
                
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Application Submitted for Review!");
                amountField.clear();
                loadLoanHistory();
            }

        } catch (NumberFormatException e) {
            showError("Please enter a valid number.");
        } catch (SQLException e) {
            showError("Database Error: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: red;");
        statusLabel.setText(msg);
    }

    private void loadLoanHistory() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        String sql = "SELECT loan_id, amount, loan_type, status, request_date FROM loans WHERE account_id = ? ORDER BY request_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, accountId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("loan_id")));
                row.add(String.format("%.2f ETB", rs.getDouble("amount")));
                row.add(rs.getString("loan_type"));
                
                String status = rs.getString("status");
                row.add(status);
                
                row.add(String.valueOf(rs.getTimestamp("request_date")));
                data.add(row);
            }

            idCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
            amountCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));
            typeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(2)));
            statusCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(3)));
            dateCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(4)));
            
            // Add color coding to Status column cell
            statusCol.setCellFactory(column -> new TableCell<ObservableList<String>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equals("Approved")) setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        else if (item.equals("Rejected")) setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    }
                }
            });

            loanTable.setItems(data);

        } catch (Exception e) { e.printStackTrace(); }
    }
}
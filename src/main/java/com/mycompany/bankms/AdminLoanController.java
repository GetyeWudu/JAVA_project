package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.sql.*;

public class AdminLoanController {

    @FXML private TableView<ObservableList<String>> loanTable;
    @FXML private TableColumn<ObservableList<String>, String> idCol;
    @FXML private TableColumn<ObservableList<String>, String> userCol; // Shows username/name
    @FXML private TableColumn<ObservableList<String>, String> amountCol;
    @FXML private TableColumn<ObservableList<String>, String> typeCol;
    @FXML private TableColumn<ObservableList<String>, String> statusCol;
    @FXML private TableColumn<ObservableList<String>, String> dateCol;
    
    @FXML private Label actionStatusLabel;

    private int adminId;
    private String adminUsername;

    public void setAdminSession(int adminId, String adminUsername) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        loadPendingLoans();
    }

    private void loadPendingLoans() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        
        // Complex Query: Get Loan Info + User Info
        String sql = "SELECT l.loan_id, u.username, l.amount, l.loan_type, l.status, l.request_date " +
                     "FROM loans l " +
                     "JOIN accounts a ON l.account_id = a.account_id " +
                     "JOIN customers c ON a.customer_id = c.customer_id " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "ORDER BY l.status DESC, l.request_date DESC"; // Show Pending first

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("loan_id")));
                row.add(rs.getString("username"));
                row.add(String.format("$ %.2f", rs.getDouble("amount")));
                row.add(rs.getString("loan_type"));
                row.add(rs.getString("status"));
                row.add(String.valueOf(rs.getTimestamp("request_date")));
                data.add(row);
            }

            idCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
            userCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));
            amountCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(2)));
            typeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(3)));
            statusCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(4)));
            dateCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(5)));

            loanTable.setItems(data);

        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleApprove() {
        processLoan("Approved");
    }

    @FXML
    private void handleReject() {
        processLoan("Rejected");
    }

    private void processLoan(String decision) {
        ObservableList<String> selectedItem = loanTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            actionStatusLabel.setStyle("-fx-text-fill: red;");
            actionStatusLabel.setText("Please select a loan request first.");
            return;
        }

        String loanId = selectedItem.get(0);
        String currentStatus = selectedItem.get(4);
        
        // Parse amount removing "$ "
        double amount = Double.parseDouble(selectedItem.get(2).replace("$ ", "").replace(",", ""));

        if (!currentStatus.equalsIgnoreCase("Pending")) {
            actionStatusLabel.setStyle("-fx-text-fill: orange;");
            actionStatusLabel.setText("This loan has already been processed.");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // 1. Update Loan Status
            String updateLoan = "UPDATE loans SET status = ? WHERE loan_id = ?";
            PreparedStatement stmtLoan = conn.prepareStatement(updateLoan);
            stmtLoan.setString(1, decision);
            stmtLoan.setInt(2, Integer.parseInt(loanId));
            stmtLoan.executeUpdate();

            // 2. IF APPROVED: Deposit Money into Account
            if (decision.equals("Approved")) {
                // Get Account ID associated with this loan
                PreparedStatement getAcc = conn.prepareStatement("SELECT account_id FROM loans WHERE loan_id = ?");
                getAcc.setInt(1, Integer.parseInt(loanId));
                ResultSet rsAcc = getAcc.executeQuery();
                
                if (rsAcc.next()) {
                    int accId = rsAcc.getInt("account_id");
                    
                    // Update Balance
                    PreparedStatement updateBal = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_id = ?");
                    updateBal.setDouble(1, amount);
                    updateBal.setInt(2, accId);
                    updateBal.executeUpdate();
                    
                    // Log Transaction
                    PreparedStatement logTrans = conn.prepareStatement("INSERT INTO transactions (account_id, transaction_type, amount, description, date) VALUES (?, 'Loan Disbursed', ?, 'Loan Approval Credit', NOW())");
                    logTrans.setInt(1, accId);
                    logTrans.setDouble(2, amount);
                    logTrans.executeUpdate();
                }
            }

            conn.commit(); // SAVE CHANGES
            actionStatusLabel.setStyle("-fx-text-fill: green;");
            actionStatusLabel.setText("Loan " + decision + " Successfully.");
            loadPendingLoans(); // Refresh Table

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
            actionStatusLabel.setStyle("-fx-text-fill: red;");
            actionStatusLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception e) {}
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setAdminSession(adminId, adminUsername);
            Stage stage = (Stage) loanTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
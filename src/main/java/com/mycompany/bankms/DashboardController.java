package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.sql.*;
import java.util.Optional;

public class DashboardController {

    @FXML private Label adminNameLabel;
    @FXML private Label totalClientsLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label activeAccountsLabel;

    private int adminId;
    private String adminUsername;

    public void setAdminSession(int adminId, String adminUsername) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        if(adminNameLabel != null) adminNameLabel.setText("Logged in as: " + adminUsername);
        loadSystemStats();
    }

    private void loadSystemStats() {
        try (Connection conn = DBConnection.getConnection()) {
            
            String sqlClients = "SELECT COUNT(*) FROM users WHERE role = 'client'";
            PreparedStatement stmt1 = conn.prepareStatement(sqlClients);
            ResultSet rs1 = stmt1.executeQuery();
            if (rs1.next()) totalClientsLabel.setText(String.valueOf(rs1.getInt(1)));

            double totalDeposits = 0;
            String sqlBalance = "SELECT SUM(balance) FROM accounts";
            PreparedStatement stmt2 = conn.prepareStatement(sqlBalance);
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                totalDeposits = rs2.getDouble(1);
                totalBalanceLabel.setText(String.format("%.2f ETB", totalDeposits));
            }

            String sqlActive = "SELECT COUNT(*) FROM users WHERE status = 'active' AND role = 'client'";
            PreparedStatement stmt3 = conn.prepareStatement(sqlActive);
            ResultSet rs3 = stmt3.executeQuery();
            if (rs3.next()) activeAccountsLabel.setText(String.valueOf(rs3.getInt(1)));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- FIXED: USES 'password_hash' ---
    @FXML
    private void handleAddAdmin() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create New Admin");
        dialog.setHeaderText("Enter details for the new administrator");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                // FIXED QUERY: Uses password_hash and removed failed_attempts
                String sql = "INSERT INTO users (username, password_hash, role, status) VALUES (?, ?, 'admin', 'active')";
                
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    if(username.getText().isEmpty() || password.getText().isEmpty()) return null;

                    stmt.setString(1, username.getText());
                    stmt.setString(2, password.getText());
                    stmt.executeUpdate();
                    
                    return null;
                } catch (SQLException e) { e.printStackTrace(); }
            }
            return null;
        });

        dialog.showAndWait();
        showAlert("Success", "Action completed. Try logging in with the new admin.");
    }

    @FXML
    private void handleSimulateMonthEnd() {
        TextInputDialog dialog = new TextInputDialog("2.5");
        dialog.setTitle("End of Month Simulation");
        dialog.setHeaderText("Apply Interest to Savings Accounts");
        dialog.setContentText("Enter Interest Rate (%):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(rateStr -> {
            try {
                double rate = Double.parseDouble(rateStr);
                applyInterest(rate);
            } catch (NumberFormatException e) {
                showAlert("Invalid Rate", "Please enter a valid number.");
            }
        });
    }

    private void applyInterest(double ratePercentage) {
        double multiplier = ratePercentage / 100.0;
        int processedCount = 0;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "SELECT account_id, balance FROM accounts";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            String updateSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);

            String logSql = "INSERT INTO transactions (account_id, transaction_type, amount, description, date) VALUES (?, 'Interest Credit', ?, ?, NOW())";
            PreparedStatement logStmt = conn.prepareStatement(logSql);

            while (rs.next()) {
                int accId = rs.getInt("account_id");
                double balance = rs.getDouble("balance");
                
                if (balance > 0) {
                    double interest = balance * multiplier;
                    updateStmt.setDouble(1, interest);
                    updateStmt.setInt(2, accId);
                    updateStmt.addBatch();

                    logStmt.setInt(1, accId);
                    logStmt.setDouble(2, interest);
                    logStmt.setString(3, "Monthly Interest (" + ratePercentage + "%)");
                    logStmt.addBatch();
                    processedCount++;
                }
            }

            updateStmt.executeBatch();
            logStmt.executeBatch();
            conn.commit();
            
            showAlert("Success", "Month End Processed! Interest applied to " + processedCount + " accounts.");
            loadSystemStats(); 

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to process interest: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML private void goToManageClients() { loadScene("/com/mycompany/bankms/admin_customer_list.fxml"); }
    @FXML private void goToAuditLog() { loadScene("/com/mycompany/bankms/admin_transactions.fxml"); }
    @FXML private void goToLoans() { loadScene("/com/mycompany/bankms/admin_loans.fxml"); }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) totalClientsLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            
            if (controller instanceof AdminClientsController) ((AdminClientsController) controller).setAdminSession(adminId, adminUsername);
            else if (controller instanceof AdminTransactionsController) ((AdminTransactionsController) controller).setAdminSession(adminId, adminUsername);
            else if (controller instanceof AdminCustomerListController) ((AdminCustomerListController) controller).setAdminSession(adminId, adminUsername);
            else if (controller instanceof AdminLoanController) ((AdminLoanController) controller).setAdminSession(adminId, adminUsername);

            Stage stage = (Stage) totalClientsLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { 
            System.err.println("Failed to load: " + fxmlPath);
            e.printStackTrace(); 
        }
    }
}
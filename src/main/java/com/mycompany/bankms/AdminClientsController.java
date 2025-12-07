package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.sql.*;
import java.util.Random;

public class AdminClientsController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    
    @FXML private ComboBox<String> accountTypeBox;
    @FXML private TextField initialDepositField;
    
    @FXML private Label statusLabel;

    private int adminId;
    private String adminUsername;

    @FXML
    public void initialize() {
        if (accountTypeBox != null) {
            accountTypeBox.getItems().addAll("Savings", "Checking", "Business");
            accountTypeBox.getSelectionModel().selectFirst();
        }
    }

    public void setAdminSession(int adminId, String adminUsername) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
    }

    @FXML
    private void handleCreateClient() {
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressArea.getText().trim();
        String user = usernameField.getText().trim();
        String pass = "Nigus@123"; // Default password for new clients
        String type = accountTypeBox.getValue();
        String depositStr = initialDepositField.getText().trim();
        String phone = phoneField.getText().trim();

        if (fName.isEmpty() || lName.isEmpty() || user.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Please fill all required fields.");
            return;
        }

        double initialDeposit = 0.0;
        try {
            if (!depositStr.isEmpty()) {
                initialDeposit = Double.parseDouble(depositStr);
                if (initialDeposit < 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Invalid deposit amount.");
            return;
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // 1. Check Duplicate Username Manually
            String checkSql = "SELECT user_id FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, user);
            if (checkStmt.executeQuery().next()) {
                throw new SQLException("Duplicate username: " + user);
            }

            // 2. Create User
            String sqlUser = "INSERT INTO users (username, password_hash, role, status) VALUES (?, ?, 'client', 'active')";
            PreparedStatement stmtUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            stmtUser.setString(1, user);
            stmtUser.setString(2, pass); 
            stmtUser.executeUpdate();

            ResultSet rs = stmtUser.getGeneratedKeys();
            int newUserId = 0;
            if (rs.next()) newUserId = rs.getInt(1);
            else throw new SQLException("User creation failed.");

            // 3. Create Customer (Includes phone_number now that DB is fixed)
            String sqlCustomer = "INSERT INTO customers (user_id, first_name, last_name, email, phone_number, address) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmtCust = conn.prepareStatement(sqlCustomer, Statement.RETURN_GENERATED_KEYS);
            stmtCust.setInt(1, newUserId);
            stmtCust.setString(2, fName);
            stmtCust.setString(3, lName);
            stmtCust.setString(4, email);
            stmtCust.setString(5, phone.isEmpty() ? "N/A" : phone);
            stmtCust.setString(6, address.isEmpty() ? "N/A" : address);
            stmtCust.executeUpdate();

            ResultSet rsCust = stmtCust.getGeneratedKeys();
            int newCustId = 0;
            if (rsCust.next()) newCustId = rsCust.getInt(1);
            else throw new SQLException("Customer profile creation failed.");

            // 4. Create Account (Branch ID 1, Safe Account Number)
            String sqlAcc = "INSERT INTO accounts (customer_id, account_number, account_type, balance, status, branch_id) VALUES (?, ?, ?, ?, 'active', 1)";
            PreparedStatement stmtAcc = conn.prepareStatement(sqlAcc);
            stmtAcc.setInt(1, newCustId);
            
            // Generate 8-digit safe Account Number
            int randomAcc = 10000000 + new Random().nextInt(89999999);
            stmtAcc.setString(2, String.valueOf(randomAcc));
            
            stmtAcc.setString(3, type);
            stmtAcc.setDouble(4, initialDeposit);
            stmtAcc.executeUpdate();

            // 5. Record Deposit (Requires transaction_type column)
            if (initialDeposit > 0) {
                String sqlTrans = "INSERT INTO transactions (account_id, transaction_type, amount, description) VALUES ((SELECT account_id FROM accounts WHERE customer_id = ?), 'Deposit', ?, 'Initial Deposit')";
                PreparedStatement stmtTrans = conn.prepareStatement(sqlTrans);
                stmtTrans.setInt(1, newCustId);
                stmtTrans.setDouble(2, initialDeposit);
                stmtTrans.executeUpdate();
            }

            conn.commit(); // SAVE
            
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Client Created Successfully!");
            clearFields();

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) {} }
            
            String msg = e.getMessage();
            if (msg.contains("Duplicate") || msg.contains("username")) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Error: Username '" + user + "' is already taken.");
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Error: " + msg);
            }
            e.printStackTrace();
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) {}
        }
    }

    @FXML
    private void handleCancel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/dashboard.fxml"));
            Parent root = loader.load();
            
            DashboardController controller = loader.getController();
            controller.setAdminSession(adminId, adminUsername != null ? adminUsername : "Admin");

            Stage stage = (Stage) firstNameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        addressArea.clear();
        usernameField.clear();
        passwordField.clear();
        initialDepositField.clear();
    }
}
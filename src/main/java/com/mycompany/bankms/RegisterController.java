package com.mycompany.bankms;

//import com.mycompany.bankms.utils.DBConnection;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;
import java.time.LocalDate;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField nationalIdField;
    @FXML private DatePicker dobPicker;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField initialDepositField;
    @FXML private ChoiceBox<String> accountTypeChoice;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        accountTypeChoice.getItems().addAll("savings", "current", "fixed");
        accountTypeChoice.setValue("savings");
    }
    
    @FXML
private void goToLogin() {
    try {
        Parent root = FXMLLoader.load(getClass().getResource("/com/mycompany/bankms/login.fxml"));
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root));
    } catch (IOException e) {
        statusLabel.setText("Failed to load login screen.");
    }
}


    @FXML
    private void handleRegister() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String nationalId = nationalIdField.getText().trim();
        LocalDate dob = dobPicker.getValue();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String depositText = initialDepositField.getText().trim();
        String accountType = accountTypeChoice.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Please fill all required fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        double initialDeposit;
        try {
            initialDeposit = Double.parseDouble(depositText);
            if (initialDeposit < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid deposit amount.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String userSql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, 'client')";
            try (PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, username);
                userStmt.setString(2, password); // Hash in production
                userStmt.executeUpdate();

                ResultSet rs = userStmt.getGeneratedKeys();
                if (rs.next()) {
                    int userId = rs.getInt(1);

                    String customerSql = "INSERT INTO customers (user_id, first_name, last_name, email, phone, address, national_id, date_of_birth) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement custStmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS)) {
                        custStmt.setInt(1, userId);
                        custStmt.setString(2, firstName);
                        custStmt.setString(3, lastName);
                        custStmt.setString(4, email);
                        custStmt.setString(5, phone);
                        custStmt.setString(6, address);
                        custStmt.setString(7, nationalId);
                        custStmt.setDate(8, dob != null ? Date.valueOf(dob) : null);
                        custStmt.executeUpdate();

                        ResultSet custRs = custStmt.getGeneratedKeys();
                        if (custRs.next()) {
                            int customerId = custRs.getInt(1);

                            String accountSql = "INSERT INTO accounts (customer_id, branch_id, account_type, balance) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement accStmt = conn.prepareStatement(accountSql)) {
                                accStmt.setInt(1, customerId);
                                accStmt.setInt(2, 1); // Default branch ID
                                accStmt.setString(3, accountType);
                                accStmt.setDouble(4, initialDeposit);
                                accStmt.executeUpdate();
                            }

                            conn.commit();
                            statusLabel.setText("Registration successful!");
                        } else {
                            conn.rollback();
                            statusLabel.setText("Customer creation failed.");
                        }
                    }
                } else {
                    conn.rollback();
                    statusLabel.setText("User creation failed.");
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

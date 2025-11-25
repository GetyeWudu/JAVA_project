package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.*;

public class AccountDetailsController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label addressLabel;
    @FXML private Label nationalIdLabel;
    @FXML private Label dobLabel;
    @FXML private Label accountTypeLabel;
    @FXML private Label balanceLabel;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
        loadAccountDetails();
    }

    private void loadAccountDetails() {
 String sql = "SELECT c.first_name, c.last_name, c.email, c.phone, c.address, c.national_id, c.date_of_birth,a.account_type, a.balance FROM customers c JOIN accounts a ON c.customer_id = a.customer_id  WHERE c.user_id = ?  ";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nameLabel.setText(rs.getString("first_name") + " " + rs.getString("last_name"));
                emailLabel.setText(rs.getString("email"));
                phoneLabel.setText(rs.getString("phone"));
                addressLabel.setText(rs.getString("address"));
                nationalIdLabel.setText(rs.getString("national_id"));
                dobLabel.setText(rs.getString("date_of_birth"));
                accountTypeLabel.setText(rs.getString("account_type"));
                balanceLabel.setText(String.format("%.2f", rs.getDouble("balance")));
            }

        } catch (SQLException e) {
        }
    }
}

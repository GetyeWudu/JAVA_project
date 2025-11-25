package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.sql.*;

public class DashboardController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label accountTypeLabel;
    @FXML private Label balanceLabel;

    @FXML private TableView<ObservableList<String>> transactionTable;
    @FXML private TableColumn<ObservableList<String>, String> dateColumn;
    @FXML private TableColumn<ObservableList<String>, String> typeColumn;
    @FXML private TableColumn<ObservableList<String>, String> amountColumn;
    @FXML private TableColumn<ObservableList<String>, String> descColumn;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
        loadAccountInfo();
        loadRecentTransactions();
    }

    private void loadAccountInfo() {
 String sql = " SELECT c.first_name, c.last_name, c.email, a.account_type, a.balance FROM customers c JOIN accounts a ON c.customer_id = a.customer_id WHERE c.user_id = ? ";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "your_password");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nameLabel.setText(rs.getString("first_name") + " " + rs.getString("last_name"));
                emailLabel.setText(rs.getString("email"));
                accountTypeLabel.setText(rs.getString("account_type"));
                balanceLabel.setText(String.format("%.2f", rs.getDouble("balance")));
            }

        } catch (SQLException e) {
        }
    }

    private void loadRecentTransactions() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
 String sql = "  SELECT transaction_date, transaction_type, amount, description FROM transactions WHERE account_id = (SELECT account_id FROM accounts WHERE customer_id = ( SELECT customer_id FROM customers WHERE user_id = ?  ) ) ORDER BY transaction_date DESC LIMIT 5";

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BankManagement", "root", "");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(rs.getString("transaction_date"));
                row.add(rs.getString("transaction_type"));
                row.add(String.format("%.2f", rs.getDouble("amount")));
                row.add(rs.getString("description"));
                data.add(row);
            }

            dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(0)));
            typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(1)));
            amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(2)));
            descColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(3)));

            transactionTable.setItems(data);

        } catch (SQLException e) {
        }
    }
}

package com.mycompany.bankms;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;

public class TransactionHistoryController {

    @FXML private TableView<ObservableList<String>> historyTable;
    @FXML private TableColumn<ObservableList<String>, String> dateCol;
    @FXML private TableColumn<ObservableList<String>, String> typeCol;
    @FXML private TableColumn<ObservableList<String>, String> amountCol;
    @FXML private TableColumn<ObservableList<String>, String> descCol;

    private int userId;

    public void setUserSession(int userId) {
        this.userId = userId;
        loadHistory();
    }

    private void loadHistory() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        
        String sql = "SELECT t.date, t.transaction_type, t.amount, t.description " +
                     "FROM transactions t " +
                     "JOIN accounts a ON t.account_id = a.account_id " +
                     "JOIN customers c ON a.customer_id = c.customer_id " +
                     "WHERE c.user_id = ? ORDER BY t.date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getTimestamp("date")));
                row.add(rs.getString("transaction_type"));
                row.add(String.format("ETB %.2f", rs.getDouble("amount")));
                row.add(rs.getString("description"));
                data.add(row);
            }

            dateCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
            typeCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));
            amountCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(2)));
            descCol.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(3)));

            historyTable.setItems(data);

        } catch (SQLException e) { e.printStackTrace(); }
    }
}
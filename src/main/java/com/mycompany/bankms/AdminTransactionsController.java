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

public class AdminTransactionsController {

    @FXML private TableView<ObservableList<String>> transactionTable;
    @FXML private TableColumn<ObservableList<String>, String> idColumn;
    @FXML private TableColumn<ObservableList<String>, String> accountColumn;
    @FXML private TableColumn<ObservableList<String>, String> typeColumn;
    @FXML private TableColumn<ObservableList<String>, String> amountColumn;
    @FXML private TableColumn<ObservableList<String>, String> dateColumn;
    @FXML private TableColumn<ObservableList<String>, String> descColumn;

    private int adminId;
    private String adminUsername;

    public void setAdminSession(int adminId, String adminUsername) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        loadTransactions();
    }

    private void loadTransactions() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        
        // CHANGED: Using 't.date' as per your database schema
        String sql = "SELECT t.transaction_id, a.account_number, t.transaction_type, t.amount, t.date, t.description " +
                     "FROM transactions t " +
                     "LEFT JOIN accounts a ON t.account_id = a.account_id " +
                     "ORDER BY t.date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("transaction_id")));
                
                String accNum = rs.getString("account_number");
                row.add(accNum != null ? accNum : "Deleted Account");
                
                String type = rs.getString("transaction_type");
                row.add(type != null ? type : "General");
                
                row.add(String.format("%.2f ETB", rs.getDouble("amount")));
                
                // Fetching from the 'date' column now
                row.add(String.valueOf(rs.getTimestamp("date")));
                
                row.add(rs.getString("description"));
                data.add(row);
            }

            idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(0)));
            accountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(1)));
            typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(2)));
            amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(3)));
            dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(4)));
            descColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(5)));

            transactionTable.setItems(data);

        } catch (SQLException e) {
            System.err.println("SQL Error in Audit Log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setAdminSession(adminId, adminUsername);
            Stage stage = (Stage) transactionTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }
    // Add inside AdminTransactionsController class
    @FXML
    private void handleExportCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Account,Type,Amount,Date,Description\n");
        
        for (ObservableList<String> row : transactionTable.getItems()) {
            csv.append(String.join(",", row)).append("\n");
        }
        
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get("Transaction_Report.csv"), csv.toString().getBytes());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("Report saved as 'Transaction_Report.csv' in project folder.");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

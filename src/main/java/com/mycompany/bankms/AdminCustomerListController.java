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

public class AdminCustomerListController {

    @FXML private TableView<ObservableList<String>> clientTable;
    @FXML private TableColumn<ObservableList<String>, String> idColumn;
    @FXML private TableColumn<ObservableList<String>, String> nameColumn;
    @FXML private TableColumn<ObservableList<String>, String> emailColumn;
    @FXML private TableColumn<ObservableList<String>, String> phoneColumn;
    @FXML private TableColumn<ObservableList<String>, String> accountColumn;
    @FXML private TableColumn<ObservableList<String>, String> balanceColumn;
    @FXML private TableColumn<ObservableList<String>, String> statusColumn;
    
    @FXML private Label statusMsgLabel; 

    private int adminId;
    private String adminUsername;

    public void setAdminSession(int adminId, String adminUsername) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        loadClients();
    }

    private void loadClients() {
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        String sql = "SELECT u.user_id, c.first_name, c.last_name, c.email, c.phone_number, a.account_number, a.balance, u.status " +
                     "FROM customers c " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "LEFT JOIN accounts a ON c.customer_id = a.customer_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("user_id")));
                row.add(rs.getString("first_name") + " " + rs.getString("last_name"));
                row.add(rs.getString("email"));
                row.add(rs.getString("phone_number"));
                row.add(rs.getString("account_number"));
                row.add(String.format("%.2f ETB", rs.getDouble("balance")));
                row.add(rs.getString("status"));
                data.add(row);
            }
            
            idColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(0)));
            nameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(1)));
            emailColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(2)));
            phoneColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(3)));
            accountColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(4)));
            balanceColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(5)));
            statusColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().get(6)));

            clientTable.setItems(data);
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    @FXML
    private void handleToggleFreeze() {
        ObservableList<String> selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if(statusMsgLabel != null) statusMsgLabel.setText("Select a client first.");
            return;
        }

        int userId = Integer.parseInt(selected.get(0));
        String currentStatus = selected.get(6);
        
        // Uses 'inactive' based on your setup
        String newStatus = currentStatus.equalsIgnoreCase("active") ? "inactive" : "active";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET status = ? WHERE user_id = ?")) {
            
            stmt.setString(1, newStatus);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            
            if(statusMsgLabel != null) {
                statusMsgLabel.setStyle("-fx-text-fill: " + (newStatus.equals("active") ? "green" : "red"));
                statusMsgLabel.setText("User status updated to: " + newStatus);
            }
            loadClients(); 
            
        } catch (SQLException e) { 
            e.printStackTrace();
            if(statusMsgLabel != null) statusMsgLabel.setText("Database Error: " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.setAdminSession(adminId, adminUsername);
            Stage stage = (Stage) clientTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void goToAddClient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/mycompany/bankms/admin_clients.fxml"));
            Parent root = loader.load();
            AdminClientsController controller = loader.getController();
            controller.setAdminSession(adminId, adminUsername);
            Stage stage = (Stage) clientTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    // --- FIXED: Uses 'password_hash' ---
    @FXML
    private void handleResetPassword() {
        ObservableList<String> selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if(statusMsgLabel != null) statusMsgLabel.setText("Select a client first.");
            return;
        }

        int userId = Integer.parseInt(selected.get(0));
        String defaultPass = "Bank@123";

        try (Connection conn = DBConnection.getConnection();
             // FIXED QUERY: using password_hash
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE user_id = ?")) {
            
            stmt.setString(1, defaultPass);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
            
            if(statusMsgLabel != null) {
                statusMsgLabel.setStyle("-fx-text-fill: green;");
                statusMsgLabel.setText("Password reset to: " + defaultPass);
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
            if(statusMsgLabel != null) statusMsgLabel.setText("DB Error: " + e.getMessage());
        }
    }
}
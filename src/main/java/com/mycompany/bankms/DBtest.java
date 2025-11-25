
package com.mycompany.bankms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class DBtest {

    public static Connection connect() {
        Connection conn = null;
        try {
            // ✅ Load driver (optional in new JDBC versions, but safe)
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://127.0.0.1:3306/BankManagement";
            String user = "root";  // your MySQL username
            String password = "";  // your MySQL password

            conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connected successfully!");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("❌ Database connection failed: " + e.getMessage());
        }
        return conn;
    }
//    public static void main(String []args) {
//        
//        connect();
//    }
}

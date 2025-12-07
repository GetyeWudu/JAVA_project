# Banking Management System - Defense Preparation Notes

## Project Overview
A comprehensive Banking Management System built with JavaFX and MySQL that provides functionality for both administrators and clients to manage banking operations.

---

## System Architecture

### Technology Stack
- **Frontend**: JavaFX (version 13)
- **Backend**: Java 11
- **Database**: MySQL
- **Build Tool**: Maven
- **UI Design**: FXML + CSS

### Database Connection
- **Class**: `DBConnection.java`
- **Connection String**: `jdbc:mysql://localhost:3306/bankms`
- **Implementation**: Static method `getConnection()` returns a new database connection for each operation
- **Thread Safety**: Each operation gets its own connection to avoid concurrency issues

---

## Database Schema

### Core Tables

1. **users**
   - Stores authentication and role information
   - Fields: user_id (PK), username, password_hash, role (admin/client/employee), status, created_at
   - Used for login and access control

2. **customers**
   - Stores customer profile information
   - Fields: customer_id (PK), user_id (FK), first_name, last_name, email, phone, address, national_id, date_of_birth
   - Links to users table via user_id

3. **accounts**
   - Stores bank account information
   - Fields: account_id (PK), customer_id (FK), branch_id (FK), account_number, account_type, balance, status
   - Links to customers and branches tables

4. **transactions**
   - Records all financial transactions
   - Fields: transaction_id (PK), account_id (FK), transaction_type, amount, target_account, description, date, status
   - Provides audit trail for all account activities

5. **loans**
   - Manages loan applications and approvals
   - Fields: loan_id (PK), account_id (FK), amount, loan_type, status, request_date
   - Supports pending, approved, and rejected states

6. **branches**
   - Stores branch location information
   - Fields: branch_id (PK), name, address, city, state, zip_code

---

## Major Functionalities

### 1. Authentication & Authorization

#### Login System
- **Controller**: `LoginController.java`
- **FXML**: `login.fxml`
- **Implementation**:
  1. User enters username and password
  2. System queries `users` table: `SELECT user_id, password_hash, role, status FROM users WHERE username = ?`
  3. Validates password (plain text comparison - note: in production should use hashing)
  4. Checks account status (active/inactive)
  5. Routes to appropriate dashboard based on role (admin → `dashboard.fxml`, client → `client_dashboard.fxml`)
  6. Sets maximized window mode for full-screen experience

#### User Registration
- **Controller**: `RegisterController.java`
- **FXML**: `register.fxml`
- **Implementation**:
  1. Collects: first name, last name, username, email, phone, address
  2. **Default Password**: Sets `Nigus@123` as the default password for all new clients
  3. **Transaction-based approach**:
     - Creates user in `users` table
     - Creates customer profile in `customers` table
     - Generates 8-digit account number
     - Creates default savings account in `accounts` table with ETB 0.00 balance
  4. Uses `conn.setAutoCommit(false)` and `conn.commit()` to ensure atomicity
  5. Rolls back on any failure to maintain data integrity

### 2. Admin Dashboard

#### Statistics Display
- **Controller**: `DashboardController.java`
- **FXML**: `dashboard.fxml`
- **Metrics Shown**:
  - Total Clients: `SELECT COUNT(*) FROM users WHERE role = 'client'`
  - Total Liquidity: `SELECT SUM(balance) FROM accounts` (displayed in ETB)
  - Active Accounts: `SELECT COUNT(*) FROM users WHERE status = 'active' AND role = 'client'`

#### Client Management
- **Controller**: `AdminCustomerListController.java`
- **FXML**: `admin_customer_list.fxml`
- **Features**:
  
  **a) View All Clients**
  - Displays table with: user_id, name, email, phone, account_number, balance (ETB), status
  - Query: Joins `users`, `customers`, and `accounts` tables
  
  **b) Toggle Client Status (Make Active/Inactive)** - *CASE STUDY*
  - **Flow**:
    1. Admin selects a client from the table
    2. Clicks "Toggle Freeze" button
    3. System reads current status from selected row
    4. Toggles status: active → inactive OR inactive → active
    5. Executes: `UPDATE users SET status = ? WHERE user_id = ?`
    6. Updates UI to show new status with color coding (green for active, red for inactive)
    7. Refreshes table to reflect changes
  - **Database Interaction**: Direct UPDATE query on users table
  - **UI Feedback**: Status message label shows result, table row updates immediately
  - **Access Control**: When user with inactive status tries to login, they see "Account Locked. Contact Admin."
  
  **c) Reset User Password**
  - Resets any client password to default `Nigus@123`
  - Query: `UPDATE users SET password_hash = 'Nigus@123' WHERE user_id = ?`
  - Use case: Client forgot password and contacts admin

#### Create New Client (Admin)
- **Controller**: `AdminClientsController.java`
- **FXML**: `admin_clients.fxml`
- **Implementation**:
  - Similar to self-registration but admin-initiated
  - **Default Password**: Automatically sets `Nigus@123` (password field removed from UI)
  - Allows setting initial deposit amount
  - Creates complete user → customer → account chain in single transaction

#### Transaction Audit
- **Controller**: `AdminTransactionsController.java`
- **FXML**: `admin_transactions.fxml`
- **Features**:
  - Lists all system transactions
  - Query: `SELECT t.transaction_id, a.account_number, t.transaction_type, t.amount, t.date, t.description FROM transactions t LEFT JOIN accounts a ON t.account_id = a.account_id ORDER BY t.date DESC`
  - Displays: transaction ID, account number, type, amount (ETB), date, description
  - Export to CSV functionality: writes all visible transactions to `Transaction_Report.csv`

#### Loan Management
- **Controller**: `AdminLoanController.java`
- **FXML**: `admin_loans.fxml`
- **Features**:
  
  **a) View Loan Applications**
  - Shows all loans with status (Pending, Approved, Rejected)
  - Joins loans, accounts, customers, and users tables to show username
  
  **b) Approve/Reject Loans**
  - **Approval Process**:
    1. Admin selects pending loan
    2. Clicks "Approve"
    3. System updates loan status to "Approved"
    4. **Disburses funds**: Adds loan amount to customer's account balance
    5. Records transaction: `INSERT INTO transactions ... ('Loan Disbursed', ...)`
    6. All in single transaction with rollback capability
  - **Rejection Process**:
    1. Simply updates loan status to "Rejected"
    2. No fund transfer occurs

#### Month-End Processing (Interest)
- **Implementation**:
  - Admin enters interest rate (e.g., 2.5%)
  - System calculates interest for all accounts: `balance * (rate / 100)`
  - Updates all account balances: `UPDATE accounts SET balance = balance + interest`
  - Logs each transaction: `INSERT INTO transactions ... ('Interest Credit', ...)`
  - Uses batch processing for efficiency

### 3. Client Dashboard

#### Account Overview
- **Controller**: `AccountDetailsController.java`
- **FXML**: `account_details.fxml`
- **Display**: Account number, current balance (ETB), status (Active)
- **Implementation**: Loaded on dashboard initialization using session data

#### Deposit Funds
- **Controller**: `DepositController.java`
- **FXML**: `deposit.fxml`
- **Flow**:
  1. Client enters amount
  2. Validates: amount > 0
  3. **Transaction**:
     - `UPDATE accounts SET balance = balance + ? WHERE account_id = ?`
     - `INSERT INTO transactions (account_id, type, amount, description, status) VALUES (?, 'deposit', ?, 'Cash Deposit', 'completed')`
  4. Uses `conn.setAutoCommit(false)` for atomicity
  5. Redirects to dashboard showing updated balance

#### Withdraw Funds
- **Controller**: `WithdrawController.java`
- **FXML**: `withdraw.fxml`
- **Flow**:
  1. Client enters amount
  2. Validates:
     - amount > 0
     - amount ≤ currentBalance (prevents overdraft)
  3. **Transaction**:
     - `UPDATE accounts SET balance = balance - ? WHERE account_id = ?`
     - `INSERT INTO transactions ... ('Withdraw', ...)`
  4. Commits or rolls back as atomic operation

#### Transfer Money
- **Controller**: `TransferController.java`
- **FXML**: `transfer.fxml`
- **Flow**:
  1. Client enters recipient account number and amount
  2. Validations:
     - Recipient account exists
     - Cannot transfer to self
     - Sufficient balance
  3. **Transaction** (4 operations in single atomic unit):
     - Deduct from sender: `UPDATE accounts SET balance = balance - ? WHERE account_id = ?`
     - Add to recipient: `UPDATE accounts SET balance = balance + ? WHERE account_id = ?`
     - Log sender transaction: `INSERT INTO transactions ... ('Transfer Out', ...)`
     - Log recipient transaction: `INSERT INTO transactions ... ('Transfer In', ...)`
  4. All-or-nothing: If any operation fails, entire transfer is rolled back

#### Loan Application
- **Controller**: `LoanController.java`
- **FXML**: `loans.fxml`
- **Features**:
  
  **a) Apply for Loan**
  - Client selects loan type (Personal, Home, Car, Education)
  - Enters desired amount
  - **Business Rules**:
    - Personal loans capped at ETB 10,000
    - Car loans capped at ETB 50,000
    - Minimum balance requirement: ETB 50 for loans > ETB 1,000
  - Creates loan record with status = 'Pending'
  
  **b) View Application History**
  - Shows all client's loan applications with status
  - Color-coded status: Green (Approved), Red (Rejected), Orange (Pending)

#### Transaction History
- **Controller**: `TransactionHistoryController.java`
- **FXML**: `transaction_history.fxml`
- **Implementation**:
  - Query: `SELECT t.date, t.transaction_type, t.amount, t.description FROM transactions t JOIN accounts a ON t.account_id = a.account_id JOIN customers c ON a.customer_id = c.customer_id WHERE c.user_id = ? ORDER BY t.date DESC`
  - Shows: date, type, amount (ETB), description
  - Provides complete audit trail for client

#### Update Profile
- **Controller**: `UpdateProfileController.java`
- **FXML**: `update_profile.fxml`
- **Features**:
  
  **a) Update Contact Information**
  - Client can update: email, phone number, address
  - Query: `UPDATE customers SET email=?, phone_number=?, address=? WHERE user_id=?`
  
  **b) Change Password**
  - **NEW FEATURE**: Button opens change password dialog
  - Opens `change_password.fxml` in new window
  - **Process**:
    1. Client enters old password
    2. Enters new password twice (confirmation)
    3. Validates old password matches current
    4. Validates new passwords match
    5. Updates: `UPDATE users SET password_hash = ? WHERE user_id = ?`
  - Allows users to change from default `Nigus@123` to personal password

---

## Integration Flow

### Session Management
- **Pattern**: Session data passed through controller initialization
- **Admin Session**:
  ```java
  controller.setAdminSession(adminId, adminUsername);
  ```
- **Client Session**:
  ```java
  controller.setUserSession(userId, username);
  ```
- **Account Context** (for transaction screens):
  ```java
  controller.setSession(userId, username, accountId, accountNumber, currentBalance);
  ```

### Navigation Flow

1. **Application Start** → `App.java` → `login.fxml`
2. **Login Success**:
   - Admin → `dashboard.fxml` (DashboardController)
   - Client → `client_dashboard.fxml` (ClientDashboardController)
3. **Client Dashboard Navigation**:
   - Uses StackPane (`contentPane`) to swap views
   - Loads child FXML into content area
   - Maintains session context across views
4. **Admin Dashboard Navigation**:
   - Replaces entire scene
   - Passes admin session to new controller
5. **Logout**: Returns to `login.fxml` (clears session)

### Data Flow Example: Making a Deposit

1. **User Action**: Client clicks "Deposit Funds" in sidebar
2. **Navigation**: `ClientDashboardController.showDeposit()` called
3. **Loading**:
   - Refreshes account data from database
   - Loads `deposit.fxml` using FXMLLoader
   - Gets `DepositController` instance
   - Calls `controller.setSession(userId, username, accountId, accountNumber, currentBalance)`
4. **Display**: DepositController populates UI with current balance (ETB)
5. **User Input**: Client enters amount (e.g., ETB 500)
6. **Submission**: `handleDeposit()` method triggered
7. **Validation**: Checks amount > 0
8. **Database Transaction**:
   - Start transaction: `conn.setAutoCommit(false)`
   - Update balance: `UPDATE accounts SET balance = balance + 500 WHERE account_id = ?`
   - Log transaction: `INSERT INTO transactions ...`
   - Commit: `conn.commit()`
   - If any error: `conn.rollback()`
9. **Feedback**: Success alert shown
10. **Return**: Redirects to dashboard with refreshed balance

---

## Security Features

### Authentication
- Username/password verification against database
- Role-based access control (admin vs. client views)
- Status checking (active/inactive accounts)

### Password Management
- **Default Password**: `Nigus@123` for all new accounts
- **Admin Reset**: Admin can reset any client password to default
- **Client Change**: Clients can change password in Update Profile section
- **Current Implementation**: Plain text (Note: Production should use bcrypt or similar)

### Account Status Control
- Inactive accounts cannot login
- Admin can freeze/unfreeze accounts instantly
- Prevents unauthorized access to frozen accounts

### Transaction Integrity
- All financial operations use database transactions
- Automatic rollback on failure
- Prevents partial updates (e.g., money deducted but not credited)

---

## UI/UX Features

### Currency Localization
- All monetary values displayed in **ETB (Ethiopian Birr)**
- Format: `ETB 1,234.56`
- Consistent across all screens

### Full-Screen Mode
- Application launches maximized: `stage.setMaximized(true)`
- Maintained across all screen transitions
- Provides immersive banking experience

### Standardized Logout Button
- **Text**: "Log Out" (consistent across admin and client dashboards)
- **Dimensions**: 
  - `maxWidth="Infinity"` (fills available width)
  - `padding: top=15, bottom=15`
  - `margin: 20px all around`
- **Position**: Bottom of sidebar in both dashboards
- **Style**: Uses `logout-button` CSS class

### User Feedback
- Status labels on all forms (green for success, red for errors)
- Confirmation dialogs for important actions
- Real-time balance updates after transactions
- Color-coded status indicators (loans, accounts)

---

## Case Study: Make Client Active/Inactive

### Business Context
Administrators need the ability to temporarily suspend client access to the banking system without deleting their account. This is useful for:
- Security concerns (suspicious activity)
- Account verification pending
- Client request for temporary freeze
- Regulatory compliance

### Technical Implementation

#### Database Level
- **Table**: `users`
- **Field**: `status` ENUM('active', 'inactive')
- **Default**: 'active' when account created

#### UI Components
- **Location**: Admin Customer List (`admin_customer_list.fxml`)
- **Button**: "Toggle Freeze" 
- **Status Column**: Shows current status with color coding

#### Code Flow (AdminCustomerListController.java)

```java
@FXML
private void handleToggleFreeze() {
    // 1. Get selected client from table
    ObservableList<String> selected = clientTable.getSelectionModel().getSelectedItem();
    
    // 2. Validation: Ensure a client is selected
    if (selected == null) {
        statusMsgLabel.setText("Select a client first.");
        return;
    }

    // 3. Extract data from selected row
    int userId = Integer.parseInt(selected.get(0));      // First column: user_id
    String currentStatus = selected.get(6);              // Seventh column: status
    
    // 4. Toggle logic: Flip the status
    String newStatus = currentStatus.equalsIgnoreCase("active") ? "inactive" : "active";

    // 5. Database update
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
             "UPDATE users SET status = ? WHERE user_id = ?")) {
        
        stmt.setString(1, newStatus);
        stmt.setInt(2, userId);
        stmt.executeUpdate();
        
        // 6. UI feedback
        statusMsgLabel.setStyle("-fx-text-fill: " + 
            (newStatus.equals("active") ? "green" : "red"));
        statusMsgLabel.setText("User status updated to: " + newStatus);
        
        // 7. Refresh table to show new status
        loadClients(); 
        
    } catch (SQLException e) { 
        statusMsgLabel.setText("Database Error: " + e.getMessage());
    }
}
```

#### Integration Points

1. **Login System Integration**:
   - When user attempts login, system checks status:
   ```java
   if ("inactive".equalsIgnoreCase(status) || "frozen".equalsIgnoreCase(status)) {
       messageLabel.setText("Account Locked. Contact Admin.");
       return;
   }
   ```

2. **Statistics Impact**:
   - Active Accounts count updates automatically
   - Query: `SELECT COUNT(*) FROM users WHERE status = 'active' AND role = 'client'`

3. **Visual Feedback**:
   - Table row shows "active" or "inactive"
   - CSS can be applied to color-code rows based on status

### State Transition Diagram
```
[New Account Created]
         ↓
    status = 'active'
         ↓
    ↙         ↘
Admin clicks      Normal usage
"Toggle Freeze"   continues
    ↓                 ↓
status = 'inactive'  status = 'active'
    ↓                 ↓
Login blocked     Login allowed
    ↓                 ↓
Admin clicks      (cycles back)
"Toggle Freeze"       
    ↓
status = 'active'
```

### Error Handling
- **No client selected**: Alert user to select a row first
- **Database connection failure**: Show error message, no status change
- **Concurrent updates**: Last write wins (could be improved with versioning)

### Extensibility
This same pattern can be extended to:
- Account suspension (different from user status)
- Multiple status levels (frozen, suspended, pending, active)
- Status change audit logging
- Automated status changes based on rules

---

## Error Handling & Validation

### Input Validation
- **Numeric Fields**: Try-catch for parseDouble/parseInt
- **Required Fields**: Empty string checks before submission
- **Business Rules**: Balance checks, loan limits, etc.

### Database Error Handling
- Try-catch around all database operations
- Meaningful error messages to users
- Stack traces to console for debugging
- Transaction rollback on failures

### User Feedback
- Alert dialogs for critical errors
- Status labels for form validation
- Console logging for developer debugging

---

## Performance Considerations

### Database Queries
- Uses PreparedStatements to prevent SQL injection
- Indexes on foreign keys (user_id, account_id, customer_id)
- Efficient joins for complex queries

### Memory Management
- Closes all database connections in finally blocks
- Uses try-with-resources for automatic resource cleanup
- Reloads data on demand rather than caching

### UI Responsiveness
- Database operations on JavaFX application thread (could be improved with background threads)
- Immediate UI feedback for user actions
- Minimal data transfer for display operations

---

## Future Enhancements

### Security
- Implement password hashing (bcrypt, SHA-256)
- Add session timeout
- Implement two-factor authentication
- Add CAPTCHA for login

### Features
- Bill payment functionality
- Scheduled/recurring transactions
- Account statements (PDF generation)
- SMS/Email notifications
- Multi-language support
- Mobile app integration

### Technical
- Move to microservices architecture
- Add RESTful API layer
- Implement connection pooling
- Add caching layer (Redis)
- Comprehensive unit testing
- Integration testing
- Load testing for concurrent users

---

## Conclusion

This Banking Management System demonstrates a complete client-server application with:
- Secure authentication and authorization
- Comprehensive CRUD operations
- Transaction management with ACID properties
- Role-based feature access
- Localized currency display (ETB)
- Full-screen user experience
- Intuitive admin controls for client management

The system successfully handles core banking operations while maintaining data integrity through proper transaction management and providing a user-friendly interface for both administrators and clients.

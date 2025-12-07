# PROJECT DEFENSE NOTES - Banking Management System (BankMS)

## System Overview
The Banking Management System is a comprehensive JavaFX-based desktop application that facilitates banking operations for both administrators and clients. The system uses MySQL database for data persistence and follows a Model-View-Controller (MVC) architecture pattern.

---

## Major Functionalities

### 1. User Authentication & Authorization

**How it's implemented:**
- **Database Schema**: The `users` table stores credentials with fields: `user_id`, `username`, `password_hash`, `role` ('admin' or 'client'), and `status` ('active' or 'inactive')
- **Login Flow** (LoginController.java):
  1. User enters username and password in login.fxml
  2. System queries: `SELECT user_id, password_hash, role, status FROM users WHERE username = ?`
  3. Password is compared (currently plain text, should be hashed in production)
  4. If valid and status is 'active', user is redirected based on role:
     - Admin → DashboardController (dashboard.fxml)
     - Client → ClientDashboardController (client_dashboard.fxml)
  5. Session data (userId, username, role) is passed to the dashboard controller

**Security Features:**
- Status check prevents inactive users from logging in
- Role-based access control separates admin and client interfaces

---

### 2. Client Registration

**How it's implemented:**
- **Database Transaction** (RegisterController.java):
  1. User fills registration form (register.fxml) with: first name, last name, username, email, phone, address
  2. System initiates a database transaction (`conn.setAutoCommit(false)`)
  3. **Step 1 - Create User**:
     ```sql
     INSERT INTO users (username, password_hash, role, status) 
     VALUES (?, 'Nigus@123', 'client', 'active')
     ```
     - Default password is set to "Nigus@123"
     - Role is automatically set to 'client'
  4. **Step 2 - Create Customer Profile**:
     ```sql
     INSERT INTO customers (user_id, first_name, last_name, email, phone, address) 
     VALUES (?, ?, ?, ?, ?, ?)
     ```
  5. **Step 3 - Create Default Account**:
     ```sql
     INSERT INTO accounts (customer_id, branch_id, account_number, account_type, balance, status) 
     VALUES (?, ?, ?, 'savings', 0.00, 'active')
     ```
     - Account number is generated using random 8-digit number
     - Initial balance is 0.00 ETB
  6. Transaction is committed if all steps succeed, or rolled back on error

**Integration Flow:**
- Users table → Customers table (linked by user_id)
- Customers table → Accounts table (linked by customer_id)

---

### 3. Client Dashboard

**How it's implemented:**
- **Architecture** (ClientDashboardController.java):
  - Main layout: BorderPane with sidebar navigation (left) and dynamic content area (center)
  - Content area uses StackPane for switching between different views
  
- **Session Management**:
  ```java
  public void setUserSession(int userId, String username) {
      this.userId = userId;
      this.username = username;
      loadAccountData();  // Fetch account details
      showAccountDetails(); // Display default view
  }
  ```

- **Account Data Loading**:
  ```sql
  SELECT a.account_id, a.account_number, a.balance 
  FROM accounts a 
  JOIN customers c ON a.customer_id = c.customer_id 
  WHERE c.user_id = ?
  ```

- **Navigation System**:
  Each menu button loads a different FXML view into contentPane:
  - Account Overview → account_details.fxml
  - Deposit Funds → deposit.fxml
  - Withdraw Funds → withdraw.fxml
  - Transfer Money → transfer.fxml
  - Request Loan → loans.fxml
  - Transaction History → transaction_history.fxml
  - My Profile → update_profile.fxml

**Data Passing:**
Controllers receive session data via `setSession()` method with parameters: userId, username, accountId, accountNumber, currentBalance

---

### 4. Deposit Functionality

**How it's implemented:**
- **Transaction Flow** (DepositController.java):
  1. User enters amount in deposit.fxml
  2. Input validation:
     - Check if amount is not empty
     - Parse to double and verify > 0
  3. **Database Transaction**:
     ```java
     conn.setAutoCommit(false); // Start transaction
     ```
     a. Update account balance:
     ```sql
     UPDATE accounts SET balance = balance + ? WHERE account_id = ?
     ```
     b. Log transaction:
     ```sql
     INSERT INTO transactions (account_id, type, amount, description, status) 
     VALUES (?, 'deposit', ?, 'Cash Deposit', 'completed')
     ```
  4. Commit transaction
  5. Redirect to dashboard with updated balance

**Error Handling:**
- Rollback on any SQL exception
- User-friendly error messages displayed

---

### 5. Withdrawal Functionality

**How it's implemented:**
- **Business Logic** (WithdrawController.java):
  1. User enters withdrawal amount
  2. **Validation Checks**:
     - Amount must be positive
     - Amount must not exceed current balance: `if (amount > currentBalance)`
  3. **Database Transaction**:
     a. Deduct from balance:
     ```sql
     UPDATE accounts SET balance = balance - ? WHERE account_id = ?
     ```
     b. Log transaction:
     ```sql
     INSERT INTO transactions (account_id, transaction_type, amount, description, date) 
     VALUES (?, 'Withdraw', ?, 'Cash Withdrawal', NOW())
     ```
  4. Commit transaction

**Integration:**
- Real-time balance check prevents overdrafts
- Transaction log creates audit trail

---

### 6. Money Transfer

**How it's implemented:**
- **Complex Transaction** (TransferController.java):
  1. User enters recipient account number and amount
  2. **Validation**:
     - Verify recipient account exists
     - Check sender has sufficient balance
     - Prevent self-transfer
  3. **Multi-Step Database Transaction**:
     ```java
     conn.setAutoCommit(false);
     ```
     a. Verify recipient:
     ```sql
     SELECT account_id FROM accounts WHERE account_number = ?
     ```
     b. Deduct from sender:
     ```sql
     UPDATE accounts SET balance = balance - ? WHERE account_id = ?
     ```
     c. Add to recipient:
     ```sql
     UPDATE accounts SET balance = balance + ? WHERE account_id = ?
     ```
     d. Log sender's transaction:
     ```sql
     INSERT INTO transactions (...) VALUES (?, 'Transfer Out', ?, 'To Account: [number]', NOW())
     ```
     e. Log recipient's transaction:
     ```sql
     INSERT INTO transactions (...) VALUES (?, 'Transfer In', ?, 'From Account ID: [id]', NOW())
     ```
  4. Commit all changes atomically

**Key Features:**
- Atomic transactions ensure both accounts are updated or neither
- Double-entry bookkeeping (debit and credit logged separately)

---

### 7. Loan Management System

#### 7.1 Client Side - Loan Application

**How it's implemented:**
- **Business Rules** (LoanController.java):
  1. User selects loan type and enters amount
  2. **Validation Rules**:
     - Personal Loan: Max ETB 10,000
     - Car Loan: Max ETB 50,000
     - Minimum account history check: `if (currentBalance < 50 && amount > 1000)`
  3. **Database Insert**:
     ```sql
     INSERT INTO loans (account_id, amount, loan_type, status) 
     VALUES (?, ?, ?, 'Pending')
     ```
  4. Display in history table with color-coded status:
     - Pending: Orange
     - Approved: Green
     - Rejected: Red

**Data Display:**
- TableView shows: loan_id, amount (ETB format), type, status, request_date
- Auto-refresh after submission

#### 7.2 Admin Side - Loan Processing

**How it's implemented:**
- **Approval/Rejection Flow** (AdminLoanController.java):
  1. Admin views all loans via complex join query:
     ```sql
     SELECT l.loan_id, u.username, l.amount, l.loan_type, l.status, l.request_date 
     FROM loans l 
     JOIN accounts a ON l.account_id = a.account_id 
     JOIN customers c ON a.customer_id = c.customer_id 
     JOIN users u ON c.user_id = u.user_id 
     ORDER BY l.status DESC, l.request_date DESC
     ```
  2. Admin selects loan and clicks Approve/Reject
  3. **If Approved - Multi-Step Transaction**:
     a. Update loan status:
     ```sql
     UPDATE loans SET status = 'Approved' WHERE loan_id = ?
     ```
     b. Credit account:
     ```sql
     UPDATE accounts SET balance = balance + ? WHERE account_id = ?
     ```
     c. Log transaction:
     ```sql
     INSERT INTO transactions (account_id, transaction_type, amount, description, date) 
     VALUES (?, 'Loan Disbursed', ?, 'Loan Approval Credit', NOW())
     ```
  4. **If Rejected**:
     - Only update loan status to 'Rejected'

**Integration:**
- Loan approval immediately credits client's account
- Transaction log maintains complete audit trail

---

### 8. Transaction History & Audit

**How it's implemented:**
- **Client View** (TransactionHistoryController.java):
  ```sql
  SELECT t.date, t.transaction_type, t.amount, t.description 
  FROM transactions t 
  JOIN accounts a ON t.account_id = a.account_id 
  JOIN customers c ON a.customer_id = c.customer_id 
  WHERE c.user_id = ? 
  ORDER BY t.date DESC
  ```
  - Shows only user's own transactions
  - Format: Date, Type, Amount (ETB), Description

- **Admin Audit Log** (AdminTransactionsController.java):
  ```sql
  SELECT t.transaction_id, a.account_number, t.transaction_type, t.amount, t.date, t.description 
  FROM transactions t 
  LEFT JOIN accounts a ON t.account_id = a.account_id 
  ORDER BY t.date DESC
  ```
  - Shows ALL system transactions
  - Handles deleted accounts gracefully (LEFT JOIN)
  - Export to CSV functionality available

**Export Feature:**
- Generates CSV file "Transaction_Report.csv"
- Contains all visible transactions with headers

---

### 9. Profile Management & Password Change

**How it's implemented:**
- **Two-Section Interface** (UpdateProfileController.java, update_profile.fxml):

#### Section 1: Personal Information Update
```sql
UPDATE customers 
SET email=?, phone_number=?, address=? 
WHERE user_id=?
```

#### Section 2: Password Change
1. User enters current password, new password, and confirmation
2. **Validation**:
   - All fields required
   - New password and confirmation must match
3. **Verification**:
   ```sql
   SELECT password_hash FROM users WHERE user_id = ?
   ```
   - Compare entered current password with stored hash
4. **Update**:
   ```sql
   UPDATE users SET password_hash = ? WHERE user_id = ?
   ```
5. Clear password fields after successful change

**Security:**
- Current password verification prevents unauthorized changes
- Password confirmation reduces typos

---

### 10. Admin - Client Management

**How it's implemented:**
- **Client List Display** (AdminCustomerListController.java):
  ```sql
  SELECT u.user_id, c.first_name, c.last_name, c.email, c.phone_number, 
         a.account_number, a.balance, u.status 
  FROM customers c 
  JOIN users u ON c.user_id = u.user_id 
  LEFT JOIN accounts a ON c.customer_id = a.customer_id
  ```
  - TableView shows comprehensive client information
  - Balance displayed in ETB format

**Available Actions:**

#### 10.1 Toggle Active/Inactive Status
- **Implementation**:
  ```sql
  UPDATE users SET status = ? WHERE user_id = ?
  ```
  - Toggles between 'active' and 'inactive'
  - Inactive users cannot log in
  - Status displayed with color coding (green/red)

#### 10.2 Password Reset
- **Implementation**:
  ```sql
  UPDATE users SET password_hash = 'Nigus@123' WHERE user_id = ?
  ```
  - Resets client password to default "Nigus@123"
  - Used when client forgets password
  - Displays confirmation message with new password

**Case Study: Make Client Active/Inactive Flow**

This functionality demonstrates the system's state management:

1. **Initial State**: Client record displayed in table with current status
2. **Admin Action**: Clicks "Toggle Freeze/Unfreeze" button
3. **Validation**: System checks if a row is selected
4. **State Retrieval**:
   ```java
   String currentStatus = selected.get(6); // Get status from table column
   ```
5. **State Toggle Logic**:
   ```java
   String newStatus = currentStatus.equalsIgnoreCase("active") ? "inactive" : "active";
   ```
6. **Database Update**:
   ```sql
   UPDATE users SET status = ? WHERE user_id = ?
   ```
7. **UI Update**:
   - Status label updated with new status
   - Color changes (green for active, red for inactive)
   - Table automatically refreshed via `loadClients()`
8. **System Effect**:
   - If status changed to 'inactive', user cannot log in
   - If status changed to 'active', user can log in again
9. **Error Handling**: Database errors displayed to admin

**Integration Points:**
- Login system checks status during authentication
- Client list view reflects real-time status
- Audit trail could be added by logging status changes

---

### 11. Admin Dashboard & System Statistics

**How it's implemented:**
- **Statistics Display** (DashboardController.java):

1. **Total Clients Count**:
   ```sql
   SELECT COUNT(*) FROM users WHERE role = 'client'
   ```

2. **Total Liquidity (System Balance)**:
   ```sql
   SELECT SUM(balance) FROM accounts
   ```
   - Displayed in ETB format
   - Represents total money in the banking system

3. **Active Accounts Count**:
   ```sql
   SELECT COUNT(*) FROM users WHERE status = 'active' AND role = 'client'
   ```

**Admin Functions:**

#### 11.1 Create New Admin
- Dialog box input for username and password
- ```sql
  INSERT INTO users (username, password_hash, role, status) 
  VALUES (?, ?, 'admin', 'active')
  ```

#### 11.2 Month-End Interest Processing
- **Simulation of banking operations**:
  1. Admin enters interest rate (e.g., 2.5%)
  2. System calculates interest for all accounts with positive balance
  3. **Batch Update Transaction**:
     ```java
     conn.setAutoCommit(false);
     // For each account with balance > 0:
     double interest = balance * (ratePercentage / 100.0);
     ```
     a. Update balance:
     ```sql
     UPDATE accounts SET balance = balance + ? WHERE account_id = ?
     ```
     b. Log interest credit:
     ```sql
     INSERT INTO transactions (account_id, transaction_type, amount, description, date) 
     VALUES (?, 'Interest Credit', ?, 'Monthly Interest (2.5%)', NOW())
     ```
  4. Commit all updates atomically
  5. Display count of processed accounts

**Performance:**
- Uses batch processing for multiple accounts
- Single transaction ensures data consistency

---

## Database Schema Integration

### Entity Relationships:

```
users (user_id) ─┬─── 1:1 ───> customers (user_id, customer_id)
                 │
                 └─── controls authentication

customers (customer_id) ───── 1:N ───> accounts (customer_id, account_id)

accounts (account_id) ──┬─── 1:N ───> transactions (account_id)
                        │
                        └─── 1:N ───> loans (account_id)

branches (branch_id) ───── 1:N ───> accounts (branch_id)
```

### Key Foreign Key Constraints:
- `customers.user_id` → `users.user_id`
- `accounts.customer_id` → `customers.customer_id`
- `accounts.branch_id` → `branches.branch_id`
- `transactions.account_id` → `accounts.account_id`
- `loans.account_id` → `accounts.account_id`

---

## Application Flow & Module Integration

### 1. Application Startup (App.java)
```java
start(Stage stage) {
    scene = new Scene(loadFXML("login"), 1280, 800);
    stage.setMaximized(true); // Full screen mode
    stage.setTitle("Nigus Bank - ንጉስ ባንክ");
    stage.show();
}
```

### 2. Authentication Flow
```
login.fxml 
  → LoginController.handleLogin() 
  → DBConnection.getConnection() 
  → SQL: SELECT user_id, password_hash, role, status FROM users WHERE username = ?
  → If role = 'admin': dashboard.fxml + DashboardController
  → If role = 'client': client_dashboard.fxml + ClientDashboardController
```

### 3. Client Transaction Flow
```
Client Dashboard 
  → User clicks "Deposit Funds" 
  → ClientDashboardController.showDeposit() 
  → loadView("deposit.fxml") 
  → FXMLLoader loads DepositController 
  → setSession(userId, username, accountId, accountNumber, currentBalance) 
  → User enters amount 
  → DepositController.handleDeposit() 
  → Database transaction (UPDATE accounts + INSERT transactions) 
  → goBackToDashboard() 
  → Reload ClientDashboardController with updated balance
```

### 4. Data Refresh Strategy
- Controllers reload data from database when views are switched
- `loadAccountData()` called before displaying each financial view
- Ensures real-time balance display
- Transaction history auto-refreshes after operations

---

## UI/UX Design Patterns

### 1. Full Screen Mode
- **Implementation**: `stage.setMaximized(true)` in App.java
- Ensures application covers entire screen
- Consistent across all scenes

### 2. Standardized Logout Button
- **Styling**: Both admin and client dashboards use identical button:
  ```xml
  <Button maxWidth="Infinity" onAction="#handleLogout" 
          styleClass="logout-button" text="Log Out">
     <padding><Insets bottom="15.0" top="15.0" /></padding>
     <VBox.margin><Insets bottom="20.0" left="20.0" right="20.0" /></VBox.margin>
  </Button>
  ```
- Text: "Log Out"
- Padding: 15px top and bottom
- Margin: 20px all around

### 3. Currency Display
- **Format**: ETB (Ethiopian Birr)
- **Implementation**: `String.format("ETB %.2f", amount)`
- Applied consistently across all monetary displays

### 4. CSS Styling
- Centralized in `/css/` directory
- Reusable style classes: `nav-button`, `logout-button`, `card`, `action-button`
- Consistent color scheme and spacing

---

## Security Considerations

### Current Implementation:
1. **Password Storage**: Plain text (SHOULD BE HASHED - use BCrypt in production)
2. **SQL Injection Prevention**: Uses PreparedStatement throughout
3. **Session Management**: User ID and role passed between controllers
4. **Access Control**: Role-based routing (admin vs client)
5. **Status Validation**: Inactive users prevented from login

### Production Recommendations:
- Implement password hashing (BCrypt/Argon2)
- Add session timeout
- Implement HTTPS for database connection
- Add input sanitization
- Implement CAPTCHA for login
- Add two-factor authentication

---

## Error Handling Strategy

### Database Operations:
```java
Connection conn = null;
try {
    conn = DBConnection.getConnection();
    conn.setAutoCommit(false);
    // ... operations ...
    conn.commit();
} catch (SQLException e) {
    if (conn != null) {
        try { conn.rollback(); } catch (SQLException ex) {}
    }
    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
    e.printStackTrace();
} finally {
    if (conn != null) {
        try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
    }
}
```

### User Feedback:
- Alert dialogs for success/error messages
- Status labels for inline feedback
- Color-coded messages (green = success, red = error)

---

## Testing Scenarios

### 1. Registration Test:
- Input: Valid user data
- Expected: User created with default password "Nigus@123", account created, redirect to login

### 2. Login Test:
- Input: Valid credentials for active user
- Expected: Redirect to appropriate dashboard

### 3. Deposit Test:
- Input: ETB 1000
- Expected: Balance increases by 1000, transaction logged, confirmation shown

### 4. Transfer Test:
- Input: Valid recipient, amount <= balance
- Expected: Sender balance decreases, recipient balance increases, both transactions logged

### 5. Loan Application Test:
- Input: Personal loan, ETB 5000
- Expected: Loan created with 'Pending' status, appears in history

### 6. Admin Toggle Status Test:
- Input: Select active client, click toggle
- Expected: Client status changes to 'inactive', cannot log in

### 7. Password Change Test:
- Input: Correct current password, matching new passwords
- Expected: Password updated, confirmation shown, fields cleared

---

## Technology Stack

- **Frontend**: JavaFX 13
- **Backend**: Java 11
- **Database**: MySQL 8.0.33
- **Build Tool**: Maven
- **IDE**: Any Java IDE (NetBeans, IntelliJ, Eclipse)

---

## Conclusion

The Banking Management System demonstrates a complete banking application with:
- Secure authentication and authorization
- Comprehensive transaction management
- Administrative controls
- Real-time balance updates
- Complete audit trail
- User-friendly interface
- Database-driven architecture

The system successfully integrates multiple modules through a centralized database, ensuring data consistency through transaction management and providing both client and administrative functionalities in a single application.

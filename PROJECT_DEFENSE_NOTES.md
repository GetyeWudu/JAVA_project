# PROJECT DEFENSE NOTES - BankMS

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [System Architecture](#system-architecture)
4. [Core Features](#core-features)
5. [Feature Implementation Details](#feature-implementation-details)
6. [Database Schema](#database-schema)
7. [Security Features](#security-features)
8. [Testing Guide](#testing-guide)
9. [Deployment Instructions](#deployment-instructions)
10. [PPT Outline](#ppt-outline)

---

## Project Overview

**BankMS (Bank Management System)** is a JavaFX-based desktop application for managing banking operations. The system provides separate interfaces for administrators and clients, with comprehensive features for account management, transactions, loans, and administrative oversight.

### Key Objectives
- Secure user authentication and authorization
- Comprehensive transaction management (deposits, withdrawals, transfers)
- Loan application and approval workflow
- Admin tools for client management and system oversight
- Audit trail for all banking operations

---

## Technology Stack

### Frontend
- **JavaFX 13**: Desktop UI framework
- **FXML**: UI markup language
- **CSS**: Styling and theming

### Backend
- **Java 11**: Core programming language
- **JDBC**: Database connectivity
- **Maven**: Build and dependency management

### Database
- **MySQL 8.0**: Relational database management system

### Design Patterns
- **MVC (Model-View-Controller)**: Separation of concerns
- **DAO Pattern**: Database access abstraction
- **Session Management**: User state persistence

---

## System Architecture

### Application Structure
```
BankMS/
├── src/main/java/com/mycompany/bankms/
│   ├── Controllers/
│   │   ├── LoginController.java
│   │   ├── DashboardController.java (Admin)
│   │   ├── ClientDashboardController.java
│   │   ├── RegisterController.java
│   │   ├── AdminClientsController.java
│   │   ├── AdminCustomerListController.java
│   │   ├── TransactionControllers/ (Deposit, Withdraw, Transfer)
│   │   └── LoanController.java
│   ├── DBConnection.java
│   └── PasswordMigrationScript.java
├── src/main/resources/
│   ├── com/mycompany/bankms/ (FXML files)
│   └── css/ (Stylesheets)
└── db_migration_password_reset.sql
```

### Database Schema Overview
- **users**: Authentication and user management
- **customers**: Client profile information
- **accounts**: Bank account details
- **transactions**: Transaction history and audit trail
- **loans**: Loan applications and approvals
- **branches**: Branch information

---

## Core Features

### 1. User Authentication & Authorization
- **Login System**: Secure credential verification
- **Role-Based Access**: Admin vs. Client permissions
- **Password Management**: Change password with validation
- **Account Status**: Active/Inactive/Frozen states

### 2. Client Management (Admin)
- Create new client accounts
- View all client information
- Activate/Deactivate accounts
- Reset client passwords
- Audit trail logging

### 3. Transaction Management
- **Deposits**: Add funds to accounts
- **Withdrawals**: Remove funds with balance validation
- **Transfers**: Send money between accounts
- **Transaction History**: Complete audit trail

### 4. Loan Management
- Client loan applications
- Admin loan approval/rejection
- Automatic fund disbursement
- Loan status tracking

### 5. Account Management
- View account details and balance
- Update profile information
- Transaction history reports
- Account statement generation

### 6. Administrative Tools
- Dashboard with system statistics
- Client list with search/filter
- Transaction audit log
- Month-end processing (interest calculation)
- Admin user creation

---

## Feature Implementation Details

### 1. User Registration

**User Perspective**: New users can self-register through the registration form, providing personal details and creating login credentials.

**Implementation**:
- **File**: `RegisterController.java`
- **FXML**: `register.fxml`
- **Database Tables**: `users`, `customers`, `accounts`, `branches`

**Data Flow**:
1. User fills registration form (name, username, email, phone, address)
2. Controller validates input fields
3. Transaction begins
4. Create user record with role='client' and default password "Nigus@123"
5. Create customer profile linked to user
6. Generate account number and create savings account
7. Transaction commits or rolls back on error
8. User redirected to login page

**Code Path**:
```java
RegisterController.handleRegister()
  → DBConnection.getConnection()
  → INSERT INTO users (mustChangePassword=TRUE, password='Nigus@123')
  → INSERT INTO customers
  → Generate account number
  → INSERT INTO accounts
  → commit()
```

**Testing**:
1. Launch application
2. Click "Register" button
3. Fill all fields with valid data
4. Click "Create Account"
5. Verify success message with account number
6. Try logging in with created credentials
7. Verify forced password change on first login

---

### 2. User Login & Authentication

**User Perspective**: Users enter username and password to access the system.

**Implementation**:
- **File**: `LoginController.java`
- **FXML**: `login.fxml`
- **Database Tables**: `users`

**Data Flow**:
1. User enters credentials
2. Query database for username
3. Verify password matches
4. Check account status (active/inactive/frozen)
5. Check mustChangePassword flag
6. Load appropriate dashboard (admin/client) or force password change

**Code Path**:
```java
LoginController.handleLogin()
  → Query users table
  → Validate password
  → Check status
  → if (mustChangePassword) → loadPasswordChangePage()
  → else if (role == 'admin') → loadAdminDashboard()
  → else → loadClientDashboard()
```

**Security Features**:
- Password stored as-is (Note: In production, use bcrypt hashing)
- Account status verification
- Role-based dashboard loading
- Forced password change for new accounts

**Testing**:
1. Launch application
2. Enter valid credentials
3. Verify correct dashboard loads
4. Test with inactive account (should be blocked)
5. Test with new account (should force password change)

---

### 3. Client Dashboard

**User Perspective**: Clients see their account overview, balance, and can perform banking operations.

**Implementation**:
- **File**: `ClientDashboardController.java`
- **FXML**: `client_dashboard.fxml`
- **CSS**: `client.css`

**Features**:
- Account overview
- Quick balance display
- Navigation to all client features
- Session management

**Data Flow**:
1. Load user session data
2. Query account information
3. Display current balance
4. Provide navigation to sub-features
5. Each sub-screen refreshes balance on return

**Code Path**:
```java
setUserSession(userId, username)
  → loadAccountData()
  → Query accounts table
  → Store accountId, accountNumber, currentBalance
  → showAccountDetails() (default view)
```

**Testing**:
1. Login as client
2. Verify welcome message shows username
3. Verify balance displays correctly
4. Click each menu option (Deposit, Withdraw, Transfer, etc.)
5. Verify navigation works
6. Perform transaction and return to verify balance updates

---

### 4. Deposit Funds

**User Perspective**: Clients can add money to their account through the deposit feature.

**Implementation**:
- **File**: `DepositController.java`
- **FXML**: `deposit.fxml`
- **Database Tables**: `accounts`, `transactions`

**Data Flow**:
1. Display current balance
2. User enters deposit amount
3. Validate amount (positive number)
4. Begin transaction
5. Update account balance (ADD)
6. Log transaction record
7. Commit transaction
8. Show success message
9. Return to dashboard with updated balance

**Code Path**:
```java
DepositController.handleDeposit()
  → Validate amount > 0
  → conn.setAutoCommit(false)
  → UPDATE accounts SET balance = balance + amount
  → INSERT INTO transactions (type='deposit')
  → conn.commit()
  → goBackToDashboard()
```

**Business Rules**:
- Amount must be positive
- No maximum deposit limit
- Transaction logged for audit
- Real-time balance update

**Testing**:
1. Navigate to "Deposit Funds"
2. Enter amount (e.g., 1000.00)
3. Click "Confirm Deposit"
4. Verify success message
5. Return to dashboard
6. Verify balance increased by deposit amount
7. Check transaction history to confirm entry

---

### 5. Withdraw Funds

**User Perspective**: Clients can withdraw money from their account with balance validation.

**Implementation**:
- **File**: `WithdrawController.java`
- **FXML**: `withdraw.fxml`
- **Database Tables**: `accounts`, `transactions`

**Data Flow**:
1. Display current balance
2. User enters withdrawal amount
3. Validate amount (positive and ≤ balance)
4. Begin transaction
5. Update account balance (SUBTRACT)
6. Log transaction record
7. Commit transaction
8. Show success message

**Code Path**:
```java
WithdrawController.handleWithdraw()
  → Validate amount > 0 AND amount <= currentBalance
  → conn.setAutoCommit(false)
  → UPDATE accounts SET balance = balance - amount
  → INSERT INTO transactions (type='Withdraw')
  → conn.commit()
```

**Business Rules**:
- Amount must be positive
- Cannot withdraw more than available balance
- Overdraft not allowed
- Transaction logged for audit

**Testing**:
1. Navigate to "Withdraw Funds"
2. Try withdrawing more than balance (should fail)
3. Enter valid amount (e.g., 500.00)
4. Click "Confirm Withdrawal"
5. Verify success message
6. Verify balance decreased correctly
7. Check transaction history

---

### 6. Transfer Money

**User Perspective**: Clients can transfer money to other accounts using account numbers.

**Implementation**:
- **File**: `TransferController.java`
- **FXML**: `transfer.fxml`
- **Database Tables**: `accounts`, `transactions`

**Data Flow**:
1. Display sender's balance
2. User enters recipient account number and amount
3. Validate recipient exists
4. Validate sufficient balance
5. Validate not transferring to self
6. Begin transaction
7. Deduct from sender account
8. Add to recipient account
9. Log two transaction records (debit and credit)
10. Commit transaction

**Code Path**:
```java
TransferController.handleTransfer()
  → Validate recipient account exists
  → Validate amount <= currentBalance
  → Validate targetAccountId != accountId
  → conn.setAutoCommit(false)
  → UPDATE accounts SET balance = balance - amount WHERE account_id = sender
  → UPDATE accounts SET balance = balance + amount WHERE account_id = recipient
  → INSERT INTO transactions (type='Transfer Out')
  → INSERT INTO transactions (type='Transfer In')
  → conn.commit()
```

**Business Rules**:
- Recipient must exist
- Sufficient balance required
- Cannot transfer to own account
- Atomic transaction (both debit and credit succeed or both fail)
- Dual transaction logging

**Testing**:
1. Create two test accounts or use existing
2. Navigate to "Transfer Money"
3. Enter recipient account number
4. Enter amount
5. Click "Send Money"
6. Verify success message
7. Check both accounts' balances updated correctly
8. Verify transaction history shows transfer on both accounts

---

### 7. Loan Application (Client)

**User Perspective**: Clients can apply for various types of loans with instant submission.

**Implementation**:
- **File**: `LoanController.java`
- **FXML**: `loans.fxml`
- **Database Tables**: `loans`, `accounts`

**Data Flow**:
1. Display loan application form
2. User selects loan type and enters amount
3. Validate amount against business rules
4. Check account history/balance for eligibility
5. Insert loan record with status='Pending'
6. Display application history

**Code Path**:
```java
LoanController.handleApply()
  → Validate amount > 0
  → Apply business rules (Personal: max 10,000 ETB, Car: max 50,000 ETB)
  → Check account history (balance >= 50 for large loans)
  → INSERT INTO loans (status='Pending')
  → loadLoanHistory()
```

**Business Rules**:
- Personal Loan: Maximum 10,000 ETB
- Car Loan: Maximum 50,000 ETB
- Account history check: Balance must be >= 50 for loans > 1,000 ETB
- All loans start as 'Pending'

**Testing**:
1. Navigate to "Request Loan"
2. Select loan type (e.g., Personal Loan)
3. Enter amount (e.g., 5000.00)
4. Click "Submit Application"
5. Verify success message
6. Check application appears in history table
7. Verify status shows "Pending"

---

### 8. Loan Approval (Admin)

**User Perspective**: Administrators review and approve/reject loan applications.

**Implementation**:
- **File**: `AdminLoanController.java`
- **FXML**: `admin_loans.fxml`
- **Database Tables**: `loans`, `accounts`, `transactions`

**Data Flow**:
1. Display all loan applications (pending first)
2. Admin selects application
3. Admin clicks Approve or Reject
4. Update loan status
5. If approved: disburse funds to account and log transaction
6. Commit changes
7. Refresh loan list

**Code Path**:
```java
AdminLoanController.processLoan(decision)
  → Validate loan is 'Pending'
  → conn.setAutoCommit(false)
  → UPDATE loans SET status = decision
  → if (decision == 'Approved'):
      → UPDATE accounts SET balance = balance + amount
      → INSERT INTO transactions (type='Loan Disbursed')
  → conn.commit()
```

**Business Rules**:
- Only pending loans can be processed
- Approved loans automatically credit account
- Rejection has no fund movement
- All actions logged

**Testing**:
1. Create loan application as client
2. Login as admin
3. Navigate to "Loan Requests"
4. Select pending loan
5. Click "Approve"
6. Verify loan status changes to "Approved"
7. Verify client account balance increased
8. Check transaction log for loan disbursement entry

---

### 9. Client Management (Admin - Create Client)

**User Perspective**: Admins can create new client accounts with initial deposits.

**Implementation**:
- **File**: `AdminClientsController.java`
- **FXML**: `admin_clients.fxml`
- **Database Tables**: `users`, `customers`, `accounts`, `transactions`

**Data Flow**:
1. Admin fills client creation form
2. Validate required fields
3. Begin transaction
4. Create user with default password "Nigus@123"
5. Create customer profile
6. Generate account number
7. Create account with initial deposit
8. Log initial deposit transaction
9. Commit transaction

**Code Path**:
```java
AdminClientsController.handleCreateClient()
  → Validate inputs
  → conn.setAutoCommit(false)
  → Check username uniqueness
  → INSERT INTO users (password='Nigus@123', mustChangePassword=TRUE)
  → INSERT INTO customers
  → Generate random 8-digit account number
  → INSERT INTO accounts (balance=initialDeposit)
  → if (initialDeposit > 0):
      → INSERT INTO transactions (type='Deposit', description='Initial Deposit')
  → conn.commit()
```

**Business Rules**:
- Username must be unique
- Default password is always "Nigus@123"
- mustChangePassword flag set to TRUE
- Account number is random 8-digit number
- Initial deposit is optional but must be >= 0

**Testing**:
1. Login as admin
2. Navigate to "Manage Clients" → "Add New Client"
3. Fill all required fields
4. Enter initial deposit (e.g., 1000.00)
5. Click "Create Account"
6. Verify success message
7. Check client appears in customer list
8. Login as new client (should force password change)

---

### 10. Client Management (Admin - View/Manage)

**User Perspective**: Admins can view all clients, activate/deactivate accounts, and reset passwords.

**Implementation**:
- **File**: `AdminCustomerListController.java`
- **FXML**: `admin_customer_list.fxml`
- **Database Tables**: `users`, `customers`, `accounts`, `transactions`

**Features**:
- View all clients with details
- Toggle account status (Active/Inactive)
- Reset client passwords
- Audit logging

**Data Flow**:
1. Load all clients with JOIN query
2. Display in table format
3. Admin selects client
4. Admin performs action (toggle status or reset password)
5. Update database
6. Log action in audit trail
7. Refresh client list

**Code Path - Toggle Status**:
```java
handleToggleFreeze()
  → Get selected client
  → Determine new status (active ↔ inactive)
  → UPDATE users SET status = newStatus
  → Refresh table
```

**Code Path - Reset Password**:
```java
handleResetPassword()
  → Get selected client
  → conn.setAutoCommit(false)
  → UPDATE users SET password_hash='Nigus@123', mustChangePassword=TRUE
  → INSERT INTO transactions (audit log entry)
  → conn.commit()
```

**Business Rules**:
- Password always resets to "Nigus@123"
- mustChangePassword flag always set on reset
- All admin actions logged for audit
- Status toggles between active and inactive

**Testing - Activate/Deactivate**:
1. Login as admin
2. Navigate to "Manage Clients"
3. Select a client
4. Click "Toggle Freeze"
5. Verify status changes
6. Try logging in as that client
7. If inactive, should see "Account Locked" message

**Testing - Password Reset**:
1. Select a client
2. Click "Reset Password"
3. Verify success message shows "Nigus@123"
4. Login as that client
5. Verify forced password change
6. Check audit log for reset entry

---

### 11. Update Profile (Client)

**User Perspective**: Clients can update their contact information and change passwords.

**Implementation**:
- **File**: `UpdateProfileController.java`
- **FXML**: `update_profile.fxml`
- **Database Tables**: `customers`, `users`

**Features**:
- Update email, phone, address
- Change password with validation
- Password strength requirements

**Data Flow - Profile Update**:
1. Load current profile data
2. User modifies fields
3. Click "Save Changes"
4. Update customer record
5. Show success/error message

**Data Flow - Password Change**:
1. User enters current password
2. Enters new password and confirmation
3. Validate password matches
4. Validate password strength (8+ chars, upper, lower, digit, special)
5. Verify current password correct
6. Update password
7. Clear mustChangePassword flag
8. Show success message

**Code Path - Password Change**:
```java
handleChangePassword()
  → Validate all fields filled
  → Validate new == confirm
  → validatePassword(newPass)
      → Length >= 8
      → Contains uppercase [A-Z]
      → Contains lowercase [a-z]
      → Contains digit [0-9]
      → Contains special [!@#$%^&*(),.?":{}|<>]
  → Query current password
  → Verify currentPass matches DB
  → UPDATE users SET password_hash=newPass, mustChangePassword=FALSE
```

**Password Requirements**:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

**Testing - Profile Update**:
1. Login as client
2. Navigate to "My Profile"
3. Update email/phone/address
4. Click "Save Changes"
5. Verify success message
6. Logout and login again
7. Verify changes persisted

**Testing - Password Change**:
1. Navigate to password change section
2. Try weak password (should fail with specific message)
3. Try wrong current password (should fail)
4. Try mismatched confirm (should fail)
5. Enter valid password meeting all criteria
6. Verify success
7. Logout and login with new password

---

### 12. Admin Dashboard

**User Perspective**: Administrators see system overview with key metrics.

**Implementation**:
- **File**: `DashboardController.java`
- **FXML**: `dashboard.fxml`
- **Database Tables**: `users`, `accounts`

**Metrics Displayed**:
- Total number of clients
- Total system liquidity (sum of all balances)
- Active accounts count

**Additional Features**:
- Create new admin users
- Month-end processing (interest calculation)
- Navigation to all admin features

**Data Flow**:
1. Load admin session
2. Query system statistics
3. Calculate totals
4. Display in dashboard cards
5. Provide navigation menu

**Code Path**:
```java
loadSystemStats()
  → SELECT COUNT(*) FROM users WHERE role='client'
  → SELECT SUM(balance) FROM accounts
  → SELECT COUNT(*) FROM users WHERE status='active' AND role='client'
  → Display values in UI labels
```

**Month-End Processing**:
```java
handleSimulateMonthEnd()
  → Admin enters interest rate (e.g., 2.5%)
  → Query all accounts with balance > 0
  → For each account:
      → Calculate interest = balance * rate
      → UPDATE balance
      → INSERT transaction log
  → Commit batch
  → Show processed count
```

**Testing**:
1. Login as admin
2. Verify statistics display correctly
3. Test "Create New Admin" dialog
4. Enter username/password for new admin
5. Logout and login with new admin credentials
6. Test month-end processing
7. Enter interest rate
8. Verify all account balances increased
9. Check transaction log

---

### 13. Transaction History & Audit

**User Perspective**: View complete history of all transactions.

**Implementation (Client)**:
- **File**: `TransactionHistoryController.java`
- **FXML**: `transaction_history.fxml`

**Implementation (Admin)**:
- **File**: `AdminTransactionsController.java`
- **FXML**: `admin_transactions.fxml`

**Data Flow**:
1. Query transactions table
2. JOIN with accounts for account numbers
3. ORDER BY date DESC (newest first)
4. Display in table view

**Client View**:
- Shows only transactions for logged-in user's accounts
- Includes: date, type, amount (ETB), description

**Admin View**:
- Shows ALL transactions system-wide
- Includes: transaction ID, account number, type, amount (ETB), date, description
- Export to CSV functionality

**Code Path - Client**:
```java
loadHistory()
  → SELECT t.date, t.transaction_type, t.amount, t.description
    FROM transactions t
    JOIN accounts a ON t.account_id = a.account_id
    JOIN customers c ON a.customer_id = c.customer_id
    WHERE c.user_id = ?
    ORDER BY t.date DESC
  → Populate TableView
```

**Testing**:
1. Perform various transactions (deposit, withdraw, transfer)
2. Navigate to "Transaction History"
3. Verify all transactions appear
4. Verify sorted by date (newest first)
5. Verify amounts show ETB suffix
6. As admin, export CSV and verify file created

---

### 14. Password Migration Script

**Purpose**: One-time script to reset all existing client passwords to default.

**Implementation**:
- **Java**: `PasswordMigrationScript.java`
- **SQL**: `db_migration_password_reset.sql`

**What It Does**:
1. Adds mustChangePassword column to users table (if not exists)
2. Resets all client passwords to "Nigus@123"
3. Sets mustChangePassword=TRUE for all clients
4. Creates audit log entries

**Usage**:
```bash
# Method 1: SQL Script
mysql -u root -p bankms < db_migration_password_reset.sql

# Method 2: Java Program
cd /home/runner/work/JAVA_project/JAVA_project
mvn compile
java -cp target/classes:path/to/mysql-connector.jar com.mycompany.bankms.PasswordMigrationScript
```

**Expected Output**:
```
Starting password migration...
Added mustChangePassword column to users table
Found 15 client accounts
Updated 15 client passwords to default
Created 15 audit log entries

Migration completed successfully!
All client passwords have been reset to: Nigus@123
Clients will be required to change their password on next login.
```

---

## Database Schema

### users
```sql
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'client') NOT NULL,
    status ENUM('active', 'inactive', 'frozen') DEFAULT 'active',
    mustChangePassword BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### customers
```sql
CREATE TABLE customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### accounts
```sql
CREATE TABLE accounts (
    account_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    branch_id INT,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    account_type ENUM('savings', 'checking', 'business') DEFAULT 'savings',
    balance DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('active', 'closed') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (branch_id) REFERENCES branches(branch_id)
);
```

### transactions
```sql
CREATE TABLE transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    description TEXT,
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);
```

### loans
```sql
CREATE TABLE loans (
    loan_id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    loan_type VARCHAR(50) NOT NULL,
    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);
```

### branches
```sql
CREATE TABLE branches (
    branch_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    state VARCHAR(50),
    zip_code VARCHAR(10)
);
```

---

## Security Features

### 1. Authentication
- Username/password verification
- Session management
- Role-based access control (RBAC)

### 2. Authorization
- Admin vs. Client role separation
- Feature-level access control
- Account status enforcement

### 3. Password Security
- Default password with forced change on first login
- Password strength validation
- mustChangePassword flag for security
- Admin password reset capability

### 4. Transaction Security
- ACID compliance with database transactions
- Rollback on error
- Balance validation before withdrawal/transfer
- Atomic transfer operations

### 5. Audit Trail
- All transactions logged with timestamps
- Admin actions logged
- Password resets logged
- Complete transaction history

### 6. Input Validation
- SQL injection prevention (PreparedStatement)
- Amount validation (positive numbers)
- Account existence verification
- Balance sufficiency checks

---

## Testing Guide

### Manual Testing Checklist

#### User Management
- [ ] Register new user
- [ ] Login with valid credentials
- [ ] Login with invalid credentials
- [ ] Login with inactive account
- [ ] Force password change on first login
- [ ] Change password successfully
- [ ] Fail password change with weak password

#### Client Operations
- [ ] View account details
- [ ] Deposit funds
- [ ] Withdraw funds (sufficient balance)
- [ ] Withdraw funds (insufficient balance - should fail)
- [ ] Transfer to valid account
- [ ] Transfer to invalid account (should fail)
- [ ] Transfer to self (should fail)
- [ ] Apply for loan
- [ ] View loan status
- [ ] View transaction history
- [ ] Update profile information

#### Admin Operations
- [ ] View dashboard statistics
- [ ] Create new client
- [ ] View client list
- [ ] Activate/Deactivate client
- [ ] Reset client password
- [ ] View all transactions
- [ ] Approve loan
- [ ] Reject loan
- [ ] Run month-end processing
- [ ] Create new admin
- [ ] Export transaction report

### Test Data

**Admin Credentials**:
- Username: `admin`
- Password: `admin123` (or as configured in database)

**Test Client** (after migration):
- Username: (any existing client)
- Password: `Nigus@123`
- Must change on first login

---

## Deployment Instructions

### Prerequisites
- Java 11 or higher
- MySQL 8.0 or higher
- Maven 3.6+
- JavaFX SDK 13

### Database Setup
```bash
# 1. Create database
mysql -u root -p
CREATE DATABASE bankms;
USE bankms;

# 2. Import schema (create tables)
SOURCE path/to/schema.sql;

# 3. Run migration script
SOURCE db_migration_password_reset.sql;
```

### Application Build
```bash
# 1. Clone repository
git clone <repository-url>
cd JAVA_project

# 2. Build with Maven
mvn clean install

# 3. Run application
mvn javafx:run
```

### Configuration
Update `DBConnection.java` with your database credentials:
```java
private static final String URL = "jdbc:mysql://localhost:3306/bankms";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

### Running Migration Script
```bash
# After initial deployment, run migration once
java -cp target/classes:path/to/mysql-connector.jar \
  com.mycompany.bankms.PasswordMigrationScript
```

---

## PPT Outline

### Slide 1: Title
- **BankMS - Bank Management System**
- Student Name
- Date
- Institution

### Slide 2: Project Overview
- Desktop banking application
- JavaFX + MySQL
- Admin and Client portals
- Full-featured transaction management

### Slide 3: Technology Stack
- Java 11
- JavaFX 13
- MySQL 8.0
- Maven
- JDBC

### Slide 4: System Architecture
- MVC Pattern
- Database-driven
- Session management
- Role-based access

### Slide 5: Key Features
- User Authentication
- Transaction Management (Deposit, Withdraw, Transfer)
- Loan Management
- Admin Tools
- Audit Trail

### Slide 6: User Registration Flow
- Self-registration
- Default password assignment
- Automatic account creation
- Account number generation

### Slide 7: Authentication & Security
- Login verification
- Role-based access (Admin/Client)
- Password strength validation
- Forced password change
- Account status enforcement

### Slide 8: Client Features
- Account overview
- Balance checking
- Deposits and withdrawals
- Money transfers
- Loan applications
- Transaction history
- Profile management

### Slide 9: Transaction Management
- ACID compliance
- Real-time balance updates
- Dual-entry for transfers
- Complete audit trail
- ETB currency formatting

### Slide 10: Loan Management
- Client applications
- Business rule enforcement
- Admin approval workflow
- Automatic disbursement
- Status tracking

### Slide 11: Admin Dashboard
- System statistics
- Client count
- Total liquidity
- Active accounts
- Quick actions

### Slide 12: Admin Client Management
- Create new clients
- View all clients
- Activate/Deactivate accounts
- Password resets
- Audit logging

### Slide 13: Admin Operations
- Loan approvals
- Transaction audit
- Month-end processing
- Interest calculation
- Admin user creation

### Slide 14: Database Schema
- users (authentication)
- customers (profiles)
- accounts (balances)
- transactions (audit)
- loans (applications)
- branches (locations)

### Slide 15: Security Features
- Input validation
- SQL injection prevention
- Transaction integrity
- Audit trail
- Password policies

### Slide 16: Password Management
- Default password: "Nigus@123"
- mustChangePassword flag
- Strength validation
- Admin reset capability
- Migration script

### Slide 17: Currency Standardization
- All amounts in ETB (Ethiopian Birr)
- Consistent formatting
- "X,XXX.XX ETB" display
- Updated across all screens

### Slide 18: UI/UX Features
- Full-width layout (1200px minimum)
- Responsive design
- Consistent styling
- Intuitive navigation
- Professional appearance

### Slide 19: Testing & Quality
- Manual testing performed
- Transaction integrity verified
- Security validation
- Edge case handling
- Error management

### Slide 20: Challenges & Solutions
- **Challenge**: Database transaction integrity
- **Solution**: ACID compliance, rollback on error
- **Challenge**: Password security
- **Solution**: Strength validation, forced changes
- **Challenge**: Transfer atomicity
- **Solution**: Single transaction with dual entries

### Slide 21: Future Enhancements
- Password hashing (bcrypt)
- Email notifications
- SMS alerts
- Mobile application
- Report generation
- Statement downloads

### Slide 22: Demo
- Live demonstration of key features
- Login flow
- Transaction execution
- Admin operations
- Password management

### Slide 23: Code Quality
- Clean code principles
- MVC architecture
- Error handling
- Code documentation
- Maintainability

### Slide 24: Deployment
- Database setup steps
- Application build process
- Configuration requirements
- Migration script execution

### Slide 25: Conclusion
- Comprehensive banking solution
- Secure and reliable
- Feature-rich
- Professional grade
- Ready for production (with password hashing)

### Slide 26: Q&A
- Questions and Answers
- Thank You

---

## Additional Notes for Defense

### Common Questions & Answers

**Q: Why isn't password hashing implemented?**
A: The current implementation stores passwords in plain text for demonstration purposes. In a production system, we would use bcrypt or similar hashing algorithms. The structure is in place (password_hash column) to easily add hashing.

**Q: How do you ensure transaction integrity?**
A: We use database transactions with autocommit=false, perform all operations, then commit. If any error occurs, we rollback to maintain data integrity.

**Q: Why use ETB as currency?**
A: Ethiopian Birr (ETB) was chosen to localize the application for the Ethiopian market. The currency is consistently formatted throughout the application.

**Q: How does the loan approval process work?**
A: Clients submit applications which go into 'Pending' status. Admins review and approve/reject. Approved loans automatically credit the client's account and log the transaction.

**Q: What happens if a transfer fails midway?**
A: The entire transfer is wrapped in a database transaction. If deducting from sender succeeds but adding to recipient fails, the transaction is rolled back and no changes are made.

**Q: Can clients have multiple accounts?**
A: Currently, each client has one primary account. The schema supports multiple accounts through the customer_id foreign key.

**Q: How are account numbers generated?**
A: Random 8-digit numbers are generated using Java's Random class. In production, you'd want to ensure uniqueness and perhaps use a more sophisticated algorithm.

**Q: What's the purpose of the mustChangePassword flag?**
A: It forces users to change their password on first login or after admin reset, improving security by ensuring users don't continue using default passwords.

**Q: How do you handle concurrent transactions?**
A: MySQL's InnoDB engine provides row-level locking. Multiple transactions can occur simultaneously on different accounts without conflict.

**Q: Can the system handle multiple branches?**
A: Yes, the branches table exists and accounts are linked to branches via branch_id. The current implementation defaults to branch_id=1.

---

## Maintenance & Support

### Log Files
- Application logs: Check console output
- Database logs: MySQL error log
- Transaction logs: transactions table

### Common Issues
1. **Database Connection Error**: Verify MySQL is running and credentials in DBConnection.java are correct
2. **Login Fails**: Check user status is 'active' and credentials match
3. **Transaction Fails**: Check balance sufficiency and account status
4. **Migration Fails**: Ensure database backup exists before running

### Backup Recommendations
```bash
# Backup database daily
mysqldump -u root -p bankms > backup_$(date +%Y%m%d).sql

# Restore from backup
mysql -u root -p bankms < backup_20231207.sql
```

---

## Contact & Support
For questions or issues regarding this project, please refer to the project documentation or contact the development team.

---

**Document Version**: 1.0  
**Last Updated**: December 2025  
**Author**: BankMS Development Team

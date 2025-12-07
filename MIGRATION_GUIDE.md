# BankMS Migration Guide

## Quick Start After Pulling This PR

### 1. Database Migration (REQUIRED)

**Before running the application**, you must run the password migration script to add the `mustChangePassword` column and reset client passwords.

#### Option A: SQL Script (Recommended)
```bash
# Navigate to project root
cd /home/runner/work/JAVA_project/JAVA_project

# Run SQL migration
mysql -u root -p bankms < db_migration_password_reset.sql
```

#### Option B: Java Migration Script
```bash
# Compile the project
mvn clean compile

# Run migration script (Windows)
java -cp "target/classes;C:/path/to/mysql-connector-j-8.0.33.jar" com.mycompany.bankms.PasswordMigrationScript

# Run migration script (Linux/Mac)
java -cp "target/classes:/path/to/mysql-connector-j-8.0.33.jar" com.mycompany.bankms.PasswordMigrationScript
```

### 2. Verify Migration

After running the migration, verify it succeeded:

```sql
mysql -u root -p bankms

-- Check that column exists
DESCRIBE users;

-- Verify client passwords were reset
SELECT user_id, username, role, mustChangePassword 
FROM users 
WHERE role = 'client' 
LIMIT 5;
```

You should see `mustChangePassword` column with value `1` (TRUE) for all clients.

### 3. Build and Run

```bash
# Build the application
mvn clean compile

# Run the application
mvn javafx:run
```

### 4. Test Login

**New Default Credentials for All Clients**:
- Password: `Nigus@123`
- On first login, users will be forced to change their password

**Admin credentials** remain unchanged (as configured in your database).

## What Changed

### Currency
- All dollar symbols ($) replaced with ETB (Ethiopian Birr)
- Format: "1,000.00 ETB"
- Changed in all UI screens and server responses

### Layout
- Full-width layout (minimum 1200px)
- Better use of screen real estate
- Responsive design maintained

### Password Management
- Default password: "Nigus@123" for all new and reset accounts
- Password requirements: min 8 chars, 1 upper, 1 lower, 1 digit, 1 special character
- Forced password change on first login
- Update Profile page now includes password change section

### Security
- `mustChangePassword` flag enforced at login
- Admin password reset sets default and requires change
- All password resets logged in audit trail

### UI Improvements
- Removed liquidity warning banner from admin dashboard
- Consistent logout button styling across dashboards
- Cleaner, more professional appearance

## Important Notes

⚠️ **MUST RUN MIGRATION** - The application will not work correctly without the database migration.

⚠️ **All Client Passwords Reset** - Inform all existing users that their password has been reset to `Nigus@123`.

⚠️ **Password Security** - Currently passwords are stored in plain text. For production use, implement bcrypt hashing.

## Troubleshooting

### Migration fails with "Column already exists"
This is normal if you run the migration multiple times. The script checks for existence before adding the column.

### Cannot login after migration
1. Verify migration ran successfully (check DESCRIBE users)
2. Try password: `Nigus@123` (case-sensitive)
3. Check user status in database (should be 'active')

### "Database Connection Error"
1. Verify MySQL is running
2. Check DBConnection.java has correct credentials:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/bankms";
   private static final String USER = "root";
   private static final String PASSWORD = "your_password";
   ```

### Build fails
```bash
# Clean and rebuild
mvn clean
mvn compile
```

## Documentation

For complete documentation including:
- Feature implementation details
- Testing procedures
- Defense preparation
- PPT outline

See: **PROJECT_DEFENSE_NOTES.md**

## Support

For issues or questions:
1. Check PROJECT_DEFENSE_NOTES.md
2. Review this migration guide
3. Check database connection settings
4. Verify migration script ran successfully

---

**Last Updated**: December 2025  
**Version**: 1.0  
**Compatible with**: Java 11+, JavaFX 13, MySQL 8.0+

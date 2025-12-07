-- Database Migration Script for Password Reset Feature
-- This script adds the mustChangePassword column and resets all client passwords to the default

-- Step 1: Add mustChangePassword column if it doesn't exist
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS mustChangePassword BOOLEAN DEFAULT FALSE;

-- Step 2: Reset all client passwords to default "Nigus@123" and set mustChangePassword flag
UPDATE users 
SET password_hash = 'Nigus@123', 
    mustChangePassword = TRUE 
WHERE role = 'client';

-- Step 3: Verify the changes
SELECT user_id, username, role, mustChangePassword 
FROM users 
WHERE role = 'client' 
LIMIT 10;

-- Step 4: Create audit log entries for password resets
INSERT INTO transactions (account_id, transaction_type, amount, description, date)
SELECT a.account_id, 'Admin Action', 0, 'Password reset to default via migration script', NOW()
FROM accounts a
JOIN customers c ON a.customer_id = c.customer_id
JOIN users u ON c.user_id = u.user_id
WHERE u.role = 'client';

-- Migration complete
SELECT 'Migration completed successfully. All client passwords reset to Nigus@123' AS status;

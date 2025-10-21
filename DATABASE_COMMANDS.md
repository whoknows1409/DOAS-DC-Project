# 🗄️ DOAS Database Commands Cheatsheet

Quick reference for common database operations.

## 🔌 Connect to Database

```bash
docker exec -it auction-system-postgres-1 psql -U auctionuser -d auctiondb
```

---

## 👤 USER OPERATIONS

### View All Users
```sql
SELECT id, username, email, created_at FROM users;
```

### View Specific User
```sql
SELECT * FROM users WHERE username = 'admin';
SELECT * FROM users WHERE email = 'user@example.com';
```

### Delete a User
```sql
DELETE FROM users WHERE username = 'testuser';
DELETE FROM users WHERE email = 'bad@example.com';
DELETE FROM users WHERE id = 'user-uuid-here';
```

### Create Admin User (Manual)
```sql
-- Note: Password must be bcrypt hashed. Use backend to create users properly.
-- But if you need to manually update an existing user to admin:
-- (This won't work since role column might not exist, but for reference)
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
```

### Change User Email
```sql
UPDATE users SET email = 'newemail@example.com' WHERE username = 'someuser';
```

### Count Users
```sql
SELECT COUNT(*) FROM users;
```

### Delete All Users
```sql
TRUNCATE TABLE users CASCADE;
```

---

## 🛒 AUCTION OPERATIONS

### View All Auctions
```sql
SELECT id, title, current_price, status, end_time FROM auctions;
```

### View Active Auctions
```sql
SELECT id, title, current_price, end_time FROM auctions WHERE status = 'ACTIVE';
```

### View Ended Auctions
```sql
SELECT id, title, current_price, winner_name, end_time FROM auctions WHERE status = 'ENDED';
```

### Delete Specific Auction
```sql
DELETE FROM auctions WHERE title = 'Some Auction Title';
DELETE FROM auctions WHERE id = 'auction-uuid-here';
```

### Delete All Auctions
```sql
TRUNCATE TABLE auctions CASCADE;
```

### Delete Old/Ended Auctions
```sql
DELETE FROM auctions WHERE status = 'ENDED';
```

### End an Auction Manually
```sql
UPDATE auctions SET status = 'ENDED' WHERE id = 'auction-uuid-here';
```

### Change Auction Price
```sql
UPDATE auctions SET current_price = 100.00 WHERE id = 'auction-uuid-here';
```

### View Auctions by Seller
```sql
SELECT * FROM auctions WHERE seller_id = 'user-uuid-here';
```

### Count Auctions
```sql
SELECT COUNT(*) FROM auctions;
SELECT COUNT(*) FROM auctions WHERE status = 'ACTIVE';
```

---

## 💰 BID OPERATIONS

### View All Bids
```sql
SELECT id, auction_id, bidder_id, amount, timestamp FROM bids ORDER BY timestamp DESC;
```

### View Bids for Specific Auction
```sql
SELECT * FROM bids WHERE auction_id = 'auction-uuid-here' ORDER BY amount DESC;
```

### View Bids by User
```sql
SELECT * FROM bids WHERE bidder_id = 'user-uuid-here' ORDER BY timestamp DESC;
```

### Delete Specific Bid
```sql
DELETE FROM bids WHERE id = 'bid-uuid-here';
```

### Delete All Bids
```sql
TRUNCATE TABLE bids CASCADE;
```

### Delete Bids for Specific Auction
```sql
DELETE FROM bids WHERE auction_id = 'auction-uuid-here';
```

### Count Bids
```sql
SELECT COUNT(*) FROM bids;
```

### Highest Bid for Auction
```sql
SELECT * FROM bids WHERE auction_id = 'auction-uuid-here' ORDER BY amount DESC LIMIT 1;
```

---

## 📊 USEFUL QUERIES

### View Auction with Highest Bid Count
```sql
SELECT a.title, COUNT(b.id) as bid_count 
FROM auctions a 
LEFT JOIN bids b ON a.id = b.auction_id 
GROUP BY a.id, a.title 
ORDER BY bid_count DESC;
```

### View User with Most Bids
```sql
SELECT u.username, COUNT(b.id) as bid_count 
FROM users u 
LEFT JOIN bids b ON u.id = b.bidder_id 
GROUP BY u.id, u.username 
ORDER BY bid_count DESC;
```

### View User with Most Auctions
```sql
SELECT u.username, COUNT(a.id) as auction_count 
FROM users u 
LEFT JOIN auctions a ON u.id = a.seller_id 
GROUP BY u.id, u.username 
ORDER BY auction_count DESC;
```

### View Auctions with Winner Info
```sql
SELECT 
    a.title, 
    a.current_price, 
    a.winner_name, 
    a.status,
    a.end_time
FROM auctions a 
WHERE a.winner_name IS NOT NULL;
```

### View All Data Summary
```sql
SELECT 
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM auctions) as total_auctions,
    (SELECT COUNT(*) FROM bids) as total_bids,
    (SELECT COUNT(*) FROM auctions WHERE status = 'ACTIVE') as active_auctions,
    (SELECT COUNT(*) FROM auctions WHERE status = 'ENDED') as ended_auctions;
```

---

## 🔍 INSPECTION COMMANDS

### List All Tables
```sql
\dt
```

### Describe Table Structure
```sql
\d users
\d auctions
\d bids
```

### View Table with Column Details
```sql
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'users';
```

---

## 🧹 CLEANUP OPERATIONS

### Clear Everything
```sql
TRUNCATE TABLE bids CASCADE;
TRUNCATE TABLE auctions CASCADE;
TRUNCATE TABLE users CASCADE;
```

### Clear Only Auctions and Bids (Keep Users)
```sql
TRUNCATE TABLE bids CASCADE;
TRUNCATE TABLE auctions CASCADE;
```

### Clear Only Bids (Keep Auctions and Users)
```sql
TRUNCATE TABLE bids CASCADE;
```

### Delete Specific User and Their Data
```sql
-- Delete user's bids
DELETE FROM bids WHERE bidder_id = (SELECT id FROM users WHERE username = 'testuser');

-- Delete user's auctions (and associated bids via CASCADE)
DELETE FROM auctions WHERE seller_id = (SELECT id FROM users WHERE username = 'testuser');

-- Delete the user
DELETE FROM users WHERE username = 'testuser';
```

---

## 🚀 QUICK ONE-LINER COMMANDS

These can be run directly from your terminal (no need to connect to psql first):

### View All Users
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "SELECT username, email FROM users;"
```

### View All Auctions
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "SELECT title, status, current_price FROM auctions;"
```

### Delete User by Username
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "DELETE FROM users WHERE username = 'testuser';"
```

### Delete Auction by Title
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "DELETE FROM auctions WHERE title = 'My Auction';"
```

### Clear All Data
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "TRUNCATE TABLE bids, auctions, users CASCADE;"
```

### Count Everything
```bash
docker exec auction-system-postgres-1 psql -U auctionuser -d auctiondb -c "SELECT 'users' as table_name, COUNT(*) FROM users UNION ALL SELECT 'auctions', COUNT(*) FROM auctions UNION ALL SELECT 'bids', COUNT(*) FROM bids;"
```

---

## 💡 TIPS

1. **Always use WHERE clause** when deleting to avoid accidents!
2. **CASCADE** automatically deletes related records (e.g., deleting auction deletes its bids)
3. **TRUNCATE** is faster than DELETE for clearing entire tables
4. **UUIDs** are the primary keys - copy them carefully
5. **Exit psql** with `\q` or `Ctrl+D`

---

## ⚠️ COMMON MISTAKES TO AVOID

❌ **Don't run these by accident:**
```sql
DELETE FROM users;              -- Deletes ALL users!
DELETE FROM auctions;           -- Deletes ALL auctions!
TRUNCATE TABLE users CASCADE;   -- Clears ALL users!
```

✅ **Always use WHERE:**
```sql
DELETE FROM users WHERE username = 'specific_user';
DELETE FROM auctions WHERE id = 'specific_id';
```

---

## 🔐 PASSWORD HASHING

To manually create a user with a password, you need bcrypt hash:

```bash
# Use this Python one-liner to generate bcrypt hash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'mypassword', bcrypt.gensalt()).decode())"
```

Then insert:
```sql
INSERT INTO users (id, username, email, password_hash, created_at, updated_at) 
VALUES (
    gen_random_uuid(), 
    'newadmin', 
    'admin@example.com', 
    '$2b$10$hash_from_above_command_here',
    NOW(),
    NOW()
);
```

**Better approach:** Use the registration API endpoint instead!

---

## 📝 Exit psql

```sql
\q
```

Or press `Ctrl+D`



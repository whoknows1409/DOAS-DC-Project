-- Initialize auction database schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Auctions table
CREATE TABLE IF NOT EXISTS auctions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    starting_price DECIMAL(10,2) NOT NULL,
    current_price DECIMAL(10,2) NOT NULL,
    end_time TIMESTAMP NOT NULL,
    seller_id UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bids table
CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auction_id UUID REFERENCES auctions(id),
    bidder_id UUID REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logical_timestamp INTEGER NOT NULL,
    server_id INTEGER NOT NULL
);

-- Server coordination table
CREATE TABLE IF NOT EXISTS server_state (
    server_id INTEGER PRIMARY KEY,
    is_coordinator BOOLEAN DEFAULT FALSE,
    logical_clock INTEGER DEFAULT 0,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Transaction log for 2PC
CREATE TABLE IF NOT EXISTS transaction_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(100) NOT NULL,
    server_id INTEGER NOT NULL,
    operation VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Replication log
CREATE TABLE IF NOT EXISTS replication_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operation_id VARCHAR(100) NOT NULL,
    server_id INTEGER NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    record_id UUID,
    data JSONB,
    logical_timestamp INTEGER NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_auctions_end_time ON auctions(end_time);
CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_timestamp ON bids(timestamp);
CREATE INDEX IF NOT EXISTS idx_transaction_log_transaction_id ON transaction_log(transaction_id);
CREATE INDEX IF NOT EXISTS idx_replication_log_operation_id ON replication_log(operation_id);

-- Insert initial server states
INSERT INTO server_state (server_id, is_coordinator, logical_clock) VALUES 
(1, FALSE, 0),
(2, FALSE, 0),
(3, FALSE, 0)
ON CONFLICT (server_id) DO NOTHING;
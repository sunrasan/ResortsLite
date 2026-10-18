-- PostgreSQL 16 Schema Initialisation Script
-- ResortsLite Application
-- Migrated from H2/MySQL to PostgreSQL 16
-- This script is executed on startup when spring.sql.init.mode=always

-- Create bookings table (PostgreSQL-compatible DDL)
CREATE TABLE IF NOT EXISTS bookings (
    id          VARCHAR(50)  PRIMARY KEY,
    guest       VARCHAR(255) NOT NULL,
    room        VARCHAR(50)  NOT NULL,
    checkin     DATE         NOT NULL,
    checkout    DATE         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index on guest name for lookup performance
CREATE INDEX IF NOT EXISTS idx_bookings_guest ON bookings (guest);

-- Index on room type for availability queries
CREATE INDEX IF NOT EXISTS idx_bookings_room ON bookings (room);

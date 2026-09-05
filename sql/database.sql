-- ============================================================
-- Cloud-Based Lost & Found Management System
-- Database: MySQL 8.x (cloud-hosted)
-- File: sql/database.sql
-- ============================================================
-- Run this entire script once against your cloud MySQL instance
-- (via MySQL Workbench, DBeaver, or the mysql CLI) BEFORE running
-- the Java application. It creates the database, all tables,
-- relationships, and a starter admin account.
-- ============================================================

-- ------------------------------------------------------------
-- 0. DATABASE
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS lostfound_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lostfound_db;

-- ------------------------------------------------------------
-- 1. USERS
-- ------------------------------------------------------------
-- Stores both STUDENT and ADMIN accounts. role distinguishes them.
-- password stores a SHA-256 salted hash, never plain text.
-- ------------------------------------------------------------
CREATE TABLE USERS (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    password    VARCHAR(255)  NOT NULL,   -- format: salt$hash
    role        ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 2. ITEMS
-- ------------------------------------------------------------
-- One row per lost OR found report.
-- user_id  -> the student who filed the report (reporter)
-- type     -> LOST or FOUND (what the reporter is declaring)
-- status   -> current lifecycle stage of the item
--             LOST -> FOUND -> CLAIM_REQUESTED -> VERIFIED -> RETURNED
-- image_url -> reference/URL to the image in cloud object storage
--              (the actual image bytes are NOT stored in MySQL)
-- ------------------------------------------------------------
CREATE TABLE ITEMS (
    item_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    item_name    VARCHAR(150) NOT NULL,
    category     VARCHAR(80)  NOT NULL,
    type         ENUM('LOST', 'FOUND') NOT NULL,
    description  VARCHAR(1000),
    location     VARCHAR(150) NOT NULL,
    item_date    DATE NOT NULL,
    image_url    VARCHAR(500),
    status       ENUM('LOST', 'FOUND', 'CLAIM_REQUESTED', 'VERIFIED', 'RETURNED')
                 NOT NULL DEFAULT 'LOST',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_items_user
        FOREIGN KEY (user_id) REFERENCES USERS(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_items_name     ON ITEMS(item_name);
CREATE INDEX idx_items_category ON ITEMS(category);
CREATE INDEX idx_items_location ON ITEMS(location);
CREATE INDEX idx_items_status   ON ITEMS(status);
CREATE INDEX idx_items_type     ON ITEMS(type);

-- ------------------------------------------------------------
-- 3. CLAIMS
-- ------------------------------------------------------------
-- A student claims an ITEM (usually one reported as FOUND by
-- someone else). One item can receive multiple claims; admin
-- approves at most one.
-- ------------------------------------------------------------
CREATE TABLE CLAIMS (
    claim_id           INT AUTO_INCREMENT PRIMARY KEY,
    item_id            INT NOT NULL,
    user_id            INT NOT NULL,
    claim_description  VARCHAR(1000) NOT NULL,
    claim_date         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status             ENUM('PENDING', 'APPROVED', 'REJECTED')
                       NOT NULL DEFAULT 'PENDING',

    CONSTRAINT fk_claims_item
        FOREIGN KEY (item_id) REFERENCES ITEMS(item_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_claims_user
        FOREIGN KEY (user_id) REFERENCES USERS(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_claims_status ON CLAIMS(status);

-- ------------------------------------------------------------
-- 4. STATUS_HISTORY  (audit trail of every status change)
-- ------------------------------------------------------------
-- Every time ITEMS.status changes, a row is inserted here.
-- changed_by references the user (usually an admin, sometimes
-- the system) who triggered the change. This also backs the
-- Token Ring / Election coordination log used by Member 4's
-- distributed-coordination module (Phase 15-16), since every
-- committed status change is traceable to whichever admin node
-- held the token / was the elected coordinator at the time.
-- ------------------------------------------------------------
CREATE TABLE STATUS_HISTORY (
    history_id   INT AUTO_INCREMENT PRIMARY KEY,
    item_id      INT NOT NULL,
    old_status   ENUM('LOST', 'FOUND', 'CLAIM_REQUESTED', 'VERIFIED', 'RETURNED'),
    new_status   ENUM('LOST', 'FOUND', 'CLAIM_REQUESTED', 'VERIFIED', 'RETURNED') NOT NULL,
    changed_by   INT NOT NULL,
    changed_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_history_item
        FOREIGN KEY (item_id) REFERENCES ITEMS(item_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_history_user
        FOREIGN KEY (changed_by) REFERENCES USERS(user_id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- 5. STARTER DATA
-- ------------------------------------------------------------
-- One admin account so you can log in immediately.
-- Password is "Admin@123" — the value below is the real output of
-- PasswordUtil.hash("Admin@123") (Phase 6): format is
-- base64(salt) + "$" + base64(SHA-256(salt + password)).
-- Regenerate your own with PasswordUtil.hash(...) if you prefer a
-- different starter password; never hand-write a value here.
-- ------------------------------------------------------------
INSERT INTO USERS (name, email, password, role)
VALUES (
    'System Admin',
    'admin@college.edu',
    '8f5JVMJaTvqv2vgM1lR1YA==$pjrpeUeTXRdvwhkFpjIMNGuEObAISZ694rYb166vYqU=',
    'ADMIN'
);

-- ------------------------------------------------------------
-- RELATIONSHIP SUMMARY (for viva / documentation)
-- ------------------------------------------------------------
-- USERS (1) ----------- (M) ITEMS
--   One user can report many items (as reporter).
--
-- USERS (1) ----------- (M) CLAIMS
--   One user can submit many claims (as claimant).
--
-- ITEMS (1) ----------- (M) CLAIMS
--   One item can receive many claims (multiple students may
--   claim the same found item); admin approves at most one.
--
-- ITEMS (1) ----------- (M) STATUS_HISTORY
--   Every status transition for an item is logged as a new row.
--
-- USERS (1) ----------- (M) STATUS_HISTORY
--   changed_by tracks which user (typically an admin) triggered
--   each transition — an audit trail.
--
-- All foreign keys use ON DELETE CASCADE so removing a user or
-- item cleanly removes its dependent claims/history rows too
-- (appropriate for an academic project; a production system
-- might prefer soft deletes instead).
-- ============================================================

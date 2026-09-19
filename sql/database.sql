













CREATE DATABASE IF NOT EXISTS lostfound_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lostfound_db;







CREATE TABLE USERS (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    password    VARCHAR(255)  NOT NULL,   
    role        ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB;












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











INSERT INTO USERS (name, email, password, role)
VALUES (
    'System Admin',
    'admin@college.edu',
    '8f5JVMJaTvqv2vgM1lR1YA==$pjrpeUeTXRdvwhkFpjIMNGuEObAISZ694rYb166vYqU=',
    'ADMIN'
);



























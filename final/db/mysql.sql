-- i-Wish database schema (MySQL version)
-- Converted from the SQLite schema for delivery/backup purposes.

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS catalog_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS wish_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    owner_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    amount_contributed DECIMAL(10,2) NOT NULL DEFAULT 0,
    fulfilled TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS friend_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | ACCEPTED | DECLINED
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS friends (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (friend_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS contributions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    wish_item_id INT NOT NULL,
    contributor_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (wish_item_id) REFERENCES wish_items(id),
    FOREIGN KEY (contributor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Sample catalog seed
INSERT INTO catalog_items (name, description, price) VALUES
 ('RTX 5090 Ti', 'High-end graphics card', 599.0),
 ('RAM 16GB RGB', '16GB RGB desktop memory kit', 400.0),
 ('AirPods', 'Wireless earbuds', 200.0),
 ('Smart Watch', 'Fitness & notifications smartwatch', 300.0),
 ('FIFA 2027', 'Latest football video game', 69.0),
 ('GTA 6', 'Open-world action video game', 120.0);
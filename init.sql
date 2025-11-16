DROP DATABASE IF EXISTS libraryms;
CREATE DATABASE libraryms;

\c libraryms

DROP TABLE IF EXISTS borrowing;
DROP TABLE IF EXISTS copies;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS admin;

CREATE TABLE admin (
    id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    birthdate DATE,
    type VARCHAR(20) CHECK (type IN ('Standard', 'Premium')) DEFAULT 'Standard',
    borrow_limit INT DEFAULT 3
);

CREATE TABLE books (
    isbn VARCHAR(20) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1
);

CREATE TABLE copies (
    copy_id VARCHAR(20) PRIMARY KEY,
    isbn VARCHAR(20) REFERENCES books(isbn) ON DELETE CASCADE,
    status VARCHAR(20) CHECK (status IN ('Available', 'Borrowed', 'Lost')) DEFAULT 'Available',
    location VARCHAR(50) DEFAULT 'Main Shelf'
);

CREATE TABLE borrowing (
    id SERIAL PRIMARY KEY,
    copy_id VARCHAR(20) REFERENCES copies(copy_id) ON DELETE CASCADE,
    user_phone VARCHAR(20) REFERENCES users(phone) ON DELETE CASCADE,
    admin_id INT REFERENCES admin(id) ON DELETE SET NULL,
    borrow_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) CHECK (status IN ('In Progress', 'Late', 'Returned')) DEFAULT 'In Progress'
);

-- Données
INSERT INTO admin (email, password, name) VALUES ('admin@library.com', 'admin123', 'Super Admin');
INSERT INTO users (phone, name, email, birthdate, type, borrow_limit) VALUES
('+1234567890', 'John Doe', 'john@member.com', '1995-03-15', 'Standard', 3),
('+1234567891', 'Jane Smith', 'jane@member.com', '1990-07-22', 'Premium', 5);

INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES
('978-0-13-468599-1', 'Clean Code', 'Robert C. Martin', 'Programming', 5, 3),
('978-0-13-235088-4', 'Clean Architecture', 'Robert C. Martin', 'Programming', 4, 4),
('978-0-596-51624-5', 'Design Patterns', 'Gang of Four', 'Programming', 3, 1);

INSERT INTO copies (copy_id, isbn, status) VALUES
('CC-001', '978-0-13-468599-1', 'Available'),
('CC-002', '978-0-13-468599-1', 'Borrowed'),
('CC-003', '978-0-13-468599-1', 'Available'),
('CC-004', '978-0-13-468599-1', 'Available'),
('CC-005', '978-0-13-468599-1', 'Available'),
('CA-001', '978-0-13-235088-4', 'Available'),
('CA-002', '978-0-13-235088-4', 'Available'),
('CA-003', '978-0-13-235088-4', 'Available'),
('CA-004', '978-0-13-235088-4', 'Available'),
('DP-001', '978-0-596-51624-5', 'Available'),
('DP-002', '978-0-596-51624-5', 'Available'),
('DP-003', '978-0-596-51624-5', 'Borrowed');

INSERT INTO borrowing (copy_id, user_phone, admin_id, borrow_date, due_date, status) VALUES
('CC-002', '+1234567890', 1, '2025-11-01', '2025-11-15', 'In Progress'),
('DP-003', '+1234567890', 1, '2025-10-25', '2025-11-08', 'Late');
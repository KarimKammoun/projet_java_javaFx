

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

INSERT INTO admin (email, password, name) VALUES 
('admin@library.com', 'admin123', 'Super Admin');


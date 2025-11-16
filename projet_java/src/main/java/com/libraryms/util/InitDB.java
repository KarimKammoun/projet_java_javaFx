package com.libraryms.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitDB {
    private static final String DB_PATH = "libraryms.db";

    public static void initializeDatabase() {
        String url = "jdbc:sqlite:" + DB_PATH;
        boolean isNewDb = !new File(DB_PATH).exists();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            if (isNewDb) {
                System.out.println("Creating new SQLite database: " + DB_PATH);
                createTables(stmt);
                insertSampleData(stmt);
                System.out.println("Database and sample data initialized successfully.");
            } else {
                System.out.println("Database already exists: " + DB_PATH);
            }
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables(Statement stmt) throws Exception {
        // Drop existing tables (if any)
        String[] dropStatements = {
            "DROP TABLE IF EXISTS borrowing",
            "DROP TABLE IF EXISTS copies",
            "DROP TABLE IF EXISTS books",
            "DROP TABLE IF EXISTS users",
            "DROP TABLE IF EXISTS admin"
        };
        for (String sql : dropStatements) {
            try {
                stmt.execute(sql);
            } catch (Exception e) {
                // ignore if table doesn't exist
            }
        }

        // Create admin table
        stmt.execute("CREATE TABLE admin (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "name TEXT NOT NULL)");

        // Create users table
        stmt.execute("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "phone TEXT UNIQUE NOT NULL, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE, " +
                "birthdate DATE, " +
                "type TEXT CHECK (type IN ('Standard', 'Premium')) DEFAULT 'Standard', " +
                "borrow_limit INTEGER DEFAULT 3)");

        // Create books table
        stmt.execute("CREATE TABLE books (" +
                "isbn TEXT PRIMARY KEY, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL, " +
                "category TEXT NOT NULL, " +
                "total_copies INTEGER DEFAULT 1, " +
                "available_copies INTEGER DEFAULT 1)");

        // Create copies table
        stmt.execute("CREATE TABLE copies (" +
                "copy_id TEXT PRIMARY KEY, " +
                "isbn TEXT NOT NULL, " +
                "status TEXT CHECK (status IN ('Available', 'Borrowed', 'Lost')) DEFAULT 'Available', " +
                "location TEXT DEFAULT 'Main Shelf', " +
                "FOREIGN KEY(isbn) REFERENCES books(isbn) ON DELETE CASCADE)");

        // Create borrowing table
        stmt.execute("CREATE TABLE borrowing (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "copy_id TEXT NOT NULL, " +
                "user_phone TEXT NOT NULL, " +
                "admin_id INTEGER, " +
                "borrow_date DATE NOT NULL DEFAULT CURRENT_DATE, " +
                "due_date DATE NOT NULL, " +
                "return_date DATE, " +
                "status TEXT CHECK (status IN ('In Progress', 'Late', 'Returned')) DEFAULT 'In Progress', " +
                "FOREIGN KEY(copy_id) REFERENCES copies(copy_id) ON DELETE CASCADE, " +
                "FOREIGN KEY(user_phone) REFERENCES users(phone) ON DELETE CASCADE, " +
                "FOREIGN KEY(admin_id) REFERENCES admin(id) ON DELETE SET NULL)");
    }

    private static void insertSampleData(Statement stmt) throws Exception {
        // Insert admin
        stmt.execute("INSERT INTO admin (email, password, name) VALUES ('admin@library.com', 'admin123', 'Super Admin')");

        // Insert users
        stmt.execute("INSERT INTO users (phone, name, email, birthdate, type, borrow_limit) VALUES " +
                "('+1234567890', 'John Doe', 'john@member.com', '1995-03-15', 'Standard', 3)");
        stmt.execute("INSERT INTO users (phone, name, email, birthdate, type, borrow_limit) VALUES " +
                "('+1234567891', 'Jane Smith', 'jane@member.com', '1990-07-22', 'Premium', 5)");

        // Insert books
        stmt.execute("INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES " +
                "('978-0-13-468599-1', 'Clean Code', 'Robert C. Martin', 'Programming', 5, 3)");
        stmt.execute("INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES " +
                "('978-0-13-235088-4', 'Clean Architecture', 'Robert C. Martin', 'Programming', 4, 4)");
        stmt.execute("INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES " +
                "('978-0-596-51624-5', 'Design Patterns', 'Gang of Four', 'Programming', 3, 1)");

        // Insert copies
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('CC-001', '978-0-13-468599-1', 'Available')");
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('CC-002', '978-0-13-468599-1', 'Borrowed')");
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('CC-003', '978-0-13-468599-1', 'Available')");
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('CA-001', '978-0-13-235088-4', 'Available')");
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('DP-001', '978-0-596-51624-5', 'Available')");
        stmt.execute("INSERT INTO copies (copy_id, isbn, status) VALUES ('DP-003', '978-0-596-51624-5', 'Borrowed')");

        // Insert borrowings
        stmt.execute("INSERT INTO borrowing (copy_id, user_phone, admin_id, borrow_date, due_date, status) VALUES " +
                "('CC-002', '+1234567890', 1, date('now', '-15 days'), date('now', '+1 day'), 'In Progress')");
        stmt.execute("INSERT INTO borrowing (copy_id, user_phone, admin_id, borrow_date, due_date, status) VALUES " +
                "('DP-003', '+1234567890', 1, date('now', '-22 days'), date('now', '-8 days'), 'Late')");
    }
}

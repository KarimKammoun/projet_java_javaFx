package com.libraryms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestMultiAdmin {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:libraryms.db");

        // 1. Insert admin2
        System.out.println("=== Adding Admin 2 ===");
        try (PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO admin (email, password, name) VALUES (?, ?, ?)")) {
            pst.setString(1, "admin2@library.com");
            pst.setString(2, "admin123");
            pst.setString(3, "Admin 2");
            pst.executeUpdate();
            System.out.println("✓ Admin 2 created");
        } catch (Exception e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("⚠ Admin 2 already exists");
            } else {
                throw e;
            }
        }

        // 2. Show all admins
        System.out.println("\n=== All Admins ===");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, email, name FROM admin")) {
            while (rs.next()) {
                System.out.printf("ID: %d | Email: %s | Name: %s%n", 
                    rs.getInt(1), rs.getString(2), rs.getString(3));
            }
        }

        // 3. Get first 3 ISBNs from admin1 and move them to admin2
        System.out.println("\n=== Moving 3 Books from Admin 1 to Admin 2 ===");
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT isbn FROM books WHERE admin_id = 1 LIMIT 3")) {
            ResultSet isbns = pst.executeQuery();
            java.util.List<String> isbnList = new java.util.ArrayList<>();
            while (isbns.next()) {
                isbnList.add(isbns.getString(1));
            }
            // Now update those ISBNs
            if (!isbnList.isEmpty()) {
                String placeholders = String.join(",", java.util.Collections.nCopies(isbnList.size(), "?"));
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE books SET admin_id = 2 WHERE admin_id = 1 AND isbn IN (" + placeholders + ")")) {
                    for (int i = 0; i < isbnList.size(); i++) {
                        updateStmt.setString(i + 1, isbnList.get(i));
                    }
                    int updated = updateStmt.executeUpdate();
                    System.out.printf("✓ Updated %d books%n", updated);
                }
            }
        }

        // 4. Show book counts per admin
        System.out.println("\n=== Books Per Admin ===");
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT admin_id, COUNT(*) as count FROM books GROUP BY admin_id")) {
            while (rs.next()) {
                System.out.printf("Admin %d: %d books%n", rs.getInt(1), rs.getInt(2));
            }
        }

        // 5. Show books for each admin
        System.out.println("\n=== Books for Admin 1 (first 3) ===");
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT isbn, title FROM books WHERE admin_id = 1 LIMIT 3")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                System.out.printf("  • %s: %s%n", rs.getString(1), rs.getString(2));
            }
        }

        System.out.println("\n=== Books for Admin 2 (first 3) ===");
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT isbn, title FROM books WHERE admin_id = 2 LIMIT 3")) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                System.out.printf("  • %s: %s%n", rs.getString(1), rs.getString(2));
            }
        }

        // 6. Update copies for those books
        System.out.println("\n=== Moving Copies to Admin 2 ===");
        try (PreparedStatement pst = conn.prepareStatement(
                "SELECT isbn FROM books WHERE admin_id = 2 LIMIT 3")) {
            ResultSet isbns = pst.executeQuery();
            java.util.List<String> isbnList = new java.util.ArrayList<>();
            while (isbns.next()) {
                isbnList.add(isbns.getString(1));
            }
            if (!isbnList.isEmpty()) {
                String placeholders = String.join(",", java.util.Collections.nCopies(isbnList.size(), "?"));
                try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE copies SET admin_id = 2 WHERE isbn IN (" + placeholders + ")")) {
                    for (int i = 0; i < isbnList.size(); i++) {
                        updateStmt.setString(i + 1, isbnList.get(i));
                    }
                    int updated = updateStmt.executeUpdate();
                    System.out.printf("✓ Updated %d copies%n", updated);
                }
            }
        }

        System.out.println("\n✓ Test data ready! Now test:");
        System.out.println("  1. Login as admin@library.com / admin123");
        System.out.println("  2. Check Books list (should show ~97 books)");
        System.out.println("  3. Logout and login as admin2@library.com / admin123");
        System.out.println("  4. Check Books list (should show ~3 books only)");

        conn.close();
    }
}

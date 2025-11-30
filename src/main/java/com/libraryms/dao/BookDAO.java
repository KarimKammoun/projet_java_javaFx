package com.libraryms.dao;

import com.libraryms.models.Book;
import com.libraryms.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookDAO {

    public Book findByIsbn(String isbn) throws SQLException {
        String sql = "SELECT isbn, title, author, category, total_copies, available_copies, admin_id FROM books WHERE isbn = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Book(
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies"),
                            rs.getObject("admin_id") == null ? null : rs.getInt("admin_id")
                    );
                }
            }
        }
        return null;
    }

    public List<Book> listByAdmin(int adminId) throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT isbn, title, author, category, total_copies, available_copies, admin_id FROM books WHERE admin_id = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Book(
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("category"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies"),
                            rs.getObject("admin_id") == null ? null : rs.getInt("admin_id")
                    ));
                }
            }
        }
        return list;
    }

    public void create(Book book, int copies) throws SQLException {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO books (isbn, title, author, category, total_copies, available_copies, admin_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, book.getIsbn());
                ins.setString(2, book.getTitle());
                ins.setString(3, book.getAuthor());
                ins.setString(4, book.getCategory());
                ins.setInt(5, copies);
                ins.setInt(6, copies);
                if (book.getAdminId() != null) ins.setInt(7, book.getAdminId()); else ins.setNull(7, java.sql.Types.INTEGER);
                ins.executeUpdate();
            }

            // create copies
            try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, admin_id) VALUES (?, ?, 'Available', ?)")) {
                for (int i = 1; i <= copies; i++) {
                    String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    String copyId = book.getIsbn().replaceAll("[^A-Za-z0-9]", "") + "-" + i + "-" + shortId;
                    insCopy.setString(1, copyId);
                    insCopy.setString(2, book.getIsbn());
                    if (book.getAdminId() != null) insCopy.setInt(3, book.getAdminId()); else insCopy.setNull(3, java.sql.Types.INTEGER);
                    insCopy.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    public void update(Book book, int newTotalCopies) throws SQLException, IllegalStateException {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            // update metadata
            try (PreparedStatement upd = conn.prepareStatement("UPDATE books SET title = ?, author = ?, category = ? WHERE isbn = ?")) {
                upd.setString(1, book.getTitle());
                upd.setString(2, book.getAuthor());
                upd.setString(3, book.getCategory());
                upd.setString(4, book.getIsbn());
                upd.executeUpdate();
            }

            // get original counts
            int originalTotal = 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT total_copies, available_copies FROM books WHERE isbn = ?")) {
                ps.setString(1, book.getIsbn());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        originalTotal = rs.getInt("total_copies");
                    }
                }
            }

            int diff = newTotalCopies - originalTotal;
            if (diff > 0) {
                // add copies
                try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, admin_id) VALUES (?, ?, 'Available', ?)")) {
                    for (int i = 1; i <= diff; i++) {
                        String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        String copyId = book.getIsbn().replaceAll("[^A-Za-z0-9]", "") + "-" + (originalTotal + i) + "-" + shortId;
                        insCopy.setString(1, copyId);
                        insCopy.setString(2, book.getIsbn());
                        if (book.getAdminId() != null) insCopy.setInt(3, book.getAdminId()); else insCopy.setNull(3, java.sql.Types.INTEGER);
                        insCopy.executeUpdate();
                    }
                }
                try (PreparedStatement updBook = conn.prepareStatement("UPDATE books SET total_copies = total_copies + ?, available_copies = available_copies + ? WHERE isbn = ?")) {
                    updBook.setInt(1, diff);
                    updBook.setInt(2, diff);
                    updBook.setString(3, book.getIsbn());
                    updBook.executeUpdate();
                }
            } else if (diff < 0) {
                int toRemove = -diff;
                // check available
                int available = 0;
                try (PreparedStatement psAvail = conn.prepareStatement("SELECT COUNT(*) FROM copies WHERE isbn = ? AND status = 'Available'")) {
                    psAvail.setString(1, book.getIsbn());
                    try (ResultSet rs = psAvail.executeQuery()) {
                        if (rs.next()) available = rs.getInt(1);
                    }
                }
                if (available < toRemove) {
                    conn.rollback();
                    throw new IllegalStateException("Not enough available copies to remove. Available=" + available);
                }

                try (PreparedStatement del = conn.prepareStatement("DELETE FROM copies WHERE copy_id IN (SELECT copy_id FROM copies WHERE isbn = ? AND status = 'Available' LIMIT ?)")) {
                    del.setString(1, book.getIsbn());
                    del.setInt(2, toRemove);
                    del.executeUpdate();
                }
                try (PreparedStatement updBook = conn.prepareStatement("UPDATE books SET total_copies = total_copies - ?, available_copies = available_copies - ? WHERE isbn = ?")) {
                    updBook.setInt(1, toRemove);
                    updBook.setInt(2, toRemove);
                    updBook.setString(3, book.getIsbn());
                    updBook.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    public void delete(String isbn) throws SQLException {
        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delCopies = conn.prepareStatement("DELETE FROM copies WHERE isbn = ?")) {
                delCopies.setString(1, isbn);
                delCopies.executeUpdate();
            }
            try (PreparedStatement delBook = conn.prepareStatement("DELETE FROM books WHERE isbn = ?")) {
                delBook.setString(1, isbn);
                delBook.executeUpdate();
            }
            conn.commit();
        }
    }
}

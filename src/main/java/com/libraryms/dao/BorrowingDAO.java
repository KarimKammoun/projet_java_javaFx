package com.libraryms.dao;

import com.libraryms.models.Borrowing;
import com.libraryms.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;

public class BorrowingDAO {

    public List<Borrowing> listByAdmin(int adminId) throws SQLException {
        List<Borrowing> list = new ArrayList<>();
        String sql = "SELECT b.copy_id, b.user_phone, u.name as member_name, bk.title, b.borrow_date, b.due_date, b.status "
            + "FROM borrowing b "
            + "LEFT JOIN copies c ON b.copy_id = c.copy_id "
            + "LEFT JOIN books bk ON c.isbn = bk.isbn "
            + "LEFT JOIN users u ON b.user_phone = u.phone "
            + "WHERE b.admin_id = ? ORDER BY b.borrow_date DESC";

        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String copyId = rs.getString(1);
                    String userPhone = rs.getString(2);
                    String memberName = rs.getString(3);
                    String title = rs.getString(4);
                    String bdStr = rs.getString(5);
                    LocalDate borrowDate = (bdStr == null) ? null : LocalDate.parse(bdStr);
                    String ddStr = rs.getString(6);
                    LocalDate dueDate = (ddStr == null) ? null : LocalDate.parse(ddStr);
                    String status = rs.getString(7);
                    list.add(new Borrowing(copyId, userPhone, memberName, title, borrowDate, dueDate, status));
                }
            }
        }
        return list;
    }

    /**
     * Return detailed borrowing records including id, for admin listing.
     */
    public List<com.libraryms.models.BorrowingRecord> listAdminRecords(int adminId) throws SQLException {
        List<com.libraryms.models.BorrowingRecord> list = new ArrayList<>();
        String sql = "SELECT b.id, b.copy_id, c.isbn, COALESCE(b.book_title, bk.title) as book_title, u.name as member_name, b.user_phone, b.borrow_date, b.due_date, b.status "
            + "FROM borrowing b "
            + "LEFT JOIN copies c ON b.copy_id = c.copy_id "
            + "LEFT JOIN books bk ON c.isbn = bk.isbn "
            + "LEFT JOIN users u ON b.user_phone = u.phone "
            + "WHERE b.admin_id = ? ORDER BY b.borrow_date DESC";

        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String copyId = rs.getString(2);
                    String isbn = rs.getString(3);
                    String title = rs.getString(4);
                    String memberName = rs.getString(5);
                    String userPhone = rs.getString(6);
                    String bdStr = rs.getString(7);
                    LocalDate borrowDate = (bdStr == null) ? null : LocalDate.parse(bdStr);
                    String ddStr = rs.getString(8);
                    LocalDate dueDate = (ddStr == null) ? null : LocalDate.parse(ddStr);
                    String status = rs.getString(9);
                    list.add(new com.libraryms.models.BorrowingRecord(id, copyId, isbn, title, memberName, userPhone, borrowDate, dueDate, status));
                }
            }
        }
        return list;
    }

    public void createBorrowing(String copyId, String userPhone, LocalDate dueDate, Integer adminId) throws SQLException {
        String findIsbn = "SELECT isbn FROM copies WHERE copy_id = ?";
        String insertBorrow = "INSERT INTO borrowing (copy_id, user_phone, admin_id, borrow_date, due_date, status) VALUES (?, ?, ?, ?, ?, 'In Progress')";
        String updateCopy = "UPDATE copies SET status = 'Borrowed' WHERE copy_id = ?";
        String updateBook = "UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ? AND available_copies > 0";

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            String isbn = null;
            try (PreparedStatement ps = conn.prepareStatement(findIsbn)) {
                ps.setString(1, copyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) isbn = rs.getString(1);
                    else throw new SQLException("Copy not found");
                }
            }

            try (PreparedStatement ins = conn.prepareStatement(insertBorrow)) {
                ins.setString(1, copyId);
                ins.setString(2, userPhone);
                if (adminId != null) ins.setInt(3, adminId); else ins.setNull(3, java.sql.Types.INTEGER);
                ins.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
                ins.setDate(5, java.sql.Date.valueOf(dueDate));
                ins.executeUpdate();
            }

            try (PreparedStatement upc = conn.prepareStatement(updateCopy)) {
                upc.setString(1, copyId);
                upc.executeUpdate();
            }

            if (isbn != null) {
                try (PreparedStatement upb = conn.prepareStatement(updateBook)) {
                    upb.setString(1, isbn);
                    upb.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    /**
     * Find an available copy for the given ISBN and create a borrowing.
     */
    public void createBorrowingByIsbn(String isbn, String userPhone, LocalDate dueDate, Integer adminId) throws SQLException {
        String findCopy = "SELECT copy_id FROM copies WHERE isbn = ? AND status = 'Available' LIMIT 1";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(findCopy)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String copyId = rs.getString(1);
                    createBorrowing(copyId, userPhone, dueDate, adminId);
                    return;
                }
            }
        }
        throw new SQLException("No available copy for ISBN: " + isbn);
    }

    public void markReturnedByCopy(String copyId, LocalDate returnDate) throws SQLException {
        String findBorrow = "SELECT id, isbn FROM borrowing b JOIN copies c ON b.copy_id = c.copy_id WHERE b.copy_id = ? AND b.status <> 'Returned' ORDER BY b.borrow_date DESC LIMIT 1";
        String updateBorrow = "UPDATE borrowing SET return_date = ?, status = 'Returned' WHERE id = ?";
        String updateCopy = "UPDATE copies SET status = 'Available' WHERE copy_id = ?";
        String updateBook = "UPDATE books SET available_copies = available_copies + 1 WHERE isbn = ?";

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            Integer borrowId = null;
            String isbn = null;
            try (PreparedStatement ps = conn.prepareStatement(findBorrow)) {
                ps.setString(1, copyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        borrowId = rs.getInt(1);
                        // isbn may come from result set index 2 if present
                        try { isbn = rs.getString(2); } catch (Exception ignored) { isbn = null; }
                    } else {
                        throw new SQLException("No active borrowing found for copy");
                    }
                }
            }

            try (PreparedStatement upb = conn.prepareStatement(updateBorrow)) {
                upb.setDate(1, java.sql.Date.valueOf(returnDate == null ? LocalDate.now() : returnDate));
                upb.setInt(2, borrowId);
                upb.executeUpdate();
            }

            try (PreparedStatement upc = conn.prepareStatement(updateCopy)) {
                upc.setString(1, copyId);
                upc.executeUpdate();
            }

            if (isbn != null) {
                try (PreparedStatement upbk = conn.prepareStatement(updateBook)) {
                    upbk.setString(1, isbn);
                    upbk.executeUpdate();
                }
            }

            conn.commit();
        }
    }

    public void updateDueDate(int borrowingId, LocalDate dueDate) throws SQLException {
        String sql = "UPDATE borrowing SET due_date = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(dueDate));
            ps.setInt(2, borrowingId);
            ps.executeUpdate();
        }
    }

    /**
     * Returns recent borrowing counts per month (YYYY-MM) for the given admin.
     * The list is ordered ascending by month.
     */
    public List<Map.Entry<String, Integer>> listBorrowingCountsPerMonth(int adminId, int monthsBack) throws SQLException {
        List<Map.Entry<String, Integer>> out = new ArrayList<>();
        String sql = "SELECT strftime('%Y-%m', borrow_date) as ym, COUNT(*) as cnt "
                + "FROM borrowing WHERE admin_id = ? "
                + "GROUP BY ym ORDER BY ym DESC LIMIT ?";

        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, monthsBack);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map.Entry<String, Integer>> tmp = new ArrayList<>();
                while (rs.next()) {
                    tmp.add(new AbstractMap.SimpleEntry<>(rs.getString(1), rs.getInt(2)));
                }
                // reverse to ascending order
                for (int i = tmp.size() - 1; i >= 0; i--) out.add(tmp.get(i));
            }
        }
        return out;
    }

    public com.libraryms.models.BorrowingRecord findById(int borrowingId) throws SQLException {
        String sql = "SELECT b.id, b.copy_id, c.isbn, COALESCE(b.book_title, bk.title) as book_title, u.name as member_name, b.user_phone, b.borrow_date, b.due_date, b.status "
                + "FROM borrowing b "
                + "LEFT JOIN copies c ON b.copy_id = c.copy_id "
                + "LEFT JOIN books bk ON c.isbn = bk.isbn "
                + "LEFT JOIN users u ON b.user_phone = u.phone "
                + "WHERE b.id = ? LIMIT 1";
        try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    String copyId = rs.getString(2);
                    String isbn = rs.getString(3);
                    String title = rs.getString(4);
                    String memberName = rs.getString(5);
                    String userPhone = rs.getString(6);
                    String bdStr = rs.getString(7);
                    LocalDate borrowDate = (bdStr == null) ? null : LocalDate.parse(bdStr);
                    String ddStr = rs.getString(8);
                    LocalDate dueDate = (ddStr == null) ? null : LocalDate.parse(ddStr);
                    String status = rs.getString(9);
                    return new com.libraryms.models.BorrowingRecord(id, copyId, isbn, title, memberName, userPhone, borrowDate, dueDate, status);
                }
            }
        }
        return null;
    }

    public void deleteById(int borrowingId) throws SQLException {
        String findSql = "SELECT copy_id, status FROM borrowing WHERE id = ?";
        String findIsbn = "SELECT isbn FROM copies WHERE copy_id = ?";
        String deleteSql = "DELETE FROM borrowing WHERE id = ?";
        String updateCopy = "UPDATE copies SET status = 'Available' WHERE copy_id = ?";
        String updateBook = "UPDATE books SET available_copies = available_copies + 1 WHERE isbn = ?";

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            String copyId = null;
            String status = null;
            try (PreparedStatement ps = conn.prepareStatement(findSql)) {
                ps.setInt(1, borrowingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        copyId = rs.getString(1);
                        status = rs.getString(2);
                    } else {
                        throw new SQLException("Borrowing not found");
                    }
                }
            }

            String isbn = null;
            if (copyId != null) {
                try (PreparedStatement ps = conn.prepareStatement(findIsbn)) {
                    ps.setString(1, copyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) isbn = rs.getString(1);
                    }
                }
            }

            try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                del.setInt(1, borrowingId);
                del.executeUpdate();
            }

            // If borrowing wasn't returned, free the copy and increment book availability
            if (copyId != null && (status == null || !"Returned".equalsIgnoreCase(status))) {
                try (PreparedStatement upc = conn.prepareStatement(updateCopy)) {
                    upc.setString(1, copyId);
                    upc.executeUpdate();
                }
                if (isbn != null) {
                    try (PreparedStatement upb = conn.prepareStatement(updateBook)) {
                        upb.setString(1, isbn);
                        upb.executeUpdate();
                    }
                }
            }

            conn.commit();
        }
    }
}

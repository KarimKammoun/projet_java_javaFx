package com.libraryms.controller.admin;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    @FXML private Label totalBooks, totalCopies, availableCopies, totalMembers, activeBorrowings;
    @FXML private TableView<Borrowing> recentTable;
    @FXML private LineChart<String, Number> trendChart;

    @FXML
    private void initialize() {
        setupTable();
        loadStats();
        loadRecentBorrowings();
        loadTrendChart();
    }

    private void setupTable() {
        var cols = recentTable.getColumns();
        ((TableColumn<Borrowing, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<Borrowing, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("memberName"));
        ((TableColumn<Borrowing, LocalDate>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        ((TableColumn<Borrowing, LocalDate>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        ((TableColumn<Borrowing, String>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadStats() {
        Integer adminId = Session.getAdminId();
        if (adminId == null) {
            totalBooks.setText("0");
            totalCopies.setText("0");
            availableCopies.setText("0");
            totalMembers.setText("0");
            activeBorrowings.setText("0");
            return;
        }

        try (var conn = DatabaseUtil.connect()) {
            // Total Books for this admin
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE admin_id = ?")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) totalBooks.setText(rs.getString(1));
                }
            }
            // Total Copies for this admin
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM copies WHERE admin_id = ?")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) totalCopies.setText(rs.getString(1));
                }
            }
            // Available Copies for this admin
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM copies WHERE admin_id = ? AND status = 'Available'")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) availableCopies.setText(rs.getString(1));
                }
            }
            // Total Members for this admin
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE admin_id = ?")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) totalMembers.setText(rs.getString(1));
                }
            }
            // Active Borrowings for this admin
            try (var pst = conn.prepareStatement("SELECT COUNT(*) FROM borrowing WHERE admin_id = ? AND status IN ('In Progress', 'Late')")) {
                pst.setInt(1, adminId);
                try (var rs = pst.executeQuery()) {
                    if (rs.next()) activeBorrowings.setText(rs.getString(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentBorrowings() {
        Integer adminId = Session.getAdminId();
        var list = FXCollections.<Borrowing>observableArrayList();
        
        if (adminId == null) {
            recentTable.setItems(list);
            return;
        }

        try (var conn = DatabaseUtil.connect();
             var pst = conn.prepareStatement(
                     "SELECT c.copy_id, u.name, b.borrow_date, b.due_date, b.status " +
                             "FROM borrowing b " +
                             "JOIN copies c ON b.copy_id = c.copy_id " +
                             "JOIN users u ON b.user_phone = u.phone " +
                             "WHERE b.admin_id = ? " +
                             "ORDER BY b.borrow_date DESC LIMIT 5")) {
            pst.setInt(1, adminId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                try {
                    String borrowDateStr = rs.getString(3);
                    String dueDateStr = rs.getString(4);
                    LocalDate borrowDate = LocalDate.parse(borrowDateStr);
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    list.add(new Borrowing(
                            rs.getString(1), rs.getString(2),
                            borrowDate, dueDate,
                            rs.getString(5)
                    ));
                } catch (Exception e) {
                    System.err.println("Date parsing error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        recentTable.setItems(list);
    }

    private void loadTrendChart() {
        Integer adminId = Session.getAdminId();
        var series = new XYChart.Series<String, Number>();
        series.setName("Emprunts");
        
        if (adminId == null) {
            trendChart.getData().add(series);
            return;
        }

        try (var conn = DatabaseUtil.connect();
             var pst = conn.prepareStatement(
                     "SELECT DATE(borrow_date) as borrow_day, COUNT(*) as count " +
                             "FROM borrowing " +
                             "WHERE admin_id = ? AND borrow_date >= date('now', '-29 days') " +
                             "GROUP BY DATE(borrow_date) " +
                             "ORDER BY borrow_day DESC LIMIT 30")) {
            pst.setInt(1, adminId);
            ResultSet rs = pst.executeQuery();
            
            Map<String, Integer> dailyData = new HashMap<>();
            while (rs.next()) {
                String date = rs.getString(1);
                int count = rs.getInt(2);
                dailyData.put(date, count);
            }

            // Add data to chart (last 30 days)
            LocalDate today = LocalDate.now();
            for (int i = 29; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.toString();
                int count = dailyData.getOrDefault(dateStr, 0);
                String label = String.format("%d/%d", date.getMonthValue(), date.getDayOfMonth());
                series.getData().add(new XYChart.Data<>(label, count));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        trendChart.getData().add(series);
    }

    @FXML private void logout() { SceneManager.loadScene("/fxml/login/login.fxml"); }
    @FXML private void showMembers() { SceneManager.loadScene("/fxml/user/members.fxml"); }
    @FXML private void showBooks() { SceneManager.loadScene("/fxml/admin/books.fxml"); }
    @FXML private void showCopies() { SceneManager.loadScene("/fxml/admin/copies.fxml"); }
    @FXML private void showBorrowings() { SceneManager.loadScene("/fxml/admin/borrowings.fxml"); }
    @FXML private void showSettings() { SceneManager.loadScene("/fxml/admin/settings.fxml"); }
    @FXML private void showDashboard() { /* already here */ }
    
    public static class Borrowing {
        private final String copyId, memberName, status;
        private final LocalDate borrowDate, dueDate;
        public Borrowing(String copyId, String memberName, LocalDate borrowDate, LocalDate dueDate, String status) {
            this.copyId = copyId; this.memberName = memberName; this.borrowDate = borrowDate; this.dueDate = dueDate; this.status = status;
        }
        public String getCopyId() { return copyId; }
        public String getMemberName() { return memberName; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}
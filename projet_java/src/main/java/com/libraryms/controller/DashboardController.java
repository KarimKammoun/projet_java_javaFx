package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.ResultSet;
import java.time.LocalDate;

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
        try (var conn = DatabaseUtil.connect(); var stmt = conn.createStatement()) {
            // Total Books
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM books")) {
                if (rs.next()) totalBooks.setText(rs.getString(1));
            }
            // Total Copies
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM copies")) {
                if (rs.next()) totalCopies.setText(rs.getString(1));
            }
            // Available Copies
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM copies WHERE status = 'Available'")) {
                if (rs.next()) availableCopies.setText(rs.getString(1));
            }
            // Total Members
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next()) totalMembers.setText(rs.getString(1));
            }
            // Active Borrowings
            try (var rs = stmt.executeQuery("SELECT COUNT(*) FROM borrowing WHERE status IN ('In Progress', 'Late')")) {
                if (rs.next()) activeBorrowings.setText(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentBorrowings() {
        var list = FXCollections.<Borrowing>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement(
                     "SELECT c.copy_id, u.name, b.borrow_date, b.due_date, b.status " +
                             "FROM borrowing b " +
                             "JOIN copies c ON b.copy_id = c.copy_id " +
                             "JOIN users u ON b.user_phone = u.phone " +
                             "ORDER BY b.borrow_date DESC LIMIT 5")) {
            ResultSet rs = stmt.executeQuery();
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
                    // Skip rows with date parsing issues
                    System.err.println("Date parsing error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        recentTable.setItems(list);
    }

    private void loadTrendChart() {
        var series = new XYChart.Series<String, Number>();
        series.setName("Borrowings");
        series.getData().addAll(
                new XYChart.Data<>("Jun", 45),
                new XYChart.Data<>("Jul", 52),
                new XYChart.Data<>("Aug", 48),
                new XYChart.Data<>("Sep", 58),
                new XYChart.Data<>("Oct", 65),
                new XYChart.Data<>("Nov", 72)
        );
        trendChart.getData().add(series);
    }

    @FXML private void logout() { SceneManager.loadScene("/fxml/login.fxml"); }
    @FXML private void showMembers() { SceneManager.loadScene("/fxml/members.fxml"); }
    @FXML private void showBooks() { SceneManager.loadScene("/fxml/books.fxml"); }
    @FXML private void showCopies() { SceneManager.loadScene("/fxml/copies.fxml"); }
    @FXML private void showBorrowings() { SceneManager.loadScene("/fxml/borrowings.fxml"); }
    @FXML private void showSettings() { SceneManager.loadScene("/fxml/settings.fxml"); }
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
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
import com.libraryms.dao.BorrowingDAO;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.libraryms.dao.BorrowingDAO;

public class DashboardController {

    @FXML private Label totalBooks, totalCopies, availableCopies, totalMembers, activeBorrowings;
    @FXML private TableView<Borrowing> recentTable;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private javafx.scene.control.TableColumn<Borrowing, String> recent_colCopyId;
    @FXML private javafx.scene.control.TableColumn<Borrowing, String> recent_colMemberName;
    @FXML private javafx.scene.control.TableColumn<Borrowing, java.time.LocalDate> recent_colBorrowDate;
    @FXML private javafx.scene.control.TableColumn<Borrowing, java.time.LocalDate> recent_colDueDate;
    @FXML private javafx.scene.control.TableColumn<Borrowing, String> recent_colStatus;

    @FXML
    private void initialize() {
        setupTable();
        loadStats();
        loadRecentBorrowings();
        loadTrendChart();
    }

    private void setupTable() {
        recent_colCopyId.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        recent_colMemberName.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        recent_colBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        recent_colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        recent_colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
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

        try {
            BorrowingDAO dao = new BorrowingDAO();
            List<com.libraryms.models.Borrowing> rows = dao.listByAdmin(adminId);
            int added = 0;
            for (com.libraryms.models.Borrowing r : rows) {
                if (added++ >= 5) break;
                try {
                    list.add(new Borrowing(
                            r.getCopyId(),
                            // use member name from model
                            r.getMemberName() == null ? r.getMemberPhone() : r.getMemberName(),
                            r.getBorrowDate(),
                            r.getDueDate(),
                            r.getStatus()
                    ));
                } catch (Exception e) {
                    System.err.println("Date mapping error: " + e.getMessage());
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

        try {
            BorrowingDAO dao = new BorrowingDAO();
            // show last 6 months
            int monthsBack = 6;
            List<Map.Entry<String, Integer>> counts = dao.listBorrowingCountsPerMonth(adminId, monthsBack);

            // Build a map for quick lookup
            Map<String, Integer> monthly = new HashMap<>();
            for (var e : counts) monthly.put(e.getKey(), e.getValue());

            LocalDate now = LocalDate.now();
            for (int i = monthsBack - 1; i >= 0; i--) {
                LocalDate m = now.minusMonths(i);
                String ym = String.format("%04d-%02d", m.getYear(), m.getMonthValue());
                int value = monthly.getOrDefault(ym, 0);
                String label = String.format("%d/%02d", m.getMonthValue(), m.getYear());
                series.getData().add(new XYChart.Data<>(label, value));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        trendChart.getData().add(series);
    }

    @FXML private void logout() { SceneManager.loadScene("/fxml/login/login.fxml"); }
    @FXML private void showMembers() { SceneManager.loadScene("/fxml/admin/members.fxml"); }
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
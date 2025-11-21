package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MemberBorrowingHistoryController {

    @FXML private TableView<BorrowingRecord> borrowingsTable;
    @FXML private TextField searchField;

    private ObservableList<BorrowingRecord> masterData;
    private String currentFilter = "all"; // all, active, completed, overdue

    @FXML
    private void initialize() {
        var cols = borrowingsTable.getColumns();
        ((TableColumn<BorrowingRecord, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        ((TableColumn<BorrowingRecord, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<BorrowingRecord, LocalDate>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        ((TableColumn<BorrowingRecord, LocalDate>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        ((TableColumn<BorrowingRecord, LocalDate>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        ((TableColumn<BorrowingRecord, String>) cols.get(5)).setCellValueFactory(new PropertyValueFactory<>("status"));
        ((TableColumn<BorrowingRecord, String>) cols.get(6)).setCellValueFactory(new PropertyValueFactory<>("daysInfo"));

        loadBorrowings();

        FilteredList<BorrowingRecord> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(b -> {
                if (lower.isEmpty()) return applyStatusFilter(b);
                if (b.getBookTitle() != null && b.getBookTitle().toLowerCase().contains(lower)) return applyStatusFilter(b);
                if (b.getCopyId() != null && b.getCopyId().toLowerCase().contains(lower)) return applyStatusFilter(b);
                return false;
            });
        });

        SortedList<BorrowingRecord> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(borrowingsTable.comparatorProperty());
        borrowingsTable.setItems(sorted);
    }

    private boolean applyStatusFilter(BorrowingRecord b) {
        if ("all".equals(currentFilter)) return true;
        if ("active".equals(currentFilter)) return "In Progress".equals(b.getStatus()) || "Late".equals(b.getStatus());
        if ("completed".equals(currentFilter)) return "Returned".equals(b.getStatus());
        if ("overdue".equals(currentFilter)) return "Late".equals(b.getStatus());
        return true;
    }

    private void loadBorrowings() {
        var list = FXCollections.<BorrowingRecord>observableArrayList();
        String phone = Session.getPhone();
        if (phone == null) return;

        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement(
                     "SELECT b.id, b.copy_id, b.book_title, b.borrow_date, b.due_date, b.return_date, b.status " +
                     "FROM borrowing b WHERE b.user_phone = ? ORDER BY b.borrow_date DESC")) {
            stmt.setString(1, phone);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                try {
                    int id = rs.getInt("id");
                    String copyId = rs.getString("copy_id");
                    String bookTitle = rs.getString("book_title");
                    String borrowDateStr = rs.getString("borrow_date");
                    String dueDateStr = rs.getString("due_date");
                    String returnDateStr = rs.getString("return_date");
                    String status = rs.getString("status");

                    LocalDate borrowDate = LocalDate.parse(borrowDateStr);
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    LocalDate returnDate = returnDateStr != null ? LocalDate.parse(returnDateStr) : null;

                    list.add(new BorrowingRecord(id, copyId, bookTitle, borrowDate, dueDate, returnDate, status));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        masterData = list;
    }

    @FXML private void filterActive() { currentFilter = "active"; applyFilter(); }
    @FXML private void filterCompleted() { currentFilter = "completed"; applyFilter(); }
    @FXML private void filterOverdue() { currentFilter = "overdue"; applyFilter(); }
    @FXML private void filterAll() { currentFilter = "all"; applyFilter(); }

    private void applyFilter() {
        // trigger re-filter by updating search field listener
        searchField.setText("");
    }

    public static class BorrowingRecord {
        private final int id;
        private final String copyId, bookTitle, status;
        private final LocalDate borrowDate, dueDate, returnDate;

        public BorrowingRecord(int id, String copyId, String bookTitle, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate, String status) {
            this.id = id;
            this.copyId = copyId;
            this.bookTitle = bookTitle;
            this.borrowDate = borrowDate;
            this.dueDate = dueDate;
            this.returnDate = returnDate;
            this.status = status;
        }

        public int getId() { return id; }
        public String getCopyId() { return copyId; }
        public String getBookTitle() { return bookTitle; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatus() { return status; }

        public String getDaysInfo() {
            if ("Returned".equals(status)) {
                return "✓ Returned";
            }
            long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
            if (days < 0) {
                return Math.abs(days) + " days overdue";
            }
            return days + " days left";
        }
    }
}

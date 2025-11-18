package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
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

public class BorrowingsController {

    @FXML private TableView<BorrowingAdmin> borrowingsTable;
    @FXML private TextField searchField;

    private ObservableList<BorrowingAdmin> masterData;

    @FXML
    private void initialize() {
        var cols = borrowingsTable.getColumns();
        ((TableColumn<BorrowingAdmin, Integer>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("id"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("memberName"));
        ((TableColumn<BorrowingAdmin, LocalDate>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        ((TableColumn<BorrowingAdmin, LocalDate>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(5)).setCellValueFactory(new PropertyValueFactory<>("status"));
        loadBorrowings();

        FilteredList<BorrowingAdmin> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(b -> {
                if (lower.isEmpty()) return true;
                if (String.valueOf(b.getId()).contains(lower)) return true;
                if (b.getCopyId() != null && b.getCopyId().toLowerCase().contains(lower)) return true;
                if (b.getMemberName() != null && b.getMemberName().toLowerCase().contains(lower)) return true;
                if (b.getBorrowDate() != null && b.getBorrowDate().toString().toLowerCase().contains(lower)) return true;
                if (b.getDueDate() != null && b.getDueDate().toString().toLowerCase().contains(lower)) return true;
                if (b.getStatus() != null && b.getStatus().toLowerCase().contains(lower)) return true;
                return false;
            });
        });
        SortedList<BorrowingAdmin> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(borrowingsTable.comparatorProperty());
        borrowingsTable.setItems(sorted);
    }

    private void loadBorrowings() {
        var list = FXCollections.<BorrowingAdmin>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT b.id, b.copy_id, u.name, b.borrow_date, b.due_date, b.status " +
                             "FROM borrowing b JOIN users u ON b.user_phone = u.phone")) {
            while (rs.next()) {
                try {
                    String borrowDateStr = rs.getString("borrow_date");
                    String dueDateStr = rs.getString("due_date");
                    LocalDate borrowDate = LocalDate.parse(borrowDateStr);
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    list.add(new BorrowingAdmin(
                            rs.getInt("id"),
                            rs.getString("copy_id"),
                            rs.getString("name"),
                            borrowDate,
                            dueDate,
                            rs.getString("status")
                    ));
                } catch (Exception e) {
                    System.err.println("Date parsing error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        masterData = list;
        borrowingsTable.setItems(masterData);
    }

    @FXML
    private void openAddBorrowing() {
        try {
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_borrowing.fxml")).load();
            var scene = new javafx.scene.Scene(root);
            var stage = new javafx.stage.Stage();
            stage.setTitle("New Borrowing");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadBorrowings();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class BorrowingAdmin {
        private final int id;
        private final String copyId, memberName, status;
        private final LocalDate borrowDate, dueDate;
        public BorrowingAdmin(int id, String copyId, String memberName, LocalDate borrowDate, LocalDate dueDate, String status) {
            this.id = id; this.copyId = copyId; this.memberName = memberName;
            this.borrowDate = borrowDate; this.dueDate = dueDate; this.status = status;
        }
        public int getId() { return id; }
        public String getCopyId() { return copyId; }
        public String getMemberName() { return memberName; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}
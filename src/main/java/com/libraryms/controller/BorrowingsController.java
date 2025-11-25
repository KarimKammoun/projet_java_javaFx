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
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class BorrowingsController {

    @FXML private TableView<BorrowingAdmin> borrowingsTable;
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ChoiceBox<String> searchColumnChoice;

    private ObservableList<BorrowingAdmin> masterData;

    @FXML
    private void initialize() {
        var cols = borrowingsTable.getColumns();
        ((TableColumn<BorrowingAdmin, Integer>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("id"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("memberName"));
        ((TableColumn<BorrowingAdmin, LocalDate>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        ((TableColumn<BorrowingAdmin, LocalDate>) cols.get(5)).setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        ((TableColumn<BorrowingAdmin, String>) cols.get(6)).setCellValueFactory(new PropertyValueFactory<>("status"));
        loadBorrowings();

        if (searchColumnChoice != null) searchColumnChoice.getSelectionModel().select("All");

        FilteredList<BorrowingAdmin> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(b -> {
                if (lower.isEmpty()) return true;
                String col = (searchColumnChoice != null && searchColumnChoice.getValue() != null) ? searchColumnChoice.getValue() : "All";
                switch (col) {
                    case "ID":
                        return String.valueOf(b.getId()).contains(lower);
                    case "Copy ID":
                        return b.getCopyId() != null && b.getCopyId().toLowerCase().contains(lower);
                    case "Book Title":
                        return b.getBookTitle() != null && b.getBookTitle().toLowerCase().contains(lower);
                    case "Member":
                        return b.getMemberName() != null && b.getMemberName().toLowerCase().contains(lower);
                    case "Borrow Date":
                        return b.getBorrowDate() != null && b.getBorrowDate().toString().toLowerCase().contains(lower);
                    case "Due Date":
                        return b.getDueDate() != null && b.getDueDate().toString().toLowerCase().contains(lower);
                    case "Status":
                        return b.getStatus() != null && b.getStatus().toLowerCase().contains(lower);
                    default:
                        if (String.valueOf(b.getId()).contains(lower)) return true;
                        if (b.getCopyId() != null && b.getCopyId().toLowerCase().contains(lower)) return true;
                        if (b.getBookTitle() != null && b.getBookTitle().toLowerCase().contains(lower)) return true;
                        if (b.getMemberName() != null && b.getMemberName().toLowerCase().contains(lower)) return true;
                        if (b.getBorrowDate() != null && b.getBorrowDate().toString().toLowerCase().contains(lower)) return true;
                        if (b.getDueDate() != null && b.getDueDate().toString().toLowerCase().contains(lower)) return true;
                        if (b.getStatus() != null && b.getStatus().toLowerCase().contains(lower)) return true;
                        return false;
                }
            });
        });
        if (searchColumnChoice != null) {
            searchColumnChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
                if (searchField != null) searchField.setText(searchField.getText());
            });
        }
        SortedList<BorrowingAdmin> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(borrowingsTable.comparatorProperty());
        borrowingsTable.setItems(sorted);
    }

    private void loadBorrowings() {
        var list = FXCollections.<BorrowingAdmin>observableArrayList();
        try (var conn = DatabaseUtil.connect()) {
            Integer adminId = com.libraryms.util.Session.getAdminId();
            if (adminId == null) {
                System.err.println("No admin logged in");
                return;
            }
            boolean hasBookTitle = false;
            try (var pragmaStmt = conn.createStatement();
                 var prs = pragmaStmt.executeQuery("PRAGMA table_info('borrowing')")) {
                while (prs.next()) {
                    String name = prs.getString("name");
                    if ("book_title".equalsIgnoreCase(name)) { hasBookTitle = true; break; }
                }
            }

            String sql;
            if (hasBookTitle) {
                sql = "SELECT b.id, b.copy_id, b.book_title, u.name, b.borrow_date, b.due_date, b.status " +
                        "FROM borrowing b JOIN users u ON b.user_phone = u.phone WHERE b.admin_id = ?";
            } else {
                sql = "SELECT b.id, b.copy_id, k.title AS book_title, u.name, b.borrow_date, b.due_date, b.status " +
                        "FROM borrowing b " +
                        "JOIN copies c ON b.copy_id = c.copy_id " +
                        "JOIN books k ON c.isbn = k.isbn " +
                        "JOIN users u ON b.user_phone = u.phone WHERE b.admin_id = ?";
            }

            try (var ps = conn.prepareStatement(sql)) {
                ps.setInt(1, adminId);
                var rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        String borrowDateStr = rs.getString("borrow_date");
                        String dueDateStr = rs.getString("due_date");
                        LocalDate borrowDate = LocalDate.parse(borrowDateStr);
                        LocalDate dueDate = LocalDate.parse(dueDateStr);
                        list.add(new BorrowingAdmin(
                                rs.getInt("id"),
                                rs.getString("copy_id"),
                                rs.getString("book_title"),
                                rs.getString("name"),
                                borrowDate,
                                dueDate,
                                rs.getString("status")
                        ));
                    } catch (Exception e) {
                        System.err.println("Date parsing error: " + e.getMessage());
                    }
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

    @FXML private Button editBorrowingBtn;
    @FXML private Button markReceivedBtn;
    @FXML private Button deleteBorrowingBtn;

    @FXML
    private void openEditBorrowing() {
        var dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Edit Borrowing");
        dialog.setHeaderText("Enter borrowing ID to load for editing");
        dialog.setContentText("Borrowing ID:");
        var res = dialog.showAndWait();
        if (res.isPresent() && !res.get().trim().isEmpty()) {
            try {
                int id = Integer.parseInt(res.get().trim());
                var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_borrowing.fxml"));
                javafx.scene.Parent root = loader.load();
                var controller = loader.getController();
                if (controller instanceof com.libraryms.controller.AddBorrowingController) {
                    ((com.libraryms.controller.AddBorrowingController) controller).loadForEdit(id);
                }
                var scene = new javafx.scene.Scene(root);
                var stage = new javafx.stage.Stage();
                stage.setTitle("Edit Borrowing - " + id);
                stage.setScene(scene);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadBorrowings();
            } catch (NumberFormatException nfe) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "ID invalide").showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void deleteSelectedBorrowing() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/delete_borrowing.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Supprimer un emprunt");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadBorrowings();
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void markSelectedReceived() {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/mark_received.fxml"));
            javafx.scene.Parent root = loader.load();
            var scene = new javafx.scene.Scene(root);
            var stage = new javafx.stage.Stage();
            stage.setTitle("Mark Received");
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
        private final String copyId, bookTitle, memberName, status;
        private final LocalDate borrowDate, dueDate;
        public BorrowingAdmin(int id, String copyId, String bookTitle, String memberName, LocalDate borrowDate, LocalDate dueDate, String status) {
            this.id = id; this.copyId = copyId; this.bookTitle = bookTitle; this.memberName = memberName;
            this.borrowDate = borrowDate; this.dueDate = dueDate; this.status = status;
        }
        public int getId() { return id; }
        public String getCopyId() { return copyId; }
        public String getBookTitle() { return bookTitle; }
        public String getMemberName() { return memberName; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStatus() { return status; }
    }
}
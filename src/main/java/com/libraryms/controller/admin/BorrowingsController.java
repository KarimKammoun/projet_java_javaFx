package com.libraryms.controller.admin;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.dao.BorrowingDAO;
import com.libraryms.models.BorrowingRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import com.libraryms.dao.BorrowingDAO;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;

public class BorrowingsController {

    @FXML private TableView<BorrowingAdmin> borrowingsTable;
    @FXML private TableColumn<BorrowingAdmin, Integer> borrow_colId;
    @FXML private TableColumn<BorrowingAdmin, String> borrow_colCopyId;
    @FXML private TableColumn<BorrowingAdmin, String> borrow_colBookTitle;
    @FXML private TableColumn<BorrowingAdmin, String> borrow_colMemberName;
    @FXML private TableColumn<BorrowingAdmin, LocalDate> borrow_colBorrowDate;
    @FXML private TableColumn<BorrowingAdmin, LocalDate> borrow_colDueDate;
    @FXML private TableColumn<BorrowingAdmin, String> borrow_colStatus;
    @FXML private TextField searchField;
    @FXML private javafx.scene.control.ChoiceBox<String> searchColumnChoice;

    private ObservableList<BorrowingAdmin> masterData;

    @FXML
    private void initialize() {
        borrow_colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        borrow_colCopyId.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        borrow_colBookTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        borrow_colMemberName.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        borrow_colBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        borrow_colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrow_colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
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
        try {
            Integer adminId = com.libraryms.util.Session.getAdminId();
            if (adminId == null) {
                System.err.println("No admin logged in");
                return;
            }

            BorrowingDAO dao = new BorrowingDAO();
            java.util.List<BorrowingRecord> rows = dao.listAdminRecords(adminId);
            for (BorrowingRecord r : rows) {
                list.add(new BorrowingAdmin(
                        r.getId(), r.getCopyId(), r.getBookTitle(), r.getMemberName(), r.getBorrowDate(), r.getDueDate(), r.getStatus()
                ));
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
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_borrowing.fxml")).load();
            var scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
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
                var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_borrowing.fxml"));
                javafx.scene.Parent root = loader.load();
                var controller = loader.getController();
                if (controller instanceof com.libraryms.controller.pop_up.AddBorrowingController) {
                    ((com.libraryms.controller.pop_up.AddBorrowingController) controller).loadForEdit(id);
                }
                var scene = new javafx.scene.Scene(root);
                com.libraryms.util.SceneManager.applyGlobalStyles(scene);
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
        var sel = borrowingsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez sélectionner un emprunt à supprimer.").showAndWait();
            return;
        }

        var confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Supprimer l'emprunt ID " + sel.getId() + " ?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        var res = confirm.showAndWait();
        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.YES) {
            try {
                BorrowingDAO dao = new BorrowingDAO();
                dao.deleteById(sel.getId());
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Emprunt supprimé.").showAndWait();
                loadBorrowings();
            } catch (Exception e) {
                e.printStackTrace();
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void markSelectedReceived() {
        var sel = borrowingsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez sélectionner un emprunt à marquer comme reçu.").showAndWait();
            return;
        }

        var confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Marquer la copie " + sel.getCopyId() + " comme reçue ?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        var res = confirm.showAndWait();
        if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.YES) {
            try {
                BorrowingDAO dao = new BorrowingDAO();
                // mark returned by copy id; DAO will set return_date and status and restore counts
                dao.markReturnedByCopy(sel.getCopyId(), null);
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Emprunt marqué comme reçu.").showAndWait();
                loadBorrowings();
            } catch (Exception e) {
                e.printStackTrace();
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
            }
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
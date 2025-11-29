package com.libraryms.controller.admin;

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

public class CopiesController {

    @FXML private TableView<Copy> copiesTable;
    @FXML private TextField searchField;

    private ObservableList<Copy> masterData;

    @FXML
    private void initialize() {
        var cols = copiesTable.getColumns();
        ((TableColumn<Copy, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<Copy, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("isbn"));
        ((TableColumn<Copy, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("title"));
        ((TableColumn<Copy, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("status"));
        ((TableColumn<Copy, String>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("location"));
        loadCopies();

        FilteredList<Copy> filtered = new FilteredList<>(masterData != null ? masterData : FXCollections.observableArrayList(), p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            var lower = newVal == null ? "" : newVal.toLowerCase();
            filtered.setPredicate(copy -> {
                if (lower.isEmpty()) return true;
                if (copy.getCopyId() != null && copy.getCopyId().toLowerCase().contains(lower)) return true;
                if (copy.getIsbn() != null && copy.getIsbn().toLowerCase().contains(lower)) return true;
                if (copy.getTitle() != null && copy.getTitle().toLowerCase().contains(lower)) return true;
                if (copy.getStatus() != null && copy.getStatus().toLowerCase().contains(lower)) return true;
                if (copy.getLocation() != null && copy.getLocation().toLowerCase().contains(lower)) return true;
                return false;
            });
        });
        SortedList<Copy> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(copiesTable.comparatorProperty());
        copiesTable.setItems(sorted);
    }

    private void loadCopies() {
        var list = FXCollections.<Copy>observableArrayList();
        try (var conn = DatabaseUtil.connect()) {
            Integer adminId = com.libraryms.util.Session.getAdminId();
            if (adminId == null) {
                System.err.println("No admin logged in");
                return;
            }
            var ps = conn.prepareStatement(
                    "SELECT c.copy_id, c.isbn, b.title, c.status, c.location " +
                            "FROM copies c JOIN books b ON c.isbn = b.isbn WHERE c.admin_id = ?");
            ps.setInt(1, adminId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Copy(
                        rs.getString("copy_id"),
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getString("location")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        masterData = list;
        copiesTable.setItems(masterData);
    }

    @FXML
    private void openAddCopy() {
        try {
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_copy.fxml")).load();
            var scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
            var stage = new javafx.stage.Stage();
            stage.setTitle("Add Copies");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadCopies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private Button editCopyBtn;

    @FXML
    private void openEditCopy() {
        try {
                var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/add_copy.fxml"));
            javafx.scene.Parent root = loader.load();
            var scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
            var stage = new javafx.stage.Stage();
            stage.setTitle("Edit Copy (enter Copy ID to load)");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadCopies();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Copy {
        private final String copyId, isbn, title, status, location;
        public Copy(String copyId, String isbn, String title, String status, String location) {
            this.copyId = copyId; this.isbn = isbn; this.title = title; this.status = status; this.location = location;
        }
        public String getCopyId() { return copyId; }
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public String getLocation() { return location; }
    }
}

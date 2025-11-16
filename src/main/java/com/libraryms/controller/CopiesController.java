package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class CopiesController {

    @FXML private TableView<Copy> copiesTable;

    @FXML
    private void initialize() {
        var cols = copiesTable.getColumns();
        ((TableColumn<Copy, String>) cols.get(0)).setCellValueFactory(new PropertyValueFactory<>("copyId"));
        ((TableColumn<Copy, String>) cols.get(1)).setCellValueFactory(new PropertyValueFactory<>("isbn"));
        ((TableColumn<Copy, String>) cols.get(2)).setCellValueFactory(new PropertyValueFactory<>("title"));
        ((TableColumn<Copy, String>) cols.get(3)).setCellValueFactory(new PropertyValueFactory<>("status"));
        ((TableColumn<Copy, String>) cols.get(4)).setCellValueFactory(new PropertyValueFactory<>("location"));
        loadCopies();
    }

    private void loadCopies() {
        var list = FXCollections.<Copy>observableArrayList();
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT c.copy_id, c.isbn, b.title, c.status, c.location " +
                             "FROM copies c JOIN books b ON c.isbn = b.isbn")) {
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
        copiesTable.setItems(list);
    }

    @FXML
    private void openAddCopy() {
        try {
            javafx.scene.Parent root = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/add_copy.fxml")).load();
            var scene = new javafx.scene.Scene(root);
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

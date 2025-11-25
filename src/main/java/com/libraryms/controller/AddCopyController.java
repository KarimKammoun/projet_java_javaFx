package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
// no direct ResultSet import needed
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddCopyController {

    @FXML private ComboBox<String> bookCombo; // will contain "isbn - title"
    @FXML private TextField copiesField;
    @FXML private TextField locationField;
    @FXML private TextField copyIdField;

    private boolean editMode = false;
    private String originalCopyId = null;

    public void loadForEdit(String copyId) {
        // load copy details and allow editing location/status (book selection disabled)
        try (var conn = DatabaseUtil.connect();
             var ps = conn.prepareStatement("SELECT c.copy_id, c.isbn, c.status, c.location, b.title FROM copies c JOIN books b ON c.isbn = b.isbn WHERE c.copy_id = ?")) {
            ps.setString(1, copyId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                String isbn = rs.getString("isbn");
                String title = rs.getString("title");
                String item = isbn + " - " + title;
                bookCombo.getItems().setAll(item);
                bookCombo.getSelectionModel().selectFirst();
                bookCombo.setDisable(true);
                locationField.setText(rs.getString("location"));
                copiesField.setText("1");
                originalCopyId = copyId;
                editMode = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadById() {
        String copyId = copyIdField.getText().trim();
        if (copyId.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez entrer l'ID de la copie.").showAndWait();
            return;
        }
        loadForEdit(copyId);
    }

    @FXML
    private void initialize() {
        loadBooks();
    }

    private void loadBooks() {
        try (Connection conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT isbn, title FROM books ORDER BY title")) {
            List<String> items = new ArrayList<>();
            while (rs.next()) {
                items.add(rs.getString("isbn") + " - " + rs.getString("title"));
            }
            bookCombo.getItems().setAll(items);
            if (!items.isEmpty()) bookCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addCopies() {
        var sel = bookCombo.getValue();
        if (sel == null || sel.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un livre.").showAndWait();
            return;
        }
        String isbn = sel.split(" - ")[0];
        int n = 1;
        try { n = Integer.parseInt(copiesField.getText().trim()); if (n < 1) n = 1; } catch (Exception ex) { n = 1; }
        String location = locationField.getText().trim(); if (location.isEmpty()) location = "Main Shelf";

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            if (editMode && originalCopyId != null) {
                // update copy location (and leave status as is)
                try (PreparedStatement upd = conn.prepareStatement("UPDATE copies SET location = ? WHERE copy_id = ?")) {
                    upd.setString(1, location);
                    upd.setString(2, originalCopyId);
                    upd.executeUpdate();
                }
                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Copie mise à jour.").showAndWait();
                Stage s = (Stage) bookCombo.getScene().getWindow(); s.close();
                return;
            }

            // insert copies
            try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, location, admin_id) VALUES (?, ?, 'Available', ?, ?)")) {
                for (int i = 1; i <= n; i++) {
                    String shortId = UUID.randomUUID().toString().substring(0,8).toUpperCase();
                    String copyId = isbn.replaceAll("[^A-Za-z0-9]", "") + "-" + System.currentTimeMillis()%100000 + "-" + shortId;
                    insCopy.setString(1, copyId);
                    insCopy.setString(2, isbn);
                    insCopy.setString(3, location);
                    Integer adminId = com.libraryms.util.Session.getAdminId();
                    if (adminId != null) insCopy.setInt(4, adminId); else insCopy.setNull(4, java.sql.Types.INTEGER);
                    insCopy.executeUpdate();
                }
            }

            // update books counters
            try (PreparedStatement upd = conn.prepareStatement("UPDATE books SET total_copies = total_copies + ?, available_copies = available_copies + ? WHERE isbn = ?")) {
                upd.setInt(1, n);
                upd.setInt(2, n);
                upd.setString(3, isbn);
                upd.executeUpdate();
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, n + " copie(s) ajoutée(s) pour " + isbn).showAndWait();
            Stage s = (Stage) bookCombo.getScene().getWindow(); s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) bookCombo.getScene().getWindow(); s.close();
    }
}

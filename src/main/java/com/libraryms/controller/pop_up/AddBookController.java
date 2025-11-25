package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class AddBookController {

    @FXML private TextField isbnField;
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private TextField categoryField;
    @FXML private TextField copiesField;

    private boolean editMode = false;
    private String originalIsbn = null;
    private int originalTotalCopies = 0;

    public void loadForEdit(String isbn) {
        try (var conn = DatabaseUtil.connect();
             var ps = conn.prepareStatement("SELECT isbn, title, author, category, total_copies FROM books WHERE isbn = ?")) {
            ps.setString(1, isbn);
            var rs = ps.executeQuery();
            if (rs.next()) {
                isbnField.setText(rs.getString("isbn"));
                titleField.setText(rs.getString("title"));
                authorField.setText(rs.getString("author"));
                categoryField.setText(rs.getString("category"));
                originalIsbn = isbn;
                originalTotalCopies = rs.getInt("total_copies");
                editMode = true;
                isbnField.setDisable(true);
                // allow changing copies in edit mode
                copiesField.setText(String.valueOf(originalTotalCopies));
                copiesField.setDisable(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadById() {
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez entrer l'ISBN.").showAndWait();
            return;
        }
        loadForEdit(isbn);
    }

    @FXML
    private void saveBook() {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String category = categoryField.getText().trim();
        int copies = 1;
        try {
            copies = Integer.parseInt(copiesField.getText().trim());
            if (copies < 1) copies = 1;
        } catch (Exception e) {
            copies = 1;
        }

        if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || category.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").showAndWait();
            return;
        }

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

                if (editMode && originalIsbn != null) {
                    try {
                        // update metadata
                        try (PreparedStatement upd = conn.prepareStatement("UPDATE books SET title = ?, author = ?, category = ? WHERE isbn = ?")) {
                            upd.setString(1, title);
                            upd.setString(2, author);
                            upd.setString(3, category);
                            upd.setString(4, originalIsbn);
                            upd.executeUpdate();
                        }

                        // handle copies change
                        int newTotal = copies;
                        if (newTotal != originalTotalCopies) {
                            int diff = newTotal - originalTotalCopies;
                            if (diff > 0) {
                                // add diff copies
                                try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, admin_id) VALUES (?, ?, 'Available', ?)")) {
                                    for (int i = 1; i <= diff; i++) {
                                        String shortId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                                        String copyId = originalIsbn.replaceAll("[^A-Za-z0-9]", "") + "-" + (originalTotalCopies + i) + "-" + shortId;
                                        insCopy.setString(1, copyId);
                                        insCopy.setString(2, originalIsbn);
                                        Integer adminId = com.libraryms.util.Session.getAdminId();
                                        if (adminId != null) insCopy.setInt(3, adminId); else insCopy.setNull(3, java.sql.Types.INTEGER);
                                        insCopy.executeUpdate();
                                    }
                                }
                                try (PreparedStatement updBook = conn.prepareStatement("UPDATE books SET total_copies = total_copies + ?, available_copies = available_copies + ? WHERE isbn = ?")) {
                                    updBook.setInt(1, diff);
                                    updBook.setInt(2, diff);
                                    updBook.setString(3, originalIsbn);
                                    updBook.executeUpdate();
                                }
                            } else {
                                int toRemove = -diff;
                                // Ensure there are enough available copies to remove
                                int available = 0;
                                try (PreparedStatement psAvail = conn.prepareStatement("SELECT COUNT(*) FROM copies WHERE isbn = ? AND status = 'Available'")) {
                                    psAvail.setString(1, originalIsbn);
                                    try (ResultSet rs = psAvail.executeQuery()) {
                                        if (rs.next()) available = rs.getInt(1);
                                    }
                                }
                                if (available < toRemove) {
                                    conn.rollback();
                                    new Alert(Alert.AlertType.ERROR, "Impossible de réduire le nombre de copies: seules " + available + " copies disponibles peuvent être supprimées.").showAndWait();
                                    return;
                                }

                                // delete specific available copies
                                try (PreparedStatement del = conn.prepareStatement(
                                        "DELETE FROM copies WHERE copy_id IN (SELECT copy_id FROM copies WHERE isbn = ? AND status = 'Available' LIMIT ?)")) {
                                    del.setString(1, originalIsbn);
                                    del.setInt(2, toRemove);
                                    del.executeUpdate();
                                }
                                try (PreparedStatement updBook = conn.prepareStatement("UPDATE books SET total_copies = total_copies - ?, available_copies = available_copies - ? WHERE isbn = ?")) {
                                    updBook.setInt(1, toRemove);
                                    updBook.setInt(2, toRemove);
                                    updBook.setString(3, originalIsbn);
                                    updBook.executeUpdate();
                                }
                            }
                        }

                        conn.commit();
                        new Alert(Alert.AlertType.INFORMATION, "Livre mis à jour.").showAndWait();
                        Stage s = (Stage) isbnField.getScene().getWindow();
                        s.close();
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        conn.rollback();
                        new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour: " + ex.getMessage()).showAndWait();
                        return;
                    }
                }

            // Insert mode
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM books WHERE isbn = ?")) {
                check.setString(1, isbn);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        new Alert(Alert.AlertType.ERROR, "Un livre avec ce ISBN existe déjà.").showAndWait();
                        return;
                    }
                }
            }

            // Insert book
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO books (isbn, title, author, category, total_copies, available_copies, admin_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, isbn);
                ins.setString(2, title);
                ins.setString(3, author);
                ins.setString(4, category);
                ins.setInt(5, copies);
                ins.setInt(6, copies);
                Integer adminId = com.libraryms.util.Session.getAdminId();
                if (adminId != null) ins.setInt(7, adminId); else ins.setNull(7, java.sql.Types.INTEGER);
                ins.executeUpdate();
            }

            // Insert copies
            try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, admin_id) VALUES (?, ?, 'Available', ?)")) {
                for (int i = 1; i <= copies; i++) {
                    String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    String copyId = isbn.replaceAll("[^A-Za-z0-9]", "") + "-" + i + "-" + shortId;
                    insCopy.setString(1, copyId);
                    insCopy.setString(2, isbn);
                    Integer adminId = com.libraryms.util.Session.getAdminId();
                    if (adminId != null) insCopy.setInt(3, adminId); else insCopy.setNull(3, java.sql.Types.INTEGER);
                    insCopy.executeUpdate();
                }
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Livre ajouté avec succès.").showAndWait();
            // close dialog
            Stage s = (Stage) isbnField.getScene().getWindow();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur pendant l'enregistrement: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) isbnField.getScene().getWindow();
        s.close();
    }
}

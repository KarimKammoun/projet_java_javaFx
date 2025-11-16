package com.libraryms.controller;

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

            // Check if book exists
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
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO books (isbn, title, author, category, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, isbn);
                ins.setString(2, title);
                ins.setString(3, author);
                ins.setString(4, category);
                ins.setInt(5, copies);
                ins.setInt(6, copies);
                ins.executeUpdate();
            }

            // Insert copies
            try (PreparedStatement insCopy = conn.prepareStatement("INSERT INTO copies (copy_id, isbn, status, location) VALUES (?, ?, 'Available', 'Main Shelf')")) {
                for (int i = 1; i <= copies; i++) {
                    String shortId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    String copyId = isbn.replaceAll("[^A-Za-z0-9]", "") + "-" + i + "-" + shortId;
                    insCopy.setString(1, copyId);
                    insCopy.setString(2, isbn);
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

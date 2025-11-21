package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddBorrowingController {

    @FXML private TextField bookIdField;
    @FXML private TextField memberIdField;
    @FXML private DatePicker dueDatePicker;
    @FXML private TextField borrowingIdField;

    private boolean editMode = false;
    private Integer originalBorrowingId = null;

    public void loadForEdit(int borrowingId) {
        try (var conn = DatabaseUtil.connect();
             var ps = conn.prepareStatement("SELECT b.id, b.copy_id, b.user_phone, b.due_date FROM borrowing b WHERE b.id = ?")) {
            ps.setInt(1, borrowingId);
            var rs = ps.executeQuery();
            if (rs.next()) {
                String copyId = rs.getString("copy_id");
                String phone = rs.getString("user_phone");
                String due = rs.getString("due_date");
                // determine ISBN for the copy
                try (var ps2 = conn.prepareStatement("SELECT isbn FROM copies WHERE copy_id = ?")) {
                    ps2.setString(1, copyId);
                    var rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        bookIdField.setText(rs2.getString("isbn"));
                    }
                }
                // convert stored phone to member id for display
                try (var ps3 = conn.prepareStatement("SELECT id FROM users WHERE phone = ?")) {
                    ps3.setString(1, phone);
                    var rs3 = ps3.executeQuery();
                    if (rs3.next()) {
                        memberIdField.setText(String.valueOf(rs3.getInt("id")));
                    } else {
                        memberIdField.setText(phone); // fallback
                    }
                }
                if (due != null) dueDatePicker.setValue(java.time.LocalDate.parse(due));
                // lock selection changes (prevent changing copy/member in edit)
                bookIdField.setDisable(true);
                memberIdField.setDisable(true);
                originalBorrowingId = borrowingId;
                editMode = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadById() {
        String idStr = borrowingIdField.getText().trim();
        if (idStr.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez entrer l'ID d'emprunt.").showAndWait();
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            loadForEdit(id);
        } catch (NumberFormatException e) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "ID invalide.").showAndWait();
        }
    }

    @FXML
    private void initialize() {
        dueDatePicker.setValue(LocalDate.now().plusDays(14));
    }

    private void loadCopies() {
        // No-op: replaced by bookId/memberId text input workflow
    }
    

    @FXML
    private void createBorrowing() {
        String bookIsbn = bookIdField.getText().trim();
        String memberIdStr = memberIdField.getText().trim();
        var dueDate = dueDatePicker.getValue();

        if (bookIsbn.isEmpty() || memberIdStr.isEmpty() || dueDate == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs (ISBN, member ID, due date).").showAndWait();
            return;
        }

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            // resolve member id -> phone (database stores user_phone in borrowing)
            String phone = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT phone FROM users WHERE id = ?")) {
                try {
                    int mid = Integer.parseInt(memberIdStr);
                    ps.setInt(1, mid);
                    var rs = ps.executeQuery();
                    if (rs.next()) phone = rs.getString("phone");
                } catch (NumberFormatException nfe) {
                    new Alert(Alert.AlertType.ERROR, "Member ID invalide.").showAndWait();
                    return;
                }
            }
            if (phone == null) {
                new Alert(Alert.AlertType.WARNING, "Membre introuvable pour l'ID fourni.").showAndWait();
                conn.rollback();
                return;
            }

            if (editMode && originalBorrowingId != null) {
                try (PreparedStatement upd = conn.prepareStatement("UPDATE borrowing SET due_date = ? WHERE id = ?")) {
                    upd.setString(1, dueDate.toString());
                    upd.setInt(2, originalBorrowingId);
                    upd.executeUpdate();
                }
                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Emprunt mis à jour.").showAndWait();
                Stage s = (Stage) bookIdField.getScene().getWindow();
                s.close();
                return;
            }

            // find an available copy for the given ISBN
            String copyId = null;
            try (PreparedStatement find = conn.prepareStatement("SELECT copy_id FROM copies WHERE isbn = ? AND status = 'Available' LIMIT 1")) {
                find.setString(1, bookIsbn);
                var rs = find.executeQuery();
                if (rs.next()) {
                    copyId = rs.getString("copy_id");
                }
            }
            if (copyId == null) {
                new Alert(Alert.AlertType.WARNING, "Aucune copie disponible pour ce livre (ISBN: " + bookIsbn + ").").showAndWait();
                conn.rollback();
                return;
            }

            // determine the book title for this copy and insert borrowing with the title
            String bookTitle = null;
            try (PreparedStatement getTitle = conn.prepareStatement("SELECT k.title FROM copies c JOIN books k ON c.isbn = k.isbn WHERE c.copy_id = ?")) {
                getTitle.setString(1, copyId);
                var rs = getTitle.executeQuery();
                if (rs.next()) bookTitle = rs.getString(1);
            }

            // insert borrowing (store book title redundantly for easier queries)
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO borrowing (copy_id, user_phone, borrow_date, due_date, status, book_title) VALUES (?, ?, ?, ?, 'In Progress', ?)")) {
                ins.setString(1, copyId);
                ins.setString(2, phone);
                ins.setString(3, LocalDate.now().toString());
                ins.setString(4, dueDate.toString());
                ins.setString(5, bookTitle);
                ins.executeUpdate();
            }

            // update copy status
            try (PreparedStatement upd = conn.prepareStatement("UPDATE copies SET status = 'Borrowed' WHERE copy_id = ?")) {
                upd.setString(1, copyId);
                upd.executeUpdate();
            }

            // update book available_copies
            try (PreparedStatement getIsbn = conn.prepareStatement("SELECT isbn FROM copies WHERE copy_id = ?")) {
                getIsbn.setString(1, copyId);
                var rs = getIsbn.executeQuery();
                if (rs.next()) {
                    String isbn = rs.getString("isbn");
                    try (PreparedStatement updBook = conn.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE isbn = ?")) {
                        updBook.setString(1, isbn);
                        updBook.executeUpdate();
                    }
                }
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Emprunt créé.").showAndWait();
            Stage s = (Stage) bookIdField.getScene().getWindow();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) bookIdField.getScene().getWindow();
        s.close();
    }
}

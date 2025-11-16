package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddBorrowingController {

    @FXML private ComboBox<String> copyCombo;
    @FXML private ComboBox<String> memberCombo;
    @FXML private DatePicker dueDatePicker;

    @FXML
    private void initialize() {
        loadCopies();
        loadMembers();
        dueDatePicker.setValue(LocalDate.now().plusDays(14));
    }

    private void loadCopies() {
        try (Connection conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT c.copy_id, b.title FROM copies c JOIN books b ON c.isbn = b.isbn WHERE c.status = 'Available' ORDER BY b.title")) {
            List<String> items = new ArrayList<>();
            while (rs.next()) {
                items.add(rs.getString("copy_id") + " (" + rs.getString("title") + ")");
            }
            copyCombo.getItems().setAll(items);
            if (!items.isEmpty()) copyCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMembers() {
        try (Connection conn = DatabaseUtil.connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT phone, name FROM users ORDER BY name")) {
            List<String> items = new ArrayList<>();
            while (rs.next()) {
                items.add(rs.getString("phone") + " - " + rs.getString("name"));
            }
            memberCombo.getItems().setAll(items);
            if (!items.isEmpty()) memberCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void createBorrowing() {
        var copySel = copyCombo.getValue();
        var memberSel = memberCombo.getValue();
        var dueDate = dueDatePicker.getValue();

        if (copySel == null || copySel.isEmpty() || memberSel == null || memberSel.isEmpty() || dueDate == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").showAndWait();
            return;
        }

        String copyId = copySel.split(" \\(")[0];
        String phone = memberSel.split(" - ")[0];

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            // insert borrowing
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO borrowing (copy_id, user_phone, borrow_date, due_date, status) VALUES (?, ?, ?, ?, 'In Progress')")) {
                ins.setString(1, copyId);
                ins.setString(2, phone);
                ins.setString(3, LocalDate.now().toString());
                ins.setString(4, dueDate.toString());
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
            Stage s = (Stage) copyCombo.getScene().getWindow();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) copyCombo.getScene().getWindow();
        s.close();
    }
}

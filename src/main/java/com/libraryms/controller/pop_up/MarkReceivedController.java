package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class MarkReceivedController {

    @FXML private TextField copyIdField;
    @FXML private TextField borrowingIdField;

    @FXML
    private void markReceived() {
        String copyId = copyIdField.getText().trim();
        String borrowingIdStr = borrowingIdField.getText().trim();

        if (copyId.isEmpty() && borrowingIdStr.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer le Copy ID ou l'ID d'emprunt.").showAndWait();
            return;
        }

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            Integer targetBorrowingId = null;
            if (!borrowingIdStr.isEmpty()) {
                try { targetBorrowingId = Integer.parseInt(borrowingIdStr); } catch (NumberFormatException ignored) {}
            }

            // If borrowing id supplied, use it; otherwise find latest borrowing for copy
            if (targetBorrowingId == null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM borrowing WHERE copy_id = ? AND status != 'Returned' ORDER BY borrow_date DESC LIMIT 1")) {
                    ps.setString(1, copyId);
                    var rs = ps.executeQuery();
                    if (rs.next()) targetBorrowingId = rs.getInt("id");
                }
            }

            if (targetBorrowingId == null) {
                new Alert(Alert.AlertType.WARNING, "Emprunt introuvable pour la copie fournie.").showAndWait();
                conn.rollback();
                return;
            }

            try (PreparedStatement upd = conn.prepareStatement("UPDATE borrowing SET return_date = ?, status = 'Returned' WHERE id = ?")) {
                upd.setString(1, LocalDate.now().toString());
                upd.setInt(2, targetBorrowingId);
                upd.executeUpdate();
            }

            // get copy_id if not provided
            if (copyId.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT copy_id FROM borrowing WHERE id = ?")) {
                    ps.setInt(1, targetBorrowingId);
                    var rs = ps.executeQuery();
                    if (rs.next()) copyId = rs.getString("copy_id");
                }
            }

            try (PreparedStatement ps2 = conn.prepareStatement("UPDATE copies SET status = 'Available' WHERE copy_id = ?")) {
                ps2.setString(1, copyId);
                ps2.executeUpdate();
            }

            try (PreparedStatement ps3 = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE isbn = (SELECT isbn FROM copies WHERE copy_id = ? )")) {
                ps3.setString(1, copyId);
                ps3.executeUpdate();
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Emprunt marqué comme reçu.").showAndWait();
            Stage s = (Stage) copyIdField.getScene().getWindow();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) copyIdField.getScene().getWindow();
        s.close();
    }
}

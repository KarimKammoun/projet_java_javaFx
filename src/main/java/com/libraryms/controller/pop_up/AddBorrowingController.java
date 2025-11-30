package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.dao.BorrowingDAO;
import com.libraryms.util.Session;
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
        try {
            BorrowingDAO dao = new BorrowingDAO();
            var rec = dao.findById(borrowingId);
            if (rec != null) {
                // show ISBN and member name (fall back to phone)
                if (rec.getIsbn() != null) bookIdField.setText(rec.getIsbn());
                else if (rec.getBookTitle() != null) bookIdField.setText(rec.getBookTitle());

                memberIdField.setText(rec.getMemberName() == null ? rec.getUserPhone() : rec.getMemberName());

                if (rec.getDueDate() != null) dueDatePicker.setValue(rec.getDueDate());
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
        // previously used to populate copy lists; no longer used
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

        try {
            // resolve member id -> phone (database stores user_phone in borrowing)
            String phone = null;
            try (Connection conn = DatabaseUtil.connect(); PreparedStatement ps = conn.prepareStatement("SELECT phone FROM users WHERE id = ?")) {
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
                return;
            }

            BorrowingDAO dao = new BorrowingDAO();

            if (editMode && originalBorrowingId != null) {
                dao.updateDueDate(originalBorrowingId, dueDate);
                new Alert(Alert.AlertType.INFORMATION, "Emprunt mis à jour.").showAndWait();
                Stage s = (Stage) bookIdField.getScene().getWindow();
                s.close();
                return;
            }

            // create borrowing via DAO (dao will select an available copy and update book/copy atomically)
            Integer adminId = Session.getAdminId();
            dao.createBorrowingByIsbn(bookIsbn, phone, dueDate, adminId);

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

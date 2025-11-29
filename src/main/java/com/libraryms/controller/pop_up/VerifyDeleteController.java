package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class VerifyDeleteController {

    @FXML private TextField isbnField; // book
    @FXML private TextField titleField; // book

    @FXML private TextField cinField; // member
    @FXML private TextField nameField; // member / borrowing

    @FXML private TextField idField; // borrowing id

    @FXML private Label entityTypeLabel; // should contain "book" | "user" | "borrowing"

    @FXML
    private void onCancel() {
        // close the window using any available node
        if (entityTypeLabel != null && entityTypeLabel.getScene() != null) {
            entityTypeLabel.getScene().getWindow().hide();
            return;
        }
        if (isbnField != null && isbnField.getScene() != null) isbnField.getScene().getWindow().hide();
    }

    @FXML
    private void onVerify() {
        String type = entityTypeLabel == null ? "" : entityTypeLabel.getText().trim();
        try {
            if ("book".equals(type)) {
                String isbn = isbnField.getText().trim();
                String title = titleField.getText().trim();
                if (isbn.isEmpty() || title.isEmpty()) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Remplissez ISBN et Titre").showAndWait();
                    return;
                }
                boolean found = false;
                try (Connection conn = DatabaseUtil.connect();
                     PreparedStatement ps = conn.prepareStatement("SELECT isbn FROM books WHERE isbn = ? AND title = ?")) {
                    ps.setString(1, isbn);
                    ps.setString(2, title);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) found = true;
                    }
                }
                if (found) {
                    openConfirm("book", isbn, "Supprimer le livre " + title + " (" + isbn + ") ?");
                } else {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun livre trouvé avec cet ISBN et titre.").showAndWait();
                }
            } else if ("user".equals(type)) {
                String cin = cinField.getText().trim();
                String name = nameField.getText().trim();
                if (cin.isEmpty() || name.isEmpty()) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Remplissez CIN et Nom").showAndWait();
                    return;
                }
                String phone = null;
                try (Connection conn = DatabaseUtil.connect();
                     PreparedStatement ps = conn.prepareStatement("SELECT phone FROM users WHERE cin = ? AND name = ?")) {
                    ps.setString(1, cin);
                    ps.setString(2, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) phone = rs.getString("phone");
                    }
                }
                if (phone == null) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun membre trouvé avec ce CIN et nom.").showAndWait();
                    return;
                }
                openConfirm("user", phone, "Supprimer le membre " + name + " (" + phone + ") ?");
            } else if ("borrowing".equals(type)) {
                String idText = idField.getText().trim();
                String name = nameField.getText().trim();
                if (idText.isEmpty() || name.isEmpty()) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Remplissez l'ID et le nom").showAndWait();
                    return;
                }
                try {
                    int borrowingId = Integer.parseInt(idText);
                    boolean found = false;
                    try (Connection conn = DatabaseUtil.connect();
                         PreparedStatement ps = conn.prepareStatement("SELECT b.id FROM borrowing b JOIN users u ON b.user_phone = u.phone WHERE b.id = ? AND u.name = ?")) {
                        ps.setInt(1, borrowingId);
                        ps.setString(2, name);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) found = true;
                        }
                    }
                    if (found) {
                        openConfirm("borrowing", idText, "Supprimer l'emprunt ID " + borrowingId + " ?");
                    } else {
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun emprunt trouvé avec cet ID et nom.").showAndWait();
                    }
                } catch (NumberFormatException nfe) {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "ID invalide").showAndWait();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }

    private void openConfirm(String entityType, String identifier, String message) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/confirm_delete.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ConfirmDeleteController) {
                ((ConfirmDeleteController) controller).setContext(entityType, identifier, message);
            }
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Confirmer la suppression");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // close own window
            onCancel();
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }
}

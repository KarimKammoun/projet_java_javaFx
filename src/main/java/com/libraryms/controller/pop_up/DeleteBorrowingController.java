package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteBorrowingController {

    @FXML private TextField idField;
    @FXML private TextField nameField;

    @FXML
    private void onCancel() {
        idField.getScene().getWindow().hide();
    }

    @FXML
    private void onVerify() {
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
                    if (rs.next()) {
                        found = true;
                    }
                }
            }
            
            if (found) {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/confirm_delete.fxml"));
                javafx.scene.Parent root = loader.load();
                Object controller = loader.getController();
                if (controller instanceof ConfirmDeleteController) {
                    ((ConfirmDeleteController) controller).setContext("borrowing", idText, "Supprimer l'emprunt ID " + borrowingId + " ?");
                }
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Confirmer la suppression");
                stage.setScene(scene);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                idField.getScene().getWindow().hide();
            } else {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun emprunt trouvé avec cet ID et nom.").showAndWait();
            }
        } catch (NumberFormatException nfe) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "ID invalide").showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }
}

package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteBookController {

    @FXML private TextField isbnField;
    @FXML private TextField titleField;

    @FXML
    private void onCancel() {
        isbnField.getScene().getWindow().hide();
    }

    @FXML
    private void onVerify() {
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
                if (rs.next()) {
                    found = true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
            return;
        }
        
        if (found) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/confirm_delete.fxml"));
                javafx.scene.Parent root = loader.load();
                Object controller = loader.getController();
                if (controller instanceof ConfirmDeleteController) {
                    ((ConfirmDeleteController) controller).setContext("book", isbn, "Supprimer le livre " + title + " (" + isbn + ") ?");
                }
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                com.libraryms.util.SceneManager.applyGlobalStyles(scene);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Confirmer la suppression");
                stage.setScene(scene);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();
                isbnField.getScene().getWindow().hide();
            } catch (Exception ex) {
                ex.printStackTrace();
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
            }
        } else {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun livre trouvé avec cet ISBN et titre.").showAndWait();
        }
    }
}

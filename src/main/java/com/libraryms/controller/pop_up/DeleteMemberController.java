package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeleteMemberController {

    @FXML private TextField cinField;
    @FXML private TextField nameField;

    @FXML
    private void onCancel() {
        cinField.getScene().getWindow().hide();
    }

    @FXML
    private void onVerify() {
        String cin = cinField.getText().trim();
        String name = nameField.getText().trim();
        if (cin.isEmpty() || name.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Remplissez CIN et Nom").showAndWait();
            return;
        }

        // Find member by CIN and Name and get phone
        String phone = null;
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT phone FROM users WHERE cin = ? AND name = ?")) {
            ps.setString(1, cin);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    phone = rs.getString("phone");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
            return;
        }

        if (phone == null) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Aucun membre trouvé avec ce CIN et nom.").showAndWait();
            return;
        }

        // open confirmation dialog
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/confirm_delete.fxml"));
            javafx.scene.Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ConfirmDeleteController) {
                ((ConfirmDeleteController) controller).setContext("user", phone, "Supprimer le membre " + name + " (" + phone + ") ?");
            }
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Confirmer la suppression");
            stage.setScene(scene);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            cinField.getScene().getWindow().hide();
        } catch (Exception ex) {
            ex.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
        }
    }
}

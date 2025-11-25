package com.libraryms.controller.pop_up;

import com.libraryms.util.DatabaseUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CreateAdminController {

    @FXML private TextField emailField;
    @FXML private TextField nameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    private void createAdmin() {
        String email = emailField.getText().trim();
        String name = nameField.getText().trim();
        String pw = passwordField.getText();
        String pw2 = confirmPasswordField.getText();

        if (email.isEmpty() || name.isEmpty() || pw.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Tous les champs sont obligatoires.").showAndWait();
            return;
        }
        if (!pw.equals(pw2)) {
            new Alert(Alert.AlertType.WARNING, "Les mots de passe ne correspondent pas.").showAndWait();
            return;
        }

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);
            String hashed = BCrypt.withDefaults().hashToString(12, pw.toCharArray());
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO admin (email, password, name) VALUES (?,?,?)")) {
                ps.setString(1, email);
                ps.setString(2, hashed);
                ps.setString(3, name);
                ps.executeUpdate();
            }
            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Administrateur créé.").showAndWait();
            Stage s = (Stage) emailField.getScene().getWindow();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) emailField.getScene().getWindow();
        s.close();
    }
}

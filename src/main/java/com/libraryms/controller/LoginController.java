package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ToggleButton adminBtn;


    @FXML
    private void handleLogin() {
        String emailOrPhone = emailField.getText().trim();
        String password = passwordField.getText();
        boolean isAdmin = adminBtn.isSelected();

        if (isAdmin) {
            // Admin login: requires email and password
            if (emailOrPhone.isEmpty() || password.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir l'email et le mot de passe").show();
                return;
            }
            loginAsAdmin(emailOrPhone, password);
        } else {
            // Member login: phone only (no password check)
            if (emailOrPhone.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir le numéro de téléphone").show();
                return;
            }
            loginAsMember(emailOrPhone);
        }
    }

    private void loginAsAdmin(String email, String password) {
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement("SELECT email, password, name FROM admin WHERE email = ?")) {
            stmt.setString(1, email);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                if (stored != null && stored.equals(password)) {
                    Session.setAdmin(true);
                    Session.setEmail(rs.getString("email"));
                    Session.setName(rs.getString("name"));
                    Session.setPhone(null);
                    SceneManager.loadScene("/fxml/main_layout.fxml");
                } else {
                    new Alert(Alert.AlertType.ERROR, "Mot de passe incorrect").show();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Admin non trouvé").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur DB: " + e.getMessage()).show();
        }
    }

    private void loginAsMember(String phone) {
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement("SELECT phone, name, email FROM users WHERE phone = ?")) {
            stmt.setString(1, phone);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                Session.setAdmin(false);
                Session.setEmail(rs.getString("email"));
                Session.setName(rs.getString("name"));
                Session.setPhone(rs.getString("phone"));
                SceneManager.loadScene("/fxml/main_layout.fxml");
            } else {
                new Alert(Alert.AlertType.ERROR, "Membre non trouvé").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur DB: " + e.getMessage()).show();
        }
    }
}
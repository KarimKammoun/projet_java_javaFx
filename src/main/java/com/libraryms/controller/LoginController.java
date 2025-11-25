package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ToggleButton adminBtn;
    @FXML private ToggleButton memberBtn;

    @FXML
    private void initialize() {
        // make the two toggle buttons exclusive
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        adminBtn.setToggleGroup(group);
        memberBtn.setToggleGroup(group);
        adminBtn.setSelected(true);
    }

    @FXML
    private void openCreateAdmin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/create_admin.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Créer un administrateur");
            stage.setScene(scene);
            stage.initOwner(emailField.getScene().getWindow());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Impossible d'ouvrir la fenêtre: " + e.getMessage()).show();
        }
    }

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
            // Member login: phone and password required
            if (emailOrPhone.isEmpty() || password.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir le téléphone et le mot de passe").show();
                return;
            }
            loginAsMember(emailOrPhone, password);
        }
    }

    private void loginAsAdmin(String email, String password) {
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement("SELECT id, email, password, name FROM admin WHERE email = ?")) {
            stmt.setString(1, email);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password");
                boolean ok = false;
                // support legacy plain-text seed and hashed passwords
                if (stored != null && stored.equals(password)) {
                    ok = true;
                } else {
                    try {
                        ok = at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password.toCharArray(), stored).verified;
                    } catch (Exception ex) {
                        ok = false;
                    }
                }
                if (ok) {
                    Session.setAdmin(true);
                    Session.setEmail(rs.getString("email"));
                    Session.setName(rs.getString("name"));
                    Session.setPhone(null);
                    Session.setAdminId(rs.getInt("id"));
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

    private void loginAsMember(String phone, String password) {
        try (var conn = DatabaseUtil.connect();
             var stmt = conn.prepareStatement("SELECT phone, name, email, password FROM users WHERE phone = ?")) {
            stmt.setString(1, phone);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHashedPassword = rs.getString("password");
                // Verify the password against the hashed password in the DB
                if (storedHashedPassword != null && BCrypt.verifyer().verify(password.toCharArray(), storedHashedPassword).verified) {
                    Session.setAdmin(false);
                    Session.setEmail(rs.getString("email"));
                    Session.setName(rs.getString("name"));
                    Session.setPhone(rs.getString("phone"));
                    SceneManager.loadScene("/fxml/member_layout.fxml");
                } else {
                    new Alert(Alert.AlertType.ERROR, "Mot de passe incorrect").show();
                }
            } else {
                new Alert(Alert.AlertType.ERROR, "Membre non trouvé").show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur DB: " + e.getMessage()).show();
        }
    }
}
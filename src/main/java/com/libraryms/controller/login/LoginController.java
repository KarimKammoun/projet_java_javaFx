package com.libraryms.controller.login;

import com.libraryms.dao.AdminDAO;
import com.libraryms.dao.MemberDAO;
import com.libraryms.models.Admin;
import com.libraryms.models.Member;
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/pop_up/create_admin.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            com.libraryms.util.SceneManager.applyGlobalStyles(scene);
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
        try {
            AdminDAO dao = new AdminDAO();
            Admin admin = dao.findByEmail(email);
            
            if (admin != null) {
                if (dao.verifyPassword(admin, password)) {
                    Session.setAdmin(true);
                    Session.setEmail(admin.getEmail());
                    Session.setName(admin.getName());
                    Session.setPhone(null);
                    Session.setAdminId(admin.getId());
                    SceneManager.loadScene("/fxml/admin/main_layout.fxml");
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
        try {
            MemberDAO dao = new MemberDAO();
            Member member = dao.findByPhone(phone);
            
            if (member != null) {
                // Verify password
                boolean ok = false;
                String stored = member.getPasswordHash();
                if (stored != null && BCrypt.verifyer().verify(password.toCharArray(), stored).verified) {
                    ok = true;
                }
                
                if (ok) {
                    Session.setAdmin(false);
                    Session.setEmail(member.getEmail());
                    Session.setName(member.getName());
                    Session.setPhone(member.getPhone());
                    // set the admin id the member belongs to so member views are scoped
                    try {
                        Integer aId = member.getAdminId();
                        Session.setAdminId(aId);
                    } catch (Exception ex) {
                        Session.setAdminId(null);
                    }
                    SceneManager.loadScene("/fxml/user/member_layout.fxml");
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
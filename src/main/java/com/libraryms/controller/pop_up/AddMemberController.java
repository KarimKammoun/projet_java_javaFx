package com.libraryms.controller.pop_up;

import com.libraryms.dao.MemberDAO;
import com.libraryms.models.Member;
import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.SQLException;

public class AddMemberController {

    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private TextField cinField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> typeCombo;

    @FXML
    private void initialize() {
        if (typeCombo != null) typeCombo.getItems().setAll("Standard", "Premium");
    }

    private boolean editMode = false;
    private String originalPhone = null;

    public void loadForEdit(String phone) {
        MemberDAO dao = new MemberDAO();
        try {
            Member m = dao.findByPhone(phone);
            if (m != null) {
                phoneField.setText(m.getPhone());
                nameField.setText(m.getName());
                emailField.setText(m.getEmail());
                cinField.setText(m.getCin());
                typeCombo.setValue(m.getType());
                originalPhone = phone;
                editMode = true;
                passwordField.clear();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load member for editing by matching email and CIN.
     * Returns true if a member was found and loaded, false otherwise.
     */
    public boolean loadForEditByEmailAndCin(String email, String cin) {
        if (email == null || email.trim().isEmpty() || cin == null || cin.trim().isEmpty()) {
            return false;
        }
        MemberDAO dao = new MemberDAO();
        try {
            Member m = dao.findByEmailAndCin(email.trim(), cin.trim());
            if (m != null) {
                loadForEdit(m.getPhone());
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    @FXML
    private void loadById() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer un numéro de téléphone").showAndWait();
            return;
        }
        loadForEdit(phone);
    }

    @FXML
    private void saveMember() {
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String cin = cinField.getText().trim();
        String type = typeCombo.getValue() == null ? "Standard" : typeCombo.getValue();

        if (phone.isEmpty() || name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Phone et Name sont obligatoires.").showAndWait();
            return;
        }

        if (!editMode && password.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Le mot de passe est obligatoire pour un nouveau membre.").showAndWait();
            return;
        }

        MemberDAO dao = new MemberDAO();
        try {
            if (editMode && originalPhone != null) {
                if (!originalPhone.equals(phone) && dao.existsByPhone(phone)) {
                    new Alert(Alert.AlertType.ERROR, "Un membre avec ce téléphone existe déjà.").showAndWait();
                    return;
                }

                if (email != null && !email.isEmpty() && dao.existsByEmailExceptPhone(email, originalPhone)) {
                    new Alert(Alert.AlertType.ERROR, "Un membre avec cet email existe déjà.").showAndWait();
                    return;
                }

                String hashed = null;
                if (!password.isEmpty()) hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());

                Member m = new Member(phone, name, email.isEmpty() ? null : email, cin.isEmpty() ? null : cin, type, hashed, com.libraryms.util.Session.getAdminId());
                dao.update(m, originalPhone);

                new Alert(Alert.AlertType.INFORMATION, "Membre mis à jour.").showAndWait();
                Stage s = (Stage) phoneField.getScene().getWindow();
                s.close();
                return;
            }

            // create
            if (dao.existsByPhone(phone)) {
                new Alert(Alert.AlertType.ERROR, "Un membre avec ce téléphone existe déjà.").showAndWait();
                return;
            }

            if (email != null && !email.isEmpty() && dao.existsByEmailExceptPhone(email, null)) {
                new Alert(Alert.AlertType.ERROR, "Un membre avec cet email existe déjà.").showAndWait();
                return;
            }

            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            Member m = new Member(phone, name, email.isEmpty() ? null : email, cin.isEmpty() ? null : cin, type, hashedPassword, com.libraryms.util.Session.getAdminId());
            dao.create(m);

            new Alert(Alert.AlertType.INFORMATION, "Membre ajouté.").showAndWait();
            Stage s = (Stage) phoneField.getScene().getWindow();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void cancel() {
        Stage s = (Stage) phoneField.getScene().getWindow();
        s.close();
    }
}

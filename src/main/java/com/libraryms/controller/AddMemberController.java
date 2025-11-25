package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
        // populate fields from DB for editing
        try (var conn = DatabaseUtil.connect();
             var ps = conn.prepareStatement("SELECT phone, name, email, type, cin FROM users WHERE phone = ?")) {
            ps.setString(1, phone);
            var rs = ps.executeQuery();
            if (rs.next()) {
                phoneField.setText(rs.getString("phone"));
                nameField.setText(rs.getString("name"));
                emailField.setText(rs.getString("email"));
                cinField.setText(rs.getString("cin"));
                typeCombo.setValue(rs.getString("type"));
                originalPhone = phone;
                editMode = true;
                // Password field is cleared for security (user must re-enter if they want to change it)
                passwordField.clear();
            }
        } catch (Exception e) {
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
        try (var conn = DatabaseUtil.connect();
             var ps = conn.prepareStatement("SELECT phone FROM users WHERE email = ? AND cin = ?")) {
            ps.setString(1, email.trim());
            ps.setString(2, cin.trim());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String phone = rs.getString("phone");
                    loadForEdit(phone);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @FXML
    private void loadById() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING, "Veuillez entrer le téléphone.").showAndWait();
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

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

            if (editMode && originalPhone != null) {
                // if phone changed, ensure new phone not already used
                if (!originalPhone.equals(phone)) {
                    try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE phone = ?")) {
                        check.setString(1, phone);
                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                conn.rollback();
                                new Alert(Alert.AlertType.ERROR, "Un membre avec ce téléphone existe déjà.").showAndWait();
                                return;
                            }
                        }
                    }
                }

                try (PreparedStatement upd = conn.prepareStatement("UPDATE users SET phone = ?, name = ?, email = ?, type = ?, cin = ?" + (password.isEmpty() ? "" : ", password = ?") + " WHERE phone = ?")) {
                    upd.setString(1, phone);
                    upd.setString(2, name);
                    upd.setString(3, email.isEmpty() ? null : email);
                    upd.setString(4, type);
                    upd.setString(5, cin.isEmpty() ? null : cin);
                    int idx = 6;
                    if (!password.isEmpty()) {
                        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
                        upd.setString(idx++, hashedPassword);
                    }
                    upd.setString(idx, originalPhone);
                    upd.executeUpdate();
                }

                // update borrowings referencing the old phone
                if (!originalPhone.equals(phone)) {
                    try (PreparedStatement updBorrow = conn.prepareStatement("UPDATE borrowing SET user_phone = ? WHERE user_phone = ?")) {
                        updBorrow.setString(1, phone);
                        updBorrow.setString(2, originalPhone);
                        updBorrow.executeUpdate();
                    }
                }

                conn.commit();
                new Alert(Alert.AlertType.INFORMATION, "Membre mis à jour.").showAndWait();
                Stage s = (Stage) phoneField.getScene().getWindow();
                s.close();
                return;
            }

            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE phone = ?")) {
                check.setString(1, phone);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        new Alert(Alert.AlertType.ERROR, "Un membre avec ce téléphone existe déjà.").showAndWait();
                        return;
                    }
                }
            }

            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users (phone, name, email, type, cin, password, admin_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ins.setString(1, phone);
                ins.setString(2, name);
                ins.setString(3, email.isEmpty() ? null : email);
                ins.setString(4, type);
                ins.setString(5, cin.isEmpty() ? null : cin);
                ins.setString(6, hashedPassword);
                // link member to current admin if available
                Integer adminId = com.libraryms.util.Session.getAdminId();
                if (adminId != null) ins.setInt(7, adminId); else ins.setNull(7, java.sql.Types.INTEGER);
                ins.executeUpdate();
            }

            conn.commit();
            new Alert(Alert.AlertType.INFORMATION, "Membre ajouté.").showAndWait();
            Stage s = (Stage) phoneField.getScene().getWindow();
            s.close();
        } catch (Exception e) {
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

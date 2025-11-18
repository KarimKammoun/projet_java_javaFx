package com.libraryms.controller;

import com.libraryms.util.DatabaseUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddMemberController {

    @FXML private TextField phoneField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> typeCombo;

    @FXML
    private void initialize() {
        if (typeCombo != null) typeCombo.getItems().setAll("Standard", "Premium");
    }

    @FXML
    private void saveMember() {
        String phone = phoneField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String type = typeCombo.getValue() == null ? "Standard" : typeCombo.getValue();

        if (phone.isEmpty() || name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Phone et Name sont obligatoires.").showAndWait();
            return;
        }

        try (Connection conn = DatabaseUtil.connect()) {
            conn.setAutoCommit(false);

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

            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users (phone, name, email, type) VALUES (?, ?, ?, ?)")) {
                ins.setString(1, phone);
                ins.setString(2, name);
                ins.setString(3, email.isEmpty() ? null : email);
                ins.setString(4, type);
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

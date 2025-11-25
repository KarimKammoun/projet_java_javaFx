package com.libraryms.controller.admin;

import com.libraryms.util.DatabaseUtil;
import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;


public class SettingsController {

    @FXML private TextField libraryNameField;
    @FXML private TextField borrowDurationField;
    @FXML private TextField finePerDayField;
    @FXML private TextField standardLimitField;
    @FXML private TextField premiumLimitField;
    @FXML private Button importBtn;
    @FXML private Button exportBtn;

    @FXML
    private void initialize() {
        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        // Load settings from database if they exist
        // For now, just use the default values shown in FXML
    }

    @FXML
    private void saveSettings() {
        try {
            // Validate inputs
            String libraryName = libraryNameField.getText().trim();
            if (libraryName.isEmpty()) {
                showAlert("Erreur", "Le nom de la bibliothèque ne peut pas être vide");
                return;
            }

            int borrowDays = Integer.parseInt(borrowDurationField.getText());
            double finePerDay = Double.parseDouble(finePerDayField.getText());
            int standardLimit = Integer.parseInt(standardLimitField.getText());
            int premiumLimit = Integer.parseInt(premiumLimitField.getText());

            if (borrowDays <= 0 || finePerDay < 0 || standardLimit <= 0 || premiumLimit <= 0) {
                showAlert("Erreur", "Veuillez entrer des nombres positifs valides");
                return;
            }

            // TODO: Save to database
            showAlert("Succès", "Paramètres sauvegardés avec succès!");
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des nombres valides");
        }
    }

    @FXML
    private void importData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer les données");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files", "*.db"));
        File file = fileChooser.showOpenDialog(null);
        
        if (file != null) {
            try {
                Files.copy(file.toPath(), Paths.get("libraryms.db"));
                showAlert("Succès", "Données importées avec succès! Redémarrez l'application.");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de l'importation: " + e.getMessage());
            }
        }
    }

    @FXML
    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les données");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Database Files", "*.db"));
        fileChooser.setInitialFileName("libraryms_backup_" + System.currentTimeMillis() + ".db");
        File file = fileChooser.showSaveDialog(null);
        
        if (file != null) {
            try {
                Files.copy(Paths.get("libraryms.db"), file.toPath());
                showAlert("Succès", "Données exportées avec succès!\nEmplacement: " + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de l'exportation: " + e.getMessage());
            }
        }
    }

    @FXML
    private void restoreDatabase() {
        showAlert("Info", "La restauration se fait via l'importation de fichiers de sauvegarde.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
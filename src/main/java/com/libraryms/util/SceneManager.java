package com.libraryms.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage s) { stage = s; }

    public static void loadScene(String fxml) {
        if (stage == null) {
            throw new IllegalStateException("Stage non défini. Appelez SceneManager.setStage(stage) depuis Application.start().");
        }
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxml));
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + fxml + "\n" + e.getMessage()).show();
        }
    }
}
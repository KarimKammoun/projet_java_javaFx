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

        // Protect access: only allow login page if not logged in, and only allow authenticated pages if logged in
        boolean isLoginPage = fxml.contains("login");
        boolean isLoggedIn = Session.isLoggedIn();

        if (!isLoginPage && !isLoggedIn) {
            // Trying to access protected page without login
            new Alert(Alert.AlertType.WARNING, "Veuillez vous connecter d'abord.").show();
            loadScene("/fxml/login.fxml");
            return;
        }

        if (isLoginPage && isLoggedIn) {
            // Already logged in, don't allow going back to login page
            // (optional: comment this out if you want to allow logout via login page)
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
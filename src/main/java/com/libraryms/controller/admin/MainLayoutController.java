package com.libraryms.controller.admin;

import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class MainLayoutController {

    @FXML private AnchorPane contentPane;
    @FXML private Button btnDashboard, btnMembers, btnBooks, btnBorrowings, btnSettings;

    @FXML
    private void initialize() {
        // Load default content based on session
        if (Session.isAdmin()) showDashboard(); else showMemberHome();
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            Node root = FXMLLoader.load(getClass().getResource(fxmlPath));
            if (contentPane == null) {
                System.err.println("contentPane is null in MainLayoutController when loading: " + fxmlPath);
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Internal UI error: content area missing.");
                a.showAndWait();
                return;
            }
            contentPane.getChildren().setAll(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
            // show user-friendly alert with error details
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur en chargeant: " + fxmlPath + "\n" + msg);
            alert.setHeaderText("Erreur de chargement de la page");
            alert.showAndWait();
        }
    }

    @FXML private void showDashboard() { loadIntoContent("/fxml/admin/dashboard.fxml"); }
    @FXML private void showMembers() { loadIntoContent("/fxml/admin/members.fxml"); }
    @FXML private void showBooks() { loadIntoContent("/fxml/admin/books.fxml"); }
    @FXML private void showBorrowings() { loadIntoContent("/fxml/admin/borrowings.fxml"); }
    @FXML private void showSettings() { loadIntoContent("/fxml/admin/settings.fxml"); }

    private void showMemberHome() { loadIntoContent("/fxml/user/member_home.fxml"); }

    @FXML
    private void logout() {
        // Clear session and redirect to login
        Session.logout();
        SceneManager.loadScene("/fxml/login/login.fxml");
    }
}

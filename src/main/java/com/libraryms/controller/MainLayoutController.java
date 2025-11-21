package com.libraryms.controller;

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
        if (Session.isAdmin()) showDashboard(); else showMemberDashboard();
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            Node root = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(root);
            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void showDashboard() { loadIntoContent("/fxml/dashboard.fxml"); }
    @FXML private void showMembers() { loadIntoContent("/fxml/members.fxml"); }
    @FXML private void showBooks() { loadIntoContent("/fxml/books.fxml"); }
    @FXML private void showBorrowings() { loadIntoContent("/fxml/borrowings.fxml"); }
    @FXML private void showSettings() { loadIntoContent("/fxml/settings.fxml"); }

    private void showMemberDashboard() { loadIntoContent("/fxml/member_dashboard.fxml"); }

    @FXML
    private void logout() {
        // Clear session and redirect to login
        Session.logout();
        SceneManager.loadScene("/fxml/login.fxml");
    }
}

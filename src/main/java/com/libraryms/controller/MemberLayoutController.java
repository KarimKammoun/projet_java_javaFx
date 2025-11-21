package com.libraryms.controller;

import com.libraryms.util.SceneManager;
import com.libraryms.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

public class MemberLayoutController {

    @FXML private AnchorPane contentPane;

    @FXML
    private void initialize() {
        goHome();
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

    @FXML private void goHome() { loadIntoContent("/fxml/member_home.fxml"); }
    @FXML private void goBorrowingHistory() { loadIntoContent("/fxml/member_borrowing_history.fxml"); }

    @FXML
    private void logout() {
        Session.logout();
        SceneManager.loadScene("/fxml/login.fxml");
    }
}

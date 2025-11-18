package com.libraryms.controller;

import com.libraryms.util.SceneManager;
import javafx.fxml.FXML;

public class SettingsController {

    @FXML private void goToDashboard() { SceneManager.loadScene("/fxml/dashboard.fxml"); }
    @FXML private void goToBooks() { SceneManager.loadScene("/fxml/books.fxml"); }
    @FXML private void goToCopies() { SceneManager.loadScene("/fxml/copies.fxml"); }
    @FXML private void goToMembers() { SceneManager.loadScene("/fxml/members.fxml"); }
    @FXML private void goToBorrowings() { SceneManager.loadScene("/fxml/borrowings.fxml"); }
    @FXML private void goToSettings() { SceneManager.loadScene("/fxml/settings.fxml"); }
}
package com.libraryms.controller;

import com.libraryms.util.SceneManager;
import javafx.fxml.FXML;

public class MemberDashboardController {
    @FXML private void goToBrowse() { SceneManager.loadScene("/fxml/member_dashboard.fxml"); }
    @FXML private void goToBorrowings() { SceneManager.loadScene("/fxml/member_borrowings.fxml"); }
}
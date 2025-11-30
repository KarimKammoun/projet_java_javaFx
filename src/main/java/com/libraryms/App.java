package com.libraryms;

import com.libraryms.util.InitDB;
import com.libraryms.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize database on app start
        try {
            InitDB.initializeDatabase();
        } catch (Exception e) {
            System.out.println("Failed to initialize database: " + e.getMessage());
        }

        SceneManager.setStage(stage);
        SceneManager.loadScene("/fxml/login/login.fxml");
        stage.setTitle("Library Management System");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
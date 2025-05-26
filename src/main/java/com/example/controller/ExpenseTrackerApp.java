package com.example.controller;

import com.example.util.HibernateUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ExpenseTrackerApp extends Application {
    private Stage primaryStage;
    private Scene loginScene;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Personal Expenses Tracker");

        // Initialize login scene
        LoginController loginController = new LoginController(primaryStage);
        loginScene = new Scene(loginController.createLoginPage(), 800, 600);
        loginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set initial scene
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package com.example.controller;

import com.example.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainMenuController {
    private final Stage primaryStage;
    private final User user;

    public MainMenuController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
    }

    public VBox createMainMenu() {
        VBox layout = new VBox(30);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Personal Expenses Tracker");
        title.setStyle("-fx-font-size: 24px;"); // Kept for emphasis, styled in CSS

        Button expensesButton = new Button("Manage Expenses");
        Button summaryButton = new Button("View Summary & Reports");
        Button logoutButton = new Button("Logout");

        expensesButton.setOnAction(e -> {
            ExpenseController expenseController = new ExpenseController(primaryStage, user);
            Scene expenseScene = new Scene(expenseController.createExpensePage(), 800, 600);
            expenseScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(expenseScene);
        });

        summaryButton.setOnAction(e -> {
            SummaryController summaryController = new SummaryController(primaryStage, user);
            Scene summaryScene = new Scene(summaryController.createSummaryPage(), 800, 600);
            summaryScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(summaryScene);
        });

        logoutButton.setOnAction(e -> {
            LoginController loginController = new LoginController(primaryStage);
            Scene loginScene = new Scene(loginController.createLoginPage(), 800, 600);
            loginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(loginScene);
        });

        layout.getChildren().addAll(title, expensesButton, summaryButton, logoutButton);
        return layout;
    }
}
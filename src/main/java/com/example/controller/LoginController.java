package com.example.controller;

import com.example.model.User;
import com.example.service.UserService;
import com.example.util.HibernateUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {
    private final Stage primaryStage;
    private final UserService userService;

    public LoginController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.userService = new UserService(HibernateUtil.getSessionFactory());
    }

    public VBox createLoginPage() {
        VBox layout = new VBox(30);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Login");
        title.setStyle("-fx-font-size: 24px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Button signupButton = new Button("Go to Signup");

        loginButton.setOnAction(e -> {
            User user = userService.login(usernameField.getText(), passwordField.getText());
            if (user != null) {
                MainMenuController mainMenuController = new MainMenuController(primaryStage, user);
                Scene mainMenuScene = new Scene(mainMenuController.createMainMenu(), 800, 600);
                mainMenuScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                primaryStage.setScene(mainMenuScene);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }
        });

        signupButton.setOnAction(e -> {
            SignupController signupController = new SignupController(primaryStage);
            Scene signupScene = new Scene(signupController.createSignupPage(), 800, 600);
            signupScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(signupScene);
        });

        layout.getChildren().addAll(title, usernameField, passwordField, loginButton, signupButton);
        return layout;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
package com.example.controller;

import com.example.service.UserService;
import com.example.util.HibernateUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignupController {
    private final Stage primaryStage;
    private final UserService userService;

    public SignupController(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.userService = new UserService(HibernateUtil.getSessionFactory());
    }

    public VBox createSignupPage() {
        VBox layout = new VBox(30);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label title = new Label("Signup");
        title.setStyle("-fx-font-size: 24px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 characters)");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");

        Button signupButton = new Button("Signup");
        Button loginButton = new Button("Back to Login");

        signupButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Passwords do not match.");
                return;
            }
            if (userService.signup(username, password)) {
                LoginController loginController = new LoginController(primaryStage);
                Scene loginScene = new Scene(loginController.createLoginPage(), 800, 600);
                loginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                primaryStage.setScene(loginScene);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Signup Failed", "Username already exists or password too short.");
            }
        });

        loginButton.setOnAction(e -> {
            LoginController loginController = new LoginController(primaryStage);
            Scene loginScene = new Scene(loginController.createLoginPage(), 800, 600);
            loginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(loginScene);
        });

        layout.getChildren().addAll(title, usernameField, passwordField, confirmPasswordField, signupButton,
                loginButton);
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
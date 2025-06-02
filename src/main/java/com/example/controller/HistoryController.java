
package com.example.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.model.Expense;
import com.example.model.User;
import com.example.service.ExpenseService;
import com.example.util.HibernateUtil;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HistoryController {
    private final Stage primaryStage;
    private final User user;
    private final ExpenseService expenseService;
    private TreeView<String> expenseTreeView;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    public HistoryController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.expenseService = new ExpenseService(HibernateUtil.getSessionFactory());
    }

    public VBox createHistoryPage() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        // Date range filter
        Label startDateLabel = new Label("Start Date:");
        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Select start date");
        Label endDateLabel = new Label("End Date:");
        endDatePicker = new DatePicker(); // Corrected from Label to DatePicker
        endDatePicker.setPromptText("Select end date");
        Button filterButton = new Button("Apply Filter");

        HBox filterBox = new HBox(10, startDateLabel, startDatePicker, endDateLabel, endDatePicker, filterButton);
        filterBox.setPadding(new Insets(5));

        // TreeView for categorized expenses
        expenseTreeView = new TreeView<>();
        expenseTreeView.setPrefHeight(400);
        TreeItem<String> rootItem = new TreeItem<>("Expenses");
        rootItem.setExpanded(true);
        expenseTreeView.setRoot(rootItem);

        // Load initial data
        loadExpenseHistory(null, null);

        // Filter action
        filterButton.setOnAction(e -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
                loadExpenseHistory(startDate, endDate);
            } else if (startDate == null && endDate == null) {
                loadExpenseHistory(null, null);
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid Filter", "Please select valid start and end dates.");
            }
        });

        // Back button
        Button backButton = new Button("Back to Menu");
        backButton.setOnAction(e -> {
            MainMenuController mainMenuController = new MainMenuController(primaryStage, user);
            Scene mainMenuScene = new Scene(mainMenuController.createMainMenu(), 800, 600);
            mainMenuScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(mainMenuScene);
        });

        layout.getChildren().addAll(new Label("Expense History by Category"), filterBox, expenseTreeView, backButton);
        return layout;
    }

    private void loadExpenseHistory(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = expenseService.getExpensesByUser(user);
        Map<String, TreeItem<String>> categoryNodes = new HashMap<>();

        // Clear existing children
        expenseTreeView.getRoot().getChildren().clear();

        if (expenses.isEmpty()) {
            System.out.println("No expenses found for user: " + user.getId());
            TreeItem<String> emptyNode = new TreeItem<>("No expenses available.");
            expenseTreeView.getRoot().getChildren().add(emptyNode);
            return;
        }

        // Group expenses by category
        for (Expense expense : expenses) {
            if (expense.getCategory() == null) {
                System.err.println("Warning: Expense " + expense.getDescription() + " has no category.");
                continue;
            }
            // Apply date filter if provided
            if ((startDate != null && expense.getDate().isBefore(startDate)) ||
                    (endDate != null && expense.getDate().isAfter(endDate))) {
                continue;
            }

            String categoryName = expense.getCategory().getName();
            TreeItem<String> categoryNode = categoryNodes.computeIfAbsent(categoryName, k -> {
                TreeItem<String> node = new TreeItem<>(categoryName);
                node.setExpanded(true);
                return node;
            });

            String expenseDetails = String.format("%s - $%.2f (Date: %s, Income: %s)",
                    expense.getDescription(),
                    expense.getAmount(),
                    expense.getDate(),
                    expense.getIncome() != null ? expense.getIncome().getDescription() : "N/A");
            TreeItem<String> expenseNode = new TreeItem<>(expenseDetails);
            categoryNode.getChildren().add(expenseNode);

            if (!expenseTreeView.getRoot().getChildren().contains(categoryNode)) {
                expenseTreeView.getRoot().getChildren().add(categoryNode);
            }
        }

        if (expenseTreeView.getRoot().getChildren().isEmpty()) {
            TreeItem<String> emptyNode = new TreeItem<>("No expenses match the selected date range.");
            expenseTreeView.getRoot().getChildren().add(emptyNode);
        }

        System.out.println(
                "Loaded " + expenses.size() + " expenses, grouped into " + categoryNodes.size() + " categories.");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
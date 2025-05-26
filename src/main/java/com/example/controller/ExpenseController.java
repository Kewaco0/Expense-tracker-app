package com.example.controller;

import java.time.LocalDate;

import com.example.model.Category;
import com.example.model.Expense;
import com.example.model.User;
import com.example.service.CategoryService;
import com.example.service.ExpenseService;
import com.example.util.HibernateUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExpenseController {
    private final Stage primaryStage;
    private final User user;
    private final ExpenseService expenseService;
    private final CategoryService categoryService;
    private final ObservableList<Expense> expenseData = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryData = FXCollections.observableArrayList();
    private TableView<Expense> expenseTable;
    private TextField descField;
    private TextField amountField;
    private DatePicker datePicker;
    private ComboBox<Category> categoryCombo;
    private Button updateButton;
    private boolean isUpdateMode = false;

    public ExpenseController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.expenseService = new ExpenseService(HibernateUtil.getSessionFactory());
        this.categoryService = new CategoryService(HibernateUtil.getSessionFactory());

        categoryService.initializeCategories();
        categoryData.setAll(categoryService.getAllCategories());
        if (categoryData.isEmpty()) {
            System.out.println("Warning: No categories loaded into ComboBox");
        } else {
            System.out.println("Loaded categories: " + categoryData);
        }
        expenseData.setAll(expenseService.getExpensesByUser(user));
    }

    public VBox createExpensePage() {
        expenseTable = new TableView<>();
        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Expense, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCategory().getName()));
        expenseTable.getColumns().addAll(descCol, amountCol, dateCol, categoryCol);
        expenseTable.setItems(expenseData);

        GridPane form = new GridPane();
        form.setPadding(new Insets(20));
        form.setHgap(15);
        form.setVgap(15);

        descField = new TextField();
        descField.setPromptText("Description");
        amountField = new TextField();
        amountField.setPromptText("Amount");
        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        categoryCombo = new ComboBox<>(categoryData);
        categoryCombo.setPromptText("Select Category");
        categoryCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        categoryCombo.setButtonCell(new javafx.scene.control.ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                descField.setText(newSelection.getDescription());
                amountField.setText(String.valueOf(newSelection.getAmount()));
                datePicker.setValue(newSelection.getDate());
                categoryCombo.setValue(newSelection.getCategory());
                isUpdateMode = true;
                updateButton.setText("Save Changes");
            } else {
                clearFields();
                isUpdateMode = false;
                updateButton.setText("Update Selected");
            }
        });

        Button addButton = new Button("Add Expense");
        updateButton = new Button("Update Selected");
        Button deleteButton = new Button("Delete Selected");
        Button backButton = new Button("Back to Menu");

        addButton.setOnAction(e -> addExpense());
        updateButton.setOnAction(e -> updateExpense());
        deleteButton.setOnAction(e -> deleteExpense());
        backButton.setOnAction(e -> {
            MainMenuController mainMenuController = new MainMenuController(primaryStage, user);
            Scene mainMenuScene = new Scene(mainMenuController.createMainMenu(), 800, 600);
            mainMenuScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(mainMenuScene);
        });

        Label descLabel = new Label("Description:");
        Label amountLabel = new Label("Amount:");
        Label dateLabel = new Label("Date:");
        Label categoryLabel = new Label("Category:");

        form.add(descLabel, 0, 0);
        form.add(descField, 1, 0);
        form.add(amountLabel, 0, 1);
        form.add(amountField, 1, 1);
        form.add(dateLabel, 0, 2);
        form.add(datePicker, 1, 2);
        form.add(categoryLabel, 0, 3);
        form.add(categoryCombo, 1, 3);
        form.add(addButton, 0, 4);
        form.add(updateButton, 1, 4);
        form.add(deleteButton, 2, 4);
        form.add(backButton, 0, 5, 3, 1);

        VBox layout = new VBox(20, expenseTable, form);
        layout.setPadding(new Insets(20));
        return layout;
    }

    private void addExpense() {
        try {
            String description = descField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = datePicker.getValue();
            Category category = categoryCombo.getValue();

            if (description.isEmpty() || amount <= 0 || date == null || category == null) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Invalid input. Ensure all fields are filled and amount is positive.");
                return;
            }

            Expense expense = new Expense(description, amount, date, category, user);
            expenseService.addExpense(expense);
            expenseData.setAll(expenseService.getExpensesByUser(user));
            clearFields();
            isUpdateMode = false;
            updateButton.setText("Update Selected");
            expenseTable.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount format.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error adding expense: " + e.getMessage());
        }
    }

    private void updateExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an expense to update.");
            return;
        }

        try {
            String newDescription = descField.getText().trim();
            double newAmount = Double.parseDouble(amountField.getText().trim());
            LocalDate newDate = datePicker.getValue();
            Category newCategory = categoryCombo.getValue();

            if (newDescription.equals(selected.getDescription()) &&
                    newAmount == selected.getAmount() &&
                    newDate.equals(selected.getDate()) &&
                    newCategory.equals(selected.getCategory())) {
                showAlert(Alert.AlertType.INFORMATION, "No Changes", "No changes were made to the expense.");
                return;
            }

            if (newDescription.isEmpty() || newAmount <= 0 || newDate == null || newCategory == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "All fields must be filled with valid values.");
                return;
            }

            selected.setDescription(newDescription);
            selected.setAmount(newAmount);
            selected.setDate(newDate);
            selected.setCategory(newCategory);

            expenseService.updateExpense(selected);
            expenseData.setAll(expenseService.getExpensesByUser(user));

            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense updated successfully.");

            clearFields();
            isUpdateMode = false;
            updateButton.setText("Update Selected");
            expenseTable.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid amount.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error updating expense: " + e.getMessage());
        }
    }

    private void deleteExpense() {
        Expense selected = expenseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an expense to delete.");
            return;
        }

        expenseService.deleteExpense(selected.getId());
        expenseData.setAll(expenseService.getExpensesByUser(user));
        clearFields();
        isUpdateMode = false;
        updateButton.setText("Update Selected");
        expenseTable.getSelectionModel().clearSelection();
    }

    private void clearFields() {
        descField.clear();
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        categoryCombo.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
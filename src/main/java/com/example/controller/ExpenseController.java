package com.example.controller;

import java.time.LocalDate;

import com.example.model.Category;
import com.example.model.Expense;
import com.example.model.Income;
import com.example.model.User;
import com.example.service.CategoryService;
import com.example.service.ExpenseService;
import com.example.service.IncomeService;
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
    private final IncomeService incomeService;
    private final CategoryService categoryService;
    private final ObservableList<Expense> expenseData = FXCollections.observableArrayList();
    private final ObservableList<Category> categoryData = FXCollections.observableArrayList();
    private final ObservableList<Income> incomeData = FXCollections.observableArrayList();
    private TableView<Expense> expenseTable;
    private TextField itemField;
    private TextField amountSpentField;
    private DatePicker dateSpentPicker;
    private ComboBox<Category> categoryCombo;
    private ComboBox<Income> incomeCombo;
    private Button updateButton;
    private boolean isUpdateMode = false;

    public ExpenseController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.expenseService = new ExpenseService(HibernateUtil.getSessionFactory());
        this.incomeService = new IncomeService(HibernateUtil.getSessionFactory());
        this.categoryService = new CategoryService(HibernateUtil.getSessionFactory());

        categoryService.initializeCategories();
        categoryData.setAll(categoryService.getAllCategories());
        if (categoryData.isEmpty()) {
            System.out.println("Warning: No categories loaded into ComboBox");
        } else {
            System.out.println("Loaded categories: " + categoryData);
        }
        incomeData.setAll(incomeService.getIncomesByUser(user));
        System.out.println("Loaded incomes: " + incomeData);
        for (Income income : incomeData) {
            System.out.println("Income: " + income.getDescription() + ", Remaining: " + income.getRemainingAmount());
        }
        expenseData.setAll(expenseService.getExpensesByUser(user));
    }

    public VBox createExpensePage() {
        expenseTable = new TableView<>();
        TableColumn<Expense, String> itemCol = new TableColumn<>("Item");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount Spent");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Expense, LocalDate> dateCol = new TableColumn<>("Date Spent");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<Expense, String> categoryCol = new TableColumn<>("Expense Category");
        categoryCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCategory().getName()));
        TableColumn<Expense, String> incomeCol = new TableColumn<>("Income Source");
        incomeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getIncome() != null ? cellData.getValue().getIncome().getDescription() : ""));
        expenseTable.getColumns().addAll(itemCol, amountCol, dateCol, categoryCol, incomeCol);
        expenseTable.setItems(expenseData);

        GridPane form = new GridPane();
        form.setPadding(new Insets(20));
        form.setHgap(15);
        form.setVgap(15);

        itemField = new TextField();
        itemField.setPromptText("Item Purchased (e.g., Groceries)");
        amountSpentField = new TextField();
        amountSpentField.setPromptText("Amount Spent");
        dateSpentPicker = new DatePicker();
        dateSpentPicker.setValue(LocalDate.now());
        categoryCombo = new ComboBox<>(categoryData);
        categoryCombo.setPromptText("Select Expense Category");
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
        incomeCombo = new ComboBox<>(incomeData);
        incomeCombo.setPromptText("Select Income Source");
        incomeCombo.setCellFactory(listView -> new javafx.scene.control.ListCell<Income>() {
            @Override
            protected void updateItem(Income item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : item.getDescription() + " ($" + String.format("%.2f", item.getRemainingAmount())
                                + " remaining)");
            }
        });
        incomeCombo.setButtonCell(new javafx.scene.control.ListCell<Income>() {
            @Override
            protected void updateItem(Income item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? ""
                        : item.getDescription() + " ($" + String.format("%.2f", item.getRemainingAmount())
                                + " remaining)");
            }
        });

        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                itemField.setText(newSelection.getDescription());
                amountSpentField.setText(String.valueOf(newSelection.getAmount()));
                dateSpentPicker.setValue(newSelection.getDate());
                categoryCombo.setValue(newSelection.getCategory());
                incomeCombo.setValue(newSelection.getIncome());
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

        Label itemLabel = new Label("Item:");
        Label amountLabel = new Label("Amount Spent:");
        Label dateLabel = new Label("Date Spent:");
        Label categoryLabel = new Label("Expense Category:");
        Label incomeLabel = new Label("Income Source:");

        form.add(itemLabel, 0, 0);
        form.add(itemField, 1, 0);
        form.add(amountLabel, 0, 1);
        form.add(amountSpentField, 1, 1);
        form.add(dateLabel, 0, 2);
        form.add(dateSpentPicker, 1, 2);
        form.add(categoryLabel, 0, 3);
        form.add(categoryCombo, 1, 3);
        form.add(incomeLabel, 0, 4);
        form.add(incomeCombo, 1, 4);
        form.add(addButton, 0, 5);
        form.add(updateButton, 1, 5);
        form.add(deleteButton, 2, 5);
        form.add(backButton, 0, 6, 3, 1);

        VBox layout = new VBox(20, expenseTable, form);
        layout.setPadding(new Insets(20));
        return layout;
    }

    private void addExpense() {
        try {
            String description = itemField.getText().trim();
            double amount = Double.parseDouble(amountSpentField.getText().trim());
            LocalDate date = dateSpentPicker.getValue();
            Category category = categoryCombo.getValue();
            Income selectedIncome = incomeCombo.getValue();

            System.out.println("Adding expense: " + description + ", Amount: " + amount +
                    ", Income: " + (selectedIncome != null ? selectedIncome.getDescription() : "null") +
                    ", Remaining: " + (selectedIncome != null ? selectedIncome.getRemainingAmount() : "N/A"));

            if (description.isEmpty() || amount <= 0 || date == null || category == null || selectedIncome == null) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Invalid input. Ensure all fields are filled and amount is positive.");
                return;
            }

            if (selectedIncome.getRemainingAmount() < amount) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Insufficient funds in " + selectedIncome.getDescription() +
                                ". Requested: $" + amount + ", Remaining: $" + selectedIncome.getRemainingAmount());
                return;
            }

            incomeService.deductFromIncome(selectedIncome, amount);
            Expense expense = new Expense(description, amount, date, category, user, selectedIncome);
            expenseService.addExpense(expense);
            expenseData.setAll(expenseService.getExpensesByUser(user));
            incomeData.setAll(incomeService.getIncomesByUser(user));
            clearFields();
            isUpdateMode = false;
            updateButton.setText("Update Selected");
            expenseTable.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount format.");
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
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
            String newDescription = itemField.getText().trim();
            double newAmount = Double.parseDouble(amountSpentField.getText().trim());
            LocalDate newDate = dateSpentPicker.getValue();
            Category newCategory = categoryCombo.getValue();
            Income newIncome = incomeCombo.getValue();

            if (newDescription.equals(selected.getDescription()) &&
                    newAmount == selected.getAmount() &&
                    newDate.equals(selected.getDate()) &&
                    newCategory.equals(selected.getCategory()) &&
                    newIncome.equals(selected.getIncome())) {
                showAlert(Alert.AlertType.INFORMATION, "No Changes", "No changes were made to the expense.");
                return;
            }

            if (newDescription.isEmpty() || newAmount <= 0 || newDate == null || newCategory == null
                    || newIncome == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "All fields must be filled with valid values.");
                return;
            }

            // Check if new income has sufficient funds
            double amountDifference = newAmount - selected.getAmount();
            if (amountDifference > 0 && newIncome.getRemainingAmount() < amountDifference) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Insufficient funds in " + newIncome.getDescription() +
                                ". Requested: $" + amountDifference + ", Remaining: $"
                                + newIncome.getRemainingAmount());
                return;
            }

            // Revert the old amount to the previous income source
            Income oldIncome = selected.getIncome();
            oldIncome.setRemainingAmount(oldIncome.getRemainingAmount() + selected.getAmount());
            incomeService.updateIncome(oldIncome);
            System.out.println("Reverted " + selected.getAmount() + " to income " + oldIncome.getDescription() +
                    ", new remaining: " + oldIncome.getRemainingAmount());

            // Deduct the new amount from the new income source
            incomeService.deductFromIncome(newIncome, newAmount);

            selected.setDescription(newDescription);
            selected.setAmount(newAmount);
            selected.setDate(newDate);
            selected.setCategory(newCategory);
            selected.setIncome(newIncome);

            expenseService.updateExpense(selected);
            expenseData.setAll(expenseService.getExpensesByUser(user));
            incomeData.setAll(incomeService.getIncomesByUser(user));

            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense updated successfully.");

            clearFields();
            isUpdateMode = false;
            updateButton.setText("Update Selected");
            expenseTable.getSelectionModel().clearSelection();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter a valid amount.");
        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
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

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to delete this expense?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Income income = selected.getIncome();
                double oldAmount = selected.getAmount();
                income.setRemainingAmount(income.getRemainingAmount() + oldAmount);
                incomeService.updateIncome(income);
                System.out.println("Reverted " + oldAmount + " to income " + income.getDescription() +
                        ", new remaining: " + income.getRemainingAmount());

                expenseService.deleteExpense(selected.getId());
                expenseData.setAll(expenseService.getExpensesByUser(user));
                incomeData.setAll(incomeService.getIncomesByUser(user));
                clearFields();
                isUpdateMode = false;
                updateButton.setText("Update Selected");
                expenseTable.getSelectionModel().clearSelection();
            }
        });
    }

    private void clearFields() {
        itemField.clear();
        amountSpentField.clear();
        dateSpentPicker.setValue(LocalDate.now());
        categoryCombo.setValue(null);
        incomeCombo.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
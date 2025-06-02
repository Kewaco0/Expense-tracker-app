
package com.example.controller;

import java.time.LocalDate;

import com.example.model.Income;
import com.example.model.User;
import com.example.service.IncomeService;
import com.example.util.HibernateUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class IncomeController {
    private final Stage primaryStage;
    private final User user;
    private final IncomeService incomeService;
    private final ObservableList<Income> incomeData = FXCollections.observableArrayList();
    private TableView<Income> incomeTable;
    private TextField sourceField;
    private TextField amountField;
    private DatePicker dateReceivedPicker;
    private TextArea incomeStats;
    private Button updateButton;
    private boolean isUpdateMode = false;

    public IncomeController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.incomeService = new IncomeService(HibernateUtil.getSessionFactory());
        incomeData.setAll(incomeService.getIncomesByUser(user));
        System.out.println("Loaded incomes: " + incomeData);
        for (Income income : incomeData) {
            System.out.println("Income: " + income.getDescription() + ", Amount: " + income.getAmount() +
                    ", Remaining: " + income.getRemainingAmount());
        }
    }

    public VBox createIncomePage() {
        // Income Table
        incomeTable = new TableView<>();
        TableColumn<Income, String> sourceCol = new TableColumn<>("Source");
        sourceCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        TableColumn<Income, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Income, Double> remainingCol = new TableColumn<>("Remaining");
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingAmount"));
        TableColumn<Income, LocalDate> dateCol = new TableColumn<>("Date Received");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        incomeTable.getColumns().addAll(sourceCol, amountCol, remainingCol, dateCol);
        incomeTable.setItems(incomeData);

        // Form for adding/editing income
        GridPane form = new GridPane();
        form.setPadding(new Insets(20));
        form.setHgap(15);
        form.setVgap(15);

        sourceField = new TextField();
        sourceField.setPromptText("Source of Income (e.g., Salary)");
        amountField = new TextField();
        amountField.setPromptText("Amount Received");
        dateReceivedPicker = new DatePicker();
        dateReceivedPicker.setValue(LocalDate.now());

        // Selection listener for editing
        incomeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                sourceField.setText(newSelection.getDescription());
                amountField.setText(String.valueOf(newSelection.getAmount()));
                dateReceivedPicker.setValue(newSelection.getDate());
                isUpdateMode = true;
                updateButton.setText("Save Changes");
            } else {
                clearFields();
                isUpdateMode = false;
                updateButton.setText("Update Selected");
            }
        });

        // Buttons
        Button addIncomeButton = new Button("Add Income");
        updateButton = new Button("Update Selected");
        Button deleteButton = new Button("Delete Selected");
        Button backButton = new Button("Back to Menu");

        addIncomeButton.setOnAction(e -> addIncome());
        updateButton.setOnAction(e -> updateIncome());
        deleteButton.setOnAction(e -> deleteIncome());
        backButton.setOnAction(e -> {
            MainMenuController mainMenuController = new MainMenuController(primaryStage, user);
            Scene mainMenuScene = new Scene(mainMenuController.createMainMenu(), 800, 600);
            mainMenuScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(mainMenuScene);
        });

        // Form layout
        Label sourceLabel = new Label("Source:");
        Label amountLabel = new Label("Amount Received:");
        Label dateLabel = new Label("Date Received:");

        form.add(sourceLabel, 0, 0);
        form.add(sourceField, 1, 0);
        form.add(amountLabel, 0, 1);
        form.add(amountField, 1, 1);
        form.add(dateLabel, 0, 2);
        form.add(dateReceivedPicker, 1, 2);
        form.add(addIncomeButton, 0, 3);
        form.add(updateButton, 1, 3);
        form.add(deleteButton, 2, 3);
        form.add(backButton, 0, 4, 3, 1);

        // Income statistics
        incomeStats = new TextArea();
        incomeStats.setEditable(false);
        updateIncomeStats();

        VBox layout = new VBox(20, new Label("Income Entries"), incomeTable, new Label("Income Statistics"),
                incomeStats, form);
        layout.setPadding(new Insets(20));
        return layout;
    }

    private void addIncome() {
        try {
            String source = sourceField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = dateReceivedPicker.getValue();

            if (source.isEmpty() || amount <= 0 || date == null) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Invalid input. Ensure all fields are filled and amount is positive.");
                return;
            }

            Income income = new Income(source, amount, date, user);
            incomeService.addIncome(income);
            incomeData.setAll(incomeService.getIncomesByUser(user));
            clearFields();
            updateIncomeStats();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Income added successfully.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount format.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error adding income: " + e.getMessage());
        }
    }

    private void updateIncome() {
        Income selected = incomeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an income to update.");
            return;
        }

        try {
            String newSource = sourceField.getText().trim();
            double newAmount = Double.parseDouble(amountField.getText().trim());
            LocalDate newDate = dateReceivedPicker.getValue();

            if (newSource.isEmpty() || newAmount <= 0 || newDate == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "All fields must be filled with valid values.");
                return;
            }

            if (newSource.equals(selected.getDescription()) &&
                    newAmount == selected.getAmount() &&
                    newDate.equals(selected.getDate())) {
                showAlert(Alert.AlertType.INFORMATION, "No Changes", "No changes were made to the income.");
                return;
            }

            // Calculate the difference in amount
            double amountDifference = newAmount - selected.getAmount();
            double newRemaining = selected.getRemainingAmount() + amountDifference;
            if (newRemaining < 0) {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Cannot update income: new remaining amount would be negative ($" + newRemaining + ").");
                return;
            }

            selected.setDescription(newSource);
            selected.setAmount(newAmount);
            selected.setRemainingAmount(newRemaining);
            selected.setDate(newDate);

            incomeService.updateIncome(selected);
            incomeData.setAll(incomeService.getIncomesByUser(user));
            clearFields();
            isUpdateMode = false;
            updateButton.setText("Update Selected");
            incomeTable.getSelectionModel().clearSelection();
            updateIncomeStats();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Income updated successfully.");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount format.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error updating income: " + e.getMessage());
        }
    }

    private void deleteIncome() {
        Income selected = incomeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select an income to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText(null);
        confirmation.setContentText(
                "Are you sure you want to delete this income? This cannot be undone if expenses are associated with it.");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    incomeService.deleteIncome(selected.getId());
                    incomeData.setAll(incomeService.getIncomesByUser(user));
                    clearFields();
                    isUpdateMode = false;
                    updateButton.setText("Update Selected");
                    incomeTable.getSelectionModel().clearSelection();
                    updateIncomeStats();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Income deleted successfully.");
                } catch (IllegalStateException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Error deleting income: " + e.getMessage());
                }
            }
        });
    }

    private void clearFields() {
        sourceField.clear();
        amountField.clear();
        dateReceivedPicker.setValue(LocalDate.now());
    }

    private void updateIncomeStats() {
        StringBuilder stats = new StringBuilder();
        if (incomeData.isEmpty()) {
            stats.append("No income entries available.\n");
        } else {
            stats.append("Total Remaining Income: $")
                    .append(String.format("%.2f", incomeData.stream().mapToDouble(Income::getRemainingAmount).sum()))
                    .append("\n\n");
            stats.append("Income Entries:\n");
            for (Income income : incomeData) {
                stats.append("- ").append(income.getDescription()).append(": Original $")
                        .append(String.format("%.2f", income.getAmount()))
                        .append(", Remaining $").append(String.format("%.2f", income.getRemainingAmount()))
                        .append(" (Received on ").append(income.getDate()).append(")\n");
            }
        }
        incomeStats.setText(stats.toString());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

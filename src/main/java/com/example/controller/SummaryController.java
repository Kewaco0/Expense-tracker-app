
package com.example.controller;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.example.model.Expense;
import com.example.model.Income;
import com.example.model.User;
import com.example.service.ExpenseService;
import com.example.service.IncomeService;
import com.example.util.HibernateUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SummaryController {
    private final Stage primaryStage;
    private final User user;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;

    public SummaryController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.expenseService = new ExpenseService(HibernateUtil.getSessionFactory());
        this.incomeService = new IncomeService(HibernateUtil.getSessionFactory());
    }

    public VBox createSummaryPage() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));

        TextField yearMonthField = new TextField();
        yearMonthField.setPromptText("Enter Year-Month (YYYY-MM)");
        Button summaryButton = new Button("Generate Summary");
        Button reportButton = new Button("Generate PDF Report");
        Button backButton = new Button("Back to Menu");
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);

        PieChart expensePieChart = new PieChart();
        PieChart incomePieChart = new PieChart();

        summaryButton.setOnAction(e -> {
            try {
                YearMonth yearMonth = YearMonth.parse(yearMonthField.getText());
                generateSummary(yearMonth, summaryArea, expensePieChart, incomePieChart);
            } catch (Exception ex) {
                summaryArea.setText("Invalid Year-Month format. Use YYYY-MM.");
                expensePieChart.setData(FXCollections.observableArrayList());
                incomePieChart.setData(FXCollections.observableArrayList());
            }
        });

        reportButton.setOnAction(e -> {
            try {
                YearMonth yearMonth = YearMonth.parse(yearMonthField.getText());
                String filePath = generatePDFReport(yearMonth);
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "PDF report generated at: " + filePath);
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Failed to generate PDF report: " + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> {
            MainMenuController mainMenuController = new MainMenuController(primaryStage, user);
            Scene mainMenuScene = new Scene(mainMenuController.createMainMenu(), 800, 600);
            mainMenuScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            primaryStage.setScene(mainMenuScene);
        });

        Label yearMonthLabel = new Label("Enter Year-Month for Summary:");
        HBox inputBox = new HBox(15, yearMonthLabel, yearMonthField, summaryButton, reportButton);
        layout.getChildren().addAll(inputBox, summaryArea, expensePieChart, incomePieChart, backButton);
        return layout;
    }

    private void generateSummary(YearMonth yearMonth, TextArea summaryArea, PieChart expensePieChart,
            PieChart incomePieChart) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        double totalExpenses = expenseService.getTotalExpenses(yearMonth, user);
        double totalIncome = incomeService.getTotalIncome(yearMonth, user);
        double availableBalance = totalIncome;

        StringBuilder sb = new StringBuilder();
        sb.append("Summary for ").append(yearMonth).append("\n\n");
        sb.append("Total Expenses: $").append(String.format("%.2f", totalExpenses)).append("\n");
        sb.append("Total Remaining Income: $").append(String.format("%.2f", totalIncome)).append("\n");
        sb.append("Available Balance: $").append(String.format("%.2f", availableBalance)).append("\n\n");

        Map<String, Double> expenseSummary = expenseService.getSummaryByCategory(yearMonth, user);
        sb.append("Monthly Expense Breakdown:\n");
        ObservableList<PieChart.Data> expensePieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : expenseSummary.entrySet()) {
            sb.append(entry.getKey()).append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
            expensePieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        List<Income> incomes = incomeService.getIncomesByUser(user);
        sb.append("\nMonthly Income Breakdown:\n");
        ObservableList<PieChart.Data> incomePieChartData = FXCollections.observableArrayList();
        for (Income income : incomes) {
            if (!income.getDate().isBefore(startDate) && !income.getDate().isAfter(endDate)) {
                String desc = income.getDescription();
                double remaining = income.getRemainingAmount();
                sb.append(desc).append(": $").append(String.format("%.2f", remaining))
                        .append(" (Original: $").append(String.format("%.2f", income.getAmount())).append(")\n");
                incomePieChartData.add(new PieChart.Data(desc, remaining));
            }
        }

        sb.append("\nWeekly Expense Breakdown:\n");
        Map<Integer, Map<String, Double>> weeklyExpenses = new HashMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        for (Expense expense : expenseService.getExpensesByUser(user)) {
            LocalDate date = expense.getDate();
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                int weekNumber = date.get(weekFields.weekOfMonth());
                weeklyExpenses.computeIfAbsent(weekNumber, k -> new HashMap<>());
                weeklyExpenses.get(weekNumber).merge(
                        expense.getCategory().getName(),
                        expense.getAmount(),
                        Double::sum);
            }
        }
        for (Map.Entry<Integer, Map<String, Double>> weekEntry : weeklyExpenses.entrySet()) {
            sb.append("Week ").append(weekEntry.getKey()).append(":\n");
            double weekTotal = 0;
            for (Map.Entry<String, Double> categoryEntry : weekEntry.getValue().entrySet()) {
                sb.append("  ").append(categoryEntry.getKey()).append(": $")
                        .append(String.format("%.2f", categoryEntry.getValue())).append("\n");
                weekTotal += categoryEntry.getValue();
            }
            sb.append("  Week Total: $").append(String.format("%.2f", weekTotal)).append("\n");
        }

        sb.append("\nWeekly Income Breakdown:\n");
        Map<Integer, Map<String, Double>> weeklyIncomes = new HashMap<>();
        for (Income income : incomes) {
            LocalDate date = income.getDate();
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                int weekNumber = date.get(weekFields.weekOfMonth());
                weeklyIncomes.computeIfAbsent(weekNumber, k -> new HashMap<>());
                weeklyIncomes.get(weekNumber).merge(
                        income.getDescription(),
                        income.getRemainingAmount(),
                        Double::sum);
            }
        }
        for (Map.Entry<Integer, Map<String, Double>> weekEntry : weeklyIncomes.entrySet()) {
            sb.append("Week ").append(weekEntry.getKey()).append(":\n");
            double weekTotal = 0;
            for (Map.Entry<String, Double> descEntry : weekEntry.getValue().entrySet()) {
                sb.append("  ").append(descEntry.getKey()).append(": $")
                        .append(String.format("%.2f", descEntry.getValue())).append("\n");
                weekTotal += descEntry.getValue();
            }
            sb.append("  Week Total: $").append(String.format("%.2f", weekTotal)).append("\n");
        }

        summaryArea.setText(sb.toString());
        expensePieChart.setData(expensePieChartData);
        expensePieChart.setTitle("Expense Distribution for " + yearMonth);
        incomePieChart.setData(incomePieChartData);
        incomePieChart.setTitle("Remaining Income Distribution for " + yearMonth);
    }

    private String generatePDFReport(YearMonth yearMonth) throws Exception {
        File reportsDir = new File("reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        String fileName = "ExpenseReport_" + yearMonth + ".pdf";
        String filePath = new File(reportsDir, fileName).getAbsolutePath();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Financial Report for " + yearMonth);
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float yPosition = 700;

                double totalExpenses = expenseService.getTotalExpenses(yearMonth, user);
                double totalIncome = incomeService.getTotalIncome(yearMonth, user);
                double availableBalance = totalIncome;

                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Total Expenses: $" + String.format("%.2f", totalExpenses));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Total Remaining Income: $" + String.format("%.2f", totalIncome));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Available Balance: $" + String.format("%.2f", availableBalance));
                contentStream.endText();

                yPosition -= 60;
                contentStream.moveTo(50, yPosition);
                contentStream.lineTo(550, yPosition);
                contentStream.stroke();

                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Monthly Expense Breakdown");
                contentStream.endText();

                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Category");
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText("Total Amount ($)");
                contentStream.endText();

                yPosition -= 20;
                Map<String, Double> expenseSummary = expenseService.getSummaryByCategory(yearMonth, user);
                for (Map.Entry<String, Double> entry : expenseSummary.entrySet()) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(entry.getKey());
                    contentStream.newLineAtOffset(200, 0);
                    contentStream.showText(String.format("%.2f", entry.getValue()));
                    contentStream.endText();
                    yPosition -= 20;
                }

                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Monthly Income Breakdown");
                contentStream.endText();

                yPosition -= 20;
                List<Income> incomes = incomeService.getIncomesByUser(user);
                for (Income income : incomes) {
                    if (!income.getDate().isBefore(yearMonth.atDay(1))
                            && !income.getDate().isAfter(yearMonth.atEndOfMonth())) {
                        contentStream.beginText();
                        contentStream.newLineAtOffset(50, yPosition);
                        contentStream.showText(income.getDescription());
                        contentStream.newLineAtOffset(200, 0);
                        contentStream.showText("Remaining: $" + String.format("%.2f", income.getRemainingAmount()));
                        contentStream.newLineAtOffset(0, -15);
                        contentStream.showText("(Original: $" + String.format("%.2f", income.getAmount()) + ")");
                        contentStream.endText();
                        yPosition -= 30;
                    }
                }
            }
            document.save(filePath);
        }
        return filePath;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
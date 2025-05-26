package com.example.controller;

import java.io.File;
import java.time.YearMonth;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.example.model.User;
import com.example.service.ExpenseService;
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

    public SummaryController(Stage primaryStage, User user) {
        this.primaryStage = primaryStage;
        this.user = user;
        this.expenseService = new ExpenseService(HibernateUtil.getSessionFactory());
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

        PieChart pieChart = new PieChart();

        summaryButton.setOnAction(e -> {
            try {
                YearMonth yearMonth = YearMonth.parse(yearMonthField.getText());
                generateSummary(yearMonth, summaryArea, pieChart);
            } catch (Exception ex) {
                summaryArea.setText("Invalid Year-Month format. Use YYYY-MM.");
                pieChart.setData(FXCollections.observableArrayList());
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
        layout.getChildren().addAll(inputBox, summaryArea, pieChart, backButton);
        return layout;
    }

    private void generateSummary(YearMonth yearMonth, TextArea summaryArea, PieChart pieChart) {
        Map<String, Double> summary = expenseService.getSummaryByCategory(yearMonth, user);
        StringBuilder sb = new StringBuilder();
        sb.append("Summary for ").append(yearMonth).append("\n\n");
        double total = 0;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : summary.entrySet()) {
            sb.append(entry.getKey()).append(": $").append(String.format("%.2f", entry.getValue())).append("\n");
            total += entry.getValue();
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        sb.append("\nTotal: $").append(String.format("%.2f", total));
        summaryArea.setText(sb.toString());
        pieChart.setData(pieChartData);
        pieChart.setTitle("Expense Distribution for " + yearMonth);
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
                contentStream.showText("Expense Report for " + yearMonth);
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float yPosition = 700;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Category");
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText("Total Amount ($)");
                contentStream.endText();

                yPosition -= 20;
                contentStream.moveTo(50, yPosition);
                contentStream.lineTo(550, yPosition);
                contentStream.stroke();

                Map<String, Double> summary = expenseService.getSummaryByCategory(yearMonth, user);
                double total = 0;
                for (Map.Entry<String, Double> entry : summary.entrySet()) {
                    yPosition -= 20;
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, yPosition);
                    contentStream.showText(entry.getKey());
                    contentStream.newLineAtOffset(200, 0);
                    contentStream.showText(String.format("%.2f", entry.getValue()));
                    contentStream.endText();
                    total += entry.getValue();
                }

                yPosition -= 20;
                contentStream.moveTo(50, yPosition);
                contentStream.lineTo(550, yPosition);
                contentStream.stroke();

                yPosition -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText("Total");
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText(String.format("%.2f", total));
                contentStream.endText();
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
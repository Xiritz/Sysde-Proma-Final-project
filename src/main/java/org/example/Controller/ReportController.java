package org.example.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.App;
import org.example.Model.Expense;
import org.example.Model.Transaction;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class ReportController {

    @FXML private ComboBox<String> rangeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label revenueLabel;
    @FXML private Label expenseLabel;
    @FXML private Label profitLabel;

    @FXML private TextField expenseNameField;
    @FXML private TextField expenseCostField;

    @FXML private VBox cardsContainer;

    @FXML
    public void initialize() {
        rangeComboBox.setItems(FXCollections.observableArrayList("Daily", "Weekly", "Monthly", "Yearly", "Custom"));
        rangeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCustom = "Custom".equals(newVal);
            startDatePicker.setDisable(!isCustom);
            endDatePicker.setDisable(!isCustom);
        });

        rangeComboBox.getSelectionModel().select("Daily");
        handleGenerateReport();
    }

    private void renderCards(List<Expense> expenses) {
        cardsContainer.getChildren().clear();
        
        if (expenses.isEmpty()) {
            Label noData = new Label("No expenses recorded for this period.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (Expense e : expenses) {
            cardsContainer.getChildren().add(createExpenseCard(e));
        }
    }

    private VBox createExpenseCard(Expense e) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(e.getExpenseName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -qmar-primary;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label costLabel = new Label(String.format("- Php %.2f", e.getCost()));
        costLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -qmar-danger;");
        
        header.getChildren().addAll(nameLabel, spacer, costLabel);

        Label dateLabel = new Label("📅 " + e.getDateIncurred().toString().substring(0, 10));
        dateLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 12px;");

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == org.example.Model.Role.ADMIN || currentUser.getRole() == org.example.Model.Role.OWNER)) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: -qmar-primary; -fx-border-color: -qmar-primary; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(evt -> handleEdit(e));

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            removeBtn.setOnAction(evt -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Deletion");
                confirm.setHeaderText("Delete Expense: " + e.getExpenseName());
                confirm.setContentText("Are you sure you want to permanently remove this expense record?");
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            App.expenseService.removeExpense(currentUser, e.getExpenseId());
                            handleGenerateReport();
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense removed.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                        }
                    }
                });
            });
            footer.getChildren().addAll(editBtn, removeBtn);
        }

        card.getChildren().addAll(header, dateLabel, footer);
        return card;
    }

    private void handleEdit(Expense e) {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle("Edit Expense");
        dialog.setHeaderText("Update details for " + e.getExpenseName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        TextField name = new TextField(e.getExpenseName());
        name.setPromptText("Expense Name");
        TextField cost = new TextField(String.valueOf(e.getCost()));
        cost.setPromptText("Cost");

        grid.getChildren().addAll(new Label("Name:"), name, new Label("Cost:"), cost);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    return new Expense(e.getExpenseId(), name.getText(), Double.parseDouble(cost.getText()), e.getDateIncurred());
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Cost must be a valid number.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedExpense -> {
            try {
                App.expenseService.updateExpense(App.loginService.getCurrentUser(), updatedExpense);
                handleGenerateReport();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Expense updated successfully!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void markFieldInvalid(Control field, boolean isInvalid) {
        Pane container = (Pane) field.getParent();
        field.getStyleClass().remove("error-field");
        container.getChildren().removeIf(node -> node instanceof Label && node.getStyleClass().contains("error-message"));

        if (isInvalid) {
            field.getStyleClass().add("error-field");
            Label errorLabel = new Label("This field is required");
            errorLabel.getStyleClass().add("error-message");
            container.getChildren().add(errorLabel);
        }
    }

    @FXML
    private void handleGenerateReport() {
        markFieldInvalid(startDatePicker, false);
        markFieldInvalid(endDatePicker, false);

        String range = rangeComboBox.getSelectionModel().getSelectedItem();
        if (range == null) return;

        Date start = null;
        Date end = null;

        if ("Custom".equals(range)) {
            boolean hasError = false;
            if (startDatePicker.getValue() == null) {
                markFieldInvalid(startDatePicker, true);
                hasError = true;
            }
            if (endDatePicker.getValue() == null) {
                markFieldInvalid(endDatePicker, true);
                hasError = true;
            }
            if (hasError) return;

            start = Date.from(startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            end = Date.from(endDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        List<Expense> expenses = App.expenseService.getExpensesByRange(range, start, end);
        double totalExpenses = expenses.stream().mapToDouble(Expense::getCost).sum();

        Date rangeStart = getStartDateForRange(range, start);
        Date rangeEnd = (end != null) ? end : new Date();

        List<Transaction> transactions = App.transactionService.getTransactionsByDateRange(rangeStart, rangeEnd);
        double totalRevenue = transactions.stream().mapToDouble(Transaction::getAmountPaid).sum();

        revenueLabel.setText(String.format("Php %.2f", totalRevenue));
        expenseLabel.setText(String.format("Php %.2f", totalExpenses));
        profitLabel.setText(String.format("Php %.2f", totalRevenue - totalExpenses));

        renderCards(expenses);
    }

    private Date getStartDateForRange(String range, Date customStart) {
        if ("Custom".equals(range)) return customStart;
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        switch (range.toLowerCase()) {
            case "daily":
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                break;
            case "weekly":
                cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
                break;
            case "monthly":
                cal.add(java.util.Calendar.MONTH, -1);
                break;
            case "yearly":
                cal.add(java.util.Calendar.YEAR, -1);
                break;
        }
        return cal.getTime();
    }

    @FXML
    private void handleAddExpense() {
        markFieldInvalid(expenseNameField, false);
        markFieldInvalid(expenseCostField, false);

        try {
            String name = expenseNameField.getText().trim();
            String costStr = expenseCostField.getText().trim();

            boolean hasError = false;
            if (name.isEmpty()) {
                markFieldInvalid(expenseNameField, true);
                hasError = true;
            }
            if (costStr.isEmpty()) {
                markFieldInvalid(expenseCostField, true);
                hasError = true;
            }

            if (hasError) return;

            double cost;
            try {
                cost = Double.parseDouble(costStr);
            } catch (NumberFormatException e) {
                markFieldInvalid(expenseCostField, true);
                throw new Exception("Cost must be a valid number.");
            }

            String id = App.expenseService.generateNextId();
            App.expenseService.addExpense(new Expense(id, name, cost, new Date()));

            expenseNameField.clear();
            expenseCostField.clear();
            handleGenerateReport();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Expense recorded.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

}

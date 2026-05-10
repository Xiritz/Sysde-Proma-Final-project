package org.example.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.example.App;
import org.example.Model.Customer;
import org.example.Model.InitialPaymentMethod;
import org.example.Model.Service;
import org.example.Model.Status;
import org.example.Model.Transaction;
import org.example.Model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TransactionController {

    @FXML private ComboBox<Customer> customerComboBox;
    @FXML private ComboBox<Service> serviceComboBox;
    @FXML private TextField weightField;
    @FXML private Spinner<Integer> loadsSpinner;
    @FXML private ComboBox<InitialPaymentMethod> paymentMethodComboBox;
    @FXML private VBox paymentFieldContainer;
    @FXML private TextField paymentField;
    @FXML private Label totalAmountLabel;
    @FXML private VBox cardsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    private ObservableList<Transaction> transactionList;
    private ObservableList<Customer> allCustomers;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

    @FXML
    public void initialize() {
        // Input Restrictions: Weight Field (Positive Only)
        weightField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.length() > 5) return null;
            if (newText.matches("|[0-9]{1,3}(\\.[0-9]{0,1})?")) {
                return change;
            }
            return null;
        }));

        // Input Restrictions: Payment Field (Positive Only)
        paymentField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("|[0-9]{0,5}(\\.[0-9]{0,2})?")) {
                return change;
            }
            return null;
        }));

        // Initial Spinner Setup
        loadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));

        customerComboBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) { return c == null ? "" : c.getCustomerName(); }
            @Override
            public Customer fromString(String s) { 
                return allCustomers.stream()
                        .filter(c -> c.getCustomerName().equalsIgnoreCase(s))
                        .findFirst()
                        .orElse(null);
            }
        });

        setupCustomerSearch();

        serviceComboBox.setItems(FXCollections.observableArrayList(Service.values()));
        serviceComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateTotalAmount();
        });

        paymentMethodComboBox.setItems(FXCollections.observableArrayList(InitialPaymentMethod.values()));
        paymentMethodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updatePaymentFieldState();
        });

        // Initialize payment field state
        updatePaymentFieldState();

        // Status Filter Setup
        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "Unpaid", "Pending", "Processing", "Ready for Delivery", "Delivered"
        ));
        statusFilter.getSelectionModel().select("All");
        statusFilter.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> renderCards());

        // Auto-Calculation & Strict Constraints Listener
        weightField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                try {
                    double weight = Double.parseDouble(newVal);
                    int[] range = calculateLoadRange(weight);
                    loadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(range[0], range[1], Math.min(range[2], range[1])));
                    updateTotalAmount();
                } catch (NumberFormatException e) {
                    loadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
                }
            } else {
                loadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1));
                updateTotalAmount();
            }
        });

        loadsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateTotalAmount());
        
        // Search Listener for Cards
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());

        refreshData();
    }

    private int[] calculateLoadRange(double weight) {
        if (weight <= 0) return new int[]{1, 1, 1};
        int min = (int) Math.ceil(weight / 10.0);
        int recommended = (int) Math.ceil(weight / 8.0);
        int max = recommended; 
        if (min == 0) min = 1;
        if (max < min) max = min;
        if (recommended < min) recommended = min;
        return new int[]{min, max, recommended};
    }

    private void setupCustomerSearch() {
        allCustomers = FXCollections.observableArrayList(App.customerService.getAllCustomers());
        FilteredList<Customer> filteredCustomers = new FilteredList<>(allCustomers, p -> true);

        customerComboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (customerComboBox.getSelectionModel().getSelectedItem() == null || 
                !customerComboBox.getSelectionModel().getSelectedItem().getCustomerName().equals(newVal)) {
                
                filteredCustomers.setPredicate(customer -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    return customer.getCustomerName().toLowerCase().contains(newVal.toLowerCase());
                });
                customerComboBox.show();
            }
        });

        customerComboBox.setItems(filteredCustomers);
    }

    private void renderCards() {
        String filterText = searchField.getText();
        String selectedStatus = statusFilter.getValue();
        
        cardsContainer.getChildren().clear();
        
        List<Transaction> filtered = transactionList.stream()
            .filter(t -> {
                // Search Filter
                boolean matchesSearch = true;
                if (filterText != null && !filterText.isEmpty()) {
                    String lowerFilter = filterText.toLowerCase();
                    matchesSearch = t.getTransactionId().toLowerCase().contains(lowerFilter) ||
                                    (t.getCustomer() != null && t.getCustomer().getCustomerName().toLowerCase().contains(lowerFilter));
                }
                
                // Status Filter
                boolean matchesStatus = true;
                if (selectedStatus != null && !selectedStatus.equals("All")) {
                    switch (selectedStatus) {
                        case "Unpaid":
                            matchesStatus = t.getOutstandingBalance() > 0;
                            break;
                        case "Pending":
                            matchesStatus = t.getLaundryStatus() == Status.PENDING;
                            break;
                        case "Processing":
                            matchesStatus = t.getLaundryStatus() == Status.PROCESSING;
                            break;
                        case "Ready for Delivery":
                            matchesStatus = t.getLaundryStatus() == Status.READY_FOR_DELIVERY;
                            break;
                        case "Delivered":
                            matchesStatus = t.getLaundryStatus() == Status.DELIVERED;
                            break;
                    }
                }
                
                return matchesSearch && matchesStatus;
            })
            .toList();

        if (filtered.isEmpty()) {
            Label noData = new Label("No transactions found matching filters.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (Transaction t : filtered) {
            cardsContainer.getChildren().add(createTransactionCard(t));
        }
    }

    private void renderCards(String filter) {
        renderCards();
    }

    private VBox createTransactionCard(Transaction t) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 15;");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label idLabel = new Label(t.getTransactionId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -qmar-primary;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusBadge = new Label(t.getLaundryStatus().toString());
        statusBadge.getStyleClass().add("badge");
        switch (t.getLaundryStatus()) {
            case PENDING: statusBadge.getStyleClass().add("status-pending"); break;
            case PROCESSING: statusBadge.getStyleClass().add("status-processing"); break;
            case READY_FOR_DELIVERY:
            case DELIVERED: statusBadge.getStyleClass().add("status-completed"); break;
        }
        
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        GridPane body = new GridPane();
        body.setHgap(30);
        body.setVgap(5);
        
        Label custName = new Label(t.getCustomer() != null ? t.getCustomer().getCustomerName() : "N/A");
        custName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label serviceLabel = new Label(t.getService().toString());
        serviceLabel.setStyle("-fx-text-fill: -qmar-text-muted;");
        
        Label weightLabel = new Label(String.format("%.1f kg (%d loads)", t.getWeightKg(), t.getLoadCount()));
        
        body.add(new Label("Customer:"), 0, 0);
        body.add(custName, 1, 0);
        body.add(new Label("Service:"), 0, 1);
        body.add(serviceLabel, 1, 1);
        body.add(new Label("Weight:"), 2, 0);
        body.add(weightLabel, 3, 0);
        body.add(new Label("Date:"), 2, 1);
        body.add(new Label(t.getDatePlaced() != null ? dateFormat.format(t.getDatePlaced()) : "N/A"), 3, 1);

        HBox footer = new HBox(15);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: -qmar-border-color; -fx-border-width: 1 0 1 0;");
        
        VBox paymentInfo = new VBox(2);
        Label totalCost = new Label(String.format("Total: Php %.2f", t.getTotalCost()));
        totalCost.setStyle("-fx-font-weight: bold;");
        Label balanceLabel = new Label(String.format("Balance: Php %.2f", t.getOutstandingBalance()));
        balanceLabel.setStyle("-fx-text-fill: " + (t.isFullyPaid() ? "-qmar-success" : "-qmar-danger") + "; -fx-font-weight: bold;");
        paymentInfo.getChildren().addAll(totalCost, balanceLabel);
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);
        
        Button statusBtn = new Button("Update Status");
        statusBtn.getStyleClass().add("btn-receive");
        statusBtn.setOnAction(e -> {
            ChoiceDialog<Status> dialog = new ChoiceDialog<>(t.getLaundryStatus(), Status.values());
            dialog.setTitle("Update Status");
            dialog.setHeaderText("Change status for " + t.getTransactionId());
            dialog.showAndWait().ifPresent(s -> {
                App.transactionService.updateTransactionStatus(t.getTransactionId(), s);
                refreshData();
            });
        });

        Button payBtn = new Button("Receive Payment");
        payBtn.getStyleClass().add("success-button");
        payBtn.setDisable(t.isFullyPaid());
        payBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(String.valueOf(t.getOutstandingBalance()));
            dialog.setTitle("Receive Payment");
            dialog.setHeaderText("Remaining Balance: Php " + t.getOutstandingBalance());
            dialog.setContentText("Enter amount to pay:");
            dialog.showAndWait().ifPresent(amountStr -> {
                try {
                    App.transactionService.receivePayment(t.getTransactionId(), Double.parseDouble(amountStr));
                    refreshData();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                }
            });
        });

        footer.getChildren().addAll(paymentInfo, spacer2, statusBtn, payBtn);

        Label creatorLabel = new Label("Transaction placed by: " + (t.getCreatedBy() != null ? t.getCreatedBy() : "Unknown"));
        creatorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -qmar-text-muted; -fx-font-style: italic;");

        javafx.scene.layout.HBox removeFooter = new javafx.scene.layout.HBox();
        removeFooter.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == org.example.Model.Role.ADMIN || currentUser.getRole() == org.example.Model.Role.OWNER)) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: -qmar-primary; -fx-border-color: -qmar-primary; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEdit(t));

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Deletion");
                confirm.setHeaderText("Delete Transaction: " + t.getTransactionId());
                confirm.setContentText("Are you sure you want to permanently remove this transaction?");
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            App.transactionService.removeTransaction(currentUser, t.getTransactionId());
                            refreshData();
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction removed.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                        }
                    }
                });
            });
            removeFooter.getChildren().addAll(editBtn, removeBtn);
            removeFooter.setSpacing(10);
        }

        card.getChildren().addAll(header, body, footer, creatorLabel, removeFooter);
        return card;
    }

    private void handleEdit(Transaction t) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("Edit Transaction");
        dialog.setHeaderText("Update details for " + t.getTransactionId());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        
        ComboBox<Service> servBox = new ComboBox<>(FXCollections.observableArrayList(Service.values()));
        servBox.setValue(t.getService());
        
        TextField weight = new TextField(String.valueOf(t.getWeightKg()));
        TextField loads = new TextField(String.valueOf(t.getLoadCount()));
        
        grid.getChildren().addAll(
            new Label("Service:"), servBox, 
            new Label("Weight (kg):"), weight, 
            new Label("Loads:"), loads
        );
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    Transaction updated = new Transaction(
                        t.getTransactionId(), 
                        t.getCustomer(), 
                        servBox.getValue(), 
                        t.getDatePlaced(), 
                        Double.parseDouble(weight.getText()), 
                        Integer.parseInt(loads.getText()), 
                        t.getTotalCost(), 
                        t.getAmountPaid(), 
                        t.getOutstandingBalance(), 
                        t.getLaundryStatus(), 
                        t.getInitialPaymentMethod(), 
                        t.getCreatedBy()
                    );
                    updated.calculateTotalCost(); // Re-calculate based on new loads/weight
                    return updated;
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid numeric input.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTrans -> {
            try {
                App.transactionService.updateTransaction(App.loginService.getCurrentUser(), updatedTrans);
                refreshData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transaction updated successfully!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });
    }

    private void updatePaymentFieldState() {
        InitialPaymentMethod method = paymentMethodComboBox.getValue();
        if (method == null) {
            paymentFieldContainer.setVisible(false);
            paymentField.setDisable(true);
            paymentField.setText("");
            return;
        }

        double total = calculateCurrentTotal();
        switch (method) {
            case DOWN_PAYMENT:
                paymentFieldContainer.setVisible(true);
                paymentField.setDisable(false);
                paymentField.setText("0.0");
                break;
            case PAY_LATER:
                paymentFieldContainer.setVisible(false);
                paymentField.setText("0.0");
                break;
            case FULL_PAYMENT:
                paymentFieldContainer.setVisible(true);
                paymentField.setDisable(true);
                paymentField.setText(String.format("%.2f", total));
                break;
        }
    }

    private double calculateCurrentTotal() {
        Integer loads = loadsSpinner.getValue();
        Service service = serviceComboBox.getValue();
        if (loads != null && service != null) {
            double pricePerLoad = switch (service) {
                case WASH -> 90.0;
                case WASH_DRY -> 180.0;
                case WASH_DRY_FOLD -> 220.0;
            };
            return pricePerLoad * loads;
        }
        return 0.0;
    }

    private void updateTotalAmount() {
        double total = calculateCurrentTotal();
        totalAmountLabel.setText(String.format("Php %.2f", total));
        
        if (paymentMethodComboBox.getValue() == InitialPaymentMethod.FULL_PAYMENT) {
            paymentField.setText(String.format("%.2f", total));
        }
    }

    private void refreshData() {
        allCustomers = FXCollections.observableArrayList(App.customerService.getAllCustomers());
        customerComboBox.setItems(allCustomers);
        transactionList = FXCollections.observableArrayList(App.transactionService.getAllTransactions());
        renderCards();
    }

    public void selectCustomer(Customer customer) {
        refreshData();
        customerComboBox.getSelectionModel().select(customer);
    }

    @FXML
    private void handleQuickAddCustomer() {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Quick Add Customer");
        dialog.setHeaderText("Register a new customer");
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);
        
        VBox grid = new VBox(10);
        TextField name = new TextField(); name.setPromptText("Full Name");
        TextArea address = new TextArea(); address.setPromptText("Address"); address.setPrefRowCount(3);
        TextField contact = new TextField(); contact.setPromptText("Contact Number (Numeric)");
        
        // Strict Numeric Filter for Phone Number
        contact.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("[0-9]*")) return change;
            return null;
        }));

        grid.getChildren().addAll(new Label("Name:"), name, new Label("Address:"), address, new Label("Contact:"), contact);
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                String n = name.getText().trim();
                String a = address.getText().trim();
                String c = contact.getText().trim();

                if (n.isEmpty() || a.isEmpty() || c.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "All fields are required.");
                    return null;
                }
                
                // Name Sanitation (Alpha-spaces only)
                if (!n.matches("[a-zA-Z\\\\s.]+")) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Name should only contain letters, spaces, and dots.");
                    return null;
                }

                try {
                    String id = App.customerService.generateNextId();
                    Customer customer = new Customer(id, n, a, c);
                    App.customerService.registerCustomer(customer);
                    return customer;
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Registration Error", e.getMessage());
                    return null;
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(this::selectCustomer);
    }

    private void markFieldInvalid(Control field, String errorMessage) {
        Pane container = (Pane) field.getParent();
        field.getStyleClass().remove("error-field");
        container.getChildren().removeIf(node -> node instanceof Label && node.getStyleClass().contains("error-message"));
        if (errorMessage != null) {
            field.getStyleClass().add("error-field");
            Label errorLabel = new Label(errorMessage);
            errorLabel.getStyleClass().add("error-message");
            container.getChildren().add(errorLabel);
        }
    }

    @FXML
    private void handleCreateOrder() {
        markFieldInvalid(customerComboBox, null);
        markFieldInvalid(serviceComboBox, null);
        markFieldInvalid(paymentMethodComboBox, null);
        markFieldInvalid(weightField, null);
        markFieldInvalid(loadsSpinner, null);
        markFieldInvalid(paymentField, null);

        try {
            boolean hasError = false;
            Customer customer = customerComboBox.getSelectionModel().getSelectedItem();
            if (customer == null && !customerComboBox.getEditor().getText().isEmpty()) {
                String name = customerComboBox.getEditor().getText();
                customer = allCustomers.stream().filter(c -> c.getCustomerName().equalsIgnoreCase(name)).findFirst().orElse(null);
            }
            Service service = serviceComboBox.getSelectionModel().getSelectedItem();
            InitialPaymentMethod method = paymentMethodComboBox.getValue();

            if (customer == null) { markFieldInvalid(customerComboBox, "Required"); hasError = true; }
            if (service == null) { markFieldInvalid(serviceComboBox, "Required"); hasError = true; }
            if (method == null) { markFieldInvalid(paymentMethodComboBox, "Required"); hasError = true; }
            
            double weight = 0;
            try {
                if (weightField.getText().trim().isEmpty()) { markFieldInvalid(weightField, "Required"); hasError = true; }
                else {
                    weight = Double.parseDouble(weightField.getText());
                    if (weight < 1.0) { markFieldInvalid(weightField, "Min 1.0 kg"); hasError = true; }
                }
            } catch (NumberFormatException e) { markFieldInvalid(weightField, "Invalid"); hasError = true; }

            int loads = loadsSpinner.getValue();
            if (weight >= 1.0) {
                int[] range = calculateLoadRange(weight);
                if (loads < range[0] || loads > range[1]) { markFieldInvalid(loadsSpinner, range[0]+"-"+range[1]); hasError = true; }
            }

            double payment = 0;
            if (paymentField.isVisible() && !paymentField.isDisable()) {
                try {
                    if (paymentField.getText().trim().isEmpty()) { markFieldInvalid(paymentField, "Required"); hasError = true; }
                    else {
                        payment = Double.parseDouble(paymentField.getText());
                        if (payment < 0) { markFieldInvalid(paymentField, "Invalid"); hasError = true; }
                        if (method == InitialPaymentMethod.DOWN_PAYMENT && payment <= 0) {
                            markFieldInvalid(paymentField, "Must be > 0");
                            hasError = true;
                        }
                    }
                } catch (NumberFormatException e) { markFieldInvalid(paymentField, "Invalid"); hasError = true; }
            } else if (paymentField.isDisable() && !paymentField.getText().isEmpty()) {
                payment = Double.parseDouble(paymentField.getText());
            }

            if (hasError) return;
            double totalCost = calculateCurrentTotal();
            if (payment > totalCost) { markFieldInvalid(paymentField, "Exceeds total"); return; }

            String id = App.transactionService.generateNextId();
            User currentUser = App.loginService.getCurrentUser();
            String creatorName = (currentUser != null) ? currentUser.getUsername() : "Unknown";
            
            Transaction t = new Transaction(id, customer, service, new Date(), weight, loads, 0, payment, 0, Status.PENDING, method, creatorName);
            try {
                App.transactionService.createTransaction(t);
                handleClear();
                refreshData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Order created successfully!");
            } catch (IllegalStateException e) {
                showAlert(Alert.AlertType.ERROR, "Inventory Error", e.getMessage());
            }
        } catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Error", e.getMessage()); }
    }

    @FXML
    private void handleClear() {
        customerComboBox.getSelectionModel().clearSelection();
        customerComboBox.getEditor().clear();
        serviceComboBox.getSelectionModel().clearSelection();
        paymentMethodComboBox.getSelectionModel().clearSelection();
        weightField.clear();
        loadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1));
        paymentField.clear();
        paymentField.setDisable(false);
        paymentFieldContainer.setVisible(true);
        totalAmountLabel.setText("Php 0.00");
        markFieldInvalid(customerComboBox, null);
        markFieldInvalid(serviceComboBox, null);
        markFieldInvalid(paymentMethodComboBox, null);
        markFieldInvalid(weightField, null);
        markFieldInvalid(loadsSpinner, null);
        markFieldInvalid(paymentField, null);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

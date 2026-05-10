package org.example.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.App;
import org.example.Model.Customer;

public class CustomerController {

    @FXML private TextField nameField;
    @FXML private TextArea addressField;
    @FXML private TextField contactField;
    @FXML private TextField searchField;

    @FXML private VBox cardsContainer;

    private ObservableList<Customer> customerList;
    private javafx.collections.transformation.FilteredList<Customer> filteredData;

    @FXML
    public void initialize() {
        refreshTable();
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(customer -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (customer.getCustomerName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getContactNumber().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (customer.getCustomerId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
            renderCards();
        });
    }

    private void refreshTable() {
        customerList = FXCollections.observableArrayList(App.customerService.getAllCustomers());
        filteredData = new javafx.collections.transformation.FilteredList<>(customerList, p -> true);
        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        
        if (filteredData.isEmpty()) {
            Label noData = new Label(searchField.getText().isEmpty() ? "No customers found." : "No customers match your search.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (Customer c : filteredData) {
            cardsContainer.getChildren().add(createCustomerCard(c));
        }
    }

    private VBox createCustomerCard(Customer c) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(c.getCustomerName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -qmar-primary;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label idLabel = new Label(c.getCustomerId());
        idLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 12px;");
        
        header.getChildren().addAll(nameLabel, spacer, idLabel);

        Label contactLabel = new Label("📞 " + c.getContactNumber());
        contactLabel.setStyle("-fx-text-fill: -qmar-text-main; -fx-font-weight: bold;");
        
        Label addressLabel = new Label("📍 " + c.getAddress());
        addressLabel.setStyle("-fx-text-fill: -qmar-text-muted;");
        addressLabel.setWrapText(true);

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == org.example.Model.Role.ADMIN || currentUser.getRole() == org.example.Model.Role.OWNER)) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: -qmar-primary; -fx-border-color: -qmar-primary; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEdit(c));

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Removal");
                confirm.setHeaderText("Remove Customer: " + c.getCustomerName());
                confirm.setContentText("Are you sure you want to remove this customer and their history?");
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            App.customerService.removeCustomer(currentUser, c.getCustomerId());
                            refreshTable();
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Customer removed.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                        }
                    }
                });
            });
            footer.getChildren().addAll(editBtn, removeBtn);
        }

        card.getChildren().addAll(header, contactLabel, addressLabel, footer);
        return card;
    }

    private void handleEdit(Customer c) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle("Edit Customer");
        dialog.setHeaderText("Update details for " + c.getCustomerName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        TextField name = new TextField(c.getCustomerName());
        name.setPromptText("Full Name");
        TextArea address = new TextArea(c.getAddress());
        address.setPromptText("Address");
        address.setPrefRowCount(3);
        TextField contact = new TextField(c.getContactNumber());
        contact.setPromptText("Contact Number");

        grid.getChildren().addAll(new Label("Name:"), name, new Label("Address:"), address, new Label("Contact:"), contact);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                if (name.getText().trim().isEmpty() || address.getText().trim().isEmpty() || contact.getText().trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "All fields are required.");
                    return null;
                }
                return new Customer(c.getCustomerId(), name.getText(), address.getText(), contact.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedCustomer -> {
            try {
                App.customerService.updateCustomer(App.loginService.getCurrentUser(), updatedCustomer);
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Customer updated successfully!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        });
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
    private void handleRegister() {
        markFieldInvalid(nameField, false);
        markFieldInvalid(addressField, false);
        markFieldInvalid(contactField, false);

        String name = nameField.getText();
        String address = addressField.getText();
        String contact = contactField.getText();

        boolean hasError = false;
        if (name.trim().isEmpty()) {
            markFieldInvalid(nameField, true);
            hasError = true;
        }
        if (address.trim().isEmpty()) {
            markFieldInvalid(addressField, true);
            hasError = true;
        }
        if (contact.trim().isEmpty()) {
            markFieldInvalid(contactField, true);
            hasError = true;
        }

        if (hasError) return;

        String id = App.customerService.generateNextId();
        Customer customer = new Customer(id, name, address, contact);
        
        try {
            App.customerService.registerCustomer(customer);
            handleClear();
            refreshTable();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Customer registered successfully!");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Registration Error", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        nameField.clear();
        addressField.clear();
        contactField.clear();
        markFieldInvalid(nameField, false);
        markFieldInvalid(addressField, false);
        markFieldInvalid(contactField, false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

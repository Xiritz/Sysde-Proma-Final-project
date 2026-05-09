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

    @FXML private VBox cardsContainer;

    private ObservableList<Customer> customerList;

    @FXML
    public void initialize() {
        refreshTable();
    }

    private void refreshTable() {
        customerList = FXCollections.observableArrayList(App.customerService.getAllCustomers());
        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        
        if (customerList.isEmpty()) {
            Label noData = new Label("No customers found.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (Customer c : customerList) {
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

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == org.example.Model.Role.ADMIN || currentUser.getRole() == org.example.Model.Role.OWNER)) {
            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                try {
                    App.customerService.removeCustomer(currentUser, c.getCustomerId());
                    refreshTable();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Customer removed.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                }
            });
            footer.getChildren().add(removeBtn);
        }

        card.getChildren().addAll(header, contactLabel, addressLabel, footer);
        return card;
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
        App.customerService.registerCustomer(customer);

        handleClear();
        refreshTable();
        showAlert(Alert.AlertType.INFORMATION, "Success", "Customer registered successfully!");
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

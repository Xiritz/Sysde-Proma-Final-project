package org.example.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.example.App;
import org.example.Model.InventoryItem;

import java.util.List;

public class InventoryController {

    @FXML private TextField newItemNameField;
    @FXML private TextField newStockField;
    @FXML private TextField newThresholdField;

    @FXML private ComboBox<InventoryItem> itemComboBox;
    @FXML private TextField adjustmentQuantityField;

    @FXML private VBox addItemContainer;
    @FXML private Label lowStockAlertLabel;
    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;

    private ObservableList<InventoryItem> inventoryList;
    private javafx.collections.transformation.FilteredList<InventoryItem> filteredData;

    @FXML
    public void initialize() {
        itemComboBox.setConverter(new StringConverter<InventoryItem>() {
            @Override
            public String toString(InventoryItem item) {
                return item == null ? "" : item.getItemName();
            }

            @Override
            public InventoryItem fromString(String string) {
                return null;
            }
        });

        refreshData();

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == org.example.Model.Role.EMPLOYEE) {
            addItemContainer.setVisible(false);
            addItemContainer.setManaged(false);
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (item.getItemName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (item.getItemId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
            renderCards();
        });
    }

    private void refreshData() {
        List<InventoryItem> allItems = App.inventoryService.getAllItems();
        inventoryList = FXCollections.observableArrayList(allItems);
        filteredData = new javafx.collections.transformation.FilteredList<>(inventoryList, p -> true);
        itemComboBox.setItems(inventoryList);

        boolean hasLowStock = !App.inventoryService.getLowStockItems().isEmpty();
        lowStockAlertLabel.setVisible(hasLowStock);
        
        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        
        if (filteredData.isEmpty()) {
            Label noData = new Label(searchField.getText().isEmpty() ? "No inventory items found." : "No items match your search.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (InventoryItem item : filteredData) {
            cardsContainer.getChildren().add(createInventoryCard(item));
        }
    }

    private VBox createInventoryCard(InventoryItem item) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -qmar-primary;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusBadge = new Label(item.isStockLow() ? "LOW STOCK" : "OK");
        statusBadge.getStyleClass().add("badge");
        if (item.isStockLow()) {
            statusBadge.setStyle("-fx-background-color: -qmar-danger;");
        } else {
            statusBadge.setStyle("-fx-background-color: -qmar-success;");
        }
        
        header.getChildren().addAll(nameLabel, spacer, statusBadge);

        javafx.scene.layout.GridPane body = new javafx.scene.layout.GridPane();
        body.setHgap(30);
        body.setVgap(5);
        
        Label stockLabel = new Label(String.valueOf(item.getCurrentStock()));
        stockLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + (item.isStockLow() ? "-qmar-danger" : "-qmar-text-main") + ";");
        
        Label thresholdLabel = new Label(String.valueOf(item.getLowStockThreshold()));
        thresholdLabel.setStyle("-fx-text-fill: -qmar-text-muted;");
        
        body.add(new Label("Current Stock:"), 0, 0);
        body.add(stockLabel, 1, 0);
        body.add(new Label("Threshold:"), 0, 1);
        body.add(thresholdLabel, 1, 1);
        
        Label idLabel = new Label("ID: " + item.getItemId());
        idLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 11px;");

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && (currentUser.getRole() == org.example.Model.Role.ADMIN || currentUser.getRole() == org.example.Model.Role.OWNER)) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: -qmar-primary; -fx-border-color: -qmar-primary; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEdit(item));

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            
            // PROTECTION RULES for core items
            boolean isCoreItem = item.getItemId().equals("I001") || item.getItemId().equals("I002");
            boolean isOwner = currentUser.getRole() == org.example.Model.Role.OWNER;

            if (isCoreItem && !isOwner) {
                removeBtn.setDisable(true);
            }

            removeBtn.setOnAction(e -> {
                String warningMessage = "Are you sure you want to permanently remove this item from inventory?";
                if (isCoreItem) {
                    warningMessage = "STERN WARNING: This is a CORE inventory item (Detergent/Softener). " +
                            "Removing this will disrupt the transaction system and order processing. " +
                            "ONLY proceed if you are absolutely certain of the consequences!";
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle(isCoreItem ? "STERN WARNING: CORE ITEM REMOVAL" : "Confirm Removal");
                confirm.setHeaderText("Remove Item: " + item.getItemName());
                confirm.setContentText(warningMessage);
                
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            App.inventoryService.removeInventoryItem(currentUser, item.getItemId());
                            refreshData();
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Item removed.");
                        } catch (Exception ex) {
                            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                        }
                    }
                });
            });
            footer.getChildren().addAll(editBtn, removeBtn);
            footer.setSpacing(10);
        }

        card.getChildren().addAll(header, body, idLabel, footer);
        return card;
    }

    private void handleEdit(InventoryItem item) {
        Dialog<InventoryItem> dialog = new Dialog<>();
        dialog.setTitle("Edit Item");
        dialog.setHeaderText("Update details for " + item.getItemName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        TextField name = new TextField(item.getItemName());
        name.setPromptText("Item Name");
        TextField stock = new TextField(String.valueOf(item.getCurrentStock()));
        stock.setPromptText("Current Stock");
        TextField threshold = new TextField(String.valueOf(item.getLowStockThreshold()));
        threshold.setPromptText("Low Stock Threshold");

        grid.getChildren().addAll(new Label("Name:"), name, new Label("Stock:"), stock, new Label("Threshold:"), threshold);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                try {
                    return new InventoryItem(item.getItemId(), name.getText(), Integer.parseInt(stock.getText()), Integer.parseInt(threshold.getText()));
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Stock and Threshold must be numbers.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedItem -> {
            try {
                App.inventoryService.updateInventoryItem(App.loginService.getCurrentUser(), updatedItem);
                refreshData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item updated successfully!");
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
    private void handleAddItem() {
        org.example.Model.User currentUser = App.loginService.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == org.example.Model.Role.EMPLOYEE) {
            showAlert(Alert.AlertType.ERROR, "Access Denied", "Employees are not authorized to add new inventory items.");
            return;
        }

        markFieldInvalid(newItemNameField, false);
        markFieldInvalid(newStockField, false);
        markFieldInvalid(newThresholdField, false);

        try {
            String name = newItemNameField.getText().trim();
            String stockStr = newStockField.getText().trim();
            String thresholdStr = newThresholdField.getText().trim();

            boolean hasError = false;
            if (name.isEmpty()) {
                markFieldInvalid(newItemNameField, true);
                hasError = true;
            }
            if (stockStr.isEmpty()) {
                markFieldInvalid(newStockField, true);
                hasError = true;
            }
            if (thresholdStr.isEmpty()) {
                markFieldInvalid(newThresholdField, true);
                hasError = true;
            }

            if (hasError) return;

            int stock;
            try {
                stock = Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                markFieldInvalid(newStockField, true);
                throw new Exception("Stock must be a valid number.");
            }

            int threshold;
            try {
                threshold = Integer.parseInt(thresholdStr);
            } catch (NumberFormatException e) {
                markFieldInvalid(newThresholdField, true);
                throw new Exception("Threshold must be a valid number.");
            }

            // The Service handles duplicate names by adding to stock
            App.inventoryService.addItem(new InventoryItem(App.inventoryService.generateNextId(), name, stock, threshold));

            newItemNameField.clear();
            newStockField.clear();
            newThresholdField.clear();
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Inventory Updated", "Item processed successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleRestock() {
        markFieldInvalid(itemComboBox, false);
        markFieldInvalid(adjustmentQuantityField, false);

        InventoryItem selected = itemComboBox.getSelectionModel().getSelectedItem();
        boolean hasError = false;

        if (selected == null) {
            markFieldInvalid(itemComboBox, true);
            hasError = true;
        }

        try {
            String qtyStr = adjustmentQuantityField.getText().trim();
            if (qtyStr.isEmpty()) {
                markFieldInvalid(adjustmentQuantityField, true);
                hasError = true;
            }
            
            if (hasError) return;

            int qty = Integer.parseInt(qtyStr);
            App.inventoryService.addStockToItem(selected.getItemId(), qty);
            adjustmentQuantityField.clear();
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock updated.");
        } catch (NumberFormatException e) {
            markFieldInvalid(adjustmentQuantityField, true);
            showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a valid number.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleConsume() {
        markFieldInvalid(itemComboBox, false);
        markFieldInvalid(adjustmentQuantityField, false);

        InventoryItem selected = itemComboBox.getSelectionModel().getSelectedItem();
        boolean hasError = false;

        if (selected == null) {
            markFieldInvalid(itemComboBox, true);
            hasError = true;
        }

        try {
            String qtyStr = adjustmentQuantityField.getText().trim();
            if (qtyStr.isEmpty()) {
                markFieldInvalid(adjustmentQuantityField, true);
                hasError = true;
            }

            if (hasError) return;

            int qty = Integer.parseInt(qtyStr);
            App.inventoryService.consumeInventory(selected.getItemId(), qty);
            adjustmentQuantityField.clear();
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Stock consumed.");
        } catch (NumberFormatException e) {
            markFieldInvalid(adjustmentQuantityField, true);
            showAlert(Alert.AlertType.ERROR, "Input Error", "Quantity must be a valid number.");
        } catch (IllegalArgumentException e) {
            markFieldInvalid(adjustmentQuantityField, true);
            showAlert(Alert.AlertType.ERROR, "Stock Error", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

}

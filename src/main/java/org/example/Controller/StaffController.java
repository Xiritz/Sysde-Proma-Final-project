package org.example.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.App;
import org.example.Model.Role;
import org.example.Model.User;

public class StaffController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private TextField searchField;

    @FXML private VBox cardsContainer;

    private ObservableList<User> staffList;
    private javafx.collections.transformation.FilteredList<User> filteredData;

    @FXML
    public void initialize() {
        // Filter roles: Do not allow assigning the OWNER role via the UI
        ObservableList<Role> assignableRoles = FXCollections.observableArrayList(Role.values());
        assignableRoles.remove(Role.OWNER);
        roleComboBox.setItems(assignableRoles);
        
        refreshData();

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                
                String lowerCaseFilter = newValue.toLowerCase();
                
                if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (user.getUserId().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (user.getRole().toString().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
            renderCards();
        });
    }

    private void refreshData() {
        staffList = FXCollections.observableArrayList(App.userService.getAllUsers());
        filteredData = new javafx.collections.transformation.FilteredList<>(staffList, p -> true);
        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        
        if (filteredData.isEmpty()) {
            Label noData = new Label(searchField.getText().isEmpty() ? "No staff members found." : "No staff match your search.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (User u : filteredData) {
            cardsContainer.getChildren().add(createStaffCard(u));
        }
    }

    private VBox createStaffCard(User u) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(u.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: -qmar-primary;");
        
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label roleBadge = new Label(u.getRole().toString());
        roleBadge.getStyleClass().add("badge");
        if (u.getRole() == Role.ADMIN || u.getRole() == Role.OWNER) {
            roleBadge.setStyle("-fx-background-color: -qmar-primary-dark;");
        } else {
            roleBadge.setStyle("-fx-background-color: -qmar-success;");
        }
        
        header.getChildren().addAll(nameLabel, spacer, roleBadge);

        Label idLabel = new Label("ID: " + u.getUserId());
        idLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 12px;");

        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(10);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #f0f9ff; -fx-text-fill: -qmar-primary; -fx-border-color: -qmar-primary; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEdit(u));

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: -qmar-danger; -fx-border-color: -qmar-danger; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
        
        User currentUser = App.loginService.getCurrentUser();
        boolean isCurrentUserOwner = currentUser != null && currentUser.getRole() == Role.OWNER;

        // PROTECTION RULES:
        // 1. Cannot remove an OWNER account
        if (u.getRole() == Role.OWNER) {
            removeBtn.setDisable(true);
        }
        // 2. Only OWNER can remove the default 'admin' account
        else if (u.getUsername().equals("admin") && !isCurrentUserOwner) {
            removeBtn.setDisable(true);
        }

        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Removal");
            confirm.setHeaderText("Remove Staff Member: " + u.getUsername());
            confirm.setContentText("Are you sure you want to remove this account?");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        App.userService.removeUser(App.loginService.getCurrentUser(), u.getUserId());
                        refreshData();
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Staff member removed.");
                    } catch (Exception ex) {
                        showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
                    }
                }
            });
        });

        footer.getChildren().addAll(editBtn, removeBtn);

        card.getChildren().addAll(header, idLabel, footer);
        return card;
    }

    private void handleEdit(User u) {
        User currentUser = App.loginService.getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getRole() == Role.OWNER;

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit Staff");
        dialog.setHeaderText("Update details for " + u.getUsername());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        TextField username = new TextField(u.getUsername());
        username.setPromptText("Username");
        
        grid.getChildren().addAll(new Label("Username:"), username);

        PasswordField password = new PasswordField();
        ObservableList<Role> editableRoles = FXCollections.observableArrayList(Role.values());
        editableRoles.remove(Role.OWNER);
        ComboBox<Role> role = new ComboBox<>(editableRoles);

        if (isOwner) {
            password.setPromptText("New Password (leave blank to keep)");
            role.setValue(u.getRole());
            grid.getChildren().addAll(new Label("Password:"), password, new Label("Role:"), role);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                String pass = (isOwner && !password.getText().isEmpty()) ? password.getText() : u.getPassword();
                Role r = isOwner ? role.getValue() : u.getRole();
                return new User(u.getUserId(), username.getText(), pass, r);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedUser -> {
            try {
                App.userService.updateUser(App.loginService.getCurrentUser(), updatedUser);
                refreshData();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Staff updated successfully!");
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
    private void handleAddStaff() {
        markFieldInvalid(usernameField, false);
        markFieldInvalid(passwordField, false);
        markFieldInvalid(roleComboBox, false);

        try {
            String username = usernameField.getText();
            String password = passwordField.getText();
            Role role = roleComboBox.getSelectionModel().getSelectedItem();

            boolean hasError = false;
            if (username.trim().isEmpty()) {
                markFieldInvalid(usernameField, true);
                hasError = true;
            }
            if (password.trim().isEmpty()) {
                markFieldInvalid(passwordField, true);
                hasError = true;
            }
            if (role == null) {
                markFieldInvalid(roleComboBox, true);
                hasError = true;
            }

            if (hasError) return;

            User currentUser = App.loginService.getCurrentUser();
            String id = App.userService.generateNextId();
            User newUser = new User(id, username, password, role);
            
            App.userService.addUser(currentUser, newUser);

            handleClear();
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Staff member added.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.getSelectionModel().clearSelection();
        markFieldInvalid(usernameField, false);
        markFieldInvalid(passwordField, false);
        markFieldInvalid(roleComboBox, false);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

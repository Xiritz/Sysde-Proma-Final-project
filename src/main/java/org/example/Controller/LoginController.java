package org.example.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.App;
import org.example.Model.Role;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

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
    private void handleLogin() throws IOException {
        markFieldInvalid(usernameField, false);
        markFieldInvalid(passwordField, false);
        errorLabel.setText("");

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            if (username.trim().isEmpty()) markFieldInvalid(usernameField, true);
            if (password.trim().isEmpty()) markFieldInvalid(passwordField, true);
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        if (App.loginService.login(username, password)) {
            // Success! Load Dashboard
            App.setRoot("dashboard");
        } else {
            markFieldInvalid(usernameField, true);
            markFieldInvalid(passwordField, true);
            errorLabel.setText("Invalid username or password.");
        }
    }
}

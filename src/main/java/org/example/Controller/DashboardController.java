package org.example.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.App;
import org.example.Model.Role;
import org.example.Model.User;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label userLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentArea;
    @FXML
    private StackPane notificationBell;
    @FXML
    private Label notificationBadge;

    // Sidebar buttons
    @FXML private Button homeBtn;
    @FXML private Button transBtn;
    @FXML private Button custBtn;
    @FXML private Button invBtn;
    @FXML private Button staffBtn;
    @FXML private Button reportsBtn;

    @FXML
    public void initialize() {
        User user = App.loginService.getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getUsername());
            if (roleLabel != null) {
                roleLabel.setText(user.getRole().toString());
            }
            
            // Hide admin buttons for employees (Allow ADMIN and OWNER)
            if (user.getRole() == Role.EMPLOYEE) {
                staffBtn.setVisible(false);
                staffBtn.setManaged(false);
                reportsBtn.setVisible(false);
                reportsBtn.setManaged(false);
            }
        }
        
        updateNotificationBadge();
        // Load default view
        loadView("home");
        setActiveNavItem(homeBtn);
    }

    private void setActiveNavItem(Button activeBtn) {
        homeBtn.getStyleClass().remove("nav-item-active");
        transBtn.getStyleClass().remove("nav-item-active");
        custBtn.getStyleClass().remove("nav-item-active");
        invBtn.getStyleClass().remove("nav-item-active");
        staffBtn.getStyleClass().remove("nav-item-active");
        reportsBtn.getStyleClass().remove("nav-item-active");
        
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("nav-item-active");
        }
    }

    private void updateNotificationBadge() {
        int lowStockCount = App.inventoryService.getLowStockItems().size();
        if (lowStockCount > 0) {
            notificationBadge.setText(String.valueOf(lowStockCount));
            notificationBadge.setVisible(true);
        } else {
            notificationBadge.setVisible(false);
        }
    }

    @FXML
    private void showLowStockMenu() {
        javafx.scene.control.ContextMenu lowStockMenu = new javafx.scene.control.ContextMenu();
        var lowItems = App.inventoryService.getLowStockItems();
        
        if (lowItems.isEmpty()) {
            lowStockMenu.getItems().add(new javafx.scene.control.MenuItem("All items in stock"));
        } else {
            lowItems.forEach(item -> {
                javafx.scene.control.MenuItem mi = new javafx.scene.control.MenuItem("Low Stock: " + item.getItemName() + " (" + item.getCurrentStock() + ")");
                mi.setOnAction(e -> handleInventory());
                lowStockMenu.getItems().add(mi);
            });
        }
        lowStockMenu.show(notificationBell, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleHome() { 
        loadView("home"); 
        setActiveNavItem(homeBtn);
    }
    
    @FXML
    private void handleTransactions() { 
        loadView("transactions"); 
        setActiveNavItem(transBtn);
    }
    
    @FXML
    private void handleInventory() { 
        loadView("inventory"); 
        setActiveNavItem(invBtn);
    }
    
    @FXML
    private void handleCustomers() { 
        loadView("customers"); 
        setActiveNavItem(custBtn);
    }
    
    @FXML
    private void handleStaff() { 
        loadView("staff"); 
        setActiveNavItem(staffBtn);
    }
    
    @FXML
    private void handleReports() { 
        loadView("reports"); 
        setActiveNavItem(reportsBtn);
    }

    @FXML
    private void handleLogout() throws IOException {
        App.loginService.logout();
        App.setRoot("login");
    }

    public Object loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Could not load FXML: " + fxml);
            e.printStackTrace();
            return null;
        }
    }
}

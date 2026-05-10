package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.Service.*;
import org.example.Util.BackupScheduler;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;
    private static final BackupScheduler backupScheduler = new BackupScheduler();
    
    // Shared services
    public static final UserService userService = new UserService();
    public static final LoginService loginService = new LoginService(userService);
    public static final CustomerService customerService = new CustomerService();
    public static final InventoryService inventoryService = new InventoryService();
    public static final ExpenseService expenseService = new ExpenseService();
    public static final TransactionService transactionService = new TransactionService(expenseService, inventoryService);

    static {
        org.example.Util.DataLoader.loadPlaceholderData(customerService, inventoryService, expenseService, transactionService);
    }

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("login"), 1200, 800);
        stage.setTitle("Laundry Shop Management System");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        
        // Start auto-backup to Supabase
        backupScheduler.startAutoBackup();
    }

    @Override
    public void stop() {
        backupScheduler.stop();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}

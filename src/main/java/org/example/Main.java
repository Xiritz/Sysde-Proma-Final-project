package org.example;

import org.example.Model.*;
import org.example.Service.*;

import java.util.Date;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        // Initialize Services
        UserService userService = new UserService();
        LoginService loginService = new LoginService(userService);
        CustomerService customerService = new CustomerService();
        InventoryService inventoryService = new InventoryService();
        TransactionService transactionService = new TransactionService();

        System.out.println("=== Laundry Shop System Demo ===");

        // 1. Authentication via LoginService
        System.out.println("\n--- Step 1: Authentication ---");
        boolean loginSuccess = loginService.login("admin", "admin123");
        
        if (loginSuccess && loginService.isLoggedIn()) {
            User admin = loginService.getCurrentUser();
            System.out.println("Session active for: " + admin.getUsername());

            // 2. Add User (Employee role)
            User employee = new User("E001", "juan", "pass123", Role.EMPLOYEE);
            userService.addUser(admin, employee);
            System.out.println("Employee added: " + employee.getUsername());

            // 3. Register Customer
            System.out.println("\n--- Step 2: Customer Registration ---");
            Customer customer = new Customer("C001", "Maria Clara", "Manila", "09123456789");
            customerService.registerCustomer(customer);
            System.out.println("Customer registered: " + customer.getCustomerName());

            // 4. Setup Inventory
            System.out.println("\n--- Step 3: Inventory Setup ---");
            InventoryItem soap = new InventoryItem("I001", "Detergent", 10, 5);
            inventoryService.addItem(soap);
            inventoryService.generateInventoryReport();

            // 5. Create Transaction
            System.out.println("\n--- Step 4: Transaction Creation ---");
            Transaction trans1 = new Transaction(
                    "T001",
                    customer,
                    Service.WASH_DRY_FOLD,
                    new Date(),
                    5.5,
                    2,
                    0, 0, 0,
                    Status.PENDING
            );
            transactionService.createTransaction(trans1);
            trans1.printReceipt();

            // 6. Process Payment
            System.out.println("\n--- Step 5: Payment Processing ---");
            transactionService.receivePayment("T001", 150.0);
            trans1.printReceipt();

            // 7. Consume Inventory
            System.out.println("\n--- Step 6: Inventory Consumption ---");
            inventoryService.consumeInventory("I001", 2);
            inventoryService.generateInventoryReport();

            // 8. Generate Reports
            System.out.println("\n--- Step 7: Reports ---");
            Date now = new Date();
            transactionService.generateFinancialReport(new Date(now.getTime() - 86400000), new Date(now.getTime() + 86400000));

        } else {
            System.out.println("Login Failed!");
        }
    }
}

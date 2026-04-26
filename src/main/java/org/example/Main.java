package org.example;

import org.example.Model.*;
import org.example.Service.*;

import java.util.Date;
import java.util.Scanner;
import java.util.List;
import java.util.Optional;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static UserService userService = new UserService();
    private static LoginService loginService = new LoginService(userService);
    private static CustomerService customerService = new CustomerService();
    private static InventoryService inventoryService = new InventoryService();
    private static ExpenseService expenseService = new ExpenseService();
    private static TransactionService transactionService = new TransactionService(expenseService);

    public static void main(String[] args) {
        // Pre-populate some inventory
        inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), "Detergent", 50, 10));
        inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), "Fabric Softener", 20, 5));

        while (true) {
            System.out.println("\n========================================");
            System.out.println("   LAUNDRY SHOP MANAGEMENT SYSTEM      ");
            System.out.println("========================================");
            
            if (!loginService.isLoggedIn()) {
                showLoginMenu();
            } else {
                User user = loginService.getCurrentUser();
                if (user.getRole() == Role.ADMIN) {
                    showAdminMenu();
                } else {
                    showEmployeeMenu();
                }
            }
        }
    }

    private static void pause() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void showLoginMenu() {
        System.out.println("--- LOGIN SCREEN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        loginService.login(username, password);
        if (!loginService.isLoggedIn()) {
            pause();
        }
    }

    private static void showAdminMenu() {
        System.out.println("\n--- ADMIN DASHBOARD ---");
        System.out.println("1. Manage Staff (Add Employee)");
        System.out.println("2. View All Users");
        System.out.println("3. Manage Inventory (Add/Restock/Consume)");
        System.out.println("4. Create New Laundry Order");
        System.out.println("5. View All Customers");
        System.out.println("6. View All Transactions");
        System.out.println("7. Manage Payments (Pending Orders)");
        System.out.println("8. Update Laundry Status");
        System.out.println("9. Record Business Expense");
        System.out.println("10. View Financial Reports");
        System.out.println("11. Logout");
        System.out.print("Select action: ");

        String choice = scanner.nextLine();
        User admin = loginService.getCurrentUser();

        switch (choice) {
            case "1" -> { addEmployee(admin); pause(); }
            case "2" -> { viewAllUsers(); pause(); }
            case "3" -> { manageInventory(); pause(); }
            case "4" -> { createLaundryOrder(); pause(); }
            case "5" -> { viewAllCustomers(); pause(); }
            case "6" -> { viewAllTransactions(); pause(); }
            case "7" -> { receivePayment(); pause(); }
            case "8" -> { updateLaundryStatus(); pause(); }
            case "9" -> { recordExpense(); pause(); }
            case "10" -> { viewFinancialReports(admin); pause(); }
            case "11" -> loginService.logout();
            default -> System.out.println("Invalid choice.");
        }
    }

    private static void showEmployeeMenu() {
        System.out.println("\n--- EMPLOYEE DASHBOARD ---");
        System.out.println("1. Register New Customer");
        System.out.println("2. Create New Laundry Order");
        System.out.println("3. Manage Payments (Pending Orders)");
        System.out.println("4. Update Laundry Status");
        System.out.println("5. Update Item Stock (Use Supplies)");
        System.out.println("6. View All Customers");
        System.out.println("7. View All Transactions");
        System.out.println("8. Logout");
        System.out.print("Select action: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1" -> { registerCustomer(); pause(); }
            case "2" -> { createLaundryOrder(); pause(); }
            case "3" -> { receivePayment(); pause(); }
            case "4" -> { updateLaundryStatus(); pause(); }
            case "5" -> { consumeSupplies(); pause(); }
            case "6" -> { viewAllCustomers(); pause(); }
            case "7" -> { viewAllTransactions(); pause(); }
            case "8" -> loginService.logout();
            default -> System.out.println("Invalid choice.");
        }
    }

    // --- SHARED FUNCTIONALITIES ---

    private static void addEmployee(User admin) {
        try {
            System.out.print("Enter Username: ");
            String user = scanner.nextLine();
            System.out.print("Enter Password: ");
            String pass = scanner.nextLine();
            userService.addUser(admin, new User(userService.generateNextId(), user, pass, Role.EMPLOYEE));
            System.out.println("Employee added successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void viewAllUsers() {
        System.out.println("\n--- Registered Users ---");
        userService.getAllUsers().forEach(u -> System.out.println(u.getUserId() + " | " + u.getUsername() + " [" + u.getRole() + "]"));
    }

    private static void manageInventory() {
        System.out.println("\n--- Inventory Management ---");
        System.out.println("1. Add New Item Type");
        System.out.println("2. Restock Existing Item");
        System.out.println("3. Consume Item (Use Supplies)");
        System.out.println("4. View Inventory Report");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1" -> {
                System.out.print("Item Name: ");
                String name = scanner.nextLine();
                System.out.print("Initial Quantity: ");
                int qty = Integer.parseInt(scanner.nextLine());
                System.out.print("Low Stock Threshold: ");
                int threshold = Integer.parseInt(scanner.nextLine());
                inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), name, qty, threshold));
                System.out.println("Item added.");
            }
            case "2" -> {
                viewInventoryBrief();
                System.out.print("Enter Item ID to restock: ");
                String id = scanner.nextLine();
                System.out.print("Quantity to add: ");
                int qty = Integer.parseInt(scanner.nextLine());
                try {
                    inventoryService.addStockToItem(id, qty);
                    System.out.println("Inventory restocked.");
                } catch (Exception e) { System.out.println(e.getMessage()); }
            }
            case "3" -> consumeSupplies();
            case "4" -> inventoryService.generateInventoryReport();
        }
    }

    private static void viewInventoryBrief() {
        inventoryService.getAllItems().forEach(i -> System.out.println(i.getItemId() + ": " + i.getItemName() + " (Stock: " + i.getCurrentStock() + ")"));
    }

    private static void consumeSupplies() {
        viewInventoryBrief();
        System.out.print("Enter Item ID: ");
        String id = scanner.nextLine();
        System.out.print("Quantity used: ");
        int qty = Integer.parseInt(scanner.nextLine());
        try {
            inventoryService.consumeInventory(id, qty);
            System.out.println("Stock updated.");
        } catch (Exception e) { System.out.println(e.getMessage()); }
    }

    private static void registerCustomer() {
        System.out.print("Customer Name: ");
        String name = scanner.nextLine();
        System.out.print("Address: ");
        String addr = scanner.nextLine();
        System.out.print("Contact: ");
        String contact = scanner.nextLine();
        Customer c = new Customer(customerService.generateNextId(), name, addr, contact);
        customerService.registerCustomer(c);
        System.out.println("Customer registered: " + c.getCustomerId());
    }

    private static void createLaundryOrder() {
        List<Customer> customers = customerService.getAllCustomers();
        Customer selectedCustomer;

        System.out.println("\nSelect Customer:");
        System.out.println("0. [Register New Customer]");
        for (int i = 0; i < customers.size(); i++) {
            System.out.println((i + 1) + ". " + customers.get(i).getCustomerName());
        }
        
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice == 0) {
            registerCustomer();
            customers = customerService.getAllCustomers();
            selectedCustomer = customers.get(customers.size() - 1);
        } else {
            selectedCustomer = customers.get(choice - 1);
        }

        System.out.println("Select Service:");
        Service[] services = Service.values();
        for (int i = 0; i < services.length; i++) {
            System.out.println((i + 1) + ". " + services[i]);
        }
        int sIdx = Integer.parseInt(scanner.nextLine());
        Service service = services[sIdx - 1];

        System.out.print("Total Weight (kg): ");
        double weight = Double.parseDouble(scanner.nextLine());
        
        Transaction t = new Transaction(transactionService.generateNextId(), selectedCustomer, service, new Date(), weight, 0, 0, 0, 0, Status.PENDING);
        t.calculateTotalCost();
        
        System.out.println("Total Cost: Php " + t.getTotalCost() + " (" + t.getLoadCount() + " loads)");
        System.out.print("Enter Down Payment amount (Enter 0 if none): ");
        double dp = Double.parseDouble(scanner.nextLine());
        t.makePayment(dp);
        
        transactionService.createTransaction(t);
        System.out.println("Order created successfully!");
        t.printReceipt();
    }

    private static void receivePayment() {
        List<Transaction> pending = transactionService.getPendingTransactions();
        if (pending.isEmpty()) {
            System.out.println("No pending payments.");
            return;
        }

        System.out.println("\n--- Pending Transactions ---");
        for (int i = 0; i < pending.size(); i++) {
            Transaction t = pending.get(i);
            System.out.println((i + 1) + ". ID: " + t.getTransactionId() + " | Customer: " + t.getCustomer().getCustomerName() + " | Balance: Php " + t.getOutstandingBalance());
        }
        System.out.print("Select transaction number (or 0 to cancel): ");
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice == 0) return;

        Transaction selected = pending.get(choice - 1);
        System.out.print("Enter Payment Amount: ");
        double amt = Double.parseDouble(scanner.nextLine());
        selected.makePayment(amt);
        System.out.println("Payment recorded.");
        selected.printReceipt();
    }

    private static void updateLaundryStatus() {
        List<Transaction> all = transactionService.getAllTransactions();
        if (all.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.println("\n--- Current Orders ---");
        for (int i = 0; i < all.size(); i++) {
            Transaction t = all.get(i);
            System.out.println((i + 1) + ". " + t.getTransactionId() + " | Status: " + t.getLaundryStatus() + " | Customer: " + t.getCustomer().getCustomerName());
        }
        System.out.print("Select order number (or 0 to cancel): ");
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice == 0) return;

        Transaction selected = all.get(choice - 1);
        System.out.println("Select New Status: (1. PENDING, 2. PROCESSING, 3. COMPLETED)");
        int sIdx = Integer.parseInt(scanner.nextLine());
        selected.setLaundryStatus(Status.values()[sIdx - 1]);
        System.out.println("Status updated to " + selected.getLaundryStatus());
    }

    private static void viewAllCustomers() {
        System.out.println("\n--- Registered Customers ---");
        customerService.getAllCustomers().forEach(c -> 
            System.out.println(c.getCustomerId() + " | " + c.getCustomerName() + " | " + c.getAddress() + " | " + c.getContactNumber()));
    }

    private static void viewAllTransactions() {
        System.out.println("\n--- Transaction History ---");
        transactionService.getAllTransactions().forEach(t -> {
            String balStr = t.isFullyPaid() ? "FULLY PAID" : "Bal: Php " + t.getOutstandingBalance();
            System.out.println(t.getTransactionId() + " | " + t.getCustomer().getCustomerName() + " | " + t.getLaundryStatus() + " | " + balStr);
        });
    }

    private static void recordExpense() {
        System.out.print("Expense Name: ");
        String name = scanner.nextLine();
        System.out.print("Cost: ");
        double cost = Double.parseDouble(scanner.nextLine());
        expenseService.addExpense(new Expense(expenseService.generateNextId(), name, cost, new Date()));
        System.out.println("Expense recorded.");
    }

    private static void viewFinancialReports(User admin) {
        System.out.println("Select Range: (daily, weekly, monthly, quarterly, yearly)");
        String range = scanner.nextLine();
        transactionService.generateFinancialReport(admin, range, null, null);
    }
}

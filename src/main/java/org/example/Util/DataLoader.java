package org.example.Util;

import org.example.Model.*;
import org.example.Service.*;

import java.util.Calendar;
import java.util.Date;

public class DataLoader {

    public static void loadPlaceholderData(
            CustomerService customerService,
            InventoryService inventoryService,
            ExpenseService expenseService,
            TransactionService transactionService
    ) {
        // 1. Add Customers (Only if they don't exist)
        if (customerService.getAllCustomers().isEmpty()) {
            Customer c1 = new Customer("C001", "John Doe", "123 Maple St", "09171234567");
            Customer c2 = new Customer("C002", "Jane Smith", "456 Oak Ave", "09187654321");
            Customer c3 = new Customer("C003", "Bob Johnson", "789 Pine Rd", "09190001111");
            Customer c4 = new Customer("C004", "Alice Brown", "321 Cedar Blvd", "09202223333");

            customerService.registerCustomer(c1);
            customerService.registerCustomer(c2);
            customerService.registerCustomer(c3);
            customerService.registerCustomer(c4);
        }

        // 2. Add Inventory Items (Only if empty)
        if (inventoryService.getAllItems().isEmpty()) {
            inventoryService.addItem(new InventoryItem("I001", "Detergent (Liquid)", 100, 20));
            inventoryService.addItem(new InventoryItem("I002", "Fabric Softener", 50, 10));
            inventoryService.addItem(new InventoryItem("I003", "Bleach", 30, 5));
        }

        // 3. Generate Historical Data (Only if no transactions exist)
        if (transactionService.getAllTransactions().isEmpty()) {
            Calendar cal = Calendar.getInstance();
            Customer c1 = customerService.getCustomerById("C001").orElse(null);
            Customer c2 = customerService.getCustomerById("C002").orElse(null);
            Customer c3 = customerService.getCustomerById("C003").orElse(null);
            Customer c4 = customerService.getCustomerById("C004").orElse(null);

            if (c1 != null && c2 != null && c3 != null && c4 != null) {
                // --- TODAY (Daily Range) ---
                addTransaction(transactionService, c1, Service.WASH_DRY_FOLD, 8.0, cal.getTime(), true);
                addTransaction(transactionService, c2, Service.WASH_DRY, 5.0, cal.getTime(), false);
                addTransaction(transactionService, c3, Service.WASH_DRY_FOLD, 6.0, cal.getTime(), true, Status.READY_FOR_DELIVERY);
                addTransaction(transactionService, c4, Service.WASH, 4.0, cal.getTime(), false, Status.READY_FOR_DELIVERY);
                expenseService.addExpense(new Expense("E001", "Daily Snacks", 150.0, cal.getTime()));

                // --- YESTERDAY ---
                cal.add(Calendar.DAY_OF_YEAR, -1);
                addTransaction(transactionService, c3, Service.WASH, 10.0, cal.getTime(), true);
                expenseService.addExpense(new Expense("E002", "Water Refill", 200.0, cal.getTime()));

                // --- LAST WEEK ---
                cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -4);
                addTransaction(transactionService, c4, Service.WASH_DRY_FOLD, 15.0, cal.getTime(), true);
                addTransaction(transactionService, c1, Service.DRY, 7.0, cal.getTime(), true);
                expenseService.addExpense(new Expense("E003", "Cleaning Supplies", 800.0, cal.getTime()));

                // --- LAST MONTH ---
                cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -15);
                addTransaction(transactionService, c2, Service.WASH_DRY, 12.0, cal.getTime(), true);
                addTransaction(transactionService, c3, Service.WASH_DRY_FOLD, 9.0, cal.getTime(), false);
                expenseService.addExpense(new Expense("E004", "Electricity Bill", 3500.0, cal.getTime()));

                // --- 2 MONTHS AGO ---
                cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -2);
                addTransaction(transactionService, c4, Service.WASH_DRY_FOLD, 20.0, cal.getTime(), true);
                expenseService.addExpense(new Expense("E005", "Rent - Feb", 15000.0, cal.getTime()));
            }
        }
    }

    private static void addTransaction(TransactionService service, Customer customer, Service type, double weight, Date date, boolean fullyPaid, Status status) {
        InitialPaymentMethod initialMethod = fullyPaid ? InitialPaymentMethod.FULL_PAYMENT : InitialPaymentMethod.DOWN_PAYMENT;
        Transaction t = new Transaction(service.generateNextId(), customer, type, date, weight, 0, 0, 0, 0, status, initialMethod, "System");
        t.calculateTotalCost();
        if (fullyPaid) {
            t.makePayment(t.getTotalCost());
        } else {
            t.makePayment(t.getTotalCost() * 0.5); // 50% downpayment
        }
        service.createTransaction(t);
    }

    private static void addTransaction(TransactionService service, Customer customer, Service type, double weight, Date date, boolean fullyPaid) {
        addTransaction(service, customer, type, weight, date, fullyPaid, fullyPaid ? Status.DELIVERED : Status.PROCESSING);
    }

}

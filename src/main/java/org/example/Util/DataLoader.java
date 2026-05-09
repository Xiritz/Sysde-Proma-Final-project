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
        // 1. Add Customers
        Customer c1 = new Customer(customerService.generateNextId(), "John Doe", "123 Maple St", "09171234567");
        Customer c2 = new Customer(customerService.generateNextId(), "Jane Smith", "456 Oak Ave", "09187654321");
        Customer c3 = new Customer(customerService.generateNextId(), "Bob Johnson", "789 Pine Rd", "09190001111");
        Customer c4 = new Customer(customerService.generateNextId(), "Alice Brown", "321 Cedar Blvd", "09202223333");
        
        customerService.registerCustomer(c1);
        customerService.registerCustomer(c2);
        customerService.registerCustomer(c3);
        customerService.registerCustomer(c4);

        // 2. Add Inventory Items
        inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), "Detergent (Liquid)", 100, 20));
        inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), "Fabric Softener", 50, 10));
        inventoryService.addItem(new InventoryItem(inventoryService.generateNextId(), "Bleach", 30, 5));

        // 3. Generate Historical Data
        Calendar cal = Calendar.getInstance();

        // --- TODAY (Daily Range) ---
        addTransaction(transactionService, c1, Service.WASH_DRY_FOLD, 8.0, cal.getTime(), true);
        addTransaction(transactionService, c2, Service.WASH_DRY, 5.0, cal.getTime(), false);
        addTransaction(transactionService, c3, Service.WASH_DRY_FOLD, 6.0, cal.getTime(), true, Status.READY_FOR_DELIVERY);
        addTransaction(transactionService, c4, Service.WASH, 4.0, cal.getTime(), false, Status.READY_FOR_DELIVERY);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Daily Snacks", 150.0, cal.getTime()));

        // --- YESTERDAY ---
        cal.add(Calendar.DAY_OF_YEAR, -1);
        addTransaction(transactionService, c3, Service.WASH, 10.0, cal.getTime(), true);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Water Refill", 200.0, cal.getTime()));

        // --- LAST WEEK (Within Weekly Range) ---
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -4);
        addTransaction(transactionService, c4, Service.WASH_DRY_FOLD, 15.0, cal.getTime(), true);
        addTransaction(transactionService, c1, Service.DRY, 7.0, cal.getTime(), true);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Cleaning Supplies", 800.0, cal.getTime()));

        // --- LAST MONTH (Within Monthly Range) ---
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -15);
        addTransaction(transactionService, c2, Service.WASH_DRY, 12.0, cal.getTime(), true);
        addTransaction(transactionService, c3, Service.WASH_DRY_FOLD, 9.0, cal.getTime(), false);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Electricity Bill", 3500.0, cal.getTime()));

        // --- 2 MONTHS AGO (Within Quarterly/Yearly Range) ---
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -2);
        addTransaction(transactionService, c4, Service.WASH_DRY_FOLD, 20.0, cal.getTime(), true);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Rent - Feb", 15000.0, cal.getTime()));

        // --- 5 MONTHS AGO (Within Yearly Range) ---
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -5);
        addTransaction(transactionService, c1, Service.WASH, 25.0, cal.getTime(), true);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Machine Maintenance", 5000.0, cal.getTime()));

        // --- 13 MONTHS AGO (Outside Yearly Range - Good for testing "Yearly" filters) ---
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -13);
        addTransaction(transactionService, c2, Service.WASH_DRY_FOLD, 10.0, cal.getTime(), true);
        expenseService.addExpense(new Expense(expenseService.generateNextId(), "Old License Fee", 2000.0, cal.getTime()));
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

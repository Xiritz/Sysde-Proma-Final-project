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
        // Data loading is disabled as per user request to remove hardcoded data.
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

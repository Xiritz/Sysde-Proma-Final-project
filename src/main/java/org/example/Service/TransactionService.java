package org.example.Service;

import org.example.Model.Role;
import org.example.Model.Status;
import org.example.Model.Transaction;
import org.example.Model.User;
import org.example.Model.Expense;
import org.example.Model.InventoryItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionService {
    private List<Transaction> transactions;
    private ExpenseService expenseService;
    private InventoryService inventoryService;
    private int idCounter;

    public TransactionService(ExpenseService expenseService, InventoryService inventoryService) {
        this.transactions = new ArrayList<>();
        this.expenseService = expenseService;
        this.inventoryService = inventoryService;
        this.idCounter = 1;
    }

    public String generateNextId() {
        return String.format("T%03d", idCounter++);
    }

    public void createTransaction(Transaction transaction) {
        transaction.calculateTotalCost();
        transactions.add(transaction);
        
        // Auto-consume supplies
        consumeSupplies();
    }

    private void consumeSupplies() {
        if (inventoryService == null) return;

        List<InventoryItem> allItems = inventoryService.getAllItems();
        
        // Find Liquid Detergent and Fabric Softener by name
        Optional<InventoryItem> detergent = allItems.stream()
                .filter(i -> i.getItemName().toLowerCase().contains("detergent") && i.getItemName().toLowerCase().contains("liquid"))
                .findFirst();
        
        Optional<InventoryItem> softener = allItems.stream()
                .filter(i -> i.getItemName().toLowerCase().contains("fabric") && i.getItemName().toLowerCase().contains("softener"))
                .findFirst();

        detergent.ifPresent(item -> {
            try {
                inventoryService.consumeInventory(item.getItemId(), 1);
            } catch (Exception e) {
                System.err.println("Could not consume detergent: " + e.getMessage());
            }
        });

        softener.ifPresent(item -> {
            try {
                inventoryService.consumeInventory(item.getItemId(), 1);
            } catch (Exception e) {
                System.err.println("Could not consume softener: " + e.getMessage());
            }
        });
    }

    public List<Transaction> getPendingTransactions() {
        return transactions.stream()
                .filter(t -> !t.isFullyPaid())
                .collect(Collectors.toList());
    }

    public void updateTransactionStatus(String transactionId, Status newStatus) {
        transactions.stream()
                .filter(t -> t.getTransactionId().trim().equalsIgnoreCase(transactionId.trim()))
                .findFirst()
                .ifPresent(t -> t.setLaundryStatus(newStatus));
    }

    public void receivePayment(String transactionId, double amount) {
        transactions.stream()
                .filter(t -> t.getTransactionId().trim().equalsIgnoreCase(transactionId.trim()))
                .findFirst()
                .ifPresent(t -> t.makePayment(amount));
    }

    public List<Transaction> getTransactionsByDateRange(Date start, Date end) {
        return transactions.stream()
                .filter(t -> !t.getDatePlaced().before(start) && !t.getDatePlaced().after(end))
                .collect(Collectors.toList());
    }

    public void generateFinancialReport(User requester, String rangeType, Date customStart, Date customEnd) {
        if (requester.getRole() != Role.ADMIN) {
            System.out.println("Access Denied: You do not have permission to view financial reports.");
            return;
        }

        Date start, end;
        Calendar cal = Calendar.getInstance();
        end = cal.getTime();

        switch (rangeType.toLowerCase()) {
            case "daily":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                start = cal.getTime();
                break;
            case "weekly":
                cal.add(Calendar.DAY_OF_YEAR, -7);
                start = cal.getTime();
                break;
            case "monthly":
                cal.add(Calendar.MONTH, -1);
                start = cal.getTime();
                break;
            case "quarterly":
                cal.add(Calendar.MONTH, -3);
                start = cal.getTime();
                break;
            case "yearly":
                cal.add(Calendar.YEAR, -1);
                start = cal.getTime();
                break;
            case "custom":
                start = customStart;
                end = customEnd;
                break;
            default:
                System.out.println("Invalid range type.");
                return;
        }

        List<Transaction> filteredTrans = getTransactionsByDateRange(start, end);
        double totalRevenue = filteredTrans.stream().mapToDouble(Transaction::getAmountPaid).sum();
        double totalOutstanding = filteredTrans.stream().mapToDouble(Transaction::getOutstandingBalance).sum();
        
        List<Expense> filteredExpenses = expenseService.getExpensesByDateRange(start, end);
        double totalExpenses = filteredExpenses.stream().mapToDouble(Expense::getCost).sum();
        
        double netProfit = totalRevenue - totalExpenses;

        System.out.println("---------- FINANCIAL REPORT (" + rangeType.toUpperCase() + ") ----------");
        System.out.println("Period: " + start + " to " + end);
        System.out.println("Total Transactions: " + filteredTrans.size());
        System.out.println("Total Revenue Collected: Php " + totalRevenue);
        System.out.println("Total Outstanding Balance: Php " + totalOutstanding);
        System.out.println("Total Expenses Incurred: Php " + totalExpenses);
        System.out.println("Net Profit: Php " + netProfit);
        
        if (!filteredExpenses.isEmpty()) {
            System.out.println("\nExpense Breakdown:");
            for (Expense e : filteredExpenses) {
                System.out.printf("- %-20s: Php %.2f (%s)%n", e.getExpenseName(), e.getCost(), e.getDateIncurred());
            }
        }
        System.out.println("-------------------------------------------------------");
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public void removeTransaction(User requester, String transactionId) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove transactions.");
        }
        transactions.removeIf(t -> t.getTransactionId().equals(transactionId));
    }
}

package org.example.Service;

import org.example.App;
import org.example.Model.*;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionService {
    private ExpenseService expenseService;
    private InventoryService inventoryService;

    public TransactionService(ExpenseService expenseService, InventoryService inventoryService) {
        this.expenseService = expenseService;
        this.inventoryService = inventoryService;
    }

    public String generateNextId() {
        String query = "SELECT transactionId FROM transactions ORDER BY transactionId DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("transactionId");
                int numericPart = Integer.parseInt(lastId.substring(1));
                return String.format("T%03d", numericPart + 1);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return "T001"; // Fallback
    }

    public void createTransaction(Transaction transaction) {
        if (!hasEnoughSupplies(transaction.getLoadCount())) {
            throw new IllegalStateException("Insufficient inventory! Transaction cannot proceed because there is not enough Detergent or Fabric Softener for " + transaction.getLoadCount() + " loads.");
        }

        transaction.calculateTotalCost();
        
        String query = "INSERT INTO transactions (transactionId, customerId, service, datePlaced, weightKg, loadCount, totalCost, amountPaid, outstandingBalance, laundryStatus, initialPaymentMethod, createdBy) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, transaction.getTransactionId());
            stmt.setString(2, transaction.getCustomer().getCustomerId());
            stmt.setString(3, transaction.getService().name());
            stmt.setTimestamp(4, new Timestamp(transaction.getDatePlaced().getTime()));
            stmt.setDouble(5, transaction.getWeightKg());
            stmt.setInt(6, transaction.getLoadCount());
            stmt.setDouble(7, transaction.getTotalCost());
            stmt.setDouble(8, transaction.getAmountPaid());
            stmt.setDouble(9, transaction.getOutstandingBalance());
            stmt.setString(10, transaction.getLaundryStatus().name());
            stmt.setString(11, transaction.getInitialPaymentMethod().name());
            stmt.setString(12, transaction.getCreatedBy());

            stmt.executeUpdate();
            
            // Auto-consume supplies based on load count
            consumeSupplies(transaction.getLoadCount());
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasEnoughSupplies(int loadCount) {
        if (inventoryService == null) return true;
        
        List<InventoryItem> allItems = inventoryService.getAllItems();
        
        Optional<InventoryItem> detergent = allItems.stream().filter(i -> i.getItemId().equals("I001")).findFirst();
        Optional<InventoryItem> softener = allItems.stream().filter(i -> i.getItemId().equals("I002")).findFirst();

        boolean detergentOk = detergent.isPresent() && detergent.get().getCurrentStock() >= loadCount;
        boolean softenerOk = softener.isPresent() && softener.get().getCurrentStock() >= loadCount;

        return detergentOk && softenerOk;
    }

    private void consumeSupplies(int loadCount) {
        if (inventoryService == null) return;

        try {
            inventoryService.consumeInventory("I001", loadCount); // Detergent
            inventoryService.consumeInventory("I002", loadCount); // Softener
        } catch (Exception e) {
            System.err.println("Could not consume supplies: " + e.getMessage());
        }
    }

    public List<Transaction> getPendingTransactions() {
        return getAllTransactions().stream()
                .filter(t -> !t.isFullyPaid())
                .collect(Collectors.toList());
    }

    public void updateTransactionStatus(String transactionId, Status newStatus) {
        String query = "UPDATE transactions SET laundryStatus = ? WHERE transactionId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newStatus.name());
            stmt.setString(2, transactionId.trim());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void receivePayment(String transactionId, double amount) {
        // First get current state
        String selectQuery = "SELECT totalCost, amountPaid FROM transactions WHERE transactionId = ?";
        String updateQuery = "UPDATE transactions SET amountPaid = ?, outstandingBalance = ? WHERE transactionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            double totalCost = 0;
            double currentPaid = 0;
            
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setString(1, transactionId.trim());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        totalCost = rs.getDouble("totalCost");
                        currentPaid = rs.getDouble("amountPaid");
                    } else {
                        return;
                    }
                }
            }
            
            double newAmountPaid = currentPaid + amount;
            double newBalance = totalCost - newAmountPaid;
            
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setDouble(1, newAmountPaid);
                updateStmt.setDouble(2, newBalance);
                updateStmt.setString(3, transactionId.trim());
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Transaction> getTransactionsByDateRange(Date start, Date end) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transactions WHERE datePlaced BETWEEN ? AND ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setTimestamp(1, new Timestamp(start.getTime()));
            stmt.setTimestamp(2, new Timestamp(end.getTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transactions";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Transaction t = mapResultSetToTransaction(rs);
                    if (t != null) {
                        transactions.add(t);
                    }
                } catch (Exception e) {
                    System.err.println("Error mapping transaction: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        String customerId = rs.getString("customerId");
        Customer customer = App.customerService.getCustomerById(customerId).orElse(null);
        
        try {
            return new Transaction(
                    rs.getString("transactionId"),
                    customer,
                    Service.valueOf(rs.getString("service")),
                    new Date(rs.getTimestamp("datePlaced").getTime()),
                    rs.getDouble("weightKg"),
                    rs.getInt("loadCount"),
                    rs.getDouble("totalCost"),
                    rs.getDouble("amountPaid"),
                    rs.getDouble("outstandingBalance"),
                    Status.valueOf(rs.getString("laundryStatus")),
                    InitialPaymentMethod.valueOf(rs.getString("initialPaymentMethod")),
                    rs.getString("createdBy")
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Skipping malformed transaction " + rs.getString("transactionId") + ": " + e.getMessage());
            return null;
        }
    }

    public void removeTransaction(User requester, String transactionId) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove transactions.");
        }
        
        String query = "DELETE FROM transactions WHERE transactionId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, transactionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTransaction(User requester, Transaction updatedTransaction) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
            throw new SecurityException("Only admins or owners can update transactions.");
        }
        
        String query = "UPDATE transactions SET customerId = ?, service = ?, datePlaced = ?, weightKg = ?, loadCount = ?, totalCost = ?, amountPaid = ?, outstandingBalance = ?, laundryStatus = ?, initialPaymentMethod = ?, createdBy = ? WHERE transactionId = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updatedTransaction.getCustomer().getCustomerId());
            stmt.setString(2, updatedTransaction.getService().name());
            stmt.setTimestamp(3, new Timestamp(updatedTransaction.getDatePlaced().getTime()));
            stmt.setDouble(4, updatedTransaction.getWeightKg());
            stmt.setInt(5, updatedTransaction.getLoadCount());
            stmt.setDouble(6, updatedTransaction.getTotalCost());
            stmt.setDouble(7, updatedTransaction.getAmountPaid());
            stmt.setDouble(8, updatedTransaction.getOutstandingBalance());
            stmt.setString(9, updatedTransaction.getLaundryStatus().name());
            stmt.setString(10, updatedTransaction.getInitialPaymentMethod().name());
            stmt.setString(11, updatedTransaction.getCreatedBy());
            stmt.setString(12, updatedTransaction.getTransactionId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateFinancialReport(User requester, String rangeType, Date customStart, Date customEnd) {
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.OWNER) {
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
}

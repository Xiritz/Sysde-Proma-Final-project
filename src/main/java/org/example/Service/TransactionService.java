package org.example.Service;

import org.example.Model.Status;
import org.example.Model.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransactionService {
    private List<Transaction> transactions;

    public TransactionService() {
        this.transactions = new ArrayList<>();
    }

    public void createTransaction(Transaction transaction) {
        transaction.calculateTotalCost();
        transactions.add(transaction);
    }

    public void updateTransactionStatus(String transactionId, Status newStatus) {
        transactions.stream()
                .filter(t -> t.getTransactionId().equals(transactionId))
                .findFirst()
                .ifPresent(t -> t.setLaundryStatus(newStatus));
    }

    public void receivePayment(String transactionId, double amount) {
        transactions.stream()
                .filter(t -> t.getTransactionId().equals(transactionId))
                .findFirst()
                .ifPresent(t -> t.makePayment(amount));
    }

    public List<Transaction> getTransactionsByDateRange(Date start, Date end) {
        return transactions.stream()
                .filter(t -> !t.getDatePlaced().before(start) && !t.getDatePlaced().after(end))
                .collect(Collectors.toList());
    }

    public void generateFinancialReport(Date start, Date end) {
        List<Transaction> filtered = getTransactionsByDateRange(start, end);
        double totalRevenue = filtered.stream().mapToDouble(Transaction::getAmountPaid).sum();
        double totalOutstanding = filtered.stream().mapToDouble(Transaction::getOutstandingBalance).sum();

        System.out.println("---------- FINANCIAL REPORT ----------");
        System.out.println("Period: " + start + " to " + end);
        System.out.println("Total Transactions: " + filtered.size());
        System.out.println("Total Revenue Collected: Php " + totalRevenue);
        System.out.println("Total Outstanding Balance: Php " + totalOutstanding);
        System.out.println("--------------------------------------");
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }
}

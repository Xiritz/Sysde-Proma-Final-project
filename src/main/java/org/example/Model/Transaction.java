package org.example.Model;

import java.util.Date;

public class Transaction {
    private String transactionId;
    private Customer customer;
    private Service service;
    private Date datePlaced;
    private double weightKg;
    private int loadCount;
    private double totalCost;
    private double amountPaid;
    private double outstandingBalance;
    private Status laundryStatus;
    private InitialPaymentMethod initialPaymentMethod; // "Down Payment", "Pay Later", "Full Payment"
    private String createdBy;

    public Transaction(String transactionId, Customer customer, Service service, Date datePlaced, double weightKg,
                       int loadCount, double totalCost, double amountPaid, double outstandingBalance,
                       Status laundryStatus, InitialPaymentMethod initialPaymentMethod, String createdBy) {
        this.transactionId = transactionId;
        this.customer = customer;
        this.service = service;
        this.datePlaced = datePlaced;
        this.weightKg = weightKg;
        this.loadCount = loadCount;
        this.totalCost = totalCost;
        this.amountPaid = amountPaid;
        this.outstandingBalance = outstandingBalance;
        this.laundryStatus = laundryStatus;
        this.initialPaymentMethod = initialPaymentMethod;
        this.createdBy = createdBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }


    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Date getDatePlaced() {
        return datePlaced;
    }

    public void setDatePlaced(Date datePlaced) {
        this.datePlaced = datePlaced;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public int getLoadCount() {
        return loadCount;
    }

    public void setLoadCount(int loadCount) {
        this.loadCount = loadCount;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public double getOutstandingBalance() {
        return outstandingBalance;
    }

    public double getBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public Status getLaundryStatus() {
        return laundryStatus;
    }

    public void setLaundryStatus(Status laundryStatus) {
        this.laundryStatus = laundryStatus;
    }

    public InitialPaymentMethod getInitialPaymentMethod() {
        return initialPaymentMethod;
    }

    public void setInitialPaymentMethod(InitialPaymentMethod initialPaymentMethod) {
        this.initialPaymentMethod = initialPaymentMethod;
    }

    public double calculateTotalCost() {
        double pricePerLoad = switch (this.service) {
            case WASH -> 90.0;
            case WASH_DRY -> 180.0;
            case WASH_DRY_FOLD -> 220.0;
        };
        
        // Auto-compute loads if not set: Use 8kg soft cap as standard
        if (this.loadCount == 0 && this.weightKg > 0) {
            this.loadCount = (int) Math.ceil(this.weightKg / 8.0);
        }
        this.totalCost = pricePerLoad * loadCount;
        updateBalance();
        return this.totalCost;
    }

    public void makePayment(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative.");
        }
        if (amount > this.outstandingBalance && this.totalCost > 0) {
            throw new IllegalArgumentException(String.format("Payment (Php %.2f) exceeds the outstanding balance (Php %.2f).", amount, this.outstandingBalance));
        }
        this.amountPaid += amount;
        updateBalance();
    }

    public double calculateOutstandingBalance() {
        updateBalance();
        return this.outstandingBalance;
    }

    private void updateBalance() {
        this.outstandingBalance = this.totalCost - this.amountPaid;
    }

    public boolean isFullyPaid() {
        return outstandingBalance <= 0;
    }

    public void printReceipt() {
        System.out.println("---------- RECEIPT ----------");
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Customer: " + customer.getCustomerName());
        System.out.println("Service: " + service);
        System.out.println("Total Weight: " + weightKg + " kg (" + loadCount + " loads)");
        System.out.println("Total Cost: Php " + totalCost);
        System.out.println("Amount Paid: Php " + amountPaid);
        String balanceStr = isFullyPaid() ? "FULLY PAID" : "Php " + outstandingBalance;
        System.out.println("Outstanding Balance: " + balanceStr);
        System.out.println("Status: " + laundryStatus);
        System.out.println("-----------------------------");
    }
}

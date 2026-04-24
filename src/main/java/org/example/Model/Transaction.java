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

    public Transaction(String transactionId, Customer customer, Service service, Date datePlaced, double weightKg,
                       int loadCount, double totalCost, double amountPaid, double outstandingBalance,
                       Status laundryStatus) {
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

    public void setOutstandingBalance(double outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public Status getLaundryStatus() {
        return laundryStatus;
    }

    public void setLaundryStatus(Status laundryStatus) {
        this.laundryStatus = laundryStatus;
    }

    public double calculateTotalCost(){
        return 0;
    }

    public void makePayment(){

    }

    public double calculateOutstandingBalance(){
        return 0;
    }

    public boolean isFullyPaid(){
        return outstandingBalance == 0;
    }

    public void printReceipt(){

    }
}

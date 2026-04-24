package org.example.Model;

public class Employee extends User{

    public Employee(String userId, String username, String password, Role role) {
        super(userId, username, password, role);
    }

    public void registerCustomer(Customer customer){

    }

    public void updateCustomer(Customer customer){

    }

    public void createTransaction(Transaction transaction){

    }

    public void updateTransaction(String transactionId, Status status){

    }

    public void receivePayment(String transactionId, double amount){

    }

    public void consumeInventory(String itemId, int quantity){

    }
}

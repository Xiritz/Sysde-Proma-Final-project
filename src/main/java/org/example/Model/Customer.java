package org.example.Model;

public class Customer {
    private String customerId;
    private String customerName;
    private String address;
    private String contactNumber;

    public Customer(String customerId, String customerName, String address, String contactNumber) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}

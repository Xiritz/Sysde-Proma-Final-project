package org.example.Service;

import org.example.Model.Customer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerService {
    private List<Customer> customers;
    private int idCounter;

    public CustomerService() {
        this.customers = new ArrayList<>();
        this.idCounter = 1;
    }

    public String generateNextId() {
        return String.format("C%03d", idCounter++);
    }

    public void registerCustomer(Customer customer) {
        customers.add(customer);
    }

    public Optional<Customer> getCustomerById(String customerId) {
        return customers.stream()
                .filter(c -> c.getCustomerId().equals(customerId))
                .findFirst();
    }

    public List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    public void updateCustomer(Customer updatedCustomer) {
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId().equals(updatedCustomer.getCustomerId())) {
                customers.set(i, updatedCustomer);
                return;
            }
        }
    }
}

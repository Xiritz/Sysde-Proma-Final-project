package org.example.Service;

import org.example.Model.Customer;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerService {

    public CustomerService() {
        // Data is now in DB
    }

    public String generateNextId() {
        String query = "SELECT customerId FROM customers ORDER BY customerId DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("customerId");
                int numericPart = Integer.parseInt(lastId.substring(1));
                return String.format("C%03d", numericPart + 1);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return "C001"; // Fallback
    }

    public void registerCustomer(Customer customer) {
        if (isDuplicate(customer)) {
            throw new IllegalArgumentException("A customer with the exact same name, address, and contact number already exists.");
        }

        String query = "INSERT INTO customers (customerId, customerName, address, contactNumber) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getCustomerName());
            stmt.setString(3, customer.getAddress());
            stmt.setString(4, customer.getContactNumber());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isDuplicate(Customer customer) {
        String query = "SELECT COUNT(*) FROM customers WHERE customerName = ? AND address = ? AND contactNumber = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, customer.getCustomerName());
            stmt.setString(2, customer.getAddress());
            stmt.setString(3, customer.getContactNumber());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Customer> getCustomerById(String customerId) {
        String query = "SELECT * FROM customers WHERE customerId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, customerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = new Customer(
                            rs.getString("customerId"),
                            rs.getString("customerName"),
                            rs.getString("address"),
                            rs.getString("contactNumber")
                    );
                    return Optional.of(customer);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                customers.add(new Customer(
                        rs.getString("customerId"),
                        rs.getString("customerName"),
                        rs.getString("address"),
                        rs.getString("contactNumber")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public void updateCustomer(org.example.Model.User requester, Customer updatedCustomer) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can update customers.");
        }

        String query = "UPDATE customers SET customerName = ?, address = ?, contactNumber = ? WHERE customerId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updatedCustomer.getCustomerName());
            stmt.setString(2, updatedCustomer.getAddress());
            stmt.setString(3, updatedCustomer.getContactNumber());
            stmt.setString(4, updatedCustomer.getCustomerId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeCustomer(org.example.Model.User requester, String customerId) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove customers.");
        }

        // Rule: Cannot delete if they have pending payments
        if (hasPendingPayments(customerId)) {
            throw new IllegalStateException("Cannot delete customer: They still have transactions with an outstanding balance.");
        }

        // Rule: Cannot delete if they have active laundry orders (not Delivered)
        if (hasActiveOrders(customerId)) {
            throw new IllegalStateException("Cannot delete customer: They still have active/pending laundry orders.");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Delete transaction history (cascading manual delete)
                String deleteTrans = "DELETE FROM transactions WHERE customerId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteTrans)) {
                    stmt.setString(1, customerId);
                    stmt.executeUpdate();
                }

                // 2. Delete customer
                String deleteCust = "DELETE FROM customers WHERE customerId = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteCust)) {
                    stmt.setString(1, customerId);
                    stmt.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean hasPendingPayments(String customerId) {
        String query = "SELECT COUNT(*) FROM transactions WHERE customerId = ? AND outstandingBalance > 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean hasActiveOrders(String customerId) {
        String query = "SELECT COUNT(*) FROM transactions WHERE customerId = ? AND laundryStatus != 'Delivered'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

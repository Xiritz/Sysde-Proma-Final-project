package org.example.Service;

import org.example.Model.Expense;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ExpenseService {

    public ExpenseService() {
        // Data is now in DB
    }

    public String generateNextId() {
        String query = "SELECT expenseId FROM expenses ORDER BY expenseId DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("expenseId");
                // Extract numeric part from "EX001"
                int numericPart = Integer.parseInt(lastId.substring(2));
                return String.format("EX%03d", numericPart + 1);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return "EX001"; // Fallback
    }

    public void addExpense(Expense expense) {
        String query = "INSERT INTO expenses (expenseId, expenseName, cost, dateIncurred) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, expense.getExpenseId());
            stmt.setString(2, expense.getExpenseName());
            stmt.setDouble(3, expense.getCost());
            stmt.setTimestamp(4, new Timestamp(expense.getDateIncurred().getTime()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Expense> getAllExpenses() {
        List<Expense> expenses = new ArrayList<>();
        String query = "SELECT * FROM expenses";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                expenses.add(new Expense(
                        rs.getString("expenseId"),
                        rs.getString("expenseName"),
                        rs.getDouble("cost"),
                        new Date(rs.getTimestamp("dateIncurred").getTime())
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public List<Expense> getExpensesByDateRange(Date start, Date end) {
        List<Expense> expenses = new ArrayList<>();
        String query = "SELECT * FROM expenses WHERE dateIncurred BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setTimestamp(1, new Timestamp(start.getTime()));
            stmt.setTimestamp(2, new Timestamp(end.getTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(new Expense(
                            rs.getString("expenseId"),
                            rs.getString("expenseName"),
                            rs.getDouble("cost"),
                            new Date(rs.getTimestamp("dateIncurred").getTime())
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }

    public List<Expense> getExpensesByRange(String rangeType, Date customStart, Date customEnd) {
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
                throw new IllegalArgumentException("Invalid range type: " + rangeType);
        }
        return getExpensesByDateRange(start, end);
    }

    public double getTotalExpenses(Date start, Date end) {
        String query = "SELECT SUM(cost) FROM expenses WHERE dateIncurred BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setTimestamp(1, new Timestamp(start.getTime()));
            stmt.setTimestamp(2, new Timestamp(end.getTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void removeExpense(org.example.Model.User requester, String expenseId) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove expenses.");
        }
        
        String query = "DELETE FROM expenses WHERE expenseId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, expenseId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateExpense(org.example.Model.User requester, Expense updatedExpense) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can update expenses.");
        }
        
        String query = "UPDATE expenses SET expenseName = ?, cost = ?, dateIncurred = ? WHERE expenseId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updatedExpense.getExpenseName());
            stmt.setDouble(2, updatedExpense.getCost());
            stmt.setTimestamp(3, new Timestamp(updatedExpense.getDateIncurred().getTime()));
            stmt.setString(4, updatedExpense.getExpenseId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

package org.example.Service;

import org.example.Model.Expense;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ExpenseService {
    private List<Expense> expenses;
    private int idCounter;

    public ExpenseService() {
        this.expenses = new ArrayList<>();
        this.idCounter = 1;
    }

    public String generateNextId() {
        return String.format("EX%03d", idCounter++);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public List<Expense> getExpensesByDateRange(Date start, Date end) {
        return expenses.stream()
                .filter(e -> !e.getDateIncurred().before(start) && !e.getDateIncurred().after(end))
                .collect(Collectors.toList());
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
        return getExpensesByDateRange(start, end).stream()
                .mapToDouble(Expense::getCost)
                .sum();
    }

    public void removeExpense(org.example.Model.User requester, String expenseId) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove expenses.");
        }
        expenses.removeIf(e -> e.getExpenseId().equals(expenseId));
    }
}

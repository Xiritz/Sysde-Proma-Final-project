package org.example.Model;

import java.util.Date;

public class Expense {
    private String expenseId;
    private String expenseName;
    private double cost;
    private Date dateIncurred;

    public Expense(String expenseId, String expenseName, double cost, Date dateIncurred) {
        this.expenseId = expenseId;
        this.expenseName = expenseName;
        this.cost = cost;
        this.dateIncurred = dateIncurred;
    }

    public String getExpenseId() { return expenseId; }
    public String getExpenseName() { return expenseName; }
    public double getCost() { return cost; }
    public Date getDateIncurred() { return dateIncurred; }
}

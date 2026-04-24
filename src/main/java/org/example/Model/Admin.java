package org.example.Model;

import java.util.Date;

public class Admin extends User{

    public Admin(String userId, String username, String password, Role role) {
        super(userId, username, password, role);
    }

    public void addEmployee(String userId, String username, String password) {

    }

    public void updateEmployee(Employee employee){

    }

    public void removeEmployee(){

    }

    public void FinancialReport(Date startDate, Date endDate){

    }

    public void generateInventoryReport(){

    }
}

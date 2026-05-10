package org.example.Model;

public enum Role {
    OWNER("Owner"),
    ADMIN("Admin"),
    EMPLOYEE("Employee");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

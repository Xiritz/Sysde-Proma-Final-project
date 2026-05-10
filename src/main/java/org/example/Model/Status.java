package org.example.Model;

public enum Status {
    PENDING("Pending"),
    PROCESSING("Processing"),
    READY_FOR_DELIVERY("Ready for Delivery"),
    DELIVERED("Delivered");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

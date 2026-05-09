package org.example.Model;

public enum InitialPaymentMethod {
    PAY_LATER("Pay Later"),
    DOWN_PAYMENT("Down Payment"),
    FULL_PAYMENT("Full Payment");

    private final String label;

    InitialPaymentMethod(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

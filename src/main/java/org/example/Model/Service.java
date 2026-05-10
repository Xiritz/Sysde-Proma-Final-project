package org.example.Model;

public enum Service {
    WASH("Wash"),
    WASH_DRY("Wash & Dry"),
    WASH_DRY_FOLD("Wash & Dry & Fold");

    private final String label;

    Service(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

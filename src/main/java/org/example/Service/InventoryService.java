package org.example.Service;

import org.example.Model.InventoryItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InventoryService {
    private List<InventoryItem> inventoryItems;
    private int idCounter;

    public InventoryService() {
        this.inventoryItems = new ArrayList<>();
        this.idCounter = 1;
    }

    public String generateNextId() {
        return String.format("I%03d", idCounter++);
    }

    public void addItem(InventoryItem item) {
        // Check if item name already exists
        Optional<InventoryItem> existing = inventoryItems.stream()
                .filter(i -> i.getItemName().equalsIgnoreCase(item.getItemName()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().addStock(item.getCurrentStock());
            System.out.println("Item already exists. Added " + item.getCurrentStock() + " to " + item.getItemName());
        } else {
            inventoryItems.add(item);
        }
    }

    public void consumeInventory(String itemId, int quantity) {
        Optional<InventoryItem> itemOpt = inventoryItems.stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst();

        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            item.removeStock(quantity);
            if (item.isStockLow()) {
                System.out.println("\n[WARNING] " + item.getItemName() + " is running low! (Current stock: " + item.getCurrentStock() + ")");
            }
        } else {
            throw new IllegalArgumentException("Inventory item not found: " + itemId);
        }
    }

    public void addStockToItem(String itemId, int quantity) {
        inventoryItems.stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .ifPresentOrElse(
                        i -> i.addStock(quantity),
                        () -> { throw new IllegalArgumentException("Item not found: " + itemId); }
                );
    }

    public List<InventoryItem> getLowStockItems() {
        return inventoryItems.stream()
                .filter(InventoryItem::isStockLow)
                .collect(Collectors.toList());
    }

    public List<InventoryItem> getAllItems() {
        return new ArrayList<>(inventoryItems);
    }

    public void removeInventoryItem(org.example.Model.User requester, String itemId) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove inventory items.");
        }
        inventoryItems.removeIf(i -> i.getItemId().equals(itemId));
    }

    public void generateInventoryReport() {
        System.out.println("---------- INVENTORY REPORT ----------");
        for (InventoryItem item : inventoryItems) {
            String status = item.isStockLow() ? "[LOW STOCK]" : "[OK]";
            System.out.printf("ID: %s | Name: %-15s | Stock: %d | Threshold: %d %s%n",
                    item.getItemId(), item.getItemName(), item.getCurrentStock(), item.getLowStockThreshold(), status);
        }
        System.out.println("--------------------------------------");
    }
}

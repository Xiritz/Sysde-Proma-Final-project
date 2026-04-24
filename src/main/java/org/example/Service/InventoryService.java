package org.example.Service;

import org.example.Model.InventoryItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InventoryService {
    private List<InventoryItem> inventoryItems;

    public InventoryService() {
        this.inventoryItems = new ArrayList<>();
    }

    public void addItem(InventoryItem item) {
        inventoryItems.add(item);
    }

    public void consumeInventory(String itemId, int quantity) {
        Optional<InventoryItem> itemOpt = inventoryItems.stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst();

        if (itemOpt.isPresent()) {
            itemOpt.get().removeStock(quantity);
        } else {
            throw new IllegalArgumentException("Inventory item not found: " + itemId);
        }
    }

    public List<InventoryItem> getLowStockItems() {
        return inventoryItems.stream()
                .filter(InventoryItem::isStockLow)
                .collect(Collectors.toList());
    }

    public List<InventoryItem> getAllItems() {
        return new ArrayList<>(inventoryItems);
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

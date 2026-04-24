package org.example.Model;

public class InventoryItem {
    private String itemId;
    private String itemName;
    private int currentStock;
    private int lowStockThreshold;

    public InventoryItem(String itemId, String itemName, int currentStock, int lowStockThreshold) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentStock = currentStock;
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public int addStock(int amount){
        currentStock += amount;
        return currentStock;
    }

    public int removeStock(int amount){
        currentStock -= amount;
        return currentStock;
    }

    public boolean isStockLow(){
        if(currentStock < lowStockThreshold){
            return true;
        }
        return false;
    }
}

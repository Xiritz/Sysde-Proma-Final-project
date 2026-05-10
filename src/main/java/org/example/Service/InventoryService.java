package org.example.Service;

import org.example.Model.InventoryItem;
import org.example.Util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InventoryService {

    public InventoryService() {
        // Data is now in DB
    }

    public String generateNextId() {
        String query = "SELECT itemId FROM inventory ORDER BY itemId DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("itemId");
                int numericPart = Integer.parseInt(lastId.substring(1));
                return String.format("I%03d", numericPart + 1);
            }
        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        }
        return "I001"; // Fallback
    }

    public void addItem(InventoryItem item) {
        String checkQuery = "SELECT itemId, currentStock FROM inventory WHERE itemName = ?";
        String insertQuery = "INSERT INTO inventory (itemId, itemName, currentStock, lowStockThreshold) VALUES (?, ?, ?, ?)";
        String updateQuery = "UPDATE inventory SET currentStock = currentStock + ? WHERE itemId = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if item name already exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, item.getItemName());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String existingId = rs.getString("itemId");
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, item.getCurrentStock());
                            updateStmt.setString(2, existingId);
                            updateStmt.executeUpdate();
                            System.out.println("Item already exists. Added " + item.getCurrentStock() + " to " + item.getItemName());
                            return;
                        }
                    }
                }
            }

            // If doesn't exist, insert new
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, item.getItemId());
                insertStmt.setString(2, item.getItemName());
                insertStmt.setInt(3, item.getCurrentStock());
                insertStmt.setInt(4, item.getLowStockThreshold());
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void consumeInventory(String itemId, int quantity) {
        String updateQuery = "UPDATE inventory SET currentStock = currentStock - ? WHERE itemId = ? AND currentStock >= ?";
        String selectQuery = "SELECT * FROM inventory WHERE itemId = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setInt(1, quantity);
                updateStmt.setString(2, itemId);
                updateStmt.setInt(3, quantity);
                int affectedRows = updateStmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new IllegalArgumentException("Inventory item not found or insufficient stock: " + itemId);
                }
            }

            // Check for low stock warning
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setString(1, itemId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        int currentStock = rs.getInt("currentStock");
                        int threshold = rs.getInt("lowStockThreshold");
                        if (currentStock <= threshold) {
                            System.out.println("\n[WARNING] " + rs.getString("itemName") + " is running low! (Current stock: " + currentStock + ")");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addStockToItem(String itemId, int quantity) {
        String query = "UPDATE inventory SET currentStock = currentStock + ? WHERE itemId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, quantity);
            stmt.setString(2, itemId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Item not found: " + itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<InventoryItem> getLowStockItems() {
        return getAllItems().stream()
                .filter(InventoryItem::isStockLow)
                .collect(Collectors.toList());
    }

    public List<InventoryItem> getAllItems() {
        List<InventoryItem> items = new ArrayList<>();
        String query = "SELECT * FROM inventory";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(new InventoryItem(
                        rs.getString("itemId"),
                        rs.getString("itemName"),
                        rs.getInt("currentStock"),
                        rs.getInt("lowStockThreshold")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void removeInventoryItem(org.example.Model.User requester, String itemId) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can remove inventory items.");
        }

        String query = "DELETE FROM inventory WHERE itemId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateInventoryItem(org.example.Model.User requester, InventoryItem updatedItem) {
        if (requester.getRole() != org.example.Model.Role.ADMIN && requester.getRole() != org.example.Model.Role.OWNER) {
            throw new SecurityException("Only admins or owners can update inventory items.");
        }

        String query = "UPDATE inventory SET itemName = ?, currentStock = ?, lowStockThreshold = ? WHERE itemId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, updatedItem.getItemName());
            stmt.setInt(2, updatedItem.getCurrentStock());
            stmt.setInt(3, updatedItem.getLowStockThreshold());
            stmt.setString(4, updatedItem.getItemId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void generateInventoryReport() {
        System.out.println("---------- INVENTORY REPORT ----------");
        for (InventoryItem item : getAllItems()) {
            String status = item.isStockLow() ? "[LOW STOCK]" : "[OK]";
            System.out.printf("ID: %s | Name: %-15s | Stock: %d | Threshold: %d %s%n",
                    item.getItemId(), item.getItemName(), item.getCurrentStock(), item.getLowStockThreshold(), status);
        }
        System.out.println("--------------------------------------");
    }
}

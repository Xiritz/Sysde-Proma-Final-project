package org.example.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import org.example.App;
import org.example.Model.Role;
import org.example.Model.Status;
import org.example.Model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HomeController {

    @FXML private Label unpaidOrdersLabel;
    @FXML private Label activeOrdersLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label revenueLabel;
    @FXML private Label revenueTitle;

    @FXML private VBox cardsContainer;
    @FXML private VBox readyDeliveryContainer;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a");

    @FXML
    public void initialize() {
        refreshStats();
    }

    private void refreshStats() {
        List<Transaction> allTransactions = App.transactionService.getAllTransactions();
        
        // Unpaid Transactions (Balance > 0)
        long unpaidCount = allTransactions.stream()
                .filter(t -> t.getOutstandingBalance() > 0)
                .count();
        unpaidOrdersLabel.setText(String.valueOf(unpaidCount));

        // Active Transactions (Unpaid, Pending, Processing, Ready for Delivery)
        long activeCount = allTransactions.stream()
                .filter(t -> t.getLaundryStatus() == Status.PENDING 
                        || t.getLaundryStatus() == Status.PROCESSING 
                        || t.getLaundryStatus() == Status.READY_FOR_DELIVERY 
                        || t.getOutstandingBalance() > 0)
                .count();
        activeOrdersLabel.setText(String.valueOf(activeCount));

        // Low stock
        int lowStockCount = App.inventoryService.getLowStockItems().size();
        lowStockLabel.setText(String.valueOf(lowStockCount));

        // Daily Revenue (Allow ADMIN and OWNER)
        Role userRole = (App.loginService.getCurrentUser() != null) ? App.loginService.getCurrentUser().getRole() : null;
        if (userRole == Role.ADMIN || userRole == Role.OWNER) {
            double dailyRev = allTransactions.stream()
                    .filter(t -> t.getDatePlaced() != null && !t.getDatePlaced().before(getStartOfDay()))
                    .mapToDouble(Transaction::getAmountPaid)
                    .sum();
            revenueLabel.setText(String.format("Php %.2f", dailyRev));
        } else {
            revenueLabel.setText("REDACTED");
            revenueTitle.setText("Restricted Access");
        }

        // All Active Orders (Unpaid, Pending, Processing, Ready for Delivery)
        List<Transaction> activeOrders = allTransactions.stream()
                .filter(t -> t.getLaundryStatus() == Status.PENDING 
                        || t.getLaundryStatus() == Status.PROCESSING 
                        || t.getLaundryStatus() == Status.READY_FOR_DELIVERY 
                        || t.getOutstandingBalance() > 0)
                .sorted((t1, t2) -> {
                    if (t1.getDatePlaced() == null || t2.getDatePlaced() == null) return 0;
                    return t2.getDatePlaced().compareTo(t1.getDatePlaced());
                })
                .collect(Collectors.toList());
                
        renderCards(activeOrders);

        // Ready for Delivery
        List<Transaction> ready = allTransactions.stream()
                .filter(t -> t.getLaundryStatus() == Status.READY_FOR_DELIVERY)
                .sorted((t1, t2) -> {
                    if (t1.getDatePlaced() == null || t2.getDatePlaced() == null) return 0;
                    return t2.getDatePlaced().compareTo(t1.getDatePlaced());
                })
                .collect(Collectors.toList());
        renderReadyDeliveryItems(ready);
    }

    private void renderCards(List<Transaction> transactions) {
        cardsContainer.getChildren().clear();
        
        if (transactions.isEmpty()) {
            Label noData = new Label("No active orders found.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 20;");
            cardsContainer.getChildren().add(noData);
            return;
        }

        for (Transaction t : transactions) {
            cardsContainer.getChildren().add(createRecentTransactionCard(t));
        }
    }

    private VBox createRecentTransactionCard(Transaction t) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle("-fx-border-color: -qmar-border-color; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label idLabel = new Label(t.getTransactionId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -qmar-primary;");
        header.getChildren().add(idLabel);

        if (t.getOutstandingBalance() > 0) {
            Label unpaidBadge = new Label("UNPAID");
            unpaidBadge.setStyle("-fx-background-color: -qmar-danger; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 5; -fx-font-size: 9px; -fx-font-weight: bold;");
            header.getChildren().add(unpaidBadge);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusBadge = new Label(t.getLaundryStatus().toString());
        statusBadge.getStyleClass().add("badge");
        switch (t.getLaundryStatus()) {
            case PENDING: statusBadge.getStyleClass().add("status-pending"); break;
            case PROCESSING: statusBadge.getStyleClass().add("status-processing"); break;
            case READY_FOR_DELIVERY: 
            case DELIVERED:
                statusBadge.getStyleClass().add("status-completed"); 
                break;
            default: break;
        }
        
        header.getChildren().addAll(spacer, statusBadge);

        Label custLabel = new Label(t.getCustomer() != null ? t.getCustomer().getCustomerName() : "N/A");
        custLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -qmar-text-main;");
        
        Label serviceLabel = new Label(t.getService().toString().replace("_", " "));
        serviceLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 12px;");

        String dateStr = t.getDatePlaced() != null ? dateFormat.format(t.getDatePlaced()) : "No Date";
        Label dateLabel = new Label("Placed on: " + dateStr);
        dateLabel.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 11px; -fx-font-style: italic;");

        card.getChildren().addAll(header, custLabel, serviceLabel, dateLabel);
        return card;
    }

    private void renderReadyDeliveryItems(List<Transaction> ready) {
        readyDeliveryContainer.getChildren().clear();
        if (ready.isEmpty()) {
            Label noData = new Label("No orders ready for delivery.");
            noData.setStyle("-fx-text-fill: -qmar-text-muted; -fx-padding: 10; -fx-font-size: 12;");
            readyDeliveryContainer.getChildren().add(noData);
            return;
        }

        for (Transaction t : ready) {
            readyDeliveryContainer.getChildren().add(createReadyDeliveryItem(t));
        }
    }

    private VBox createReadyDeliveryItem(Transaction t) {
        VBox item = new VBox(5);
        item.setStyle("-fx-padding: 10; -fx-border-color: transparent transparent -qmar-border-color transparent; -fx-border-width: 0 0 1 0;");

        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label name = new Label(t.getCustomer() != null ? t.getCustomer().getCustomerName() : "Unknown");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        
        Label orderId = new Label("Order: " + t.getTransactionId());
        orderId.setStyle("-fx-text-fill: -qmar-text-muted; -fx-font-size: 11;");
        
        info.getChildren().addAll(name, orderId);

        Label statusPill = new Label("READY");
        statusPill.setStyle("-fx-background-color: -qmar-success; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;");
        
        hbox.getChildren().addAll(info, statusPill);
        item.getChildren().add(hbox);
        
        return item;
    }

    @FXML
    private void handleViewAllReady() {
    }

    private Date getStartOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}

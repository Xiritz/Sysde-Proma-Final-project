# Integration of Backend with Frontend Design - Tasks
## Qmar Laundry Management System

This document outlines the specific tasks required to connect the FXML layouts (Front-end) with the Java Services and Logic (Back-end) for the RACI matrix.

---

### **1. Controller-to-Service Binding**
*   **Inject Backend Services:** Connect singleton service instances (e.g., `TransactionService`, `InventoryService`) from `App.java` into Controller classes.
*   **Implement Data Persistence Hooks:** Map UI save/update buttons to backend service methods that modify the underlying data models.
*   **Configure Application Entry Point:** Finalize the `App.java` `start()` method to load the initial FXML and establish the primary stage.

### **2. Dynamic Data Injection**
*   **Populate Collection Views:** Bind backend `List` or `ObservableList` data to frontend components like `TableView`, `ComboBox`, or `ListView`.
*   **Implement Dynamic UI Composition:** Write Java logic to programmatically generate and inject UI components (like transaction cards or stock alerts) into FXML containers.
*   **Establish Two-Way Data Binding:** Set up listeners so that UI changes (e.g., typing in a search bar) instantly trigger backend filtering and refresh the view.

### **3. Business Logic Implementation**
*   **Integrate Real-Time Calculations:** Connect UI input fields (e.g., laundry weight) to backend price calculation engines for immediate total updates.
*   **Apply Business Rule Validations:** Implement "Guard Clauses" in controllers that check backend constraints (e.g., preventing a transaction if stock is insufficient) before allowing UI actions.
*   **Implement Role-Based Access Control (RBAC):** Map the `User` role from the `LoginService` to the visibility properties of specific FXML elements.

### **4. Event & State Management**
*   **Map FXML Actions to Java Methods:** Finalize all `onAction` links between buttons in FXML and their logic-heavy `@FXML` methods in Java.
*   **Implement Navigation & Context Switching:** Connect sidebar buttons to logic that swaps the `center` content of the main `BorderPane` while maintaining the application state.
*   **Configure Alert & Notification Handlers:** Link backend event triggers (like "Transaction Success" or "Low Stock") to frontend popups or toast notifications.

### **5. Integration Testing & Data Validation**
*   **End-to-End Flow Verification:** Test the complete path from a UI click to a data change in the service and back to a UI update.
*   **Error State Integration:** Connect backend exceptions (e.g., Database/Data format errors) to visual "error-field" styling and user-friendly error messages.
*   **Synchronize DataLoader Placeholders:** Ensure the `DataLoader` utility correctly populates the UI with mock data during development/testing phases.

# Work Breakdown Structure (WBS) - Front-end Development
## Qmar Laundry Management System

This document outlines the modular development phases for the front-end of the Qmar Laundry Management System, following a JavaFX architecture with a SaaS-style aesthetic.

---

### **1. Project Foundation & Core Architecture**
*   **1.1 Environment Setup**
    *   Configure JavaFX, OpenJFX, and Maven dependencies (`pom.xml`).
    *   Setup `App.java` as the entry point and scene manager.
*   **1.2 Global Aesthetic & Branding (`styles.css`)**
    *   **Theme Definition:** Variable declarations for colors (Primary Blue, Success Green, Danger Red), spacing, and shadows.
    *   **Base Component Styling:** Standardizing UI controls (TextFields, Buttons, TableViews, ScrollPanes).
    *   **Custom Class Implementation:** Defining `.card`, `.badge`, `.nav-item`, and `.status-*` classes.
*   **1.3 Navigation Framework**
    *   Implementation of the `BorderPane` shell in `dashboard.fxml`.
    *   Dynamic content switching logic in `DashboardController` using a `StackPane` content area.

### **2. Authentication & Security UI**
*   **2.1 Login Module (`login.fxml`)**
    *   **UI Layout:** Centered VBox with a branded card, logo, and credential inputs.
    *   **User Feedback:** Dynamic error labels and validation states for login failures.
*   **2.2 Session Logic (`LoginController.java`)**
    *   Integration with `LoginService` for credential verification.
    *   Post-login redirection to the Main Dashboard.

### **3. Main Application Shell (The "Frame")**
*   **3.1 Sidebar Navigation**
    *   Implementation of the left-hand navigation menu with SVG-like icons (emojis/fonts).
    *   "Active State" styling logic to highlight the current module.
*   **3.2 Top Header & Global Actions**
    *   **Global Search:** Styling the "Search everything..." bar.
    *   **User Profile Widget:** Displaying current user name, role (Admin/Staff), and avatar.
    *   **Notification System:** Implementation of the Bell icon with a dynamic badge for low-stock alerts.
    *   **Quick Actions:** "+ New Order" global button implementation.

### **4. Dashboard / Home Module (`home.fxml`)**
*   **4.1 Summary Statistics (Stats Cards)**
    *   Binding live data to cards: Unpaid Transactions, Active Orders, Low Stock Items, and Daily Revenue.
    *   **Role-based Access Control:** Hiding/Redacting revenue stats for non-admin users.
*   **4.2 Live Orders Feed**
    *   **Dynamic Card Generation:** Programmatic creation of `VBox` transaction cards with status badges.
    *   **Sorting & Filtering:** Real-time ordering by date placed and status.
*   **4.3 Delivery Sidebar**
    *   UI implementation of the "Ready for Delivery" list within the Home view.

### **5. Management Modules (CRUD & Tables)**
*   **5.1 Transaction Management (`transactions.fxml`)**
    *   Implementation of a searchable `TableView` for all laundry records.
    *   Status transition UI (e.g., Moving an order from *Pending* to *Processing*).
*   **5.2 Inventory & Stock (`inventory.fxml`)**
    *   Table layout for item catalogs (Detergents, Fabric Softeners, etc.).
    *   Visual "Low Stock" markers and reorder-point indicators.
*   **5.3 Customer & Staff CRM (`customers.fxml`, `staff.fxml`)**
    *   Management views for customer profiles and employee roles.
    *   Staff permission management UI (distinguishing Admin vs. Staff views).

### **6. Financial Reporting Module (`reports.fxml`)**
*   **6.1 Financial Summary View**
    *   Layout for revenue vs. expenses comparison.
    *   Data tables for tracking operational costs and daily earnings.

### **7. Final Polish & Optimization**
*   **7.1 Interaction Design**
    *   Implementation of hover effects, transitions, and click feedback across all buttons.
*   **7.2 Data Validation**
    *   Front-end input masking and "Error-field" styling for forms.
*   **7.3 Component Reusability**
    *   Refactoring common JavaFX UI patterns into reusable methods or components.

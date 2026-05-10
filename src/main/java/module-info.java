module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires java.sql;
    requires com.github.librepdf.openpdf;

    opens org.example to javafx.fxml;
    opens org.example.Controller to javafx.fxml;
    opens org.example.Model to javafx.base;
    opens org.example.Service to javafx.fxml;
    
    exports org.example;
    exports org.example.Model;
    exports org.example.Service;
    exports org.example.Controller;
}

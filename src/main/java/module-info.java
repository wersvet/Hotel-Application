module com.example.javajxproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires sqlite.jdbc;

    opens com.example.javajxproject to javafx.fxml;
    exports com.example.javajxproject;
}
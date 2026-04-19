module socratesGui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.sql;
    requires mysql.connector.j;

    opens socratesGui to javafx.fxml;
    exports socratesGui;
}

module socratesGui {
    // Módulos de Java que se necesitan
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.sql;
    requires mysql.connector.j;

    // Abre socratesGui a JavaFX para que pueda leer los FXML por reflexión
    opens socratesGui to javafx.fxml;

    // Exporta todos los paquetes del proyecto para que JPMS los reconozca
    exports socratesGui;
    exports entidades;
    exports database;
    exports dao;
    exports LogicaCita;
    

    // Abre 'database' para que el classloader pueda leer db.properties
    // dentro del módulo usando getModule().getResourceAsStream()
    opens database;
    opens entidades;
    opens dao;
    opens LogicaCita;
}
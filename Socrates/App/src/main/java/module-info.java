module socratesGui {

    // ── Dependencias directas ────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.net.http;
    requires jdk.httpserver;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires mysql.connector.j;

    // requires transitive: tipos de estos módulos aparecen en la API pública
    // de clases exportadas (Connection en Conexion.java, Stage en App.java)
    requires transitive java.sql;
    requires transitive javafx.graphics; // Stage, Scene, Parent

    // ── Exports: paquetes visibles para otros módulos ────────────────────────
    exports socratesGui;
    exports entidades;
    exports database;
    exports dao;
    exports LogicaCita;
    exports negocio;

    // ── Opens: reflexión para JavaFX FXML y DAO ──────────────────────────────
    opens socratesGui  to javafx.fxml;   // FXMLLoader necesita reflexión sobre los controladores
    opens entidades;
    opens database;                      // classloader puede leer db.properties dentro del módulo
    opens dao;
    opens LogicaCita;
    opens negocio;
}

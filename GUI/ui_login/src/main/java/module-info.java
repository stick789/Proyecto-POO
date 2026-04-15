module socrates {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.sql;
    requires mysql.connector.j;

    opens socrates to javafx.fxml;
    exports socrates;
}

module socrates {
    requires javafx.controls;
    requires javafx.fxml;

    opens socrates to javafx.fxml;
    exports socrates;
}

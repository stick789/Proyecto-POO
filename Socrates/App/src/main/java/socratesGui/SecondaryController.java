package socratesGui;

import java.io.IOException;

import javafx.fxml.FXML;

/**
 * SecondaryController — Controlador de la pantalla "Acceso con Compensar".
 *
 * Esta pantalla es informativa y permite al usuario volver al login principal.
 */
public class SecondaryController {

    @FXML
    private void switchToPrimary() {
        try {
            App.setRoot("primary");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

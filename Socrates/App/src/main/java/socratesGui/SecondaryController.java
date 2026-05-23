package socratesGui;

import java.io.IOException;

import javafx.application.HostServices;
import javafx.fxml.FXML;

/**
 * SecondaryController — Controlador de la pantalla "Acceso con Compensar".
 *
 * Permite al usuario:
 *   - Abrir el portal de autenticación de Compensar en el navegador del sistema.
 *   - Volver a la pantalla de login principal.
 */
public class SecondaryController {

    private static final String COMPENSAR_URL =
            "https://seguridad.compensar.com/sign-in?serviceProviderName=HER-SP&protocol=SAML";

    @FXML
    private void onContinuarCompensar() {
        HostServices hs = App.getAppHostServices();
        if (hs != null) {
            hs.showDocument(COMPENSAR_URL);
        }
    }

    @FXML
    private void switchToPrimary() {
        try {
            App.setRoot("primary");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

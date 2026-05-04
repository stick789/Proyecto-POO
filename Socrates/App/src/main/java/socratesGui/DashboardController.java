package socratesGui;

import java.io.IOException;

import entidades.Persona;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * DashboardController — Controlador de la pantalla principal post-login.
 *
 * Carga el nombre del usuario desde SesionActual y gestiona la
 * navegación del menú lateral.
 */
public class DashboardController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblContenido;

    @FXML
    public void initialize() {
        Persona usuario = SesionActual.getUsuario();
        if (usuario != null) {
            lblBienvenida.setText("Hola, " + usuario.getNombre()
                    + "\nRol: " + usuario.getRol());
        }
        lblContenido.setText("Selecciona una opción del menú");
    }

    @FXML private void onInicio()         { lblContenido.setText("📊 Panel de inicio"); }
    @FXML private void onTurnos()         { lblContenido.setText("📅 Gestión de Turnos"); }
    @FXML private void onUsuarios()       { lblContenido.setText("👤 Gestión de Usuarios"); }
    @FXML private void onInstalaciones()  { lblContenido.setText("🏊 Gestión de Instalaciones"); }
    @FXML private void onPagos()          { lblContenido.setText("💳 Gestión de Pagos"); }

    @FXML
    private void onLogout() {
        SesionActual.cerrar();
        try {
            App.setRoot("primary");
        } catch (IOException e) {
            lblContenido.setText("Error al cerrar sesión: " + e.getMessage());
        }
    }
}

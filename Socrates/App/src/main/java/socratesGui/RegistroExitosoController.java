package socratesGui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * RegistroExitosoController — Ventana de confirmación post-registro.
 *
 * Muestra el nombre (persona) y el correo (usuario) del recién registrado.
 * Al pulsar "Ir al inicio de sesión", cierra esta ventana; el listener
 * setOnHidden de RegistroController se encarga de cerrar también el formulario,
 * dejando sólo el login visible.
 */
public class RegistroExitosoController {

    @FXML private Label lblPersona;
    @FXML private Label lblUsuario;

    /**
     * Inyecta los datos del usuario recién registrado.
     * Debe llamarse justo después de cargar el FXML.
     *
     * @param persona nombre completo (campo persona)
     * @param usuario correo electrónico (campo usuario / login)
     */
    public void setDatos(String persona, String usuario) {
        lblPersona.setText(persona != null ? persona : "—");
        lblUsuario.setText(usuario != null ? usuario : "—");
    }

    private Stage registroStage;

public void setRegistroStage(Stage registroStage) {
    this.registroStage = registroStage;
}

@FXML
private void onAceptar() {
    // Cerrar ventana de éxito
    Stage stageExito = (Stage) lblPersona.getScene().getWindow();
    stageExito.close();
    // Cerrar ventana de registro
    if (registroStage != null) {
        registroStage.close();
    }
}}

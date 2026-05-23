package socratesGui;

import java.io.IOException;
import java.util.Optional;

import dao.PersonaDAO;
import entidades.Administrador;
import entidades.Persona;
import entidades.Usuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * LoginController — Controlador del formulario de login.
 *
 * USUARIOS DEMO (sin BD):
 *   admin@demo.com   / admin123   → DashboardAdmin
 *   usuario@demo.com / user123    → DashboardUsuario
 *
 * Si el email NO es demo, se consulta la BD real.
 */
public class LoginController {

    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtContrasena;
    @FXML private Label         lblError;

    private final PersonaDAO personaDAO = new PersonaDAO();

    // ── Credenciales demo ─────────────────────────────────────────────────────
    private static final String DEMO_ADMIN_EMAIL   = "admin@demo.com";
    private static final String DEMO_ADMIN_PASS    = "admin123";
    private static final String DEMO_USUARIO_EMAIL = "usuario@demo.com";
    private static final String DEMO_USUARIO_PASS  = "user123";

    @FXML
private void onLogin() {
    lblError.setText("");

    String email = txtEmail.getText().trim();
    String clave  = txtContrasena.getText();

    if (email.isEmpty() || clave.isEmpty()) {
        lblError.setText("Ingresa tu correo y contraseña.");
        return;
    }

    // ── 1. Intentar login demo (sin BD) ──────────────────────
    Persona personaDemo = intentarLoginDemo(email, clave);
    if (personaDemo != null) {
        SesionActual.setUsuario(personaDemo);
        navegarSegunRol(personaDemo);
        return;
    }

    // ── 2. Login real contra la BD ────────────────────────────
    try {
        Optional<Persona> resultado = personaDAO.buscarPorEmail(email);

        if (resultado.isEmpty()) {
            lblError.setText("Correo no registrado.");
            return;
        }

        Persona persona = resultado.get();

        if (!verificarClave(persona, clave)) {
            lblError.setText("Contraseña incorrecta.");
            txtContrasena.clear();
            return;
        }

        SesionActual.setUsuario(persona);
        navegarSegunRol(persona);

    } catch (RuntimeException e) {
    lblError.setText(e.getCause() != null 
        ? e.getCause().getMessage() 
        : e.getMessage());
}
}

    /**
     * Devuelve un Persona demo si las credenciales coinciden, o null si no es demo.
     * El id = 0 activa el modo demo en los dashboards (sin consultas a BD).
     */
    private Persona intentarLoginDemo(String email, String clave) {
        if (DEMO_ADMIN_EMAIL.equals(email) && DEMO_ADMIN_PASS.equals(clave)) {
            return new Administrador(0, "Admin Demo", email, "admin123", "CC", "00000000");
        }
        if (DEMO_USUARIO_EMAIL.equals(email) && DEMO_USUARIO_PASS.equals(clave)) {
            return new Usuario(0, "Usuario Demo", email, "CC", "00000000", true, "A");
        }
        return null;
    }

    /** Redirige al dashboard correcto según el tipo de Persona. */
    private void navegarSegunRol(Persona persona) {
        try {
            if (persona instanceof Administrador) {
                App.setRoot("dashboardAdmin");
            } else {
                App.setRoot("dashboardUsuario");
            }
        } catch (IOException e) {
            lblError.setText("Error de navegación: " + e.getMessage());
        }
    }

    /**
     * Compara la clave ingresada con la almacenada en el objeto Persona.
     * Comparación en texto plano — reemplazar por hash cuando se implemente PBKDF2.
     */
    private boolean verificarClave(Persona persona, String claveIngresada) {
        if (persona instanceof Administrador) {
            String hash = ((Administrador) persona).getContraseñaAdmin();
            return claveIngresada.equals(hash);
        }
        if (persona instanceof entidades.Usuario) {
            String hash = ((entidades.Usuario) persona).getContraseña();
            return claveIngresada.equals(hash);
        }
        return false;
    }

    /**
     * Abre el formulario de registro como una ventana independiente (Stage).
     * Al cerrarla (éxito o cancelación) el login permanece visible.
     */
    @FXML
    private void onRegistrar() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    App.class.getResource("/Interface/registro.fxml"));
            Parent root = loader.load();

            RegistroController ctrl = loader.getController();

            Stage stageRegistro = new Stage();
            stageRegistro.setTitle("Crear cuenta — Sócrates");
            stageRegistro.setScene(new Scene(root, 460, 680));
            stageRegistro.setResizable(false);
            Stage loginStage = (Stage) txtEmail.getScene().getWindow();
            stageRegistro.initOwner(loginStage);
            stageRegistro.initModality(Modality.WINDOW_MODAL);

            ctrl.setStage(stageRegistro);

            stageRegistro.show();

        } catch (IOException e) {
            lblError.setText("No se pudo abrir el formulario de registro: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelar() {
        Platform.exit();
    }

    @FXML
    private void onLoginCompensar() {
        try {
            App.setRoot("secondary");
        } catch (IOException e) {
            lblError.setText("No se pudo abrir el acceso Compensar: " + e.getMessage());
        }
    }
}
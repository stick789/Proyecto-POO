package socratesGui;

import java.io.IOException;
import java.util.UUID;

import dao.PersonaDAO;
import entidades.Administrador;
import entidades.Persona;
import entidades.Usuario;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import negocio.PersonaControl;
import service.GoogleOAuthService;
import service.GoogleOAuthService.GoogleProfile;

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
    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();

    // ── Credenciales demo ─────────────────────────────────────────────────────
    private static final String DEMO_ADMIN_EMAIL   = "admin@demo.com";
    private static final String DEMO_ADMIN_PASS    = "admin123";
    private static final String DEMO_USUARIO_EMAIL = "usuario@demo.com";
    private static final String DEMO_USUARIO_PASS  = "user123";

    @FXML
    private void onLogin() {
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String clave = txtContrasena.getText() != null ? txtContrasena.getText() : "";

        if (email.isBlank() || clave.isBlank()) {
            lblError.setText("Ingrese correo y contraseña.");
            return;
        }

        Persona demo = intentarLoginDemo(email, clave);
        if (demo != null) {
            SesionActual.setUsuario(demo);
            navegarSegunRol(demo);
            return;
        }

        lblError.setText("Validando credenciales...");

        Task<Persona> taskLogin = new Task<>() {
            @Override
            protected Persona call() {
                PersonaControl control = new PersonaControl(personaDAO);
                if (!"1".equals(control.login(email, clave))) {
                    return null;
                }
                return personaDAO.buscarPorEmail(email).orElse(null);
            }
        };

        taskLogin.setOnSucceeded(e -> {
            Persona persona = taskLogin.getValue();
            if (persona == null) {
                lblError.setText("Correo o contraseña incorrectos.");
                return;
            }

            SesionActual.setUsuario(persona);
            navegarSegunRol(persona);
        });

        taskLogin.setOnFailed(e -> {
            String msg = taskLogin.getException() != null ? taskLogin.getException().getMessage() : null;
            lblError.setText(msg != null ? msg : "Error de conexión.");
        });

        Thread hiloLogin = new Thread(taskLogin);
        hiloLogin.setDaemon(true);
        hiloLogin.start();
    }

    private void iniciarSesionGoogle() {
    lblError.setText("Abriendo Google...");

    Task<Persona> taskLogin = new Task<>() {
        @Override
        protected Persona call() throws Exception {
            GoogleProfile profile = googleOAuthService.autenticar(App.getAppHostServices());
            Persona personaGoogle = resolverOCrearPersonaGoogle(profile);
            if (personaGoogle == null) {
                throw new IllegalStateException("No se pudo resolver la cuenta Google.");
            }
            return personaGoogle;
        }
    };

    taskLogin.setOnSucceeded(e -> {
        Persona persona = taskLogin.getValue();
        SesionActual.setUsuario(persona);
        navegarSegunRol(persona);
    });

    taskLogin.setOnFailed(ev -> {
        String msg = taskLogin.getException().getMessage();
        lblError.setText(msg != null ? msg : "Error de conexión.");
    });

    Thread hiloLogin = new Thread(taskLogin);
    hiloLogin.setDaemon(true);
    hiloLogin.start();
}

    @FXML
    private void onLoginGoogle() {
        iniciarSesionGoogle();
    }

    private Persona resolverOCrearPersonaGoogle(GoogleProfile profile) throws Exception {
        if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new IllegalArgumentException("Google no devolvió un correo válido.");
        }

        Persona existente = personaDAO.buscarPorEmail(profile.getEmail()).orElse(null);
        if (existente != null) {
            return existente;
        }

        String nombre = (profile.getName() != null && !profile.getName().isBlank())
                ? profile.getName()
                : profile.getEmail();

        Usuario nuevoUsuario = new Usuario(
                0,
                nombre,
                profile.getEmail(),
                "GOOGLE",
                profile.getGoogleId(),
                false,
                null
        );

        String resultado = new PersonaControl(personaDAO).registrar(nuevoUsuario, UUID.randomUUID().toString());
        if ("OK".equals(resultado)) {
            Persona creado = personaDAO.buscarPorEmail(profile.getEmail()).orElse(null);
            if (creado != null) {
                return creado;
            }
        }

        if ("EMAIL_DUPLICADO".equals(resultado)) {
            Persona duplicado = personaDAO.buscarPorEmail(profile.getEmail()).orElse(null);
            if (duplicado != null) {
                return duplicado;
            }
        }

        throw new IllegalStateException("No se pudo crear o cargar la cuenta de Google.");
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
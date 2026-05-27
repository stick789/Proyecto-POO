package socratesGui;

import entidades.Usuario;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import negocio.PersonaControl;

/**
 * RegistroController — Controlador del formulario de registro de nuevos usuarios.
 *
 * Flujo:
 *   1. El usuario completa el formulario y pulsa "Crear cuenta".
 *   2. Se validan los campos localmente.
 *   3. Se delega la persistencia a PersonaControl.registrar().
 *   4. Si el resultado es "OK", se abre la ventana de éxito (RegistroExitosoController).
 *   5. Al cerrar la ventana de éxito, esta ventana también se cierra,
 *      dejando visible únicamente la pantalla de login.
 */
public class RegistroController {

    @FXML private TextField     txtNombre;
    @FXML private TextField     txtEmail;
    @FXML private ComboBox<String> cmbTipoDocumento;
    @FXML private TextField     txtNumDocumento;
    @FXML private PasswordField txtContrasena;
    @FXML private PasswordField txtConfirmar;
    @FXML private Label         lblError;

    private final PersonaControl control = new PersonaControl();

    // Stage de esta ventana — se asigna desde LoginController al abrir
    private Stage miStage;

    /** Llamado por LoginController para inyectar la referencia al Stage. */
    public void setStage(Stage stage) {
        this.miStage = stage;
    }

    @FXML
    public void initialize() {
        cmbTipoDocumento.setItems(FXCollections.observableArrayList(
                "CC", "TI", "CE", "PP", "NIT"
        ));
    }

    // ── Acción "Crear cuenta" ─────────────────────────────────────────────────

    @FXML
    private void onRegistrar() {
        lblError.setText("");

        // ── Validaciones básicas ──────────────────────────────────────────────
        String nombre      = txtNombre.getText().trim();
        String email       = txtEmail.getText().trim();
        String tipoDoc     = cmbTipoDocumento.getValue();
        String numDoc      = txtNumDocumento.getText().trim();
        String contrasena  = txtContrasena.getText();
        String confirmar   = txtConfirmar.getText();

        if (nombre.isEmpty() || email.isEmpty() || numDoc.isEmpty()
                || contrasena.isEmpty() || confirmar.isEmpty()) {
            lblError.setText("Todos los campos son obligatorios.");
            return;
        }

        if (tipoDoc == null) {
            lblError.setText("Selecciona un tipo de documento.");
            return;
        }

        if (!contrasena.equals(confirmar)) {
            lblError.setText("Las contraseñas no coinciden.");
            txtConfirmar.clear();
            return;
        }

        if (contrasena.length() < 6) {
            lblError.setText("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        if (!email.contains("@")) {
            lblError.setText("Ingresa un correo electrónico válido.");
            return;
        }

        // ── Crear objeto Usuario sin contraseña hasheada (PersonaControl la hashea) ──
        Usuario nuevoUsuario = new Usuario(
                0,           // id generado por BD
                nombre,
                email,
                tipoDoc,
                numDoc,
                false,       // esAfiliado — puede cambiarse después por admin
                null         // categoría
        );

        // ── Delegar registro a la capa de negocio (en hilo background) ───────
        // generarHashPBKDF2 tiene 65.536 iteraciones — bloquea la UI si corre
        // en el Application Thread. Lo movemos a un Task para evitar el freeze.
        lblError.setText("Creando cuenta...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return control.registrar(nuevoUsuario, contrasena);
            }
        };

        task.setOnSucceeded(e -> {
            String resultado = task.getValue();
            switch (resultado) {
                case "OK":
                    abrirVentanaExito(nombre, email);
                    break;
                case "EMAIL_DUPLICADO":
                    lblError.setText("Ya existe una cuenta con ese correo electrónico.");
                    break;
                default:
                    lblError.setText("Ocurrió un error al crear la cuenta. Intenta de nuevo.");
            }
        });

        task.setOnFailed(e ->
            Platform.runLater(() ->
                lblError.setText("Error inesperado: " + task.getException().getMessage())
            )
        );

        Thread hilo = new Thread(task);
        hilo.setDaemon(true); // se cierra solo si la app se cierra
        hilo.start();
    }

    // ── Acción "Volver" ───────────────────────────────────────────────────────

    @FXML
    private void onCancelar() {
        cerrarVentana();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Abre la ventana de registro exitoso.
     * Al cerrarla, cierra también esta ventana (volviendo al login).
     */
    private void abrirVentanaExito(String persona, String usuario) {
    try {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/Interface/registroExitoso.fxml"));
        Parent root = loader.load();

        RegistroExitosoController ctrl = loader.getController();
        ctrl.setDatos(persona, usuario);
        ctrl.setRegistroStage(miStage); // ← pasa referencia al stage de registro

        Stage stageExito = new Stage();
        stageExito.setTitle("Registro exitoso — Sócrates");
        stageExito.setScene(new Scene(root, 420, 420));
        stageExito.setResizable(false);
        stageExito.initModality(Modality.WINDOW_MODAL);
        if (miStage != null) {
            stageExito.initOwner(miStage);
        }

        // SIN setOnHidden — el cierre lo maneja RegistroExitosoController
        stageExito.centerOnScreen();
        stageExito.show();

    } catch (Exception e) {
        lblError.setText("Error al abrir la ventana de confirmación: " + e.getMessage());
    }
}

    private void cerrarVentana() {
        if (miStage != null) {
            miStage.close();
        }
    }
}
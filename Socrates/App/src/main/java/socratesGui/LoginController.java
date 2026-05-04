package socratesGui;

import java.io.IOException;
import java.util.Optional;

import dao.PersonaDAO;
import entidades.Persona;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginController — Controlador del formulario de login.
 *
 * Responsabilidades:
 *  1. Recibir email + contraseña del formulario.
 *  2. Verificar credenciales contra la BD vía PersonaDAO.
 *  3. Si OK → guardar sesión y navegar al dashboard.
 *  4. Si falla → mostrar mensaje de error inline (sin Alert emergente).
 *
 * La conexión a la BD ocurre aquí, NO en App.start(), para que la
 * pantalla de login sea siempre visible incluso si la BD está caída.
 */
public class LoginController {

    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtContrasena;
    @FXML private Label         lblError;

    private final PersonaDAO personaDAO = new PersonaDAO();

    @FXML
    private void onLogin() {
        lblError.setText("");

        String email = txtEmail.getText().trim();
        String clave  = txtContrasena.getText();

        // Validación básica en cliente
        if (email.isEmpty() || clave.isEmpty()) {
            lblError.setText("Ingresa tu correo y contraseña.");
            return;
        }

        try {
            // PersonaDAO.buscarPorEmail retorna Usuario o Administrador (polimorfismo)
            Optional<Persona> resultado = personaDAO.buscarPorEmail(email);

            if (resultado.isEmpty()) {
                lblError.setText("Correo no registrado.");
                return;
            }

            Persona persona = resultado.get();

            // Verificación de contraseña según subtipo
            boolean claveCorrecta = verificarClave(persona, clave);

            if (!claveCorrecta) {
                lblError.setText("Contraseña incorrecta.");
                txtContrasena.clear();
                return;
            }

            // Login exitoso: guardar sesión y navegar
            SesionActual.setUsuario(persona);
            App.setRoot("dashboard");

        } catch (IOException e) {
            lblError.setText("Error de navegación: " + e.getMessage());
        } catch (RuntimeException e) {
            // Captura errores de BD (RuntimeException lanzado por PersonaDAO)
            lblError.setText("No se pudo conectar a la base de datos.\nVerifica tu conexión.");
        }
    }

    /**
     * Compara la clave ingresada con la almacenada en el objeto Persona.
     *
     * Por ahora compara en texto plano para compatibilidad con la BD actual.
     * Cuando se implemente PBKDF2 (ver PersonaControl_login_snippet.java),
     * este método se reemplaza por la verificación de hash.
     */
    private boolean verificarClave(Persona persona, String claveIngresada) {
        // Obtener la contraseña almacenada según el subtipo
        if (persona instanceof entidades.Administrador) {
            String hashAdmin = ((entidades.Administrador) persona).getContraseñaAdmin();
            return claveIngresada.equals(hashAdmin);
        }
        if (persona instanceof entidades.Usuario) {
            String hash = ((entidades.Usuario) persona).getContraseña();
            return claveIngresada.equals(hash);
        }
        return false;
    }

    /** Permite salir de la aplicación con Escape (opcional, para UX). */
    @FXML
    private void onCancelar() {
        Platform.exit();
    }

    /** Navega a la pantalla de acceso institucional con Compensar. */
    @FXML
    private void onLoginCompensar() {
        try {
            App.setRoot("secondary");
        } catch (IOException e) {
            lblError.setText("No se pudo abrir el acceso Compensar: " + e.getMessage());
        }
    }
}

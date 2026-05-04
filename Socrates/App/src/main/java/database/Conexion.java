package database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

/**
 * Conexion — Gestiona la conexión a la base de datos con patrón Singleton.
 *
 * FLUJO DE ARRANQUE (conectarConFeedback):
 *   - Éxito → Alert INFO "Establecimiento de Conexión: Satisfactorio" → continúa.
 *   - Fallo  → Alert ERROR con detalle técnico + botón Cerrar → lanza RuntimeException
 *              para que App.java llame Platform.exit() y cierre la aplicación.
 *
 * FLUJO EN DAOs (conectar):
 *   - Reutiliza la conexión existente si está activa.
 *   - Si no, la reabre silenciosamente (la pantalla ya está cargada).
 */
public class Conexion {

    private static final Logger LOG = Logger.getLogger(Conexion.class.getName());

    private final String driver;
    private final String url;
    private final String db;
    private final String user;
    private final String password;

    private Connection cadena;
    private static Conexion instancia;

    private Conexion() {
        Properties props = new Properties();
        try (InputStream in = cargarConfigBd()) {
            if (in != null) {
                props.load(in);
            } else {
                LOG.warning("No se encontro properties/db.properties, usando valores por defecto.");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error al leer properties/db.properties", e);
        }

        this.driver   = props.getProperty("db.driver",   "com.mysql.cj.jdbc.Driver");
        this.url      = props.getProperty("db.url",      "jdbc:mysql://localhost:3306/");
        this.db       = props.getProperty("db.name",     "proyecto_poo");
        this.user     = props.getProperty("db.user",     "root");
        this.password = props.getProperty("db.password", "");
        this.cadena   = null;
    }

    // ── Carga db.properties por tres rutas distintas ──────────────────────────
    private InputStream cargarConfigBd() {
        InputStream in = null;
        try {
            in = Conexion.class.getModule().getResourceAsStream("properties/db.properties");
        } catch (IOException e) {
            LOG.log(Level.FINE, "Intento 1 fallo (Module API)", e);
        }
        if (in == null) in = Conexion.class.getResourceAsStream("/properties/db.properties");
        if (in == null) in = getClass().getClassLoader().getResourceAsStream("properties/db.properties");
        if (in == null) LOG.warning("db.properties no encontrado por ninguna ruta.");
        return in;
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Llamado por App.java al arrancar.
     *
     * • Éxito → muestra Alert "Establecimiento de Conexión: Satisfactorio".
     * • Fallo → muestra Alert de error con detalle → lanza RuntimeException
     *           para que App.java cierre la aplicación.
     */
    public void conectarConFeedback() {
        try {
            if (cadena == null || cadena.isClosed()) {
                Class.forName(driver);
                cadena = DriverManager.getConnection(url + db, user, password);
            }

            // ── Conexión exitosa ──
            Alert ok = new Alert(AlertType.INFORMATION);
            ok.setTitle("Conexión establecida");
            ok.setHeaderText("Establecimiento de Conexión: Satisfactorio");
            ok.setContentText("Conectado a: " + url + db + "\nUsuario: " + user);
            ok.showAndWait();

        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Driver JDBC no encontrado", e);
            mostrarErrorYCerrar(
                "Driver JDBC no encontrado: " + driver +
                "\nVerifica que mysql-connector-j esté en el pom.xml.",
                e.getMessage()
            );
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error SQL al conectar", e);
            mostrarErrorYCerrar(
                "No se pudo conectar a: " + url + db +
                "\nUsuario: " + user +
                "\nVerifica que MySQL esté corriendo y que db.properties sea correcto.",
                e.getMessage()
            );
        }
    }

    // ── Muestra el Alert de error con header + detalle técnico y lanza excepción ──
    private void mostrarErrorYCerrar(String descripcion, String detalleTecnico) {
        Alert error = new Alert(AlertType.ERROR, "", ButtonType.CLOSE);
        error.setTitle("Error de conexión");
        error.setHeaderText("No se pudo conectar a la base de datos");
        error.setContentText(descripcion + "\n\nDetalle: " + detalleTecnico);
        error.showAndWait();
        throw new RuntimeException("Fallo de conexión a BD — app cerrada.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Usado por los DAOs durante la sesión.
     * Reutiliza la conexión activa o la reabre si fue cerrada.
     * No muestra alertas (la pantalla ya está en uso).
     */
    public Connection conectar() {
        try {
            if (cadena == null || cadena.isClosed()) {
                Class.forName(driver);
                cadena = DriverManager.getConnection(url + db, user, password);
            }
        } catch (ClassNotFoundException | SQLException e) {
            LOG.log(Level.SEVERE, "Error al reconectar en DAO", e);
        }
        return cadena;
    }

    public void desconectar() {
        try {
            if (cadena != null && !cadena.isClosed()) {
                cadena.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error al desconectar", e);
        } finally {
            cadena = null;
        }
    }

    public synchronized static Conexion getInstancia() {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }
}

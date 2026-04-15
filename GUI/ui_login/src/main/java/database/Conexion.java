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

/**
 * Clase Conexion - Patrón Singleton para gestionar la conexión a la base de datos.
 *
 * PATRÓN DE DISEÑO: Singleton
 *   - Solo existe UNA instancia de esta clase en toda la aplicación.
 *   - Se garantiza mediante el método estático getInstancia().
 *   - Esto evita abrir múltiples conexiones innecesarias a la BD.
 *
 * CONCEPTOS POO APLICADOS:
 *   - Encapsulamiento: todos los atributos son privados (private).
 *   - Singleton: constructor privado + método estático de acceso.
 *   - Uso de Properties para externalizar la configuración de la BD.
 */
public class Conexion {

    /** Logger para registrar errores sin mostrarlos directamente al usuario. */
    private static final Logger LOG = Logger.getLogger(Conexion.class.getName());

    // ── Atributos de configuración (se cargan desde db.properties) ──────────
    private final String driver;    // Clase del driver JDBC (ej: com.mysql.cj.jdbc.Driver)
    private final String url;       // URL base de conexión (ej: jdbc:mysql://localhost:3306/)
    private final String db;        // Nombre de la base de datos
    private final String user;      // Usuario de la BD
    private final String password;  // Contraseña de la BD

    /** Objeto Connection activo. null si no hay conexión abierta. */
    private Connection cadena;

    /** Controla que el mensaje de conexion exitosa se muestre una sola vez. */
    private boolean mensajeExitoMostrado;

    /** Única instancia de la clase (Singleton). */
    private static Conexion instancia;

    /**
     * Constructor PRIVADO — impide instanciación directa desde otras clases.
     * Lee la configuración desde el archivo db.properties en el classpath.
     * Si no existe el archivo, usa valores por defecto.
     */
    private Conexion() {
        Properties props = new Properties();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("properties/db.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOG.warning("No se encontro properties/db.properties, usando valores por defecto.");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error al leer properties/db.properties", e);
        }

        this.driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        this.url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/");
        this.db = props.getProperty("db.name", "dbsistema");
        this.user = props.getProperty("db.user", "root");
        this.password = props.getProperty("db.password", "");
        this.cadena = null;
        this.mensajeExitoMostrado = false;
    }

    /**
     * Abre la conexión a la base de datos usando JDBC.
     *
     * @return Connection activo, o null si ocurrió un error.
     */
    public Connection conectar() {
        try {
            Class.forName(driver);
            this.cadena = DriverManager.getConnection(url + db, user, password);

            if (!mensajeExitoMostrado) {
                System.out.println("Conexion exitosa: la conexion a la base de datos fue exitosa.");
                mensajeExitoMostrado = true;
            }
        } catch (ClassNotFoundException | SQLException e) {
            LOG.log(Level.SEVERE, "Error al conectar a la base de datos", e);

            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error de conexión");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo conectar a la base de datos. Verifique la configuración.");
            alert.showAndWait();
        }
        return this.cadena;
    }

    /**
     * Cierra la conexión activa si existe y está abierta.
     * Es buena práctica llamar a este método en el bloque finally de cada DAO.
     */
    public void desconectar() {
        try {
            if (this.cadena != null && !this.cadena.isClosed()) {
                this.cadena.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error al desconectar", e);
        }
    }

    /**
     * Método estático sincronizado que devuelve la única instancia de Conexion.
     * PATRÓN SINGLETON: si aún no existe instancia, la crea; si ya existe, la devuelve.
     *
     * @return instancia única de Conexion.
     */
    public synchronized static Conexion getInstancia() {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }
}
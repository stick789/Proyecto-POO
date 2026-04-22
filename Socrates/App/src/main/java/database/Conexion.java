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
 * Conexion — Gestiona la conexión a la base de datos con patrón Singleton.
 *
 * CÓMO FUNCIONA:
 *   • conectar(): Abre una conexión a MySQL o reutiliza la existente si ya está abierta.
 *   • desconectar(): Cierra la conexión y marca que debe reabrirse en la próxima llamada.
 *   • getInstancia(): Garantiza que solo existe una instancia de esta clase en toda la aplicación.
 *
 * PROBLEMA QUE SE SOLUCIONÓ:
 *   Antes, cada vez que los DAOs llamaban a conectar() y luego desconectar(), se abría
 *   una nueva conexión pero la anterior quedaba "colgada" en memoria. Esto causaba que
 *   MySQL rechazara las conexiones con "Too many connections" después de varios accesos.
 *
 * LA SOLUCIÓN:
 *   • conectar() ahora verifica si ya hay una conexión activa. Si existe y está abierta,
 *     la reutiliza. Si no, abre una nueva.
 *   • desconectar() cierra la conexión y limpia la referencia (asigna null).
 *   • Los mensajes de error ahora muestran más detalles para diagnosticar problemas rápidamente.
 *
 * RESULTADO:
 *   Una sola conexión se mantiene durante toda la sesión del usuario,
 *   evitando fugas de memoria y errores de conexión.
 */
public class Conexion {

    private static final Logger LOG = Logger.getLogger(Conexion.class.getName());

    private final String driver;
    private final String url;
    private final String db;
    private final String user;
    private final String password;

    private Connection cadena;
    private boolean mensajeExitoMostrado;
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
        this.mensajeExitoMostrado = false;
    }

    private InputStream cargarConfigBd() {
        InputStream in = null;

        // Intento 1: JPMS Module API
        try {
            in = Conexion.class.getModule().getResourceAsStream("properties/db.properties");
        } catch (IOException e) {
            LOG.log(Level.FINE, "Intento 1 fallo (Module API)", e);
        }

        // Intento 2: classpath absoluto
        if (in == null) {
            in = Conexion.class.getResourceAsStream("/properties/db.properties");
        }

        // Intento 3: classloader relativo
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream("properties/db.properties");
        }

        if (in == null) {
            LOG.warning("db.properties no encontrado por ninguna ruta. Usando valores por defecto.");
        }

        return in;
    }

    /**
     * Obtiene la conexión a la base de datos.
     *
     * Si ya hay una conexión activa, la devuelve tal cual.
     * Si no existe o está cerrada, abre una nueva.
     * Esto evita crear múltiples conexiones innecesarias.
     */
    public Connection conectar() {
        try {
            // Verifica si la conexión ya existe y está activa
            if (cadena == null || cadena.isClosed()) {
                Class.forName(driver);
                cadena = DriverManager.getConnection(url + db, user, password);

                if (!mensajeExitoMostrado) {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Conexion exitosa");
                    alert.setHeaderText(null);
                    alert.setContentText("Conectado correctamente a: " + db);
                    alert.showAndWait();
                    mensajeExitoMostrado = true;
                }
            }
        } catch (ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Driver JDBC no encontrado", e);
            mostrarErrorConexion("Driver no encontrado: " + driver +
                                 "\nVerifica que mysql-connector-j este en el pom.xml");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error SQL al conectar", e);
            // Muestra un mensaje con información detallada del error para facilitar la solución
            mostrarErrorConexion(
                "No se pudo conectar a: " + url + db +
                "\nUsuario: " + user +
                "\nVerifica que MySQL este corriendo y que db.properties sea correcto." +
                "\n\nDetalle: " + e.getMessage()
            );
        }
        return cadena;
    }

    /**
     * Cierra la conexión a la base de datos.
     *
     * Después de cerrar, asigna null a la referencia de conexión para que
     * conectar() sepa que debe abrir una nueva en la próxima llamada.
     * Esto previene errores al intentar usar una conexión ya cerrada.
     */
    public void desconectar() {
        try {
            if (cadena != null && !cadena.isClosed()) {
                cadena.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Error al desconectar", e);
        } finally {
            cadena = null; // Indica que la conexión fue cerrada, para reabrir si es necesario
        }
    }

    private void mostrarErrorConexion(String detalle) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error de conexión");
        alert.setHeaderText("No se pudo conectar a la base de datos");
        alert.setContentText(detalle);
        alert.showAndWait();
    }

    public synchronized static Conexion getInstancia() {
        if (instancia == null) {
            instancia = new Conexion();
        }
        return instancia;
    }
}
package socratesGui;

import java.io.IOException;

import database.Conexion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * App — Punto de entrada de la aplicación JavaFX.
 *
 * FLUJO DE ARRANQUE:
 *   1. Se intenta conectar a la BD antes de mostrar cualquier pantalla.
 *   2. Si la conexión falla  → se muestra el error con detalle → la app se cierra.
 *   3. Si la conexión es exitosa → se muestra "Establecimiento de Conexión: Satisfactorio"
 *      → se carga el login (primary.fxml).
 *
 * PANTALLAS DISPONIBLES:
 *   primary          → login (640 × 730, no redimensionable)
 *   secondary        → acceso Compensar (640 × 730, no redimensionable)
 *   dashboardAdmin   → panel administrador (1280 × 800, redimensionable)
 *   dashboardUsuario → panel usuario       (1280 × 800, redimensionable)
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;
    private static App instance;

    public static javafx.application.HostServices getAppHostServices() {
        return instance.getHostServices();
    }

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        primaryStage = stage;
        stage.setTitle("Sócrates — Sistema de Gestión Deportiva");
        stage.setResizable(false);

        // ── 1. Verificar conexión a la base de datos ──────────────────────────
        // conectarConFeedback() lanza RuntimeException si falla,
        // y muestra Alert de éxito si conecta bien.
        try {
            Conexion.getInstancia().conectarConFeedback();
        } catch (RuntimeException e) {
            // La conexión falló: Conexion ya mostró el Alert de error con detalle.
            // Cerramos la aplicación limpiamente.
            Platform.exit();
            return;
        }

        // ── 2. Conexión exitosa → cargar login ────────────────────────────────
        scene = new Scene(loadFXML("primary"), 640, 730);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Cambia la pantalla activa reemplazando el root de la escena.
     *
     * Ajusta el tamaño de la ventana según la pantalla destino:
     *   - dashboardAdmin / dashboardUsuario → 1280 × 800, redimensionable
     *   - cualquier otra pantalla           →  640 × 730, fija
     */
    public static void setRoot(String fxml) throws IOException {
    Parent root = loadFXML(fxml);

    boolean esDashboard = "dashboardAdmin".equals(fxml)
                       || "dashboardUsuario".equals(fxml);

    if (esDashboard) {
        scene = new Scene(root, 1280, 800);
        primaryStage.setScene(scene);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.setResizable(true);
    } else {
        scene = new Scene(root, 640, 730);
        primaryStage.setScene(scene);
        primaryStage.setWidth(640);
        primaryStage.setHeight(730);
        primaryStage.setResizable(false);
    }

    primaryStage.centerOnScreen();
}

    static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/Interface/" + fxml + ".fxml"));
        if (loader.getLocation() == null) {
            throw new IOException(
                "No se encontró el archivo FXML: /Interface/" + fxml + ".fxml\n" +
                "Verifica que exista en src/main/resources/Interface/"
            );
        }
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
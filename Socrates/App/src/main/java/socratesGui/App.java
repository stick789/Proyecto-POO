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
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
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
     */
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        if ("dashboard".equals(fxml)) {
            primaryStage.setWidth(900);
            primaryStage.setHeight(600);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
        } else {
            primaryStage.setWidth(640);
            primaryStage.setHeight(730);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
        }
    }

    static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/socrates/" + fxml + ".fxml"));
        if (loader.getLocation() == null) {
            throw new IOException(
                "No se encontró el archivo FXML: /socrates/" + fxml + ".fxml\n" +
                "Verifica que exista en src/main/resources/socrates/"
            );
        }
        return loader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
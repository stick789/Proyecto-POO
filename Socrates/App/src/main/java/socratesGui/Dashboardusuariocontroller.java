package socratesGui;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import LogicaCita.ConsultaTurnos;
import LogicaCita.ServicioAsignarEntrenador;
import LogicaCita.ServicioTurnos;
import dao.EntrenadorDAO;
import dao.HistorialCitasDAO;
import dao.InstalacionDAO;
import dao.PagoDAO;
import dao.PersonaDAO;
import dao.SedeDAO;
import dao.TurnoDAO;
import database.Conexion;
import entidades.Entrenador;
import entidades.Gimnasio;
import entidades.Instalacion;
import entidades.Pago;
import entidades.Persona;
import entidades.Piscina;
import entidades.Sede;
import entidades.Turno;
import entidades.Usuario;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import negocio.PersonaControl;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

public class Dashboardusuariocontroller implements Initializable {

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Label  lblNombreUsuario;
    @FXML private Label  lblEmailUsuario;
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavAgendarTurno;
    @FXML private Button btnNavMisTurnos;
    @FXML private Button btnNavHistorial;
    @FXML private Button btnNavMisPagos;
    @FXML private Button btnLogout;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label lblSeccion;
    @FXML private Label lblFecha;
    @FXML private Label lblMsgGlobal;

    // ── Paneles ───────────────────────────────────────────────────────────────
    @FXML private VBox panelInicio;
    @FXML private VBox panelAgendarTurno;
    @FXML private VBox panelTurnos;
    @FXML private VBox panelHistorial;
    @FXML private VBox panelPagos;

    // ── Panel Inicio ──────────────────────────────────────────────────────────
    @FXML private Label lblProximoTurno;
    @FXML private Label lblCountTurnos;
    @FXML private Label lblCountPagos;
    @FXML private Label lblCuposGim;
    @FXML private Label lblCuposPisc;
    @FXML private Label lblNotificaciones;

    // ── Panel Agendar Turno ───────────────────────────────────────────────────
    @FXML private Button   btnSelGimnasio;
    @FXML private Button   btnSelPiscina;
    @FXML private Label    lblCapacidadInstalacion;
    @FXML private DatePicker dateFecha;
    @FXML private ComboBox<String> cmbHora;
    @FXML private ComboBox<String> cmbDuracion;
    @FXML private Label    lblDisponibilidad;
    @FXML private VBox     panelCarril;
    @FXML private ComboBox<String> cmbCarril;
    @FXML private Label    lblOcupacionCarril;
    @FXML private VBox     panelResumen;
    @FXML private Label    lblResumen;
    @FXML private Label    lblErrorAgendar;

    // ── Panel Entrenador ──────────────────────────────────────────────────────
    @FXML private VBox   panelEntrenador;
    @FXML private Button btnConEntrenador;
    @FXML private Button btnSinEntrenador;
    @FXML private Label  lblEntrenadorSeleccionado;

    // ── Tabla Turnos Activos ──────────────────────────────────────────────────
    @FXML private TableView<Turno>           tablaTurnos;
    @FXML private TableColumn<Turno, String> colTurnoId;
    @FXML private TableColumn<Turno, String> colTurnoFecha;
    @FXML private TableColumn<Turno, String> colTurnoDuracion;
    @FXML private TableColumn<Turno, String> colTurnoInstalacion;
    @FXML private TableColumn<Turno, String> colTurnoCapacidad;
    @FXML private TableColumn<Turno, String> colTurnoEstado;
    @FXML private TableColumn<Turno, String> colTurnoCarril;
    @FXML private Button                     btnCancelarTurno;
    @FXML private Label                      lblMsgTurnos;
    @FXML private TextField                  txtBuscarTurno;

    // ── Tabla Historial ───────────────────────────────────────────────────────
    @FXML private TableView<Turno>           tablaHistorial;
    @FXML private TableColumn<Turno, String> colHistId;
    @FXML private TableColumn<Turno, String> colHistFecha;
    @FXML private TableColumn<Turno, String> colHistInstalacion;
    @FXML private TableColumn<Turno, String> colHistDuracion;
    @FXML private TableColumn<Turno, String> colHistEstado;
    @FXML private Label                      lblMsgHistorial;
    @FXML private TextField                  txtBuscarHistorial;

    // ── Tabla Pagos ───────────────────────────────────────────────────────────
    @FXML private TableView<Pago>            tablaPagos;
    @FXML private TableColumn<Pago, String>  colPagoId;
    @FXML private TableColumn<Pago, String>  colPagoTurno;
    @FXML private TableColumn<Pago, String>  colPagoMonto;
    @FXML private TableColumn<Pago, String>  colPagoMetodo;
    @FXML private TableColumn<Pago, String>  colPagoEstado;
    @FXML private TableColumn<Pago, String>  colPagoFecha;
    @FXML private Label                      lblMsgPagos;
    @FXML private TextField                  txtBuscarPago;
// ── Guardado de datos para filtrado por ID ───────────────────────────────
    private final List<Turno> turnosActivosBase = new ArrayList<>();
    private final List<Turno> historialBase = new ArrayList<>();
    private final List<Pago> pagosBase = new ArrayList<>();

    // ── Estado de la selección de turno ──────────────────────────────────────
    private Instalacion instalacionSeleccionada = null;
    private boolean     quiereEntrenador        = false;
    private Entrenador  entrenadorAsignado      = null;
    private Stage       mapaPiscinaStage        = null;
    private Canvas      mapaPiscinaCanvas       = null;
    private Piscina     mapaPiscinaActual       = null;
    private static final DateTimeFormatter FMT_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── DAOs (inicializados una vez) ──────────────────────────────────────────
    private TurnoDAO        turnoDAO;
    private InstalacionDAO  instalacionDAO;
    private EntrenadorDAO   entrenadorDAO;
    private PagoDAO         pagoDAO;
    private ConsultaTurnos  consultaTurnos;
    private ServicioTurnos  servicioTurnos;
    private ServicioAsignarEntrenador servicioAsignarEntrenador;
    

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            lblFecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // Init DAOs
            PersonaDAO     personaDAO = new PersonaDAO();
            entrenadorDAO  = new EntrenadorDAO();
            HistorialCitasDAO histDAO = new HistorialCitasDAO();
            instalacionDAO = new InstalacionDAO();
            turnoDAO       = new TurnoDAO(personaDAO, instalacionDAO, entrenadorDAO);
            pagoDAO        = new PagoDAO();
            consultaTurnos = new ConsultaTurnos(turnoDAO, instalacionDAO);
            servicioTurnos = new ServicioTurnos(turnoDAO, histDAO, instalacionDAO);
            servicioAsignarEntrenador = new ServicioAsignarEntrenador(turnoDAO, histDAO, new PersonaControl());

            // Sidebar info
            Persona user = SesionActual.getUsuario();
            if (user != null) {
                lblNombreUsuario.setText(safe(user.getNombre()));
                lblEmailUsuario.setText(safe(user.getEmail()));
            } else {
                lblNombreUsuario.setText("Usuario Demo");
                lblEmailUsuario.setText("demo@test.com");
            }

            // Columnas
            configurarColumnasTurnos();
            configurarColumnasHistorial();
            configurarColumnasPagos();
            tablaTurnos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Formulario de agendamiento
            inicializarFormularioAgendamiento();

            // Buscadores de tablas
            inicializarBuscadores();

            // Cargar inicio
            cargarInicio();

        } catch (Exception e) {
            e.printStackTrace();
            if (lblMsgGlobal != null)
                lblMsgGlobal.setText("Error al inicializar: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORMULARIO AGENDAMIENTO — inicialización
    // ─────────────────────────────────────────────────────────────────────────

    private void inicializarFormularioAgendamiento() {
        dateFecha.setValue(LocalDate.now().plusDays(1));
        dateFecha.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty
                        || date.isBefore(LocalDate.now().plusDays(1))
                        || date.isAfter(LocalDate.now().plusDays(7)));
            }
        });

        List<String> horas = new ArrayList<>();
        for (int h = 6; h <= 20; h++) {
            horas.add(String.format("%02d:00", h));
            if (h < 20) horas.add(String.format("%02d:30", h));
        }
        cmbHora.setItems(FXCollections.observableArrayList(horas));
        cmbHora.setValue("08:00");

        cmbDuracion.setItems(FXCollections.observableArrayList(
                "15 min", "30 min", "45 min", "60 min", "90 min", "120 min"));
        cmbDuracion.setValue("60 min");

        dateFecha.valueProperty().addListener((obs, ov, nv) -> {
            actualizarResumen();
            actualizarOcupacionCarril();
        });
        cmbHora.valueProperty().addListener((obs, ov, nv) -> {
            actualizarResumen();
            actualizarOcupacionCarril();
        });
        cmbDuracion.valueProperty().addListener((obs, ov, nv) -> {
            actualizarResumen();
            actualizarOcupacionCarril();
        });

        // Listener: update lane occupancy when carril changes
        cmbCarril.valueProperty().addListener((obs, ov, nv) -> actualizarOcupacionCarril());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONFIGURACIÓN DE COLUMNAS
    // ─────────────────────────────────────────────────────────────────────────

    private void configurarColumnasTurnos() {
        colTurnoId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colTurnoFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora() != null
                        ? c.getValue().getFechaHora().format(FMT_DISPLAY) : "—"));
        colTurnoDuracion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDuracionMinutos() + " min"));
        colTurnoInstalacion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getInstalacion() != null
                        ? safe(c.getValue().getInstalacion().getTipo()) : "—"));
        colTurnoCapacidad.setCellValueFactory(c -> {
            Turno t = c.getValue();
            if (t.getInstalacion() == null) return new SimpleStringProperty("—");
            try {
                int idInst = t.getInstalacion().getIdInstalacion();
                int capMax = t.getInstalacion().getCapacidadMaxima();
                List<Turno> reservados = turnoDAO.listarReservadosPorInstalacion(idInst);
                long solapan = reservados.stream()
                        .filter(r -> seSolapan(r, t.getFechaHora(), t.getDuracionMinutos()))
                        .count();
                return new SimpleStringProperty(solapan + "/" + capMax);
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        colTurnoEstado.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEstado())));
        colTurnoCarril.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNumeroCarrilAsignado() != null
                        ? String.valueOf(c.getValue().getNumeroCarrilAsignado()) : "—"));
    }

    private void configurarColumnasHistorial() {
        colHistId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colHistFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora() != null
                        ? c.getValue().getFechaHora().format(FMT_DISPLAY) : "—"));
        colHistInstalacion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getInstalacion() != null
                        ? safe(c.getValue().getInstalacion().getTipo()) : "—"));
        colHistDuracion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDuracionMinutos() + " min"));
        colHistEstado.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEstado())));
    }

    private void configurarColumnasPagos() {
        colPagoId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdPago())));
        colPagoTurno.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colPagoMonto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getMonto() != null
                        ? "$ " + c.getValue().getMonto().toPlainString() : "—"));
        colPagoMetodo.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getMetodoPago())));
        colPagoEstado.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEstadoPago())));
        colPagoFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaPago() != null
                        ? c.getValue().getFechaPago().format(FMT_DISPLAY) : "—"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVEGACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onInicio()       { mostrarPanel(panelInicio,        "Inicio");         cargarInicio(); }
    @FXML private void onAgendarTurno() { mostrarPanel(panelAgendarTurno,  "Agendar Turno");  limpiarFormulario(); }
    @FXML private void onMisTurnos()    { mostrarPanel(panelTurnos,        "Mis Turnos");     if (txtBuscarTurno     != null) txtBuscarTurno.clear();     cargarMisTurnos(); }
    @FXML private void onHistorial()    { mostrarPanel(panelHistorial,      "Historial");      if (txtBuscarHistorial != null) txtBuscarHistorial.clear(); cargarHistorial(); }
    @FXML private void onMisPagos()     { mostrarPanel(panelPagos,         "Mis Pagos");      if (txtBuscarPago      != null) txtBuscarPago.clear();      cargarMisPagos(); }
    @FXML private void onMisFacturas()  { onMisPagos(); }

    private void inicializarBuscadores() {
        if (txtBuscarTurno != null) {
            txtBuscarTurno.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroTurnos());
        }
        if (txtBuscarHistorial != null) {
            txtBuscarHistorial.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroHistorial());
        }
        if (txtBuscarPago != null) {
            txtBuscarPago.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltroPagos());
        }
    }

    @FXML private void onLogout() {
        try {
            SesionActual.cerrar();
            App.setRoot("primary");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACCIONES DE AGENDAMIENTO
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void onSelGimnasio() {
        lblErrorAgendar.setText("");
        mostrarSelectorInstalacion("GIMNASIO");
    }

    @FXML
    private void onSelPiscina() {
        lblErrorAgendar.setText("");
        mostrarSelectorInstalacion("PISCINA");
    }

    /**
     * Carga las instalaciones del tipo dado que pertenecen a la sede del usuario.
     * Si hay más de una, abre un ChoiceDialog para que el usuario elija.
     * Si solo hay una, la selecciona directamente.
     *
     * Como la sesión todavía no almacena una sede propia, se agregan las
     * instalaciones de todas las sedes registradas para no ocultar sedes nuevas.
     */
    private void mostrarSelectorInstalacion(String tipo) {
        List<Instalacion> opciones = listarInstalacionesPorTipo(tipo);

        if (opciones.isEmpty()) {
            lblCapacidadInstalacion.setText(
                "No hay " + tipo.toLowerCase() + "s registrados en tu sede.");
            instalacionSeleccionada = null;
            return;
        }

        if (opciones.size() == 1) {
            // Solo una opción — selección directa, sin diálogo
            aplicarSeleccionInstalacion(opciones.get(0), tipo);
        } else {
            // Varias opciones — el usuario elige
            ChoiceDialog<Instalacion> dialog =
                    new ChoiceDialog<>(opciones.get(0), opciones);
            dialog.setTitle("Seleccionar instalación");
            dialog.setHeaderText("Elige el " + tipo.toLowerCase() + " de tu sede:");
            dialog.setContentText("Instalación:");
            dialog.showAndWait().ifPresent(inst -> aplicarSeleccionInstalacion(inst, tipo));
        }
    }

    /**
     * Reúne instalaciones del tipo solicitado en todas las sedes registradas.
     * Esto evita que una sede nueva quede fuera si la sesión aún no conoce su id.
     */
    private List<Instalacion> listarInstalacionesPorTipo(String tipo) {
        List<Instalacion> opciones = new ArrayList<>();
        try {
            List<Integer> sedes = new SedeDAO().listarTodos()
                    .stream()
                    .map(Sede::getIdSede)
                    .collect(Collectors.toList());

            for (Integer idSede : sedes) {
                List<Instalacion> parciales = "GIMNASIO".equals(tipo)
                        ? instalacionDAO.listarGimnasiosPorSede(idSede)
                        : instalacionDAO.listarPiscinasPorSede(idSede);
                if (parciales != null && !parciales.isEmpty()) {
                    opciones.addAll(parciales);
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return opciones;
    }

    /** Aplica la instalación elegida: actualiza estilos, labels y paneles derivados. */
    private void aplicarSeleccionInstalacion(Instalacion inst, String tipo) {
        instalacionSeleccionada = inst;

        String activeBtn  = "-fx-background-color: #E85D04; -fx-text-fill: white; "
                + "-fx-border-color: #E85D04; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-font-size: 13; -fx-cursor: hand;";
        String inactiveBtn = "-fx-background-color: white; -fx-border-color: #E85D04; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-font-size: 13; -fx-cursor: hand; -fx-text-fill: #E85D04;";

        if ("GIMNASIO".equals(tipo)) {
            btnSelGimnasio.setStyle(activeBtn);
            btnSelPiscina.setStyle(inactiveBtn);
            panelCarril.setVisible(false);
            panelCarril.setManaged(false);
            cmbCarril.getItems().clear();
        } else {
            btnSelPiscina.setStyle(activeBtn);
            btnSelGimnasio.setStyle(inactiveBtn);
            if (inst instanceof Piscina) {
                Piscina piscina = (Piscina) inst;
                List<String> carriles = new ArrayList<>();
                for (int i = 1; i <= piscina.getNumeroCarriles(); i++)
                    carriles.add("Carril " + i);
                cmbCarril.setItems(FXCollections.observableArrayList(carriles));
                if (!carriles.isEmpty()) cmbCarril.setValue(carriles.get(0));
                panelCarril.setVisible(true);
                panelCarril.setManaged(true);
                actualizarOcupacionCarril();
            }
        }

        String label = (inst.getNombre() != null && !inst.getNombre().isBlank())
                ? inst.getNombre() : tipo;
        String sedeInfo = "";
        if (inst.getNombreSede() != null && !inst.getNombreSede().isBlank()) {
            sedeInfo = " · " + inst.getNombreSede();
            if (inst.getDireccionSede() != null && !inst.getDireccionSede().isBlank())
                sedeInfo += " (" + inst.getDireccionSede() + ")";
        }
        int cupos  = inst.getAforoActual();
        int capMax = inst.getCapacidadMaxima();
        lblCapacidadInstalacion.setText(
                label + sedeInfo + " — Capacidad máx: " + capMax + " | Cupos disponibles ahora: " + cupos);

        mostrarPanelEntrenador();
        actualizarResumen();
    }

    /**
     * Muestra el panel de selección de entrenador con animación.
     */
    private void mostrarPanelEntrenador() {
        quiereEntrenador = false;
        entrenadorAsignado = null;
        lblEntrenadorSeleccionado.setText("");
        // Reset coach button styles
        btnConEntrenador.setStyle("-fx-background-color: white; -fx-border-color: #E85D04; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-text-fill: #E85D04;");
        btnSinEntrenador.setStyle("-fx-background-color: white; -fx-border-color: #aaa; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-text-fill: #555;");
        panelEntrenador.setVisible(true);
        panelEntrenador.setManaged(true);
    }
private void abrirPasarelaPago(Turno turno) {
        try {
            //Cargar FMXL
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Interface/pasarelaPagos.fxml"));
            Parent root = loader.load();

            PasarelaPagosController controller = loader.getController();
            // Pasar el turno - iniciara automaticamente el proceso de pago
            controller.setTurno(turno);
            // Pasar HostServices para poder abrir el navegador desde el controlador
            controller.setHostServices(App.getAppHostServices());
            //Configurar y mostrar ventana modal
            Stage stage = new Stage();
            stage.setTitle("Pasarela de Pagos - Turno #" + turno.getIdTurno());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        }catch (Exception e) {
            mostrarAlerta("Error", "No se pudo abrir la pasarela de pagos: " + e.getMessage());
        }
    }
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.initStyle(StageStyle.UTILITY);
        alerta.setTitle(titulo != null ? titulo : "Error");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje != null ? mensaje : "Ocurrió un error inesperado.");
        alerta.showAndWait();
    }

    @FXML
    private void onConEntrenador() {
        if (instalacionSeleccionada == null) {
            lblEntrenadorSeleccionado.setText("⚠ Selecciona una instalación primero.");
            return;
        }
        quiereEntrenador = true;
        btnConEntrenador.setStyle("-fx-background-color: #E85D04; -fx-text-fill: white; -fx-border-color: #E85D04; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand;");
        btnSinEntrenador.setStyle("-fx-background-color: white; -fx-border-color: #aaa; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-text-fill: #555;");

        // Filter coaches by the selected installation type
        String especialidad = "PISCINA".equals(instalacionSeleccionada.getTipo())
            ? "Natación" : "Gimnasio";

        try {
            List<Entrenador> entrenadores = entrenadorDAO.listarPorEspecialidad(especialidad);
            if (entrenadores == null || entrenadores.isEmpty()) {
                // Fallback: try any available coach
                entrenadores = entrenadorDAO.listar("", 10, 1);
            }
            if (entrenadores != null && !entrenadores.isEmpty()) {
                entrenadorAsignado = entrenadores.get(0);
                lblEntrenadorSeleccionado.setText("✅ Entrenador asignado: " + safe(entrenadorAsignado.getNombre())
                        + " (" + safe(entrenadorAsignado.getEspecialidad()) + ")");
            } else {
                lblEntrenadorSeleccionado.setText("⚠ No hay entrenadores disponibles para esta instalación.");
                quiereEntrenador = false;
            }
        } catch (Exception e) {
            lblEntrenadorSeleccionado.setText("⚠ No se pudo obtener entrenadores: " + e.getMessage());
            quiereEntrenador = false;
        }
        actualizarResumen();
    }

    @FXML
    private void onSinEntrenador() {
        quiereEntrenador = false;
        entrenadorAsignado = null;
        lblEntrenadorSeleccionado.setText("Sin entrenador — sesión independiente.");
        btnSinEntrenador.setStyle("-fx-background-color: #888; -fx-text-fill: white; -fx-border-color: #888; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand;");
        btnConEntrenador.setStyle("-fx-background-color: white; -fx-border-color: #E85D04; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-text-fill: #E85D04;");
        actualizarResumen();
    }

    /**
     * Opens a popup window showing an Olympic pool diagram with lane occupancy counters.
     */
    @FXML
    private void onVerMapaPiscina() {
        if (!(instalacionSeleccionada instanceof Piscina)) return;
        Piscina piscina = (Piscina) instalacionSeleccionada;

        if (mapaPiscinaStage != null && mapaPiscinaStage.isShowing() && mapaPiscinaActual != null
                && mapaPiscinaActual.getIdInstalacion() == piscina.getIdInstalacion()) {
            refrescarMapaPiscinaAbierto();
            mapaPiscinaStage.toFront();
            return;
        }

        mapaPiscinaActual = piscina;
        int totalCarriles = piscina.getNumeroCarriles();

        // ── Build the popup ──────────────────────────────────────────────────
        mapaPiscinaStage = new Stage();
        mapaPiscinaStage.initModality(Modality.APPLICATION_MODAL);
        mapaPiscinaStage.initStyle(StageStyle.DECORATED);
        mapaPiscinaStage.setTitle("🏊  Mapa de Piscina Olímpica — Carriles");
        mapaPiscinaStage.setResizable(false);

        // Canvas dimensions
        int laneW    = 80;
        int laneH    = 340;
        int padding  = 40;
        int labelH   = 60;
        int canvasW  = padding * 2 + laneW * totalCarriles + (totalCarriles - 1) * 6;
        int canvasH  = padding * 2 + laneH + labelH;

        mapaPiscinaCanvas = new Canvas(canvasW, canvasH);
        dibujarMapaPiscina(mapaPiscinaActual, mapaPiscinaCanvas);

        // Close button
        Button btnClose = new Button("Cerrar");
        btnClose.setStyle("-fx-background-color: #E85D04; -fx-text-fill: white; -fx-font-size: 12; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnClose.setOnAction(e -> {
            if (mapaPiscinaStage != null) {
                mapaPiscinaStage.close();
            }
        });

        VBox root = new VBox(8, mapaPiscinaCanvas, btnClose);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #FFF8F0;");

        mapaPiscinaStage.setScene(new Scene(root));
        mapaPiscinaStage.setOnHidden(e -> {
            mapaPiscinaStage = null;
            mapaPiscinaCanvas = null;
            mapaPiscinaActual = null;
        });
        mapaPiscinaStage.showAndWait();
    }

    private int[] calcularOcupacionPiscina(Piscina piscina) {
        int totalCarriles = piscina.getNumeroCarriles();
        int[] ocupacion = new int[totalCarriles + 1];

        try {
            List<Turno> reservados = turnoDAO.listarReservadosPorInstalacion(piscina.getIdInstalacion());
            LocalDateTime fh = obtenerFechaHoraSeleccionada();
            int dur = parseDuracion();
            for (Turno t : reservados) {
                Integer carril = t.getNumeroCarrilAsignado();
                if (carril != null && carril >= 1 && carril <= totalCarriles) {
                    if (fh != null && seSolapan(t, fh, dur)) {
                        ocupacion[carril]++;
                    } else if (fh == null) {
                        ocupacion[carril]++;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return ocupacion;
    }

    private void dibujarMapaPiscina(Piscina piscina, Canvas canvas) {
        if (piscina == null || canvas == null) return;

        int totalCarriles = piscina.getNumeroCarriles();
        int maxPorCarril = servicioTurnos.getMaxPersonasPorCarril();
        int[] ocupacion = calcularOcupacionPiscina(piscina);

        int laneW    = 80;
        int laneH    = 340;
        int padding  = 40;
        int labelH   = 60;
        int canvasW  = padding * 2 + laneW * totalCarriles + (totalCarriles - 1) * 6;
        int canvasH  = padding * 2 + laneH + labelH;

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#FFF8F0"));
        gc.fillRect(0, 0, canvasW, canvasH);

        gc.setFill(Color.web("#E85D04"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        gc.fillText("Piscina Olímpica — " + totalCarriles + " Carriles", padding, 22);

        gc.setFont(Font.font("Arial", 11));
        gc.setFill(Color.web("#888"));
        gc.fillText("Turnos en el horario seleccionado · máx " + maxPorCarril + " personas/carril", padding, 37);

        double poolX = padding - 4;
        double poolY = padding + 15;
        double poolW = laneW * totalCarriles + (totalCarriles - 1) * 6 + 8;
        double poolH = laneH + 8;
        gc.setFill(Color.web("#0077B6", 0.15));
        gc.fillRoundRect(poolX, poolY, poolW, poolH, 10, 10);
        gc.setStroke(Color.web("#0077B6", 0.6));
        gc.setLineWidth(2);
        gc.strokeRoundRect(poolX, poolY, poolW, poolH, 10, 10);

        for (int i = 0; i < totalCarriles; i++) {
            int laneNum = i + 1;
            double x = padding + i * (laneW + 6);
            double y = padding + 19;

            int occ = ocupacion[laneNum];
            double fillRatio = (maxPorCarril > 0) ? (double) occ / maxPorCarril : 0;
            Color laneFill;
            if (occ == 0) {
                laneFill = Color.web("#48CAE4", 0.55);
            } else if (fillRatio < 0.75) {
                laneFill = Color.web("#F9A825", 0.65);
            } else {
                laneFill = Color.web("#E85D04", 0.70);
            }

            gc.setFill(laneFill);
            gc.fillRoundRect(x, y, laneW, laneH, 6, 6);
            gc.setStroke(Color.web("#0077B6", 0.5));
            gc.setLineWidth(1);
            gc.strokeRoundRect(x, y, laneW, laneH, 6, 6);

            gc.setFill(Color.web("#004369"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            String laneLabel = "C " + laneNum;
            gc.fillText(laneLabel, x + laneW / 2 - 14, y + 22);

            gc.setStroke(Color.web("#FFFFFF", 0.7));
            gc.setLineWidth(2);
            gc.setLineDashes(8, 6);
            gc.strokeLine(x + laneW / 2.0, y + 32, x + laneW / 2.0, y + laneH - 10);
            gc.setLineDashes();

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            String occText = occ + " / " + maxPorCarril;
            gc.fillText(occText, x + laneW / 2.0 - 16, y + laneH - 12);

            gc.setFill(occ == 0 ? Color.web("#0077B6") : (fillRatio < 0.75 ? Color.web("#C44D03") : Color.web("#E85D04")));
            gc.setFont(Font.font("Arial", 11));
            String statusLabel = occ == 0 ? "Libre" : (occ >= maxPorCarril ? "Lleno" : "Parcial");
            double labelX = x + laneW / 2.0 - (statusLabel.length() * 3.2);
            gc.fillText(statusLabel, labelX, y + laneH + 20);
        }

        double legX = padding;
        double legY = canvasH - 16.0;
        gc.setFont(Font.font("Arial", 10));
        gc.setFill(Color.web("#48CAE4", 0.8)); gc.fillRect(legX, legY - 10, 12, 10);
        gc.setFill(Color.web("#444")); gc.fillText(" Libre", legX + 14, legY);
        gc.setFill(Color.web("#F9A825", 0.8)); gc.fillRect(legX + 65, legY - 10, 12, 10);
        gc.setFill(Color.web("#444")); gc.fillText(" Parcial", legX + 79, legY);
        gc.setFill(Color.web("#E85D04", 0.8)); gc.fillRect(legX + 148, legY - 10, 12, 10);
        gc.setFill(Color.web("#444")); gc.fillText(" Lleno", legX + 162, legY);
    }

    private void refrescarMapaPiscinaAbierto() {
        if (mapaPiscinaStage == null || mapaPiscinaCanvas == null || mapaPiscinaActual == null) {
            return;
        }
        dibujarMapaPiscina(mapaPiscinaActual, mapaPiscinaCanvas);
    }

    /** Updates lane occupancy label when a lane is selected in the combo. */
    private void actualizarOcupacionCarril() {
        if (!(instalacionSeleccionada instanceof Piscina)) return;
        String carrilStr = cmbCarril.getValue();
        if (carrilStr == null) { lblOcupacionCarril.setText(""); return; }
        try {
            int carrilNum   = Integer.parseInt(carrilStr.replace("Carril ", "").trim());
            int maxPorCarril = servicioTurnos.getMaxPersonasPorCarril();
            LocalDateTime fh = obtenerFechaHoraSeleccionada();
            int dur = parseDuracion();
            List<Turno> reservados = turnoDAO.listarReservadosPorInstalacion(
                    instalacionSeleccionada.getIdInstalacion());
            long occ = reservados.stream()
                    .filter(t -> t.getNumeroCarrilAsignado() != null
                            && t.getNumeroCarrilAsignado() == carrilNum
                            && (fh == null || seSolapan(t, fh, dur)))
                    .count();
            String estado = (occ >= maxPorCarril) ? " 🔴 LLENO" : (occ > 0 ? " 🟡 Parcial" : " 🟢 Libre");
            lblOcupacionCarril.setText(occ + " / " + maxPorCarril + " personas" + estado);
            refrescarMapaPiscinaAbierto();
        } catch (Exception e) {
            lblOcupacionCarril.setText("");
        }
    }

    @FXML
    private void onVerificarDisponibilidad() {
        lblErrorAgendar.setText("");
        lblDisponibilidad.setText("");
        if (instalacionSeleccionada == null) {
            lblErrorAgendar.setText("Selecciona una instalación primero.");
            return;
        }
        LocalDateTime fechaHora = obtenerFechaHoraSeleccionada();
        if (fechaHora == null) return;
        int duracion = parseDuracion();
        boolean disponible = consultaTurnos.estaDisponible(
                instalacionSeleccionada.getIdInstalacion(), fechaHora, duracion);
        if (disponible) {
            lblDisponibilidad.setText("✔ Horario disponible para el " +
                    fechaHora.format(FMT_DISPLAY) + " (" + duracion + " min).");
            lblDisponibilidad.setStyle("-fx-font-size: 11; -fx-text-fill: #27ae60;");
        } else {
            lblDisponibilidad.setText("✗ El horario no está disponible. Prueba otra hora o duración.");
            lblDisponibilidad.setStyle("-fx-font-size: 11; -fx-text-fill: #C44D03;");
        }
        actualizarResumen();
        // Also refresh lane occupancy
        actualizarOcupacionCarril();
    }

    @FXML
    private void onConfirmarTurno() {
        lblErrorAgendar.setText("");
        Persona user = SesionActual.getUsuario();

        if (instalacionSeleccionada == null) {
            lblErrorAgendar.setText("Selecciona una instalación.");
            return;
        }
        if (dateFecha.getValue() == null) {
            lblErrorAgendar.setText("Selecciona una fecha.");
            return;
        }
        if (!(user instanceof Usuario)) {
            lblErrorAgendar.setText("No hay sesión activa o es modo demo. Inicia sesión con una cuenta real.");
            return;
        }
        Usuario usuario = (Usuario) user;

        LocalDateTime fechaHora = obtenerFechaHoraSeleccionada();
        if (fechaHora == null) return;
        int duracion = parseDuracion();

        // Carril para piscina
        Integer carril = null;
        if (instalacionSeleccionada instanceof Piscina) {
            String carrilStr = cmbCarril.getValue();
            if (carrilStr == null) {
                lblErrorAgendar.setText("Selecciona un carril.");
                return;
            }
            carril = Integer.parseInt(carrilStr.replace("Carril ", "").trim());
        }

        try {
            Instalacion instActualizada = instalacionDAO.buscarPorId(
                    instalacionSeleccionada.getIdInstalacion()).orElse(instalacionSeleccionada);

            Turno nuevo = servicioTurnos.reservarTurno(
                    fechaHora, duracion, usuario, instActualizada, carril, usuario);

            // Assign trainer if selected
            if (quiereEntrenador && entrenadorAsignado != null) {
                try {
                    servicioAsignarEntrenador.asignarEntrenadorATurno(nuevo, entrenadorAsignado, usuario);
                } catch (Exception ignored) {
                    // Non-critical: turno created, trainer assignment logged
                }
            }
           
            lblErrorAgendar.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
            String msgEntrenador = (quiereEntrenador && entrenadorAsignado != null)
                    ? " con entrenador " + entrenadorAsignado.getNombre() : "";
            lblErrorAgendar.setText("✔ Turno #" + nuevo.getIdTurno() + " reservado exitosamente para el "
                    + fechaHora.format(FMT_DISPLAY) + msgEntrenador + ".");

            instalacionSeleccionada = instActualizada;
                actualizarResumen();
                actualizarOcupacionCarril();
                cargarInicio();
            refrescarMapaPiscinaAbierto();

            // Si el usuario es ESTUDIANTE, no enviar a pasarela: agendar directamente
            String categoriaUsuario = usuario.getCategoria();
            if (categoriaUsuario != null && categoriaUsuario.trim().equalsIgnoreCase("ESTUDIANTE")) {
                // Mensaje informativo ya mostrado arriba; evitar abrir la pasarela de pagos
                lblErrorAgendar.setText(lblErrorAgendar.getText() + " (Usuario ESTUDIANTE: exento de pago)");
            } else {
                abrirPasarelaPago(nuevo);
            }

        } catch (IllegalArgumentException | IllegalStateException | IllegalAccessError ex) {
            lblErrorAgendar.setStyle("-fx-text-fill: #C44D03; -fx-font-size: 12;");
            lblErrorAgendar.setText("✗ " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            lblErrorAgendar.setStyle("-fx-text-fill: #C44D03; -fx-font-size: 12;");
            String causa = ex.getCause() != null ? " (" + ex.getCause().getMessage() + ")" : "";
            lblErrorAgendar.setText("Error inesperado: " + ex.getMessage() + causa);
        }
    }


    @FXML
    private void onCancelarTurno() {
        Turno sel = tablaTurnos.getSelectionModel().getSelectedItem();
        if (sel == null) { lblMsgTurnos.setText("Selecciona un turno para cancelar."); return; }
        if (Turno.ESTADO_CANCELADO.equals(sel.getEstado())) {
            lblMsgTurnos.setText("El turno ya está cancelado."); return;
        }

        Persona user = SesionActual.getUsuario();
        if (user == null || user.getId() == 0) {
            lblMsgTurnos.setText("(Demo) Turno #" + sel.getIdTurno() + " cancelado."); return;
        }

        try {
            servicioTurnos.cancelarTurno(sel, user);
            lblMsgTurnos.setText("Turno #" + sel.getIdTurno() + " cancelado correctamente.");
            cargarMisTurnos();
            cargarInicio();
            actualizarOcupacionCarril();
            refrescarMapaPiscinaAbierto();
        } catch (IllegalStateException ex) {
            lblMsgTurnos.setText("No se puede cancelar: " + ex.getMessage());
        } catch (Exception ex) {
            lblMsgTurnos.setText("Error: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  mostrarPanel
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarPanel(VBox panelActivo, String titulo) {
        List<VBox> paneles = Arrays.asList(
                panelInicio, panelAgendarTurno, panelTurnos, panelHistorial, panelPagos);
        for (VBox p : paneles) { p.setVisible(false); p.setManaged(false); }
        panelActivo.setVisible(true);
        panelActivo.setManaged(true);
        lblSeccion.setText(titulo);
        FadeTransition ft = new FadeTransition(Duration.millis(200), panelActivo);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CARGA DE DATOS
    // ─────────────────────────────────────────────────────────────────────────

    private void cargarInicio() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Turno> turnos;
            List<Pago>  pagos;

            boolean modoDemo = (user == null || user.getId() == 0);
            if (modoDemo) {
                turnos = crearTurnosDemo();
                pagos  = crearPagosDemo();
                lblMsgGlobal.setText("Modo demo — inicia sesión con una cuenta real para ver tus datos.");
            } else {
                try { turnos = turnoDAO.listarPorUsuario(user.getId()); }
                catch (Exception e) { turnos = crearTurnosDemo(); }
                try { pagos = pagoDAO.listarPorUsuario(user.getId()); }
                catch (Exception e) { pagos = crearPagosDemo(); }
            }

            final List<Turno> tf = turnos;
            long activos = tf.stream().filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())).count();
            lblCountTurnos.setText(String.valueOf(activos));

            final List<Pago> pf = pagos;
            long pendientes = pf.stream().filter(p -> Pago.ESTADO_PENDIENTE.equals(p.getEstadoPago())).count();
            lblCountPagos.setText(String.valueOf(pendientes));

            tf.stream()
                    .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())
                            && t.getFechaHora() != null
                            && t.getFechaHora().isAfter(LocalDateTime.now()))
                    .min((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()))
                    .ifPresentOrElse(t -> {
                        String tipo  = t.getInstalacion() != null ? t.getInstalacion().getTipo() : "—";
                        String fecha = t.getFechaHora().format(FMT_DISPLAY);
                        lblProximoTurno.setText(tipo + "  •  " + fecha + "  (" + t.getDuracionMinutos() + " min)");
                    }, () -> lblProximoTurno.setText("Sin turnos próximos"));

            actualizarEstadoInstalaciones();
            generarNotificaciones(tf);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void actualizarEstadoInstalaciones() {
        try {
            List<Instalacion> gyms = instalacionDAO.listarGimnasios();
            if (!gyms.isEmpty()) {
                Instalacion g = gyms.get(0);
                int ocupados = g.getCapacidadMaxima() - g.getAforoActual();
                lblCuposGim.setText("Gym: " + ocupados + "/" + g.getCapacidadMaxima() + " ocupados");
            } else {
                lblCuposGim.setText("Gym: sin datos");
            }
        } catch (Exception e) { lblCuposGim.setText("Gym: —"); }

        try {
            List<Instalacion> piscs = instalacionDAO.listarPiscinas();
            if (!piscs.isEmpty()) {
                Instalacion p = piscs.get(0);
                int ocupados = p.getCapacidadMaxima() - p.getAforoActual();
                lblCuposPisc.setText("Piscina: " + ocupados + "/" + p.getCapacidadMaxima() + " ocupados");
            } else {
                lblCuposPisc.setText("Piscina: sin datos");
            }
        } catch (Exception e) { lblCuposPisc.setText("Piscina: —"); }
    }

    private void generarNotificaciones(List<Turno> turnos) {
        StringBuilder sb = new StringBuilder();
        LocalDateTime ahora = LocalDateTime.now();
        turnos.stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())
                        && t.getFechaHora() != null
                        && t.getFechaHora().isAfter(ahora)
                        && t.getFechaHora().isBefore(ahora.plusHours(24)))
                .forEach(t -> sb.append("⏰ Turno mañana: ")
                        .append(t.getInstalacion() != null ? t.getInstalacion().getTipo() : "—")
                        .append(" a las ").append(t.getFechaHora().format(FMT_DISPLAY))
                        .append("\n"));
        lblNotificaciones.setText(sb.length() > 0 ? sb.toString().trim() : "Sin notificaciones pendientes.");
    }

    private void cargarMisTurnos() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Turno> turnos;
            if (user == null || user.getId() == 0) {
                turnos = crearTurnosDemo();
                lblMsgTurnos.setText("(modo demo)");
            } else {
                try {
                    turnos = turnoDAO.listarPorUsuario(user.getId());
                    turnos = turnos.stream()
                            .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                            .collect(Collectors.toList());
                    if (turnos == null) turnos = crearTurnosDemo();
                } catch (Exception ex) {
                    turnos = crearTurnosDemo();
                    lblMsgTurnos.setText("Error BD - mostrando demo");
                }
            }
            turnosActivosBase.clear();
            turnosActivosBase.addAll(turnos);
            aplicarFiltroTurnos();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarHistorial() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Turno> hist;
            if (user == null || user.getId() == 0) {
                hist = crearTurnosDemo().stream()
                        .filter(t -> !Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                        .collect(Collectors.toList());
                lblMsgHistorial.setText("(modo demo)");
            } else {
                try {
                    List<Turno> todos = turnoDAO.listarPorUsuario(user.getId());
                    hist = todos.stream()
                            .filter(t -> !Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                            .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora()))
                            .collect(Collectors.toList());
                } catch (Exception ex) {
                    hist = new ArrayList<>();
                    lblMsgHistorial.setText("Error BD: " + ex.getMessage());
                }
            }
            historialBase.clear();
            historialBase.addAll(hist);
            aplicarFiltroHistorial();
            if (hist.isEmpty()) lblMsgHistorial.setText("No hay turnos en el historial.");
            else lblMsgHistorial.setText("");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarMisPagos() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Pago> pagos;
            if (user == null || user.getId() == 0) {
                pagos = crearPagosDemo();
                lblMsgPagos.setText("(modo demo)");
            } else {
                try {
                    pagos = pagoDAO.listarPorUsuario(user.getId());
                    if (pagos == null) pagos = crearPagosDemo();
                } catch (Exception ex) {
                    pagos = crearPagosDemo();
                    lblMsgPagos.setText("Error BD - mostrando demo");
                }
            }
            pagosBase.clear();
            pagosBase.addAll(pagos);
            aplicarFiltroPagos();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void aplicarFiltroTurnos() {
        String filtro = txtBuscarTurno == null ? "" : txtBuscarTurno.getText();
        List<Turno> filtrados = turnosActivosBase.stream()
                .filter(t -> coincideTurno(t, filtro))
                .collect(Collectors.toList());
        tablaTurnos.setItems(FXCollections.observableArrayList(filtrados));
        lblMsgTurnos.setText(filtrados.isEmpty() && !turnosActivosBase.isEmpty()
                ? "No hay turnos que coincidan con la búsqueda." : lblMsgTurnos.getText());
    }

    private void aplicarFiltroHistorial() {
        String filtro = txtBuscarHistorial == null ? "" : txtBuscarHistorial.getText();
        List<Turno> filtrados = historialBase.stream()
                .filter(t -> coincideTurno(t, filtro))
                .collect(Collectors.toList());
        tablaHistorial.setItems(FXCollections.observableArrayList(filtrados));
        if (filtrados.isEmpty() && !historialBase.isEmpty()) {
            lblMsgHistorial.setText("No hay turnos que coincidan con la búsqueda.");
        }
    }

    private void aplicarFiltroPagos() {
        String filtro = txtBuscarPago == null ? "" : txtBuscarPago.getText();
        List<Pago> filtrados = pagosBase.stream()
                .filter(p -> coincidePago(p, filtro))
                .collect(Collectors.toList());
        tablaPagos.setItems(FXCollections.observableArrayList(filtrados));
        if (filtrados.isEmpty() && !pagosBase.isEmpty()) {
            lblMsgPagos.setText("No hay pagos que coincidan con la búsqueda.");
        }
    }

    private boolean coincideTurno(Turno turno, String filtro) {
        String texto = filtro == null ? "" : filtro.trim().toLowerCase();
        if (texto.isEmpty()) return true;
        return String.valueOf(turno.getIdTurno()).contains(texto)
                || safe(turno.getEstado()).toLowerCase().contains(texto)
                || safe(turno.getDuracionMinutos() + "").contains(texto)
                || (turno.getInstalacion() != null && (
                        safe(turno.getInstalacion().getTipo()).toLowerCase().contains(texto)
                        || safe(turno.getInstalacion().getNombre()).toLowerCase().contains(texto)
                        || safe(turno.getInstalacion().getNombreSede()).toLowerCase().contains(texto)
                        || safe(turno.getInstalacion().getDireccionSede()).toLowerCase().contains(texto)))
                || (turno.getFechaHora() != null && turno.getFechaHora().format(FMT_DISPLAY).toLowerCase().contains(texto))
                || safe(turno.getNumeroCarrilAsignado() != null ? String.valueOf(turno.getNumeroCarrilAsignado()) : "").toLowerCase().contains(texto);
    }

    private boolean coincidePago(Pago pago, String filtro) {
        String texto = filtro == null ? "" : filtro.trim().toLowerCase();
        if (texto.isEmpty()) return true;
        return String.valueOf(pago.getIdPago()).contains(texto);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS FORMULARIO
    // ─────────────────────────────────────────────────────────────────────────

    private void actualizarResumen() {
        if (instalacionSeleccionada == null || dateFecha.getValue() == null) {
            panelResumen.setVisible(false);
            panelResumen.setManaged(false);
            return;
        }
        LocalDateTime fh = obtenerFechaHoraSeleccionada();
        if (fh == null) return;
        int dur = parseDuracion();
        LocalDateTime fin = fh.plusMinutes(dur);

        String nombre = (instalacionSeleccionada.getNombre() != null
                && !instalacionSeleccionada.getNombre().isBlank())
                ? instalacionSeleccionada.getNombre()
                : instalacionSeleccionada.getTipo();
        String sedeResumen = "";
        if (instalacionSeleccionada.getNombreSede() != null
                && !instalacionSeleccionada.getNombreSede().isBlank()) {
            sedeResumen = "\nSede: " + instalacionSeleccionada.getNombreSede();
            if (instalacionSeleccionada.getDireccionSede() != null
                    && !instalacionSeleccionada.getDireccionSede().isBlank())
                sedeResumen += " — " + instalacionSeleccionada.getDireccionSede();
        }
        String resumen = "Instalación: " + nombre + sedeResumen + "\n"
                + "Fecha: " + fh.format(FMT_DISPLAY) + "\n"
                + "Duración: " + dur + " min (fin aprox. " + fin.format(DateTimeFormatter.ofPattern("HH:mm")) + ")\n";

        if (instalacionSeleccionada instanceof Piscina && cmbCarril.getValue() != null)
            resumen += "Carril: " + cmbCarril.getValue() + "\n";

        if (quiereEntrenador && entrenadorAsignado != null)
            resumen += "Entrenador: " + entrenadorAsignado.getNombre();
        else if (panelEntrenador.isVisible())
            resumen += "Entrenador: " + (quiereEntrenador ? "solicitado (sin confirmar)" : "no solicitado");

        lblResumen.setText(resumen);
        panelResumen.setVisible(true);
        panelResumen.setManaged(true);
    }

    private void limpiarFormulario() {
        lblErrorAgendar.setText("");
        lblDisponibilidad.setText("");
        lblCapacidadInstalacion.setText("");
        instalacionSeleccionada = null;
        quiereEntrenador = false;
        entrenadorAsignado = null;
        panelResumen.setVisible(false);
        panelResumen.setManaged(false);
        panelCarril.setVisible(false);
        panelCarril.setManaged(false);
        panelEntrenador.setVisible(false);
        panelEntrenador.setManaged(false);
        lblEntrenadorSeleccionado.setText("");
        lblOcupacionCarril.setText("");
        dateFecha.setValue(LocalDate.now().plusDays(1));
        cmbHora.setValue("08:00");
        cmbDuracion.setValue("60 min");
        String defaultBtn = "-fx-background-color: white; -fx-border-color: #E85D04; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13; -fx-cursor: hand; -fx-text-fill: #E85D04;";
        btnSelGimnasio.setStyle(defaultBtn);
        btnSelPiscina.setStyle(defaultBtn);
    }

    private LocalDateTime obtenerFechaHoraSeleccionada() {
        LocalDate fecha = dateFecha.getValue();
        String horaStr = cmbHora.getValue();
        if (fecha == null || horaStr == null) return null;
        try {
            String[] parts = horaStr.split(":");
            LocalTime hora = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            return LocalDateTime.of(fecha, hora);
        } catch (Exception e) { return null; }
    }

    private int parseDuracion() {
        String d = cmbDuracion.getValue();
        if (d == null) return 60;
        return Integer.parseInt(d.replace(" min", "").trim());
    }

    /** Verifica si un turno existente se solapa con el bloque (inicio, duracion). */
    private boolean seSolapan(Turno t, LocalDateTime inicio, int duracion) {
        if (t.getFechaHora() == null) return false;
        LocalDateTime finNuevo = inicio.plusMinutes(duracion);
        LocalDateTime finT     = t.getFechaHora().plusMinutes(t.getDuracionMinutos());
        return inicio.isBefore(finT) && finNuevo.isAfter(t.getFechaHora());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATOS DEMO
    // ─────────────────────────────────────────────────────────────────────────

    private List<Turno> crearTurnosDemo() {
        Instalacion inst1 = new Gimnasio(1, "GIMNASIO", 30, 25);
        Instalacion inst2 = new Piscina(2, "PISCINA", 15, 12, 6, 1.8);
        Persona up = SesionActual.getUsuario();
        Usuario u = (up instanceof Usuario) ? (Usuario) up
                : new Usuario(0, "Demo", "demo@test.com", "CC", "00000000", true, "A");
        Turno t1 = new Turno(1, LocalDateTime.now().plusDays(1).withHour(9),  60, u, inst1);
        Turno t2 = new Turno(2, LocalDateTime.now().plusDays(3).withHour(14), 45, u, inst2);
        Turno t3 = new Turno(3, LocalDateTime.now().minusDays(2).withHour(8), 30, u, inst1);
        try { t3.setEstado(Turno.ESTADO_COMPLETADO); } catch (Exception ignored) {}
        return Arrays.asList(t1, t2, t3);
    }

    private List<Pago> crearPagosDemo() {
        Persona user = SesionActual.getUsuario();
        int id = (user != null) ? user.getId() : 0;
        return Arrays.asList(
                new Pago(1L, 1, id, new BigDecimal("35000"), "EFECTIVO",  Pago.ESTADO_COMPLETADO, LocalDateTime.now().minusDays(2)),
                new Pago(2L, 2, id, new BigDecimal("42000"), "TARJETA",   Pago.ESTADO_PENDIENTE,  null));
    }

    private String safe(String v) { return v != null ? v : "—"; }

    @FXML
    private void onVerReporte() {
        generarReporteFacturas();
    }
    private void generarReporteFacturas() {
        // Método existente: lanza diálogo de selección si hay varias plantillas
        // (La lógica ya implementada en la versión anterior). Para compatibilidad
        // mantenemos el comportamiento actual: reusar el método sin argumento.
        // Si se desea forzar una plantilla concreta desde código, use
        // `generarReporteFacturas("RPTUsuarios.jrxml")`.
        try {
            // Llamamos al método que gestiona detección/selección/compilación
            // (la implementación que compila y muestra el reporte está en el mismo archivo
            // como sobrecarga que acepta el nombre de plantilla).
            // Aquí simplemente volvemos a usar la implementación sin parámetros.
            // (Se mantiene para compatibilidad con FXML handlers.)
        } catch (Exception ignored) {}
        // Reutiliza la versión anterior que ya detecta plantillas y abre ChoiceDialog.
        try (Connection con = Conexion.getInstancia().conectar()) {
            // Buscar plantillas disponibles
            List<String> candidates = Arrays.asList("RPTUsuarios.jrxml", "RPTUsuarios1.jrxml");
            List<String> available = new ArrayList<>();
            for (String name : candidates) {
                try (InputStream is = findReportStream(name)) { if (is != null) { available.add(name); continue; } }
                catch (Exception ignored) {}
                File f = new File(new File("").getAbsolutePath(), "src/reportes/" + name);
                if (f.exists()) available.add(name);
            }

            if (available.isEmpty()) {
                throw new IllegalStateException("No se encontraron plantillas de reporte (RPTUsuarios*.jrxml) en recursos ni en src/reportes.");
            }

            String chosen = available.get(0);
            if (available.size() > 1) {
                ChoiceDialog<String> dlg = new ChoiceDialog<>(available.get(0), available);
                dlg.setTitle("Seleccionar reporte");
                dlg.setHeaderText("Elige la plantilla de reporte a mostrar");
                dlg.setContentText("Reporte:");
                java.util.Optional<String> opt = dlg.showAndWait();
                if (opt.isPresent()) chosen = opt.get(); else return;
            }

            JasperReport jasperReport;
            // Intentar compilar desde classpath
            try (InputStream reportStream = findReportStream(chosen)) {
                if (reportStream != null) jasperReport = JasperCompileManager.compileReport(reportStream);
                else {
                    File jrxmlFile = new File(new File("").getAbsolutePath(), "src/reportes/" + chosen);
                    if (jrxmlFile.exists()) jasperReport = JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());
                    else throw new IllegalStateException("No se encontró el archivo de reporte seleccionado: " + chosen);
                }
            }

            Map<String, Object> params = new HashMap<>();
            params.put("idventa", 0);

            JasperPrint print = JasperFillManager.fillReport(jasperReport, params, con);
            final JasperPrint p = print;
            // Indicar estado en la UI JavaFX
            javafx.application.Platform.runLater(() -> { if (lblMsgPagos != null) lblMsgPagos.setText("Generando reporte..."); });
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    JasperViewer viewer = new JasperViewer(p, false);
                    viewer.setVisible(true);
                    try { viewer.toFront(); viewer.setAlwaysOnTop(true); } catch (Exception _e) {}
                    // Quitar always-on-top tras 600ms para no bloquear la UX
                    new java.util.Timer().schedule(new java.util.TimerTask() { public void run() { try { viewer.setAlwaysOnTop(false); } catch (Exception ignore) {} } }, 600);
                } catch (Exception ex) {
                    mostrarAlerta("Error", "No se pudo abrir el visor de reportes: " + ex.getMessage());
                } finally {
                    javafx.application.Platform.runLater(() -> { if (lblMsgPagos != null) lblMsgPagos.setText(""); });
                }
            });

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo generar el reporte de facturas: " + e.getMessage());
        }
    }

    // Sobrecarga: generar reporte usando una plantilla concreta (ej: "RPTUsuarios.jrxml")
    private void generarReporteFacturas(String chosen) {
        try (Connection con = Conexion.getInstancia().conectar()) {
            JasperReport jasperReport;
            try (InputStream reportStream = getClass().getResourceAsStream("/" + chosen)) {
                if (reportStream != null) {
                    jasperReport = JasperCompileManager.compileReport(reportStream);
                } else {
                    File jrxmlFile = new File(new File("").getAbsolutePath(), "src/reportes/" + chosen);
                    if (jrxmlFile.exists()) jasperReport = JasperCompileManager.compileReport(jrxmlFile.getAbsolutePath());
                    else throw new IllegalStateException("No se encontró el archivo de reporte seleccionado: " + chosen);
                }
            }

            Map<String, Object> params = new HashMap<>(); params.put("idventa", 0);
            JasperPrint print = JasperFillManager.fillReport(jasperReport, params, con);
            final JasperPrint p = print;
            javafx.application.Platform.runLater(() -> { if (lblMsgPagos != null) lblMsgPagos.setText("Generando reporte..."); });
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    JasperViewer viewer = new JasperViewer(p, false);
                    viewer.setVisible(true);
                    try { viewer.toFront(); viewer.setAlwaysOnTop(true); } catch (Exception _e) {}
                    new java.util.Timer().schedule(new java.util.TimerTask() { public void run() { try { viewer.setAlwaysOnTop(false); } catch (Exception ignore) {} } }, 600);
                } catch (Exception ex) { mostrarAlerta("Error", "No se pudo abrir el visor de reportes: " + ex.getMessage()); }
                finally { javafx.application.Platform.runLater(() -> { if (lblMsgPagos != null) lblMsgPagos.setText(""); }); }
            });
        } catch (Exception e) { mostrarAlerta("Error", "No se pudo generar el reporte: " + e.getMessage()); }
    }

    // Helper: buscar el stream de un reporte JRXML en distintas ubicaciones del classpath
    private InputStream findReportStream(String name) {
        InputStream is = getClass().getResourceAsStream("/" + name);
        if (is != null) return is;
        is = getClass().getResourceAsStream("/reportes/" + name);
        return is;
    }
}

package socratesGui;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import entidades.Gimnasio;
import entidades.Instalacion;
import entidades.Pago;
import entidades.Persona;
import entidades.Turno;
import entidades.Usuario;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class Dashboardusuariocontroller implements Initializable {

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Label  lblNombreUsuario;
    @FXML private Label  lblEmailUsuario;
    @FXML private Label  lblCategoriaUsuario;
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavMisTurnos;
    @FXML private Button btnNavMisPagos;
    @FXML private Button btnLogout;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label lblSeccion;
    @FXML private Label lblFecha;

    // ── Paneles ───────────────────────────────────────────────────────────────
    @FXML private VBox panelInicio;
    @FXML private VBox panelTurnos;
    @FXML private VBox panelPagos;

    // ── Panel Inicio ──────────────────────────────────────────────────────────
    @FXML private Label lblProximoTurno;
    @FXML private Label lblCountTurnos;
    @FXML private Label lblCountPagos;

    // ── Tabla Turnos ──────────────────────────────────────────────────────────
    @FXML private TableView<Turno>             tablaTurnos;
    @FXML private TableColumn<Turno, String>   colTurnoId;
    @FXML private TableColumn<Turno, String>   colTurnoFecha;
    @FXML private TableColumn<Turno, String>   colTurnoDuracion;
    @FXML private TableColumn<Turno, String>   colTurnoInstalacion;
    @FXML private TableColumn<Turno, String>   colTurnoCapacidad;
    @FXML private TableColumn<Turno, String>   colTurnoEstado;
    @FXML private Button                       btnCancelarTurno;
    @FXML private Label                        lblMsgTurnos;

    // ── Tabla Pagos ───────────────────────────────────────────────────────────
    @FXML private TableView<Pago>              tablaPagos;
    @FXML private TableColumn<Pago, String>    colPagoId;
    @FXML private TableColumn<Pago, String>    colPagoTurno;
    @FXML private TableColumn<Pago, String>    colPagoMonto;
    @FXML private TableColumn<Pago, String>    colPagoMetodo;
    @FXML private TableColumn<Pago, String>    colPagoEstado;
    @FXML private TableColumn<Pago, String>    colPagoFecha;
    @FXML private Label                        lblMsgPagos;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            lblFecha.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            Persona user = SesionActual.getUsuario();
            if (user != null) {
                lblNombreUsuario.setText(safe(user.getNombre()));
                lblEmailUsuario.setText(safe(user.getEmail()));
                if (user instanceof Usuario) {
                    String cat = ((Usuario) user).getCategoria();
                    lblCategoriaUsuario.setText(cat != null ? cat : "SIN CATEGORÍA");
                } else {
                    lblCategoriaUsuario.setText("—");
                }
            } else {
                lblNombreUsuario.setText("Usuario Prueba");
                lblEmailUsuario.setText("user@demo.com");
                lblCategoriaUsuario.setText("Premium");
            }

            configurarColumnasTurnos();
            configurarColumnasPagos();

            tablaTurnos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            cargarInicio();

            boolean modoDemo = (user == null || user.getId() == 0);
            if (modoDemo) {
                String msg = "(modo demo - sin conexión BD)";
                lblMsgTurnos.setText(msg);
                lblMsgPagos.setText(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONFIGURACIÓN DE COLUMNAS
    // ─────────────────────────────────────────────────────────────────────────

    private void configurarColumnasTurnos() {
        colTurnoId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colTurnoFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora() != null
                        ? c.getValue().getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—"));
        colTurnoDuracion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDuracionMinutos() + " min"));
        colTurnoInstalacion.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getInstalacion() != null
                        ? safe(c.getValue().getInstalacion().getTipo()) : "—"));
        colTurnoCapacidad.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getInstalacion() != null
                        ? String.valueOf(c.getValue().getInstalacion().getCapacidadMaxima()) : "—"));
        colTurnoEstado.setCellValueFactory(c ->
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
                        ? c.getValue().getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVEGACIÓN (@FXML handlers)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onInicio()    { mostrarPanel(panelInicio,  "Inicio");      cargarInicio(); }
    @FXML private void onMisTurnos() { mostrarPanel(panelTurnos,  "Mis Turnos");  cargarMisTurnos(); }
    @FXML private void onMisPagos()  { mostrarPanel(panelPagos,   "Mis Pagos");   cargarMisPagos(); }

    @FXML private void onLogout() {
        try {
            SesionActual.cerrar();
            App.setRoot("primary");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void onCancelarTurno() {
        Turno seleccionado = tablaTurnos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            lblMsgTurnos.setText("Seleccione un turno para cancelar.");
            return;
        }
        if (Turno.ESTADO_CANCELADO.equals(seleccionado.getEstado())) {
            lblMsgTurnos.setText("El turno ya está cancelado.");
            return;
        }
        Persona user = SesionActual.getUsuario();
        if (user != null && user.getId() != 0) {
            try {
                dao.TurnoDAO daoT = new dao.TurnoDAO(
                        new dao.PersonaDAO(), new dao.InstalacionDAO(), new dao.EntrenadorDAO());
                daoT.actualizarEstado(seleccionado.getIdTurno(), Turno.ESTADO_CANCELADO);
                lblMsgTurnos.setText("Turno #" + seleccionado.getIdTurno() + " cancelado correctamente.");
                cargarMisTurnos();
            } catch (Exception ex) {
                lblMsgTurnos.setText("Error al cancelar turno: " + ex.getMessage());
            }
        } else {
            // modo demo: simular cancelación
            lblMsgTurnos.setText("(Demo) Turno #" + seleccionado.getIdTurno() + " marcado como cancelado.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  mostrarPanel
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarPanel(VBox panelActivo, String titulo) {
        List<VBox> paneles = Arrays.asList(panelInicio, panelTurnos, panelPagos);
        for (VBox p : paneles) {
            p.setVisible(false);
            p.setManaged(false);
        }
        panelActivo.setVisible(true);
        panelActivo.setManaged(true);
        lblSeccion.setText(titulo);

        FadeTransition ft = new FadeTransition(Duration.millis(250), panelActivo);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CARGA DE DATOS
    // ─────────────────────────────────────────────────────────────────────────

    private void cargarInicio() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Turno> turnos;
            List<Pago>  pagos;

            if (user == null || user.getId() == 0) {
                turnos = crearTurnosDemo();
                pagos  = crearPagosDemo();
            } else {
                try {
                    dao.TurnoDAO daoT = new dao.TurnoDAO(
                            new dao.PersonaDAO(), new dao.InstalacionDAO(), new dao.EntrenadorDAO());
                    turnos = daoT.listarPorUsuario(user.getId());
                    if (turnos == null) turnos = crearTurnosDemo();
                } catch (Exception ex) {
                    turnos = crearTurnosDemo();
                }
                try {
                    dao.PagoDAO daoP = new dao.PagoDAO();
                    pagos = daoP.listarPorUsuario(user.getId());
                    if (pagos == null) pagos = crearPagosDemo();
                } catch (Exception ex) {
                    pagos = crearPagosDemo();
                }
            }

            // Turnos activos (RESERVADO)
            final List<Turno> turnosFinal = turnos;
            long activos = turnosFinal.stream()
                    .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())).count();
            lblCountTurnos.setText(String.valueOf(activos));

            // Pagos pendientes
            final List<Pago> pagosFinal = pagos;
            long pendientes = pagosFinal.stream()
                    .filter(p -> Pago.ESTADO_PENDIENTE.equals(p.getEstadoPago())).count();
            lblCountPagos.setText(String.valueOf(pendientes));

            // Próximo turno (el más cercano al futuro con estado RESERVADO)
            turnosFinal.stream()
                    .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())
                            && t.getFechaHora() != null
                            && t.getFechaHora().isAfter(LocalDateTime.now()))
                    .min((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()))
                    .ifPresentOrElse(
                            t -> {
                                String tipo = t.getInstalacion() != null ? t.getInstalacion().getTipo() : "—";
                                String fecha = t.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                                lblProximoTurno.setText(tipo + " — " + fecha + " (" + t.getDuracionMinutos() + " min)");
                            },
                            () -> lblProximoTurno.setText("Sin turnos próximos")
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarMisTurnos() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Turno> turnos;

            if (user == null || user.getId() == 0) {
                turnos = crearTurnosDemo();
            } else {
                try {
                    dao.TurnoDAO daoT = new dao.TurnoDAO(
                            new dao.PersonaDAO(), new dao.InstalacionDAO(), new dao.EntrenadorDAO());
                    turnos = daoT.listarPorUsuario(user.getId());
                    if (turnos == null) turnos = crearTurnosDemo();
                } catch (Exception ex) {
                    turnos = crearTurnosDemo();
                    lblMsgTurnos.setText("Error BD - mostrando demo");
                }
            }
            tablaTurnos.setItems(FXCollections.observableArrayList(turnos));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarMisPagos() {
        try {
            Persona user = SesionActual.getUsuario();
            List<Pago> pagos;

            if (user == null || user.getId() == 0) {
                pagos = crearPagosDemo();
            } else {
                try {
                    dao.PagoDAO daoP = new dao.PagoDAO();
                    pagos = daoP.listarPorUsuario(user.getId());
                    if (pagos == null) pagos = crearPagosDemo();
                } catch (Exception ex) {
                    pagos = crearPagosDemo();
                    lblMsgPagos.setText("Error BD - mostrando demo");
                }
            }
            tablaPagos.setItems(FXCollections.observableArrayList(pagos));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATOS DEMO
    // ─────────────────────────────────────────────────────────────────────────

    private List<Turno> crearTurnosDemo() {
        Instalacion inst1 = new Gimnasio(1, "GIMNASIO", 30, 25);
        Instalacion inst2 = new entidades.Piscina(2, "PISCINA", 15, 12, 6, 1.8);

        Persona userActual = SesionActual.getUsuario();
        Usuario u = (userActual instanceof Usuario)
                ? (Usuario) userActual
                : new Usuario(0, "Usuario Demo", "demo@test.com", "CC", "00000000", true, "A");

        Turno t1 = new Turno(1, LocalDateTime.now().plusDays(1).withHour(9),  60, u, inst1);
        Turno t2 = new Turno(2, LocalDateTime.now().plusDays(3).withHour(14), 45, u, inst2);
        Turno t3 = new Turno(3, LocalDateTime.now().minusDays(2).withHour(8), 30, u, inst1);
        try { t3.setEstado(Turno.ESTADO_COMPLETADO); } catch (Exception ignored) {}

        return Arrays.asList(t1, t2, t3);
    }

    private List<Pago> crearPagosDemo() {
        Persona user = SesionActual.getUsuario();
        int idUser = (user != null) ? user.getId() : 0;

        Pago p1 = new Pago(1L, 1, idUser, new BigDecimal("35000"), "EFECTIVO",      Pago.ESTADO_COMPLETADO, LocalDateTime.now().minusDays(2));
        Pago p2 = new Pago(2L, 2, idUser, new BigDecimal("42000"), "TARJETA",        Pago.ESTADO_PENDIENTE,  null);
        return Arrays.asList(p1, p2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    private String safe(String value) {
        return value != null ? value : "—";
    }
}

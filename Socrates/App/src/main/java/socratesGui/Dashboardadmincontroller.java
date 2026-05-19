package socratesGui;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import entidades.Administrador;
import entidades.Entrenador;
import entidades.Gimnasio;
import entidades.Instalacion;
import entidades.Pago;
import entidades.Persona;
import entidades.Sede;
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

public class Dashboardadmincontroller implements Initializable {

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Label  lblNombreAdmin;
    @FXML private Label  lblRolAdmin;
    @FXML private Button btnNavInicio;
    @FXML private Button btnNavUsuarios;
    @FXML private Button btnNavTurnos;
    @FXML private Button btnNavInstalaciones;
    @FXML private Button btnNavPagos;
    @FXML private Button btnNavEntrenadores;
    @FXML private Button btnNavSedes;
    @FXML private Button btnLogout;

    // ── Header ────────────────────────────────────────────────────────────────
    @FXML private Label lblSeccion;
    @FXML private Label lblFecha;

    // ── Paneles ───────────────────────────────────────────────────────────────
    @FXML private VBox panelInicio;
    @FXML private VBox panelUsuarios;
    @FXML private VBox panelTurnos;
    @FXML private VBox panelInstalaciones;
    @FXML private VBox panelPagos;
    @FXML private VBox panelEntrenadores;
    @FXML private VBox panelSedes;

    // ── Inicio ────────────────────────────────────────────────────────────────
    @FXML private Label lblTotalUsuarios;
    @FXML private Label lblTotalTurnos;
    @FXML private Label lblTurnosActivos;
    @FXML private Label lblTotalEntrenadores;

    // ── Tabla Usuarios ────────────────────────────────────────────────────────
    @FXML private TableView<Persona>                tablaUsuarios;
    @FXML private TableColumn<Persona, String>      colUsuarioId;
    @FXML private TableColumn<Persona, String>      colUsuarioNombre;
    @FXML private TableColumn<Persona, String>      colUsuarioEmail;
    @FXML private TableColumn<Persona, String>      colUsuarioDocumento;
    @FXML private TableColumn<Persona, String>      colUsuarioRol;
    @FXML private TableColumn<Persona, String>      colUsuarioCategoria;
    @FXML private Label                             lblMsgUsuarios;

    // ── Tabla Turnos ──────────────────────────────────────────────────────────
    @FXML private TableView<Turno>                  tablaTurnos;
    @FXML private TableColumn<Turno, String>        colTurnoId;
    @FXML private TableColumn<Turno, String>        colTurnoFecha;
    @FXML private TableColumn<Turno, String>        colTurnoDuracion;
    @FXML private TableColumn<Turno, String>        colTurnoInstalacion;
    @FXML private TableColumn<Turno, String>        colTurnoCapacidad;
    @FXML private TableColumn<Turno, String>        colTurnoEstado;
    @FXML private TableColumn<Turno, String>        colTurnoUsuario;
    @FXML private Label                             lblMsgTurnos;

    // ── Tabla Instalaciones ───────────────────────────────────────────────────
    @FXML private TableView<Instalacion>            tablaInstalaciones;
    @FXML private TableColumn<Instalacion, String>  colInstalacionId;
    @FXML private TableColumn<Instalacion, String>  colInstalacionTipo;
    @FXML private TableColumn<Instalacion, String>  colInstalacionCapacidad;
    @FXML private TableColumn<Instalacion, String>  colInstalacionAforo;
    @FXML private TableColumn<Instalacion, String>  colInstalacionTipoEsp;
    @FXML private Label                             lblMsgInstalaciones;

    // ── Tabla Pagos ───────────────────────────────────────────────────────────
    @FXML private TableView<Pago>                   tablaPagos;
    @FXML private TableColumn<Pago, String>         colPagoId;
    @FXML private TableColumn<Pago, String>         colPagoTurno;
    @FXML private TableColumn<Pago, String>         colPagoUsuario;
    @FXML private TableColumn<Pago, String>         colPagoMonto;
    @FXML private TableColumn<Pago, String>         colPagoMetodo;
    @FXML private TableColumn<Pago, String>         colPagoEstado;
    @FXML private TableColumn<Pago, String>         colPagoFecha;
    @FXML private Label                             lblMsgPagos;

    // ── Tabla Entrenadores ────────────────────────────────────────────────────
    @FXML private TableView<Entrenador>             tablaEntrenadores;
    @FXML private TableColumn<Entrenador, String>   colEntrenadorId;
    @FXML private TableColumn<Entrenador, String>   colEntrenadorNombre;
    @FXML private TableColumn<Entrenador, String>   colEntrenadorEspecialidad;
    @FXML private TableColumn<Entrenador, String>   colEntrenadorEmail;
    @FXML private Label                             lblMsgEntrenadores;

    // ── Tabla Sedes ───────────────────────────────────────────────────────────
    @FXML private TableView<Sede>                   tablaSedesAdmin;
    @FXML private TableColumn<Sede, String>         colSedeId;
    @FXML private TableColumn<Sede, String>         colSedeNombre;
    @FXML private TableColumn<Sede, String>         colSedeDireccion;
    @FXML private TableColumn<Sede, String>         colSedeTelefono;
    @FXML private TableColumn<Sede, String>         colSedeEmail;
    @FXML private Label                             lblMsgSedes;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            lblFecha.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            Persona admin = SesionActual.getUsuario();
            if (admin != null) {
                lblNombreAdmin.setText(admin.getNombre() != null ? admin.getNombre() : "Admin");
                lblRolAdmin.setText(admin.getRol() != null ? admin.getRol() : "ADMINISTRADOR");
            } else {
                lblNombreAdmin.setText("Admin Prueba");
                lblRolAdmin.setText("ADMINISTRADOR");
            }

            configurarColumnasUsuarios();
            configurarColumnasTurnos();
            configurarColumnasInstalaciones();
            configurarColumnasPagos();
            configurarColumnasEntrenadores();
            configurarColumnasSedes();

            tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaTurnos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaInstalaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaEntrenadores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tablaSedesAdmin.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            cargarInicio();

            boolean modoDemo = (admin == null || admin.getId() == 0);
            if (modoDemo) {
                String msg = "(modo demo - sin conexión BD)";
                lblMsgUsuarios.setText(msg);
                lblMsgTurnos.setText(msg);
                lblMsgInstalaciones.setText(msg);
                lblMsgPagos.setText(msg);
                lblMsgEntrenadores.setText(msg);
                lblMsgSedes.setText(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONFIGURACIÓN DE COLUMNAS
    // ─────────────────────────────────────────────────────────────────────────

    private void configurarColumnasUsuarios() {
        colUsuarioId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colUsuarioNombre.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getNombre())));
        colUsuarioEmail.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEmail())));
        colUsuarioDocumento.setCellValueFactory(c -> {
            if (c.getValue() instanceof Usuario) {
                return new SimpleStringProperty(safe(((Usuario) c.getValue()).getNumDocumento()));
            }
            if (c.getValue() instanceof Administrador) {
                return new SimpleStringProperty(safe(((Administrador) c.getValue()).getNumDocumento()));
            }
            return new SimpleStringProperty("—");
        });
        colUsuarioRol.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getRol())));
        colUsuarioCategoria.setCellValueFactory(c -> {
            if (c.getValue() instanceof Usuario) {
                return new SimpleStringProperty(safe(((Usuario) c.getValue()).getCategoria()));
            }
            return new SimpleStringProperty("N/A");
        });
    }

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
        colTurnoUsuario.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsuario() != null
                        ? safe(c.getValue().getUsuario().getNombre()) : "—"));
    }

    private void configurarColumnasInstalaciones() {
        colInstalacionId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdInstalacion())));
        colInstalacionTipo.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getTipo())));
        colInstalacionCapacidad.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getCapacidadMaxima())));
        colInstalacionAforo.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getAforoActual())));
        colInstalacionTipoEsp.setCellValueFactory(c -> {
            String subtipo = c.getValue().getClass().getSimpleName();
            return new SimpleStringProperty(subtipo);
        });
    }

    private void configurarColumnasPagos() {
        colPagoId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdPago())));
        colPagoTurno.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colPagoUsuario.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdUsuario())));
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

    private void configurarColumnasEntrenadores() {
        colEntrenadorId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colEntrenadorNombre.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getNombre())));
        colEntrenadorEspecialidad.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEspecialidad())));
        colEntrenadorEmail.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEmail())));
    }

    private void configurarColumnasSedes() {
        colSedeId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdSede())));
        colSedeNombre.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getNombre())));
        colSedeDireccion.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getDireccion())));
        colSedeTelefono.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getTelefono())));
        colSedeEmail.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getEmail())));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVEGACIÓN (@FXML handlers)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onInicio()         { mostrarPanel(panelInicio,        "Inicio");        cargarInicio(); }
    @FXML private void onUsuarios()       { mostrarPanel(panelUsuarios,      "Usuarios");      cargarUsuarios(); }
    @FXML private void onTurnos()         { mostrarPanel(panelTurnos,        "Turnos");        cargarTurnos(); }
    @FXML private void onInstalaciones()  { mostrarPanel(panelInstalaciones, "Instalaciones"); cargarInstalaciones(); }
    @FXML private void onPagos()          { mostrarPanel(panelPagos,         "Pagos");         cargarPagos(); }
    @FXML private void onEntrenadores()   { mostrarPanel(panelEntrenadores,  "Entrenadores");  cargarEntrenadores(); }
    @FXML private void onSedes()          { mostrarPanel(panelSedes,         "Sedes");         cargarSedes(); }

    @FXML private void onLogout() {
        try {
            SesionActual.cerrar();
            App.setRoot("primary");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  mostrarPanel
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarPanel(VBox panelActivo, String titulo) {
        List<VBox> paneles = Arrays.asList(
                panelInicio, panelUsuarios, panelTurnos,
                panelInstalaciones, panelPagos, panelEntrenadores, panelSedes);
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
            List<Turno>  turnosDemo  = crearTurnosDemo();
            List<Persona> usuariosDemo = crearUsuariosDemo();
            List<Entrenador> entDemo = crearEntrenadoresDemo();

            lblTotalUsuarios.setText(String.valueOf(usuariosDemo.size()));
            lblTotalTurnos.setText(String.valueOf(turnosDemo.size()));
            long activos = turnosDemo.stream()
                    .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado())).count();
            lblTurnosActivos.setText(String.valueOf(activos));
            lblTotalEntrenadores.setText(String.valueOf(entDemo.size()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarUsuarios() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaUsuarios.setItems(FXCollections.observableArrayList(crearUsuariosDemo()));
            } else {
                try {
                    dao.PersonaDAO daoP = new dao.PersonaDAO();
                    // listar(filtro, totalPorPagina, numPagina) — 200 registros, página 1
                    List<Persona> lista = daoP.listar("", 200, 1);
                    tablaUsuarios.setItems(FXCollections.observableArrayList(
                            lista != null ? lista : crearUsuariosDemo()));
                } catch (Exception ex) {
                    tablaUsuarios.setItems(FXCollections.observableArrayList(crearUsuariosDemo()));
                    lblMsgUsuarios.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarTurnos() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaTurnos.setItems(FXCollections.observableArrayList(crearTurnosDemo()));
            } else {
                try {
                    dao.TurnoDAO daoT = new dao.TurnoDAO(new dao.PersonaDAO(), new dao.InstalacionDAO(), new dao.EntrenadorDAO());
                    List<Turno> lista = daoT.listarTodos();
                    tablaTurnos.setItems(FXCollections.observableArrayList(
                            lista != null ? lista : crearTurnosDemo()));
                } catch (Exception ex) {
                    tablaTurnos.setItems(FXCollections.observableArrayList(crearTurnosDemo()));
                    lblMsgTurnos.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarInstalaciones() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaInstalaciones.setItems(FXCollections.observableArrayList(crearInstalacionesDemo()));
            } else {
                try {
                    dao.InstalacionDAO daoI = new dao.InstalacionDAO();
                    List<Instalacion> lista = daoI.listarTodos();
                    tablaInstalaciones.setItems(FXCollections.observableArrayList(
                            lista != null ? lista : crearInstalacionesDemo()));
                } catch (Exception ex) {
                    tablaInstalaciones.setItems(FXCollections.observableArrayList(crearInstalacionesDemo()));
                    lblMsgInstalaciones.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarPagos() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaPagos.setItems(FXCollections.observableArrayList(crearPagosDemo()));
            } else {
                try {
                    // PagoDAO no expone listarTodos(); usamos demo para vista admin
                    // Para integración real, agregar listarTodos() en PagoDAO/IPagoDAO
                    tablaPagos.setItems(FXCollections.observableArrayList(crearPagosDemo()));
                    lblMsgPagos.setText("(Vista admin: conecte listarTodos() en PagoDAO para datos reales)");
                } catch (Exception ex) {
                    tablaPagos.setItems(FXCollections.observableArrayList(crearPagosDemo()));
                    lblMsgPagos.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarEntrenadores() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaEntrenadores.setItems(FXCollections.observableArrayList(crearEntrenadoresDemo()));
            } else {
                try {
                    dao.EntrenadorDAO daoE = new dao.EntrenadorDAO();
                    // listar(filtro, totalPorPagina, numPagina)
                    List<Entrenador> lista = daoE.listar("", 200, 1);
                    tablaEntrenadores.setItems(FXCollections.observableArrayList(
                            lista != null ? lista : crearEntrenadoresDemo()));
                } catch (Exception ex) {
                    tablaEntrenadores.setItems(FXCollections.observableArrayList(crearEntrenadoresDemo()));
                    lblMsgEntrenadores.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarSedes() {
        try {
            Persona admin = SesionActual.getUsuario();
            if (admin == null || admin.getId() == 0) {
                tablaSedesAdmin.setItems(FXCollections.observableArrayList(crearSedesDemo()));
            } else {
                try {
                    dao.SedeDAO daoS = new dao.SedeDAO();
                    List<Sede> lista = daoS.listarTodos();
                    tablaSedesAdmin.setItems(FXCollections.observableArrayList(
                            lista != null ? lista : crearSedesDemo()));
                } catch (Exception ex) {
                    tablaSedesAdmin.setItems(FXCollections.observableArrayList(crearSedesDemo()));
                    lblMsgSedes.setText("Error BD - mostrando demo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATOS DEMO
    // ─────────────────────────────────────────────────────────────────────────

    private List<Persona> crearUsuariosDemo() {
        Usuario u1 = new Usuario(1, "María García",    "maria@demo.com",  "CC", "12345678", true,  "A");
        Usuario u2 = new Usuario(2, "Carlos López",    "carlos@demo.com", "CC", "87654321", true,  "B");
        Usuario u3 = new Usuario(3, "Ana Martínez",    "ana@demo.com",    "CC", "11223344", false, "NO AFILIADO");

        Administrador a1 = new Administrador(10, "Admin Principal", "admin@demo.com", null, "CC", "99999999");
        return Arrays.asList(u1, u2, u3, a1);
    }

    private List<Instalacion> crearInstalacionesDemo() {
        Instalacion i1 = new Gimnasio(1, "GIMNASIO", 30, 25);
        Instalacion i2 = new Gimnasio(2, "GIMNASIO", 20, 18);
        Instalacion i3 = new entidades.Piscina(3, "PISCINA", 15, 12, 6, 1.8);
        return Arrays.asList(i1, i2, i3);
    }

    private List<Turno> crearTurnosDemo() {
        Instalacion inst1 = new Gimnasio(1, "GIMNASIO", 30, 25);
        Instalacion inst2 = new entidades.Piscina(2, "PISCINA", 15, 12, 6, 1.8);

        Usuario u1 = new Usuario(1, "María García", "maria@demo.com", "CC", "12345678", true, "A");
        Usuario u2 = new Usuario(2, "Carlos López", "carlos@demo.com", "CC", "87654321", true, "B");
        Usuario u3 = new Usuario(3, "Ana Martínez", "ana@demo.com",   "CC", "11223344", false, "NO AFILIADO");

        Turno t1 = new Turno(1, LocalDateTime.now().plusDays(1),  60, u1, inst1);
        Turno t2 = new Turno(2, LocalDateTime.now().plusDays(2),  45, u2, inst2);
        Turno t3 = new Turno(3, LocalDateTime.now().minusDays(1), 30, u3, inst1);
        try { t3.setEstado(Turno.ESTADO_COMPLETADO); } catch (Exception ignored) {}

        return Arrays.asList(t1, t2, t3);
    }

    private List<Pago> crearPagosDemo() {
        Pago p1 = new Pago(1L, 1, 1, new BigDecimal("35000"), "EFECTIVO",     Pago.ESTADO_COMPLETADO, LocalDateTime.now().minusDays(1));
        Pago p2 = new Pago(2L, 2, 2, new BigDecimal("42000"), "TARJETA",      Pago.ESTADO_PENDIENTE,  null);
        Pago p3 = new Pago(3L, 3, 3, new BigDecimal("28000"), "TRANSFERENCIA",Pago.ESTADO_COMPLETADO, LocalDateTime.now().minusDays(3));
        return Arrays.asList(p1, p2, p3);
    }

    private List<Entrenador> crearEntrenadoresDemo() {
        Entrenador e1 = new Entrenador("Juan Pérez",    "juan@gym.com",    "Gimnasio",  "CC", "55556666", 1);
        Entrenador e2 = new Entrenador("Laura Sánchez", "laura@swim.com",  "Natación",  "CC", "77778888", 2);
        Entrenador e3 = new Entrenador("Pedro Ruiz",    "pedro@gym.com",   "Gimnasio",  "CC", "99990000", 3);
        return Arrays.asList(e1, e2, e3);
    }

    private List<Sede> crearSedesDemo() {
        Sede s1 = new Sede(1, "Sede Norte",   "Calle 100 # 15-30", "601-555-1111", "norte@socrates.com");
        Sede s2 = new Sede(2, "Sede Sur",     "Calle 40 # 68-20",  "601-555-2222", "sur@socrates.com");
        Sede s3 = new Sede(3, "Sede Centro",  "Carrera 7 # 32-10", "601-555-3333", "centro@socrates.com");
        return Arrays.asList(s1, s2, s3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILIDADES
    // ─────────────────────────────────────────────────────────────────────────

    private String safe(String value) {
        return value != null ? value : "—";
    }
}

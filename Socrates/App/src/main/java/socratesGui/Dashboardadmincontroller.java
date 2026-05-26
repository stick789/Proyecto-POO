package socratesGui;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import dao.EntrenadorDAO;
import dao.InstalacionDAO;
import dao.PagoDAO;
import dao.PersonaDAO;
import dao.SedeDAO;
import dao.TurnoDAO;
import database.Conexion;
import entidades.Administrador;
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
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import negocio.AdminService;
import negocio.PersonaControl;

public class Dashboardadmincontroller implements Initializable {

    // ── SUPER ADMIN ID — solo este usuario puede crear admins y entrenadores ──
    private static final int SUPER_ADMIN_ID = 1;

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private Label  lblNombreAdmin;
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
    @FXML private Label lblCancelacionesMes;
    @FXML private Label lblIngresosMes;

    // ── Tabla Usuarios ────────────────────────────────────────────────────────
    @FXML private TableView<Persona>                tablaUsuarios;
    @FXML private TableColumn<Persona, String>      colUsuarioId;
    @FXML private TableColumn<Persona, String>      colUsuarioNombre;
    @FXML private TableColumn<Persona, String>      colUsuarioEmail;
    @FXML private TableColumn<Persona, String>      colUsuarioDocumento;
    @FXML private TableColumn<Persona, String>      colUsuarioEstado;
    @FXML private TableColumn<Persona, String>      colUsuarioAcciones;
    @FXML private Label                             lblMsgUsuarios;
    @FXML private TextField                         txtBuscarUsuario;

    // ── Tabla Turnos ──────────────────────────────────────────────────────────
    @FXML private TableView<Turno>                  tablaTurnos;
    @FXML private TableColumn<Turno, String>        colTurnoId;
    @FXML private TableColumn<Turno, String>        colTurnoFecha;
    @FXML private TableColumn<Turno, String>        colTurnoDuracion;
    @FXML private TableColumn<Turno, String>        colTurnoInstalacion;
    @FXML private TableColumn<Turno, String>        colTurnoCapacidad;
    @FXML private TableColumn<Turno, String>        colTurnoEstado;
    @FXML private TableColumn<Turno, String>        colTurnoUsuario;
    @FXML private TableColumn<Turno, String>        colTurnoAcciones;
    @FXML private Label                             lblMsgTurnos;

    // ── Tabla Instalaciones ───────────────────────────────────────────────────
    @FXML private TableView<Instalacion>            tablaInstalaciones;
    @FXML private TableColumn<Instalacion, String>  colInstalacionId;
    @FXML private TableColumn<Instalacion, String>  colInstalacionNombre;
    @FXML private TableColumn<Instalacion, String>  colInstalacionTipo;
    @FXML private TableColumn<Instalacion, String>  colInstalacionCapacidad;
    @FXML private TableColumn<Instalacion, String>  colInstalacionAforo;
    @FXML private TableColumn<Instalacion, String>  colInstalacionEstado;
    @FXML private TableColumn<Instalacion, String>  colInstalacionAcciones;
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
    @FXML private TableColumn<Sede, String>         colSedeAcciones;
    @FXML private Label                             lblMsgSedes;

    // ── Estado ────────────────────────────────────────────────────────────────
    private AdminService adminService;
    private int adminId = 0;
    private boolean esSuperAdmin = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            adminService = new AdminService();
            lblFecha.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            Persona admin = SesionActual.getUsuario();
            if (admin != null) {
                adminId      = admin.getId();
                esSuperAdmin = (adminId == SUPER_ADMIN_ID);
                lblNombreAdmin.setText(admin.getNombre() != null ? admin.getNombre() : "Admin");
            } else {
                lblNombreAdmin.setText("Admin Prueba");
            }

            configurarColumnasUsuarios();
            configurarColumnasTurnos();
            configurarColumnasInstalaciones();
            configurarColumnasPagos();
            configurarColumnasEntrenadores();
            configurarColumnasSedes();

            for (TableView<?> tv : Arrays.asList(tablaUsuarios, tablaTurnos,
                    tablaInstalaciones, tablaPagos, tablaEntrenadores, tablaSedesAdmin)) {
                tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }

            if (txtBuscarUsuario != null) {
                txtBuscarUsuario.textProperty().addListener((obs, o, n) -> filtrarUsuarios(n));
            }

            cargarInicio();

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
            if (c.getValue() instanceof Usuario)
                return new SimpleStringProperty(safe(((Usuario) c.getValue()).getNumDocumento()));
            if (c.getValue() instanceof Administrador)
                return new SimpleStringProperty(safe(((Administrador) c.getValue()).getNumDocumento()));
            return new SimpleStringProperty("—");
        });
        if (colUsuarioEstado != null)
            colUsuarioEstado.setCellValueFactory(c -> new SimpleStringProperty("ACTIVO"));

        if (colUsuarioAcciones != null) {
            colUsuarioAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnToggle   = new Button();
                private final Button btnEliminar = new Button("Eliminar");
                private final HBox   box         = new HBox(4, btnToggle, btnEliminar);
                {
                    estiloBtnPequeno(btnToggle, "#16a34a");
                    estiloBtnPequeno(btnEliminar, "#dc2626");
                    box.setAlignment(Pos.CENTER);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    Persona p = (Persona) getTableRow().getItem();
                    if (p.getId() == adminId) {
                        btnToggle.setText("(yo)");
                        btnToggle.setDisable(true);
                        btnEliminar.setDisable(true);
                    } else {
                        btnToggle.setDisable(false);
                        btnEliminar.setDisable(false);
                        btnToggle.setText("Desactivar");
                        btnToggle.setOnAction(e -> accionToggleUsuario(p));
                        btnEliminar.setOnAction(e -> accionEliminarUsuario(p));
                    }
                    setGraphic(box);
                }
            });
        }
    }

    private void configurarColumnasTurnos() {
        colTurnoId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdTurno())));
        colTurnoFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora() != null
                        ? c.getValue().getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"));
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
        if (colTurnoAcciones != null) {
            colTurnoAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnCancelar = new Button("Cancelar");
                {
                    estiloBtnPequeno(btnCancelar, "#dc2626");
                    btnCancelar.setOnAction(e -> {
                        Turno t = getTableRow() != null ? (Turno) getTableRow().getItem() : null;
                        if (t != null) accionCancelarTurno(t);
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                    Turno t = (Turno) getTableRow().getItem();
                    btnCancelar.setDisable(!"RESERVADO".equals(t.getEstado()));
                    setGraphic(btnCancelar);
                }
            });
        }
    }

    private void configurarColumnasInstalaciones() {
        colInstalacionId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getIdInstalacion())));
        if (colInstalacionNombre != null)
            colInstalacionNombre.setCellValueFactory(c ->
                    new SimpleStringProperty(safe(c.getValue().getClass().getSimpleName())));
        colInstalacionTipo.setCellValueFactory(c ->
                new SimpleStringProperty(safe(c.getValue().getTipo())));
        colInstalacionCapacidad.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getCapacidadMaxima())));
        colInstalacionAforo.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getAforoActual())));
        if (colInstalacionEstado != null)
            colInstalacionEstado.setCellValueFactory(c -> new SimpleStringProperty("ABIERTA"));
        if (colInstalacionAcciones != null) {
            colInstalacionAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEditar = new Button("Editar");
                private final Button btnCerrar = new Button("Cerrar");
                private final HBox   box       = new HBox(4, btnEditar, btnCerrar);
                {
                    estiloBtnPequeno(btnEditar, "#2563eb");
                    estiloBtnPequeno(btnCerrar, "#d97706");
                    box.setAlignment(Pos.CENTER);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                    Instalacion inst = (Instalacion) getTableRow().getItem();
                    btnEditar.setOnAction(e -> accionEditarInstalacion(inst));
                    btnCerrar.setOnAction(e -> accionCerrarInstalacion(inst));
                    setGraphic(box);
                }
            });
        }
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
                        ? c.getValue().getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"));
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
        if (colSedeAcciones != null) {
            colSedeAcciones.setCellFactory(col -> new TableCell<>() {
                private final Button btnEditar = new Button("Editar");
                {
                    estiloBtnPequeno(btnEditar, "#2563eb");
                    btnEditar.setOnAction(e -> {
                        Sede s = getTableRow() != null ? (Sede) getTableRow().getItem() : null;
                        if (s != null) accionEditarSede(s);
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : btnEditar);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NAVEGACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void onInicio()        { mostrarPanel(panelInicio,        "Inicio");        cargarInicio(); }
    @FXML private void onTurnos()        { mostrarPanel(panelTurnos,        "Turnos");        cargarTurnos(); }
    @FXML private void onInstalaciones() { mostrarPanel(panelInstalaciones, "Instalaciones"); cargarInstalaciones(); }
    @FXML private void onPagos()         { mostrarPanel(panelPagos,         "Pagos");         cargarPagos(); }
    @FXML private void onSedes()         { mostrarPanel(panelSedes,         "Sedes");         cargarSedes(); }

    // FIX: inyectarBotonesSuperAdmin() ahora se llama aquí, donde viven los botones
    @FXML private void onUsuarios() {
        mostrarPanel(panelUsuarios, "Usuarios");
        cargarUsuarios();
        inyectarBotonesSuperAdmin();
    }

    // Entrenadores no necesita inyectar botones — solo navega y carga
    @FXML private void onEntrenadores() {
        mostrarPanel(panelEntrenadores, "Entrenadores");
        cargarEntrenadores();
    }

    // FIX: verifica panelUsuarios (correcto), ya no se llama desde onEntrenadores
    private void inyectarBotonesSuperAdmin() {
        if (!esSuperAdmin) return;
        boolean yaAgregado = panelUsuarios.getChildren().stream()
                .anyMatch(n -> "superAdminBar".equals(n.getId()));
        if (yaAgregado) return;

        Button btnNuevoEntrenador = new Button("➕  Agregar Entrenador");
        Button btnNuevoAdmin      = new Button("🔐  Crear Administrador");
        estiloBtnPequeno(btnNuevoEntrenador, "#E85D04");
        estiloBtnPequeno(btnNuevoAdmin,      "#7c3aed");
        btnNuevoEntrenador.setStyle(btnNuevoEntrenador.getStyle() + " -fx-font-size: 13; -fx-padding: 8 16;");
        btnNuevoAdmin.setStyle(btnNuevoAdmin.getStyle()           + " -fx-font-size: 13; -fx-padding: 8 16;");
        btnNuevoEntrenador.setOnAction(e -> dialogoCrearEntrenador());
        btnNuevoAdmin.setOnAction(e -> dialogoCrearAdministrador());

        HBox bar = new HBox(12, btnNuevoEntrenador, btnNuevoAdmin);
        bar.setId("superAdminBar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 8, 0));

        panelUsuarios.getChildren().add(1, bar);
    }

    @FXML private void onLogout() {
        try {
            SesionActual.cerrar();
            App.setRoot("primary");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarPanel(VBox panelActivo, String titulo) {
        List<VBox> paneles = Arrays.asList(
                panelInicio, panelUsuarios, panelTurnos,
                panelInstalaciones, panelPagos, panelEntrenadores, panelSedes);
        for (VBox p : paneles) { p.setVisible(false); p.setManaged(false); }
        panelActivo.setVisible(true);
        panelActivo.setManaged(true);
        lblSeccion.setText(titulo);
        FadeTransition ft = new FadeTransition(Duration.millis(250), panelActivo);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPER ADMIN — CREAR ENTRENADOR
    //  FIX: sin campo contraseña — se genera un hash bloqueado con UUID
    // ─────────────────────────────────────────────────────────────────────────

    private void dialogoCrearEntrenador() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agregar Entrenador");
        dialog.setHeaderText("Nuevo entrenador — solo visible para el Super Admin");

        TextField        txtNombre  = new TextField();  txtNombre.setPromptText("Nombre completo");
        TextField        txtEmail   = new TextField();  txtEmail.setPromptText("Correo electrónico");
        ComboBox<String> cmbTipoDoc = new ComboBox<>(FXCollections.observableArrayList("CC","TI","CE","PP","NIT"));
        cmbTipoDoc.setPromptText("Tipo doc.");
        TextField        txtNumDoc  = new TextField();  txtNumDoc.setPromptText("Número de documento");
        ComboBox<String> cmbEspec   = new ComboBox<>(FXCollections.observableArrayList("Gimnasio","Natación"));
        cmbEspec.setPromptText("Especialidad");

        GridPane grid = crearGrid();
        grid.add(new Label("Nombre:"),       0, 0); grid.add(txtNombre,  1, 0);
        grid.add(new Label("Email:"),        0, 1); grid.add(txtEmail,   1, 1);
        grid.add(new Label("Tipo doc.:"),    0, 2); grid.add(cmbTipoDoc, 1, 2);
        grid.add(new Label("Num. doc.:"),    0, 3); grid.add(txtNumDoc,  1, 3);
        grid.add(new Label("Especialidad:"), 0, 4); grid.add(cmbEspec,   1, 4);

        Label lblMsg = new Label();
        lblMsg.setStyle("-fx-text-fill: #dc2626;");
        grid.add(lblMsg, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String nombre  = txtNombre.getText().trim();
            String email   = txtEmail.getText().trim();
            String tipoDoc = cmbTipoDoc.getValue();
            String numDoc  = txtNumDoc.getText().trim();
            String espec   = cmbEspec.getValue();

            if (nombre.isEmpty() || email.isEmpty() || numDoc.isEmpty()) {
                lblMsg.setText("Todos los campos son obligatorios."); event.consume(); return;
            }
            if (tipoDoc == null) { lblMsg.setText("Selecciona tipo de documento."); event.consume(); return; }
            if (espec == null)   { lblMsg.setText("Selecciona la especialidad.");    event.consume(); return; }
            if (!email.contains("@")) { lblMsg.setText("Email inválido.");           event.consume(); return; }
        });

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;

            // Capturar valores antes de entrar al Task (hilo background)
            final String nombre  = txtNombre.getText().trim();
            final String email   = txtEmail.getText().trim();
            final String tipoDoc = cmbTipoDoc.getValue();
            final String numDoc  = txtNumDoc.getText().trim();
            final String espec   = cmbEspec.getValue();

            Task<String> task = new Task<>() {
                @Override
                protected String call() throws Exception {
                    PersonaDAO pdao = new PersonaDAO();
                    if (pdao.existe(email)) return "EMAIL_DUPLICADO";
                    // FIX: hash bloqueado con UUID — entrenadores no inician sesión,
                    // pero la columna contraseña en BD no acepta NULL
                    String hashBloqueado = PersonaControl.generarHashPBKDF2(UUID.randomUUID().toString());
                    if (hashBloqueado == null) return "ERROR_HASH";
                    Entrenador ent = new Entrenador(nombre, email, espec, tipoDoc, numDoc, 0);
                    new EntrenadorDAO().insertar(ent, hashBloqueado);
                    return "OK";
                }
            };
            task.setOnSucceeded(e -> {
                switch (task.getValue()) {
                    case "OK":
                        mostrarInfo("Entrenador creado",
                                nombre + " fue agregado como entrenador de " + espec + ".");
                        cargarEntrenadores();
                        break;
                    case "EMAIL_DUPLICADO":
                        mostrarError("Email duplicado", "Ya existe una cuenta con ese correo.");
                        break;
                    default:
                        mostrarError("Error", "No se pudo crear el entrenador.");
                        break;
                }
            });
            task.setOnFailed(e ->
                    mostrarError("Error", task.getException().getMessage()));
            Thread t = new Thread(task); t.setDaemon(true); t.start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPER ADMIN — CREAR ADMINISTRADOR
    // ─────────────────────────────────────────────────────────────────────────

    private void dialogoCrearAdministrador() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Crear Administrador");
        dialog.setHeaderText("Nuevo administrador — acceso restringido al Super Admin");

        TextField        txtNombre  = new TextField();  txtNombre.setPromptText("Nombre completo");
        TextField        txtEmail   = new TextField();  txtEmail.setPromptText("Correo electrónico");
        ComboBox<String> cmbTipoDoc = new ComboBox<>(FXCollections.observableArrayList("CC","TI","CE","PP","NIT"));
        cmbTipoDoc.setPromptText("Tipo doc.");
        TextField        txtNumDoc  = new TextField();  txtNumDoc.setPromptText("Número de documento");
        PasswordField    txtPass    = new PasswordField(); txtPass.setPromptText("Contraseña (mín. 6 car.)");

        GridPane grid = crearGrid();
        grid.add(new Label("Nombre:"),     0, 0); grid.add(txtNombre,  1, 0);
        grid.add(new Label("Email:"),      0, 1); grid.add(txtEmail,   1, 1);
        grid.add(new Label("Tipo doc.:"),  0, 2); grid.add(cmbTipoDoc, 1, 2);
        grid.add(new Label("Num. doc.:"),  0, 3); grid.add(txtNumDoc,  1, 3);
        grid.add(new Label("Contraseña:"), 0, 4); grid.add(txtPass,    1, 4);

        Label lblMsg = new Label();
        lblMsg.setStyle("-fx-text-fill: #dc2626;");
        grid.add(lblMsg, 0, 5, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtNombre.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty()
                    || txtNumDoc.getText().trim().isEmpty() || txtPass.getText().isEmpty()) {
                lblMsg.setText("Todos los campos son obligatorios."); event.consume(); return;
            }
            if (cmbTipoDoc.getValue() == null) { lblMsg.setText("Selecciona tipo de documento."); event.consume(); return; }
            if (!txtEmail.getText().contains("@")) { lblMsg.setText("Email inválido."); event.consume(); return; }
            if (txtPass.getText().length() < 6) { lblMsg.setText("Contraseña mínimo 6 caracteres."); event.consume(); return; }
        });

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;

            // Capturar valores antes de entrar al Task (hilo background)
            final String nombre  = txtNombre.getText().trim();
            final String email   = txtEmail.getText().trim();
            final String tipoDoc = cmbTipoDoc.getValue();
            final String numDoc  = txtNumDoc.getText().trim();
            final String pass    = txtPass.getText();

            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    PersonaDAO pdao = new PersonaDAO();
                    if (pdao.existe(email)) return "EMAIL_DUPLICADO";

                    String hash = PersonaControl.generarHashPBKDF2(pass);
                    if (hash == null) return "ERROR_HASH";

                    Usuario nuevoUsuario = new Usuario(0, nombre, email, tipoDoc, numDoc, false, null);
                    nuevoUsuario.setContraseña(hash);
                    pdao.insertar(nuevoUsuario);
                    int nuevoId = nuevoUsuario.getId();

                    pdao.actualizarRol(nuevoId, "ADMINISTRADOR");

                    Conexion con = Conexion.getInstancia();
                    Connection c = con.conectar();
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO administrador (id_administrador, contraseña_administrador) VALUES (?, ?)")) {
                        ps.setInt(1, nuevoId);
                        ps.setString(2, hash);
                        ps.executeUpdate();
                    } catch (SQLException ex) {
                        throw new RuntimeException("Error al insertar en tabla administrador", ex);
                    }

                    return "OK:" + nuevoId;
                }
            };
            task.setOnSucceeded(e -> {
                String res = task.getValue();
                if (res.startsWith("OK")) {
                    mostrarInfo("Administrador creado",
                            nombre + " fue creado como administrador.\n" +
                            "Puede iniciar sesión con: " + email);
                    cargarUsuarios();
                } else if ("EMAIL_DUPLICADO".equals(res)) {
                    mostrarError("Email duplicado", "Ya existe una cuenta con ese correo.");
                } else {
                    mostrarError("Error", "No se pudo crear el administrador.");
                }
            });
            task.setOnFailed(e ->
                    mostrarError("Error", task.getException().getMessage()));
            Thread t = new Thread(task); t.setDaemon(true); t.start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CARGA DE DATOS
    // ─────────────────────────────────────────────────────────────────────────

    private void cargarInicio() {
        try {
            if (adminId > 0) {
                Map<String, String> stats = adminService.obtenerEstadisticas();
                lblTotalUsuarios.setText(stats.getOrDefault("totalUsuariosActivos", "—"));
                lblTotalTurnos.setText(stats.getOrDefault("turnosHoy", "—"));
                lblTurnosActivos.setText(stats.getOrDefault("turnosFuturosActivos", "—"));
                lblTotalEntrenadores.setText(stats.getOrDefault("totalEntrenadores", "—"));
                if (lblCancelacionesMes != null) lblCancelacionesMes.setText(stats.getOrDefault("cancelacionesMes", "—"));
                if (lblIngresosMes != null) lblIngresosMes.setText("$ " + stats.getOrDefault("ingresosMes", "—"));
            } else {
                lblTotalUsuarios.setText("3"); lblTotalTurnos.setText("2");
                lblTurnosActivos.setText("1"); lblTotalEntrenadores.setText("2");
                if (lblCancelacionesMes != null) lblCancelacionesMes.setText("0");
                if (lblIngresosMes != null) lblIngresosMes.setText("$ 77.000");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cargarUsuarios() {
        try {
            if (adminId > 0) {
                String filtro = txtBuscarUsuario != null ? txtBuscarUsuario.getText() : "";
                List<Persona> lista = new PersonaDAO().listar(filtro, 200, 1);
                tablaUsuarios.setItems(FXCollections.observableArrayList(lista != null ? lista : crearUsuariosDemo()));
                if (lblMsgUsuarios != null) lblMsgUsuarios.setText("");
            } else {
                tablaUsuarios.setItems(FXCollections.observableArrayList(crearUsuariosDemo()));
                if (lblMsgUsuarios != null) lblMsgUsuarios.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaUsuarios.setItems(FXCollections.observableArrayList(crearUsuariosDemo()));
            if (lblMsgUsuarios != null) lblMsgUsuarios.setText("Error BD – mostrando demo");
        }
    }

    private void filtrarUsuarios(String texto) {
        if (adminId > 0) {
            try {
                List<Persona> lista = new PersonaDAO().listar(texto == null ? "" : texto, 200, 1);
                tablaUsuarios.setItems(FXCollections.observableArrayList(lista != null ? lista : List.of()));
            } catch (Exception e) { /* silencioso */ }
        }
    }

    private void cargarTurnos() {
        try {
            if (adminId > 0) {
                List<Turno> lista = new TurnoDAO(new PersonaDAO(), new InstalacionDAO(), new EntrenadorDAO()).listarTodos();
                tablaTurnos.setItems(FXCollections.observableArrayList(lista != null ? lista : crearTurnosDemo()));
                if (lblMsgTurnos != null) lblMsgTurnos.setText("");
            } else {
                tablaTurnos.setItems(FXCollections.observableArrayList(crearTurnosDemo()));
                if (lblMsgTurnos != null) lblMsgTurnos.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaTurnos.setItems(FXCollections.observableArrayList(crearTurnosDemo()));
            if (lblMsgTurnos != null) lblMsgTurnos.setText("Error BD – mostrando demo");
        }
    }

    private void cargarInstalaciones() {
        try {
            if (adminId > 0) {
                List<Instalacion> lista = new InstalacionDAO().listarTodos();
                tablaInstalaciones.setItems(FXCollections.observableArrayList(lista != null ? lista : crearInstalacionesDemo()));
                if (lblMsgInstalaciones != null) lblMsgInstalaciones.setText("");
            } else {
                tablaInstalaciones.setItems(FXCollections.observableArrayList(crearInstalacionesDemo()));
                if (lblMsgInstalaciones != null) lblMsgInstalaciones.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaInstalaciones.setItems(FXCollections.observableArrayList(crearInstalacionesDemo()));
            if (lblMsgInstalaciones != null) lblMsgInstalaciones.setText("Error BD – mostrando demo");
        }
    }

    private void cargarPagos() {
        try {
            if (adminId > 0) {
                List<Pago> lista = new PagoDAO().listarTodos(200, 1);
                tablaPagos.setItems(FXCollections.observableArrayList(lista != null ? lista : crearPagosDemo()));
                if (lblMsgPagos != null) lblMsgPagos.setText("");
            } else {
                tablaPagos.setItems(FXCollections.observableArrayList(crearPagosDemo()));
                if (lblMsgPagos != null) lblMsgPagos.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaPagos.setItems(FXCollections.observableArrayList(crearPagosDemo()));
            if (lblMsgPagos != null) lblMsgPagos.setText("Error BD – mostrando demo");
        }
    }

    private void cargarEntrenadores() {
        try {
            if (adminId > 0) {
                List<Entrenador> lista = new EntrenadorDAO().listar("", 200, 1);
                tablaEntrenadores.setItems(FXCollections.observableArrayList(lista != null ? lista : crearEntrenadoresDemo()));
                if (lblMsgEntrenadores != null) lblMsgEntrenadores.setText("");
            } else {
                tablaEntrenadores.setItems(FXCollections.observableArrayList(crearEntrenadoresDemo()));
                if (lblMsgEntrenadores != null) lblMsgEntrenadores.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaEntrenadores.setItems(FXCollections.observableArrayList(crearEntrenadoresDemo()));
            if (lblMsgEntrenadores != null) lblMsgEntrenadores.setText("Error BD – mostrando demo");
        }
    }

    private void cargarSedes() {
        try {
            if (adminId > 0) {
                List<Sede> lista = new SedeDAO().listarTodos();
                tablaSedesAdmin.setItems(FXCollections.observableArrayList(lista != null ? lista : crearSedesDemo()));
                if (lblMsgSedes != null) lblMsgSedes.setText("");
            } else {
                tablaSedesAdmin.setItems(FXCollections.observableArrayList(crearSedesDemo()));
                if (lblMsgSedes != null) lblMsgSedes.setText("(modo demo)");
            }
        } catch (Exception e) {
            tablaSedesAdmin.setItems(FXCollections.observableArrayList(crearSedesDemo()));
            if (lblMsgSedes != null) lblMsgSedes.setText("Error BD – mostrando demo");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACCIONES DE ADMINISTRADOR
    // ─────────────────────────────────────────────────────────────────────────

    private void accionToggleUsuario(Persona persona) {
        if (!mostrarConfirmacion("¿Desactivar usuario?",
                "Se desactivará la cuenta de " + persona.getNombre() + " y se cancelarán sus turnos futuros.")) return;
        try {
            int cancelados = adminService.desactivarUsuario(adminId, persona.getId());
            mostrarInfo("Usuario desactivado", "Cuenta desactivada. Turnos cancelados: " + cancelados);
            cargarUsuarios();
        } catch (Exception e) { mostrarError("Error al desactivar", e.getMessage()); }
    }

    private void accionEliminarUsuario(Persona persona) {
        if (!mostrarConfirmacion("⚠ Eliminar usuario permanentemente",
                "Se eliminarán todos los datos de " + persona.getNombre() + ". Esta acción no se puede deshacer.")) return;
        try {
            adminService.eliminarUsuario(adminId, persona.getId());
            mostrarInfo("Usuario eliminado", "El usuario fue eliminado del sistema.");
            cargarUsuarios();
        } catch (Exception e) { mostrarError("Error al eliminar", e.getMessage()); }
    }

    private void accionCancelarTurno(Turno turno) {
        if (adminId == 0) { mostrarError("Sin sesión", "Debes iniciar sesión como administrador."); return; }
        String usuario = turno.getUsuario() != null ? turno.getUsuario().getNombre() : "desconocido";
        if (!mostrarConfirmacion("Cancelar turno #" + turno.getIdTurno(),
                "Turno de " + usuario + ". ¿Confirmar cancelación?")) return;
        try {
            adminService.cancelarTurnoComoAdmin(adminId, turno.getIdTurno());
            mostrarInfo("Turno cancelado", "El turno fue cancelado y el cupo fue liberado.");
            cargarTurnos(); cargarInicio();
        } catch (Exception e) { mostrarError("Error al cancelar turno", e.getMessage()); }
    }

    private void accionEditarInstalacion(Instalacion inst) {
        if (adminId == 0) { mostrarError("Sin sesión", "Inicia sesión como administrador."); return; }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Instalación #" + inst.getIdInstalacion());
        dialog.setHeaderText("Modificar instalación (" + inst.getTipo() + ")");
        TextField txtNombre = new TextField(inst.getClass().getSimpleName());
        Spinner<Integer> spCapacidad = new Spinner<>(1, 500, inst.getCapacidadMaxima());
        spCapacidad.setEditable(true);
        GridPane grid = crearGrid();
        grid.add(new Label("Nombre:"),    0, 0); grid.add(txtNombre,   1, 0);
        grid.add(new Label("Capacidad:"), 0, 1); grid.add(spCapacidad, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    adminService.editarInstalacion(adminId, inst.getIdInstalacion(),
                            txtNombre.getText().trim(), spCapacidad.getValue());
                    mostrarInfo("Instalación actualizada", "Capacidad actualizada a " + spCapacidad.getValue());
                    cargarInstalaciones();
                } catch (Exception e) { mostrarError("Error al editar", e.getMessage()); }
            }
        });
    }

    private void accionCerrarInstalacion(Instalacion inst) {
        if (adminId == 0) { mostrarError("Sin sesión", "Inicia sesión como administrador."); return; }
        if (!mostrarConfirmacion("Cerrar instalación",
                "¿Cerrar la instalación " + inst.getTipo() + " #" + inst.getIdInstalacion() + "?")) return;
        try {
            adminService.toggleCerrarInstalacion(adminId, inst.getIdInstalacion(), true);
            mostrarInfo("Instalación cerrada", "La instalación fue marcada como cerrada.");
            cargarInstalaciones();
        } catch (Exception e) { mostrarError("Error", e.getMessage()); }
    }

    private void accionEditarSede(Sede sede) {
        if (adminId == 0) { mostrarError("Sin sesión", "Inicia sesión como administrador."); return; }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Sede #" + sede.getIdSede());
        dialog.setHeaderText("Modificar datos de la sede");
        TextField txtNombre    = new TextField(safe(sede.getNombre()));
        TextField txtDireccion = new TextField(safe(sede.getDireccion()));
        TextField txtTelefono  = new TextField(safe(sede.getTelefono()));
        TextField txtEmail     = new TextField(safe(sede.getEmail()));
        GridPane grid = crearGrid();
        grid.add(new Label("Nombre:"),    0, 0); grid.add(txtNombre,    1, 0);
        grid.add(new Label("Dirección:"), 0, 1); grid.add(txtDireccion, 1, 1);
        grid.add(new Label("Teléfono:"),  0, 2); grid.add(txtTelefono,  1, 2);
        grid.add(new Label("Email:"),     0, 3); grid.add(txtEmail,     1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    sede.setNombre(txtNombre.getText().trim());
                    sede.setDireccion(txtDireccion.getText().trim());
                    sede.setTelefono(txtTelefono.getText().trim());
                    sede.setEmail(txtEmail.getText().trim());
                    adminService.editarSede(adminId, sede);
                    mostrarInfo("Sede actualizada", "Los datos de la sede fueron guardados.");
                    cargarSedes();
                } catch (Exception e) { mostrarError("Error al guardar sede", e.getMessage()); }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DATOS DEMO
    // ─────────────────────────────────────────────────────────────────────────

    private List<Persona> crearUsuariosDemo() {
        return Arrays.asList(
                new Usuario(1, "María García",   "maria@demo.com",  "CC", "12345678", true,  "A"),
                new Usuario(2, "Carlos López",   "carlos@demo.com", "CC", "87654321", true,  "B"),
                new Administrador(10, "Admin Principal", "admin@demo.com", null, "CC", "99999999"));
    }

    private List<Instalacion> crearInstalacionesDemo() {
        return Arrays.asList(new Gimnasio(1, "GIMNASIO", 30, 25), new Piscina(2, "PISCINA", 15, 12, 6, 1.8));
    }

    private List<Turno> crearTurnosDemo() {
        Instalacion inst1 = new Gimnasio(1, "GIMNASIO", 30, 25);
        Usuario u1 = new Usuario(1, "María García", "maria@demo.com", "CC", "12345678", true, "A");
        return Arrays.asList(new Turno(1, LocalDateTime.now().plusDays(1), 60, u1, inst1));
    }

    private List<Pago> crearPagosDemo() {
        return Arrays.asList(
                new Pago(1L, 1, 1, new BigDecimal("35000"), "EFECTIVO", Pago.ESTADO_COMPLETADO, LocalDateTime.now().minusDays(1)));
    }

    private List<Entrenador> crearEntrenadoresDemo() {
        return Arrays.asList(
                new Entrenador("Juan Pérez",    "juan@gym.com",   "Gimnasio",   "CC", "55556666", 1),
                new Entrenador("Laura Sánchez", "laura@swim.com", "Natación",    "CC", "77778888", 2));
    }

    private List<Sede> crearSedesDemo() {
        return Arrays.asList(new Sede(1, "Sede Norte", "Calle 100 # 15-30", "601-555-1111", "norte@socrates.com"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILIDADES UI
    // ─────────────────────────────────────────────────────────────────────────

    private GridPane crearGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        return grid;
    }

    private boolean mostrarConfirmacion(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(msg);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void mostrarInfo(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo); alert.setHeaderText(null);
        alert.setContentText(msg != null ? msg : "Error desconocido.");
        alert.showAndWait();
    }

    private void estiloBtnPequeno(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                     "-fx-font-size: 11; -fx-padding: 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
    }

    private String safe(String value) { return value != null ? value : "—"; }
}
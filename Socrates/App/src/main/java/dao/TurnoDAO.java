package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.Conexion;
import entidades.Instalacion;
import entidades.Turno;
import entidades.Usuario;

/**
 * TurnoDAO — Acceso a datos de turnos.
 *
 * <p><b>Compatibilidad SQL verificada:</b></p>
 * <ul>
 *   <li>Columna {@code numero_carril_asignado} (corregida desde
 *       {@code numero_carril_assigned} del SQL original — ver script SQL adjunto).</li>
 *   <li>Columna {@code id_entrenador} (añadida en el script SQL adjunto).</li>
 *   <li>Cast seguro de {@link entidades.Persona} → {@link entidades.Usuario}:
 *       usa instanceof antes de castear para evitar ClassCastException.</li>
 * </ul>
 *
 * <p><b>Tabla {@code turno} esperada:</b><br>
 * idTurno, fechaHora, duracionMinutos, id_usuario, id_instalacion,
 * numero_carril_asignado (nullable), estado, id_entrenador (nullable)</p>
 */
public class TurnoDAO implements ITurnoDAO {

    private final Conexion        conexion = Conexion.getInstancia();
    private final IPersonaDAO     personaDAO;
    private final IInstalacionDAO instalacionDAO;
    private final IEntrenadorDAO  entrenadorDAO;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param personaDAO     para resolver el Usuario propietario del turno.
     * @param instalacionDAO para resolver la Instalacion del turno.
     * @param entrenadorDAO  para resolver el Entrenador asignado (puede ser null).
     */
    public TurnoDAO(IPersonaDAO personaDAO,
                    IInstalacionDAO instalacionDAO,
                    IEntrenadorDAO entrenadorDAO) {
        this.personaDAO     = personaDAO;
        this.instalacionDAO = instalacionDAO;
        this.entrenadorDAO  = entrenadorDAO;
    }

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO turno (fechaHora, duracionMinutos, id_usuario, id_instalacion, " +
            "                   numero_carril_asignado, estado, id_entrenador) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT idTurno, fechaHora, duracionMinutos, id_usuario, " +
            "       id_instalacion, numero_carril_asignado, estado, id_entrenador " +
            "FROM turno ";

    private static final String SQL_SELECT_POR_ID     = SQL_SELECT_BASE + "WHERE idTurno = ?";
    private static final String SQL_SELECT_POR_USER   = SQL_SELECT_BASE +
            "WHERE id_usuario = ? ORDER BY fechaHora DESC";
    private static final String SQL_SELECT_POR_INST   = SQL_SELECT_BASE + "WHERE id_instalacion = ?";
    private static final String SQL_SELECT_RESERVADOS =
            SQL_SELECT_BASE + "WHERE id_instalacion = ? AND estado = 'RESERVADO'";
    private static final String SQL_SELECT_TODOS      =
            SQL_SELECT_BASE + "ORDER BY fechaHora DESC";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE turno SET estado = ? WHERE idTurno = ?";

    private static final String SQL_UPDATE_ENTRENADOR =
            "UPDATE turno SET id_entrenador = ? WHERE idTurno = ?";

    private static final String SQL_DELETE =
            "DELETE FROM turno WHERE idTurno = ?";

    // ── Implementación ────────────────────────────────────────────────────────

    @Override
    public void insertar(Turno turno) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(turno.getFechaHora()));
            ps.setInt(2, turno.getDuracionMinutos());
            ps.setInt(3, turno.getUsuario().getId());
            ps.setInt(4, Integer.parseInt(turno.getInstalacion().getIdInstalacion()));

            // numero_carril_asignado: NULL para gimnasios
            if (turno.getNumeroCarrilAsignado() != null) {
                ps.setInt(5, turno.getNumeroCarrilAsignado());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, turno.getEstado());

            // id_entrenador: NULL si no hay entrenador asignado
            if (turno.getIdEntrenador() != null) {
                ps.setInt(7, turno.getIdEntrenador());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) turno.setIdTurno(String.valueOf(keys.getInt(1)));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar turno", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Turno> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar turno id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Turno> listarPorUsuario(int idUsuario) {
        return ejecutarConsultaConParam(SQL_SELECT_POR_USER, idUsuario);
    }

    @Override
    public List<Turno> listarPorInstalacion(int idInstalacion) {
        return ejecutarConsultaConParam(SQL_SELECT_POR_INST, idInstalacion);
    }

    @Override
    public List<Turno> listarReservadosPorInstalacion(int idInstalacion) {
        return ejecutarConsultaConParam(SQL_SELECT_RESERVADOS, idInstalacion);
    }

    @Override
    public List<Turno> listarTodos() {
        Connection con = conexion.conectar();
        List<Turno> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar todos los turnos", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public void actualizarEstado(int idTurno, String nuevoEstado) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, nuevoEstado.trim().toUpperCase());
            ps.setInt(2, idTurno);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado turno id=" + idTurno, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarEntrenador(int idTurno, Integer idEntrenador) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ENTRENADOR)) {
            if (idEntrenador != null) {
                ps.setInt(1, idEntrenador);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, idTurno);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar entrenador turno id=" + idTurno, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar turno id=" + id, e);
        } finally {
            conexion.desconectar();
        }
    }

    // ── Mapeo ResultSet → Turno ───────────────────────────────────────────────

    /**
     * Convierte una fila de la BD en un Turno completamente hidratado.
     *
     * <p><b>BUG 8 corregido:</b> el cast de Persona a Usuario se hace con instanceof
     * para evitar ClassCastException si por algún motivo el id_usuario apunta a un
     * administrador (no debería ocurrir, pero se maneja de forma segura).</p>
     *
     * <p>La carga del entrenador es lazy-safe: si id_entrenador es NULL,
     * se deja null sin lanzar error.</p>
     */
    private Turno mapear(ResultSet rs) throws SQLException {
        String idTurnoStr   = String.valueOf(rs.getInt("idTurno"));
        LocalDateTime fecha = rs.getTimestamp("fechaHora").toLocalDateTime();
        int duracion        = rs.getInt("duracionMinutos");
        int idUsuario       = rs.getInt("id_usuario");
        int idInstalacion   = rs.getInt("id_instalacion");
        String estado       = rs.getString("estado");

        // numero_carril_asignado puede ser NULL (gimnasio) → Integer nullable
        int carrilRaw       = rs.getInt("numero_carril_asignado");
        Integer carril      = rs.wasNull() ? null : carrilRaw;

        // id_entrenador puede ser NULL (turno sin entrenador)
        int idEntRaw        = rs.getInt("id_entrenador");
        Integer idEntrenador = rs.wasNull() ? null : idEntRaw;

        // ── Resolver Usuario propietario (BUG 8: cast seguro con instanceof) ──
        entidades.Persona personaRaw = personaDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario id=" + idUsuario + " no encontrado al mapear Turno."));

        if (!(personaRaw instanceof Usuario)) {
            throw new RuntimeException(
                "Se esperaba Usuario al mapear turno, pero id=" + idUsuario +
                " es " + personaRaw.getClass().getSimpleName());
        }
        Usuario usuario = (Usuario) personaRaw;

        Instalacion instalacion = instalacionDAO.buscarPorId(idInstalacion)
                .orElseThrow(() -> new RuntimeException(
                        "Instalación id=" + idInstalacion + " no encontrada al mapear Turno."));

        Turno turno = new Turno(idTurnoStr, fecha, duracion, usuario, instalacion);
        turno.setNumeroCarrilAsignado(carril);
        turno.setEstado(estado);

        // Cargar entrenador solo si hay uno asignado
        if (idEntrenador != null) {
            entrenadorDAO.buscarPorId(idEntrenador).ifPresent(turno::setEntrenador);
        }

        return turno;
    }

    private List<Turno> ejecutarConsultaConParam(String sql, int param) {
        Connection con = conexion.conectar();
        List<Turno> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar turnos", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }
}

package dao;

import java.sql.*;
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
 * CORRECCIONES:
 *
 * 1. idTurno ahora es int en Turno.java:
 *    ANTES: turno.setIdTurno(String.valueOf(keys.getInt(1)))
 *           y String idTurnoStr = String.valueOf(rs.getInt("idTurno"))
 *    AHORA: turno.setIdTurno(keys.getInt(1)) — int directo.
 *
 * 2. idInstalacion ahora es int en Instalacion.java:
 *    ANTES: Integer.parseInt(turno.getInstalacion().getIdInstalacion())
 *    AHORA: turno.getInstalacion().getIdInstalacion() — int directo.
 *
 * 3. Cast seguro Persona → Usuario con instanceof (sin ClassCastException).
 */
public class TurnoDAO implements ITurnoDAO {

    private final Conexion        conexion = Conexion.getInstancia();
    private final IPersonaDAO     personaDAO;
    private final IInstalacionDAO instalacionDAO;
    private final IEntrenadorDAO  entrenadorDAO;

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

    private static final String SQL_UPDATE_ESTADO     =
            "UPDATE turno SET estado = ? WHERE idTurno = ?";
    private static final String SQL_UPDATE_ENTRENADOR =
            "UPDATE turno SET id_entrenador = ? WHERE idTurno = ?";
    private static final String SQL_DELETE            =
            "DELETE FROM turno WHERE idTurno = ?";

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    public void insertar(Turno turno) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setTimestamp(1, Timestamp.valueOf(turno.getFechaHora()));
            ps.setInt(2, turno.getDuracionMinutos());
            ps.setInt(3, turno.getUsuario().getId());
            ps.setInt(4, turno.getInstalacion().getIdInstalacion()); // ← int directo

            if (turno.getNumeroCarrilAsignado() != null)
                ps.setInt(5, turno.getNumeroCarrilAsignado());
            else
                ps.setNull(5, Types.INTEGER);

            ps.setString(6, turno.getEstado());

            if (turno.getIdEntrenador() != null)
                ps.setInt(7, turno.getIdEntrenador());
            else
                ps.setNull(7, Types.INTEGER);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) turno.setIdTurno(keys.getInt(1)); // ← int directo
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
        return ejecutarConParam(SQL_SELECT_POR_USER, idUsuario);
    }

    @Override
    public List<Turno> listarPorInstalacion(int idInstalacion) {
        return ejecutarConParam(SQL_SELECT_POR_INST, idInstalacion);
    }

    @Override
    public List<Turno> listarReservadosPorInstalacion(int idInstalacion) {
        return ejecutarConParam(SQL_SELECT_RESERVADOS, idInstalacion);
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
            throw new RuntimeException("Error al listar turnos", e);
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
            if (idEntrenador != null) ps.setInt(1, idEntrenador);
            else                      ps.setNull(1, Types.INTEGER);
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

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private Turno mapear(ResultSet rs) throws SQLException {
        int           idTurno      = rs.getInt("idTurno");          // ← int directo
        LocalDateTime fecha        = rs.getTimestamp("fechaHora").toLocalDateTime();
        int           duracion     = rs.getInt("duracionMinutos");
        int           idUsuario    = rs.getInt("id_usuario");
        int           idInst       = rs.getInt("id_instalacion");
        String        estado       = rs.getString("estado");

        int     carrilRaw   = rs.getInt("numero_carril_asignado");
        Integer carril      = rs.wasNull() ? null : carrilRaw;

        int     idEntRaw    = rs.getInt("id_entrenador");
        Integer idEntrenador = rs.wasNull() ? null : idEntRaw;

        // Resolver Usuario — cast seguro con instanceof
        entidades.Persona personaRaw = personaDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new RuntimeException(
                        "Usuario id=" + idUsuario + " no encontrado al mapear Turno."));

        if (!(personaRaw instanceof Usuario))
            throw new RuntimeException("Se esperaba Usuario para turno, pero id=" + idUsuario +
                    " es " + personaRaw.getClass().getSimpleName());

        Instalacion instalacion = instalacionDAO.buscarPorId(idInst)
                .orElseThrow(() -> new RuntimeException(
                        "Instalación id=" + idInst + " no encontrada al mapear Turno."));

        Turno turno = new Turno(idTurno, fecha, duracion, (Usuario) personaRaw, instalacion);
        turno.setNumeroCarrilAsignado(carril);
        turno.setEstado(estado);

        if (idEntrenador != null)
            entrenadorDAO.buscarPorId(idEntrenador).ifPresent(turno::setEntrenador);

        return turno;
    }

    private List<Turno> ejecutarConParam(String sql, int param) {
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

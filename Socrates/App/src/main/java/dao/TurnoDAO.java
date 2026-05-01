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
 * TurnoDAO — Acceso a datos de turnos en la base de datos.
 *
 * OPERACIONES:
 *   • insertar(): Crea un nuevo turno.
 *   • buscarPorId(): Obtiene un turno específico.
 *   • listarPorUsuario(): Obtiene todos los turnos de un usuario.
 *   • listarPorInstalacion(): Obtiene todos los turnos de una instalación.
 *   • listarReservadosPorInstalacion(): Obtiene solo los turnos reservados de una instalación.
 *   • actualizarEstado(): Cambia el estado de un turno (RESERVADO, CANCELADO, COMPLETADO).
 *   • eliminar(): Borra un turno de la base de datos.
 *
 * ESTRUCTURA:
 *   Cada turno está vinculado a un usuario y una instalación.
 *   Algunos turnos tienen carril asignado (en piscinas), otros no (en gimnasios).
 */
public class TurnoDAO implements ITurnoDAO {

    private final Conexion conexion = Conexion.getInstancia();
    private final IPersonaDAO personaDAO;
    private final IInstalacionDAO instalacionDAO;

    /**
     * Constructor que recibe los DAOs para obtener usuarios e instalaciones.
     * Permite reconstruir turnos completos desde la base de datos.
     */
    public TurnoDAO(IPersonaDAO personaDAO, IInstalacionDAO instalacionDAO) {
        this.personaDAO     = personaDAO;
        this.instalacionDAO = instalacionDAO;
    }

    // ── SQL ────────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO turno (fechaHora, duracionMinutos, id_usuario, id_instalacion, " +
            "                   numero_carril_asignado, estado) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT idTurno, fechaHora, duracionMinutos, id_usuario, " +
            "       id_instalacion, numero_carril_asignado, estado " +
            "FROM turno ";

    private static final String SQL_SELECT_POR_ID     = SQL_SELECT_BASE + "WHERE idTurno = ?";
    private static final String SQL_SELECT_POR_USER   = SQL_SELECT_BASE + "WHERE id_usuario = ?";
    private static final String SQL_SELECT_POR_INST   = SQL_SELECT_BASE + "WHERE id_instalacion = ?";
    private static final String SQL_SELECT_RESERVADOS =
            SQL_SELECT_BASE + "WHERE id_instalacion = ? AND estado = 'RESERVADO'";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE turno SET estado = ? WHERE idTurno = ?";

    private static final String SQL_DELETE =
            "DELETE FROM turno WHERE idTurno = ?";

    // ── Implementación ─────────────────────────────────────────────────────────

    @Override
    public void insertar(Turno turno) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            // LocalDateTime → Timestamp para MySQL
            ps.setTimestamp(1, Timestamp.valueOf(turno.getFechaHora()));
            ps.setInt(2, turno.getDuracionMinutos());
            ps.setInt(3, turno.getUsuario().getId());
            ps.setInt(4, Integer.parseInt(turno.getInstalacion().getIdInstalacion()));

            // numero_carril_asignado puede ser NULL (para gimnasios)
            if (turno.getNumeroCarrilAsignado() != null) {
                ps.setInt(5, turno.getNumeroCarrilAsignado());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, turno.getEstado());
            ps.executeUpdate();

            // Recuperar el idTurno generado y reflejarlo en el objeto Java
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    turno.setIdTurno(String.valueOf(keys.getInt(1)));
                }
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
    public void actualizarEstado(int idTurno, String nuevoEstado) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, nuevoEstado.trim().toUpperCase());
            ps.setInt(2, idTurno);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado del turno", e);
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

    // ── Mapeo ResultSet → Turno ────────────────────────────────────────────────

    /**
     * Convierte una fila de la BD en un objeto Turno completo.
     * Obtiene el usuario e instalación asociados utilizando los DAOs inyectados.
     * Maneja carriles especiales (NULL para gimnasios, valor numérico para piscinas).
     */
    private Turno mapear(ResultSet rs) throws SQLException {
        // Conversión INT (BD) → String (Java)
        String idTurnoStr = String.valueOf(rs.getInt("idTurno"));

        // Timestamp (JDBC) → LocalDateTime (Java)
        LocalDateTime fechaHora = rs.getTimestamp("fechaHora").toLocalDateTime();

        int duracion       = rs.getInt("duracionMinutos");
        int idUsuario      = rs.getInt("id_usuario");
        int idInstalacion  = rs.getInt("id_instalacion");
        String estado      = rs.getString("estado");

        int carril = rs.getInt("numero_carril_asignado");
        Integer carrilAsignado = rs.wasNull() ? null : carril; // NULL en BD → null en Java

        // Obtener objetos relacionados usando los DAOs inyectados
        Usuario usuario = personaDAO.buscarPorId(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario id=" + idUsuario + " no encontrado."));

        Instalacion instalacion = instalacionDAO.buscarPorId(idInstalacion)
                .orElseThrow(() -> new RuntimeException("Instalacion id=" + idInstalacion + " no encontrada."));

        Turno turno = new Turno(idTurnoStr, fechaHora, duracion, usuario, instalacion);
        turno.setNumeroCarrilAsignado(carrilAsignado);
        turno.setEstado(estado);
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


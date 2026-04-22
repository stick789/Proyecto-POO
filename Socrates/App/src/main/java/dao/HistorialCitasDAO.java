package dao;

import database.Conexion;
import entidades.Historial_citas;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * HistorialCitasDAO — Acceso a datos del historial de citas.
 *
 * OPERACIONES:
 *   • insertar(): Registra un evento en el historial de citas.
 *   • listarPorUsuario(): Obtiene el historial de cambios de un usuario.
 *   • listarPorTurno(): Obtiene el historial de cambios de un turno.
 *   • listarTodos(): Obtiene todos los eventos registrados.
 *
 * USO:
 *   Se utiliza para mantener un registro de todas las acciones sobre turnos:
 *   (reservas, cancelaciones, reagendamientos, completaciones).
 */
public class HistorialCitasDAO implements IHistorialCitasDAO {

    private final Conexion conexion = Conexion.getInstancia();

    private static final String SQL_INSERT =
            "INSERT INTO historial_citas " +
            "(id_turno, id_usuario, id_instalacion, estado, fecha_evento, detalle) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT idHistorial, id_turno, id_usuario, id_instalacion, " +
            "       estado, fecha_evento, detalle " +
            "FROM historial_citas ";

    private static final String SQL_SELECT_POR_USUARIO = SQL_SELECT_BASE + "WHERE id_usuario = ? ORDER BY fecha_evento DESC";
    private static final String SQL_SELECT_POR_TURNO   = SQL_SELECT_BASE + "WHERE id_turno = ?   ORDER BY fecha_evento DESC";
    private static final String SQL_SELECT_TODOS       = SQL_SELECT_BASE + "ORDER BY fecha_evento DESC";

    @Override
    public void insertar(Historial_citas historial) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            // Guarda los datos del evento en el historial
            ps.setInt(1, Integer.parseInt(historial.getIdTurno()));
            ps.setInt(2, historial.getIdUsuario());
            ps.setInt(3, Integer.parseInt(historial.getIdInstalacion()));
            ps.setString(4, historial.getEstado());

            // Si no hay fecha, usa la fecha actual
            LocalDateTime fecha = historial.getFechaEvento() != null
                    ? historial.getFechaEvento()
                    : LocalDateTime.now();
            ps.setTimestamp(5, Timestamp.valueOf(fecha));

            ps.setString(6, historial.getDetalle());
            ps.executeUpdate();

            // Asigna el ID generado al objeto
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) historial.setIdHistorial(keys.getInt(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar historial de cita", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public List<Historial_citas> listarPorUsuario(int idUsuario) {
        return ejecutarConsulta(SQL_SELECT_POR_USUARIO, idUsuario);
    }

    @Override
    public List<Historial_citas> listarPorTurno(int idTurno) {
        return ejecutarConsulta(SQL_SELECT_POR_TURNO, idTurno);
    }

    @Override
    public List<Historial_citas> listarTodos() {
        Connection con = conexion.conectar();
        List<Historial_citas> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar historial", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    // Métodos auxiliares privados 

    /**
     * Convierte una fila de la BD en un objeto Historial_citas.
     */
    private Historial_citas mapear(ResultSet rs) throws SQLException {
        return new Historial_citas(
                rs.getInt("idHistorial"),
                String.valueOf(rs.getInt("id_turno")),
                rs.getString("estado"),
                rs.getTimestamp("fecha_evento").toLocalDateTime(),
                rs.getInt("id_usuario"),
                String.valueOf(rs.getInt("id_instalacion")),
                rs.getString("detalle")
        );
    }

    private List<Historial_citas> ejecutarConsulta(String sql, int param) {
        Connection con = conexion.conectar();
        List<Historial_citas> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar historial", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }
}

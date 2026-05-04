package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.Conexion;
import entidades.Pago;

/**
 * PagoDAO — Acceso a datos para la tabla {@code pagos}.
 *
 * <p><b>Compatibilidad SQL verificada (BUG 5 corregido):</b><br>
 * El SQL original define las columnas con camelCase: {@code idPago}, {@code metodoPago},
 * {@code estadoPago}. Esta clase usa exactamente esos nombres para evitar
 * {@code SQLException: Column 'id_pago' not found}.</p>
 *
 * <p><b>Columnas adicionales requeridas (ver script SQL adjunto):</b>
 * {@code id_turno INT NOT NULL}, {@code id_usuario INT NOT NULL},
 * {@code fechaPago DATETIME DEFAULT NOW()}.</p>
 *
 * <p><b>Tabla {@code pagos} completa esperada:</b><br>
 * idPago, id_turno, id_usuario, monto, metodoPago, estadoPago, fechaPago</p>
 */
public class PagoDAO implements IPagoDAO {

    private final Conexion conexion = Conexion.getInstancia();

    // ── SQL (columnas en camelCase tal como define el SQL) ────────────────────

    private static final String SQL_INSERT =
            "INSERT INTO pagos (id_turno, id_usuario, monto, metodoPago, estadoPago) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT idPago, id_turno, id_usuario, monto, metodoPago, estadoPago, fechaPago " +
            "FROM pagos ";

    private static final String SQL_SELECT_POR_ID    = SQL_SELECT_BASE + "WHERE idPago = ?";
    private static final String SQL_SELECT_POR_USER  = SQL_SELECT_BASE +
            "WHERE id_usuario = ? ORDER BY fechaPago DESC";
    private static final String SQL_SELECT_POR_TURNO = SQL_SELECT_BASE +
            "WHERE id_turno = ? ORDER BY fechaPago DESC";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM pagos";

    private static final String SQL_UPDATE_ESTADO =
            "UPDATE pagos SET estadoPago = ? WHERE idPago = ?";

    // =================================================================== CRUD

    @Override
    public void insertar(Pago pago) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pago.getIdTurno());
            ps.setInt(2, pago.getIdUsuario());
            ps.setBigDecimal(3, pago.getMonto());
            ps.setString(4, pago.getMetodoPago());     // columna: metodoPago
            ps.setString(5, pago.getEstadoPago());     // columna: estadoPago
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) pago.setIdPago(keys.getLong(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar pago", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public boolean actualizarEstado(long idPago, String nuevoEstado) {
        Connection con = conexion.conectar();
        if (con == null) return false;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESTADO)) {
            ps.setString(1, Pago.validarEstado(nuevoEstado)); // valida antes de persistir
            ps.setLong(2, idPago);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar estado pago id=" + idPago, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Pago> buscarPorId(long id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pago id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Pago> listarPorUsuario(int idUsuario) {
        return ejecutarConsultaConParam(SQL_SELECT_POR_USER, idUsuario);
    }

    @Override
    public List<Pago> listarPorTurno(int idTurno) {
        return ejecutarConsultaConParam(SQL_SELECT_POR_TURNO, idTurno);
    }

    @Override
    public int total() {
        Connection con = conexion.conectar();
        if (con == null) return 0;

        try (PreparedStatement ps = con.prepareStatement(SQL_COUNT);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar pagos", e);
        } finally {
            conexion.desconectar();
        }
    }

    // ================================================================= MAPEO

    /**
     * Convierte una fila de pagos en un objeto Pago.
     * Usa los nombres de columna exactos del SQL (camelCase).
     */
    private Pago mapear(ResultSet rs) throws SQLException {
        return new Pago(
                rs.getLong("idPago"),                              // PK: idPago
                rs.getInt("id_turno"),                            // FK turno
                rs.getInt("id_usuario"),                          // FK usuario
                rs.getBigDecimal("monto"),
                rs.getString("metodoPago"),                       // camelCase
                rs.getString("estadoPago"),                       // camelCase
                rs.getTimestamp("fechaPago") != null              // puede ser null
                        ? rs.getTimestamp("fechaPago").toLocalDateTime()
                        : null
        );
    }

    private List<Pago> ejecutarConsultaConParam(String sql, int param) {
        Connection con = conexion.conectar();
        List<Pago> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pagos", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }
}

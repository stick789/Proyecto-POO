package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    // Constructor vacío: NO ejecutar auto-migraciones en tiempo de ejecución.
    public PagoDAO() {
    }

    // ── SQL (columnas en camelCase tal como define el SQL) ────────────────────

    private static final String SQL_SELECT_BASE =
            "SELECT idPago, id_turno, id_usuario, monto, metodoPago, estadoPago, fechaPago, epayco_session_id, epayco_ref_payco " +
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

    // =================================================================== ESQUEMA

    /**
     * Asegura que la tabla pagos tenga las columnas usadas por el DAO.
     * Permite trabajar con bases antiguas donde solo existían idPago/monto/metodoPago/estadoPago.
     */
    // No hay utilidades de migración en tiempo de ejecución; usar el script SQL

    // =================================================================== CRUD

    @Override
    public void insertar(Pago pago)  {
    String sql = "INSERT INTO pagos (monto, metodoPago, estadoPago, id_turno, id_usuario, fechaPago) VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection con = conexion.conectar();
         PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setBigDecimal(1, pago.getMonto());
        ps.setString(2, pago.getMetodoPago());
        ps.setString(3, pago.getEstadoPago());
        ps.setInt(4, pago.getIdTurno());
        ps.setInt(5, pago.getIdUsuario());
        // Si no se proporcionó fecha en el objeto, usar la fecha/hora actual
        LocalDateTime fecha = pago.getFechaPago() != null ? pago.getFechaPago() : LocalDateTime.now();
        ps.setTimestamp(6, Timestamp.valueOf(fecha));
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) pago.setIdPago(rs.getInt(1));
        }
    }   catch (SQLException ex) {
            System.getLogger(PagoDAO.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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

    public Optional<Pago> buscarPorRefPayco(String refPayco) {
        if (refPayco == null || refPayco.isBlank()) return Optional.empty();

        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        String sql = SQL_SELECT_BASE + "WHERE epayco_ref_payco = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, refPayco.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar pago por ref_payco=" + refPayco, e);
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
        Pago pago = new Pago(
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
        pago.setEpaycoSessionId(rs.getString("epayco_session_id"));
        pago.setEpaycoRefPayco(rs.getString("epayco_ref_payco"));
        return pago;
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

    // Método para registrar un pago y obtener su ID generado
public int registrarPago(Pago pago) throws SQLException {
    Connection con = conexion.conectar();
    if (con == null) throw new SQLException("No se pudo conectar a la base de datos");
    String sql = "INSERT INTO pagos (id_turno, id_usuario, monto, metodoPago, estadoPago) VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, pago.getIdTurno());
        ps.setInt(2, pago.getIdUsuario());
        ps.setBigDecimal(3, pago.getMonto());
        ps.setString(4, pago.getMetodoPago());
        ps.setString(5, pago.getEstadoPago());
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                pago.setIdPago(id);
                return id;
            }
        }
        throw new SQLException("No se pudo obtener el ID del pago");
    } finally {
        conexion.desconectar();
    }
}

// Método para actualizar el session_id de ePayco


// Método para actualizar el ref_payco y el estado (cuando llegue la confirmación)
public void actualizarSessionId(int idPago, String sessionId) throws SQLException {
    String sql = "UPDATE pagos SET epayco_session_id = ? WHERE idPago = ?";
    try (Connection con = conexion.conectar();
         PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, sessionId);
        ps.setInt(2, idPago);
        int filas = ps.executeUpdate();
        System.out.println("actualizarSessionId: idPago=" + idPago + ", sessionId=" + sessionId + ", filas=" + filas);
    }
}

    public void actualizarRefPayco(int idPago, String refPayco) throws SQLException {
        String sql = "UPDATE pagos SET epayco_ref_payco = ? WHERE idPago = ?";
        try (Connection con = conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, refPayco);
            ps.setInt(2, idPago);
            int filas = ps.executeUpdate();
            System.out.println("actualizarRefPayco: idPago=" + idPago + ", refPayco=" + refPayco + ", filas=" + filas);
        }
    }

// ─── ADMIN: listar todos los pagos ────────────────────────────────────────────

private static final String SQL_SELECT_TODOS =
        SQL_SELECT_BASE + "ORDER BY fechaPago DESC LIMIT ?, ?";

/** Lista todos los pagos del sistema con paginación (para vista de administrador). */
public List<Pago> listarTodos(int totalPorPagina, int numPagina) {
    Connection con = conexion.conectar();
    List<Pago> lista = new ArrayList<>();
    if (con == null) return lista;
    try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS)) {
        ps.setInt(1, (numPagina - 1) * totalPorPagina);
        ps.setInt(2, totalPorPagina);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error al listar todos los pagos", e);
    } finally {
        conexion.desconectar();
    }
    return lista;
}
}

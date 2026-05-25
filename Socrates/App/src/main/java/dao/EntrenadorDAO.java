package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import database.Conexion;
import entidades.Entrenador;

/**
 * EntrenadorDAO — Acceso a datos de entrenadores.
 *
 * Esquema real de la BD:
 *   persona        — datos personales (nombre, email, tipodocumento, numdocumento)
 *   usuarios       — credenciales y rol (id_rol = ENTRENADOR)
 *   entrenador     — (idEntrenador PK propio, especialidad, numDocumento FK→persona, idInstalacion)
 *
 * IMPORTANTE: la tabla entrenador NO tiene FK a usuarios.
 * Su FK es numDocumento → persona.numdocumento.
 * Por eso al insertar en entrenador se usa el numDocumento, no el idusuario.
 */
public class EntrenadorDAO implements IEntrenadorDAO {

    private static final Logger LOGGER = Logger.getLogger(EntrenadorDAO.class.getName());
    private final Conexion conexion = Conexion.getInstancia();

    // ── INSERT ────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO persona (nombre, email, tipodocumento, numdocumento) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_USUARIO =
            "INSERT INTO usuarios (id_persona, contraseña, categoria, esAfiliado, id_rol) " +
            "VALUES (?, ?, 'NO AFILIADO', 0, " +
            "  (SELECT id_rol FROM rol WHERE nombre_rol = 'ENTRENADOR' LIMIT 1))";

    // FIX: columnas reales según el SQL — idEntrenador, especialidad, numDocumento
    private static final String SQL_INSERT_ENTRENADOR =
            "INSERT INTO entrenador (idEntrenador, especialidad, numDocumento) VALUES (?, ?, ?)";

    // ── SELECT ────────────────────────────────────────────────────────────────

    private static final String SQL_SELECT_BASE =
            "SELECT u.idusuario, p.nombre, p.email, p.tipodocumento, p.numdocumento, " +
            "       e.especialidad, r.nombre_rol " +
            "FROM entrenador e " +
            "JOIN persona p ON e.numDocumento = p.numdocumento " +
            "JOIN usuarios u ON u.id_persona = p.id_persona " +
            "LEFT JOIN rol r ON u.id_rol = r.id_rol ";

    private static final String SQL_SELECT_POR_ID =
            SQL_SELECT_BASE + "WHERE e.idEntrenador = ?";

    private static final String SQL_SELECT_PAGINADO =
            SQL_SELECT_BASE +
            "WHERE u.activo = 1 AND p.nombre LIKE ? " +
            "ORDER BY p.nombre ASC LIMIT ?, ?";

    private static final String SQL_SELECT_POR_ESPECIALIDAD =
            SQL_SELECT_BASE + "WHERE u.activo = 1 AND e.especialidad = ?";

    private static final String SQL_COUNT_ACTIVOS =
            "SELECT COUNT(*) FROM entrenador e " +
            "JOIN persona p ON e.numDocumento = p.numdocumento " +
            "JOIN usuarios u ON u.id_persona = p.id_persona WHERE u.activo = 1";

    // ── UPDATE ────────────────────────────────────────────────────────────────

    private static final String SQL_UPDATE_PERSONA =
            "UPDATE persona p " +
            "JOIN usuarios u ON u.id_persona = p.id_persona " +
            "SET p.nombre = ?, p.email = ?, p.tipodocumento = ?, p.numdocumento = ? " +
            "WHERE u.idusuario = ?";

    private static final String SQL_UPDATE_ESPECIALIDAD =
            "UPDATE entrenador SET especialidad = ? WHERE idEntrenador = ?";

    private static final String SQL_DESACTIVAR =
            "UPDATE usuarios SET activo = 0 WHERE idusuario = ?";

    private static final String SQL_ACTIVAR =
            "UPDATE usuarios SET activo = 1 WHERE idusuario = ?";

    // ── DELETE ────────────────────────────────────────────────────────────────

    private static final String SQL_DELETE_ENTRENADOR =
            "DELETE FROM entrenador WHERE idEntrenador = ?";

    private static final String SQL_DELETE_USUARIO =
            "DELETE FROM usuarios WHERE idusuario = ?";

    private static final String SQL_DELETE_PERSONA =
            "DELETE FROM persona WHERE id_persona = ?";

    // =================================================================== CRUD

    @Override
    public void insertar(Entrenador entrenador, String contraseñaHash) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            // 1. Insertar en persona
            int idPersona;
            try (PreparedStatement ps = con.prepareStatement(
                    SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, entrenador.getNombre());
                ps.setString(2, entrenador.getEmail());
                ps.setString(3, entrenador.getTipoDocumento());
                ps.setString(4, entrenador.getNumDocumento());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó id_persona.");
                    idPersona = keys.getInt(1);
                }
            }

            // 2. Insertar en usuarios con rol ENTRENADOR
            int idUsuario;
            try (PreparedStatement ps = con.prepareStatement(
                    SQL_INSERT_USUARIO, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idPersona);
                ps.setString(2, contraseñaHash);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idusuario.");
                    idUsuario = keys.getInt(1);
                }
            }

            // 3. FIX: insertar en entrenador con columnas reales del esquema SQL
            //    idEntrenador = idusuario generado, numDocumento = FK a persona
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT_ENTRENADOR)) {
                ps.setInt(1, idUsuario);
                ps.setString(2, entrenador.getEspecialidad());
                ps.setString(3, entrenador.getNumDocumento());
                ps.executeUpdate();
            }

            entrenador.setIdEntrenador(idUsuario);
            con.commit();

        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al insertar entrenador");
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public void actualizar(Entrenador entrenador) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_PERSONA)) {
                ps.setString(1, entrenador.getNombre());
                ps.setString(2, entrenador.getEmail());
                ps.setString(3, entrenador.getTipoDocumento());
                ps.setString(4, entrenador.getNumDocumento());
                ps.setInt(5, entrenador.getId());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ESPECIALIDAD)) {
                ps.setString(1, entrenador.getEspecialidad());
                ps.setInt(2, entrenador.getId());
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al actualizar entrenador id=" + entrenador.getId());
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public boolean desactivar(int idEntrenador) {
        return ejecutarUpdateBooleano(SQL_DESACTIVAR, idEntrenador,
                "Error al desactivar entrenador id=" + idEntrenador);
    }

    @Override
    public boolean activar(int idEntrenador) {
        return ejecutarUpdateBooleano(SQL_ACTIVAR, idEntrenador,
                "Error al activar entrenador id=" + idEntrenador);
    }

    @Override
    public void eliminar(int idEntrenador) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            // Obtener id_persona desde usuarios para luego borrar persona
            int idPersona = -1;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_persona FROM usuarios WHERE idusuario = ?")) {
                ps.setInt(1, idEntrenador);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) idPersona = rs.getInt("id_persona");
                }
            }
            if (idPersona == -1) { con.rollback(); return; }

            // FIX: usar idEntrenador con el nombre de columna correcto
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_ENTRENADOR)) {
                ps.setInt(1, idEntrenador); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_USUARIO)) {
                ps.setInt(1, idEntrenador); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_PERSONA)) {
                ps.setInt(1, idPersona); ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al eliminar entrenador id=" + idEntrenador);
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Entrenador> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar entrenador id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Entrenador> listar(String texto, int totalPorPagina, int numPagina) {
        Connection con = conexion.conectar();
        List<Entrenador> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_PAGINADO)) {
            ps.setString(1, "%" + (texto == null ? "" : texto) + "%");
            ps.setInt(2, (numPagina - 1) * totalPorPagina);
            ps.setInt(3, totalPorPagina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar entrenadores paginados", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public List<Entrenador> listarPorEspecialidad(String especialidad) {
        Connection con = conexion.conectar();
        List<Entrenador> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ESPECIALIDAD)) {
            ps.setString(1, especialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar entrenadores por especialidad", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public int total() {
        Connection con = conexion.conectar();
        if (con == null) return 0;

        try (PreparedStatement ps = con.prepareStatement(SQL_COUNT_ACTIVOS);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar entrenadores", e);
        } finally {
            conexion.desconectar();
        }
    }

    // ================================================================= MAPEO

    private Entrenador mapear(ResultSet rs) throws SQLException {
        Entrenador e = new Entrenador(
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("especialidad"),
                rs.getString("tipodocumento"),
                rs.getString("numdocumento"),
                rs.getInt("idusuario")
        );
        e.setRolBD(rs.getString("nombre_rol"));
        return e;
    }

    // ============================================================= UTILIDADES

    private boolean ejecutarUpdateBooleano(String sql, int id, String msg) {
        Connection con = conexion.conectar();
        if (con == null) return false;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(msg, e);
        } finally {
            conexion.desconectar();
        }
    }

    private void revertirTransaccion(Connection con, SQLException error, String msg) {
        if (con != null) {
            try { con.rollback(); }
            catch (SQLException re) {
                LOGGER.log(Level.WARNING, "No se pudo revertir: {0}", re.getMessage());
            }
        }
        throw new RuntimeException(msg, error);
    }

    private void restaurarAutoCommit(Connection con) {
        if (con != null) {
            try { con.setAutoCommit(true); }
            catch (SQLException e) {
                LOGGER.log(Level.WARNING, "No se pudo restaurar autoCommit: {0}", e.getMessage());
            }
        }
    }
}
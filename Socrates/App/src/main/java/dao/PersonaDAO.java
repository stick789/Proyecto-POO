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
import entidades.Administrador;
import entidades.Persona;
import entidades.Usuario;

/**
 * PersonaDAO — Acceso a datos de personas del sistema.
 *
 * <p>Compatible con el esquema SQL del proyecto:</p>
 * <ul>
 *   <li>{@code persona}: id_persona, nombre, email, tipodocumento, numdocumento</li>
 *   <li>{@code usuarios}: idusuario, id_persona, contraseña (VARCHAR 255), categoria,
 *       esAfiliado, id_rol, activo</li>
 *   <li>{@code administrador}: id_administrador (FK → usuarios.idusuario),
 *       contrasena_administrador</li>
 *   <li>{@code rol}: id_rol, nombre_rol</li>
 * </ul>
 *
 * <p>Los métodos de lectura retornan {@link Persona} (tipo padre) para que
 * {@code PersonaControl.login()} trabaje polimórficamente con {@link Usuario}
 * y {@link Administrador} sin duplicar la lógica de autenticación.</p>
 */
public class PersonaDAO implements IPersonaDAO {

    private static final Logger LOGGER = Logger.getLogger(PersonaDAO.class.getName());
    private final Conexion conexion = Conexion.getInstancia();

    // ------------------------------------------------------------------ INSERT
    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO persona (nombre, email, tipodocumento, numdocumento) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_USUARIO =
            "INSERT INTO usuarios (id_persona, contraseña, categoria, esAfiliado) VALUES (?, ?, ?, ?)";

    // ------------------------------------------------------------------ SELECT
    /**
     * LEFT JOIN con administrador para que {@link #mapear} construya
     * Administrador cuando el usuario tenga fila en esa tabla.
     * Incluye activo para filtrar en listados paginados.
     */
    private static final String SQL_SELECT_BASE =
            "SELECT u.idusuario, p.nombre, p.email, p.tipodocumento, p.numdocumento, " +
            "       u.contraseña, u.categoria, u.esAfiliado, r.nombre_rol, " +
            "       a.contrasena_administrador, u.activo " +
            "FROM usuarios u " +
            "JOIN persona p ON u.id_persona = p.id_persona " +
            "LEFT JOIN rol r ON u.id_rol = r.id_rol " +
            "LEFT JOIN administrador a ON a.id_administrador = u.idusuario ";

    private static final String SQL_SELECT_POR_ID    = SQL_SELECT_BASE + "WHERE u.idusuario = ?";
    private static final String SQL_SELECT_POR_EMAIL = SQL_SELECT_BASE + "WHERE p.email = ?";

    /** Solo activos, paginado, filtrado por nombre. */
    private static final String SQL_SELECT_PAGINADO =
            SQL_SELECT_BASE +
            "WHERE u.activo = 1 AND p.nombre LIKE ? " +
            "ORDER BY p.nombre ASC LIMIT ?, ?";

    private static final String SQL_COUNT_ACTIVOS =
            "SELECT COUNT(*) FROM usuarios WHERE activo = 1";

    private static final String SQL_EXISTE_EMAIL =
            "SELECT 1 FROM persona WHERE email = ? LIMIT 1";

    // ------------------------------------------------------------------ UPDATE
    private static final String SQL_UPDATE_PERSONA =
            "UPDATE persona p " +
            "JOIN usuarios u ON u.id_persona = p.id_persona " +
            "SET p.nombre = ?, p.email = ?, p.tipodocumento = ?, p.numdocumento = ? " +
            "WHERE u.idusuario = ?";

    private static final String SQL_UPDATE_USUARIO =
            "UPDATE usuarios SET categoria = ?, esAfiliado = ? WHERE idusuario = ?";

    private static final String SQL_UPDATE_CONTRASENA =
            "UPDATE usuarios SET contraseña = ? WHERE idusuario = ?";

    private static final String SQL_UPDATE_ROL =
            "UPDATE usuarios SET id_rol = (SELECT id_rol FROM rol WHERE nombre_rol = ?) WHERE idusuario = ?";

    private static final String SQL_DESACTIVAR =
            "UPDATE usuarios SET activo = 0 WHERE idusuario = ?";

    private static final String SQL_ACTIVAR =
            "UPDATE usuarios SET activo = 1 WHERE idusuario = ?";

    // ------------------------------------------------------------------ DELETE
    private static final String SQL_DELETE_USUARIO =
            "DELETE FROM usuarios WHERE idusuario = ?";

    // =================================================================== CRUD

    @Override
    public void insertar(Usuario persona) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            int idPersonaGenerado;
            try (PreparedStatement psPersona = con.prepareStatement(
                    SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS)) {

                psPersona.setString(1, persona.getNombre());
                psPersona.setString(2, persona.getEmail());
                psPersona.setString(3, persona.getTipoDocumento());
                psPersona.setString(4, persona.getNumDocumento());
                psPersona.executeUpdate();

                try (ResultSet keys = psPersona.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó id_persona.");
                    idPersonaGenerado = keys.getInt(1);
                }
            }

            try (PreparedStatement psUsuario = con.prepareStatement(
                    SQL_INSERT_USUARIO, Statement.RETURN_GENERATED_KEYS)) {

                psUsuario.setInt(1, idPersonaGenerado);
                psUsuario.setString(2, persona.getContraseña());
                psUsuario.setString(3, persona.getCategoria());
                psUsuario.setBoolean(4, persona.isEsAfiliado());
                psUsuario.executeUpdate();

                try (ResultSet keys = psUsuario.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idusuario.");
                    persona.setId(keys.getInt(1));
                }
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al insertar persona");
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Persona> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar persona id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Persona> buscarPorEmail(String email) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar persona por email", e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Persona> listar(String texto, int totalPorPagina, int numPagina) {
        Connection con = conexion.conectar();
        List<Persona> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_PAGINADO)) {
            ps.setString(1, "%" + (texto == null ? "" : texto) + "%");
            ps.setInt(2, (numPagina - 1) * totalPorPagina);
            ps.setInt(3, totalPorPagina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar personas paginadas", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public void actualizar(Usuario persona) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_PERSONA)) {
                ps.setString(1, persona.getNombre());
                ps.setString(2, persona.getEmail());
                ps.setString(3, persona.getTipoDocumento());
                ps.setString(4, persona.getNumDocumento());
                ps.setInt(5, persona.getId());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_USUARIO)) {
                ps.setString(1, persona.getCategoria());
                ps.setBoolean(2, persona.isEsAfiliado());
                ps.setInt(3, persona.getId());
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al actualizar persona");
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarContrasena(int idPersona, String nuevaContrasena) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_CONTRASENA)) {
            ps.setString(1, nuevaContrasena);
            ps.setInt(2, idPersona);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar contraseña", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarRol(int idPersona, String nombreRol) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_ROL)) {
            ps.setString(1, nombreRol);
            ps.setInt(2, idPersona);
            ps.executeUpdate();
            LOGGER.log(Level.INFO, "Rol actualizado a {0} para usuario {1}",
                    new Object[]{nombreRol, idPersona});
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar rol: " + nombreRol, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public boolean desactivar(int id) {
        return ejecutarUpdateBooleano(SQL_DESACTIVAR, id, "Error al desactivar id=" + id);
    }

    @Override
    public boolean activar(int id) {
        return ejecutarUpdateBooleano(SQL_ACTIVAR, id, "Error al activar id=" + id);
    }

    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            int idPersona = -1;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id_persona FROM usuarios WHERE idusuario = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) idPersona = rs.getInt("id_persona");
                }
            }

            if (idPersona == -1) { con.rollback(); return; }

            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_USUARIO)) {
                ps.setInt(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM persona WHERE id_persona = ?")) {
                ps.setInt(1, idPersona); ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al eliminar persona id=" + id);
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public boolean existe(String email) {
        Connection con = conexion.conectar();
        if (con == null) return false;

        try (PreparedStatement ps = con.prepareStatement(SQL_EXISTE_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar email", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public int total() {
        Connection con = conexion.conectar();
        if (con == null) return 0;

        try (PreparedStatement ps = con.prepareStatement(SQL_COUNT_ACTIVOS);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar personas", e);
        } finally {
            conexion.desconectar();
        }
    }

    // ================================================================= MAPEO

    /**
     * Construye Administrador o Usuario según el rol en BD.
     * Si existe fila en {@code administrador} → Administrador (usa su contraseña propia).
     * Si no → Usuario regular o Entrenador.
     */
    private Persona mapear(ResultSet rs) throws SQLException {
        String rolBD = rs.getString("nombre_rol");

        if ("ADMINISTRADOR".equalsIgnoreCase(rolBD)) {
            Administrador admin = new Administrador(
                    rs.getInt("idusuario"),
                    rs.getString("nombre"),
                    rs.getString("email"),
                    rs.getString("contrasena_administrador"),
                    rs.getString("tipodocumento"),
                    rs.getString("numdocumento")
            );
            admin.setRolBD(rolBD);
            return admin;
        }

        Usuario usuario = new Usuario(
                rs.getInt("idusuario"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("contraseña"),
                rs.getString("tipodocumento"),
                rs.getString("numdocumento"),
                rs.getBoolean("esAfiliado"),
                rs.getString("categoria")
        );
        usuario.setRolBD(rolBD);
        return usuario;
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
            catch (SQLException e) {
                LOGGER.log(Level.WARNING, "No se pudo revertir: {0}", e.getMessage());
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

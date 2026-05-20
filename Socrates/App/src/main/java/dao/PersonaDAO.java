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
import entidades.Entrenador;
import entidades.Persona;
import entidades.Usuario;

/**
 * PersonaDAO — Acceso a datos de personas del sistema.
 *
 * CORRECCIÓN PRINCIPAL: mapear() ahora crea Entrenador cuando nombre_rol = 'ENTRENADOR'.
 *   ANTES: el else final creaba un Usuario para cualquier rol que no fuera ADMINISTRADOR,
 *          incluyendo ENTRENADOR. Esto hacía que un entrenador que iniciara sesión
 *          recibiera un objeto Usuario en lugar de Entrenador, perdiendo la especialidad
 *          y el tipo real.
 *   AHORA: se agrega caso ENTRENADOR en el switch de mapear(), y SQL_SELECT_BASE
 *          incluye LEFT JOIN con entrenador para traer la especialidad.
 */
public class PersonaDAO implements IPersonaDAO {

    private static final Logger LOGGER = Logger.getLogger(PersonaDAO.class.getName());
    private final Conexion conexion = Conexion.getInstancia();

    // ── SQL ───────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO persona (nombre, email, tipodocumento, numdocumento) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_USUARIO =
            "INSERT INTO usuarios (id_persona, contraseña, categoria, esAfiliado) VALUES (?, ?, ?, ?)";

    /**
     * LEFT JOIN con administrador (para contraseña de admin),
     * LEFT JOIN con entrenador (para especialidad — necesario para mapear Entrenador).
     */
    private static final String SQL_SELECT_BASE =
            "SELECT u.idusuario, p.nombre, p.email, p.tipodocumento, p.numdocumento, " +
            "       u.contraseña, u.categoria, u.esAfiliado, r.nombre_rol, " +
            "       a.contraseña_administrador, " +
            "       e.especialidad " +             // ← nuevo: para instanciar Entrenador
            "FROM usuarios u " +
            "JOIN  persona p   ON u.id_persona         = p.id_persona " +
            "LEFT JOIN rol r   ON u.id_rol              = r.id_rol " +
            "LEFT JOIN administrador a ON a.id_administrador = u.idusuario " +
            "LEFT JOIN entrenador    e ON e.idEntrenador    = u.idusuario ";  // ← nuevo JOIN

    private static final String SQL_SELECT_POR_ID    = SQL_SELECT_BASE + "WHERE u.idusuario = ?";
    private static final String SQL_SELECT_POR_EMAIL = SQL_SELECT_BASE + "WHERE p.email = ?";
    private static final String SQL_SELECT_TODOS     = SQL_SELECT_BASE +
            "WHERE p.nombre LIKE ? ORDER BY u.idusuario ASC LIMIT ?, ?";

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
            "UPDATE usuarios SET id_rol = (SELECT id_rol FROM rol WHERE nombre_rol = ?) " +
            "WHERE idusuario = ?";

    private static final String SQL_DESACTIVAR = "UPDATE usuarios SET activo = 0 WHERE idusuario = ?";
    private static final String SQL_ACTIVAR    = "UPDATE usuarios SET activo = 1 WHERE idusuario = ?";

    private static final String SQL_DELETE_USUARIO = "DELETE FROM usuarios WHERE idusuario = ?";
    private static final String SQL_DELETE_PERSONA = "DELETE FROM persona WHERE id_persona = ?";

    private static final String SQL_EXISTE = "SELECT 1 FROM persona WHERE email = ? LIMIT 1";
    private static final String SQL_TOTAL  = "SELECT COUNT(u.idusuario) FROM usuarios u";

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public void insertar(Usuario persona) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = con.prepareStatement(
                    SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, persona.getNombre());
                ps.setString(2, persona.getEmail());
                ps.setString(3, persona.getTipoDocumento());
                ps.setString(4, persona.getNumDocumento());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó id_persona.");
                    idPersona = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    SQL_INSERT_USUARIO, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idPersona);
                ps.setString(2, persona.getContraseña());
                ps.setString(3, persona.getCategoria());
                ps.setBoolean(4, persona.isEsAfiliado());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idusuario.");
                    persona.setId(keys.getInt(1));
                }
            }

            con.commit();
        } catch (SQLException e) {
            revertir(con, e, "Error al insertar persona");
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
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
            revertir(con, e, "Error al actualizar persona id=" + persona.getId());
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarContraseña(int idPersona, String nuevaContraseña) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_CONTRASENA)) {
            ps.setString(1, nuevaContraseña);
            ps.setInt(2, idPersona);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar contraseña id=" + idPersona, e);
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
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar rol id=" + idPersona, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public boolean desactivar(int id) {
        return ejecutarUpdateBool(SQL_DESACTIVAR, id);
    }

    @Override
    public boolean activar(int id) {
        return ejecutarUpdateBool(SQL_ACTIVAR, id);
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
            try (PreparedStatement ps = con.prepareStatement(SQL_DELETE_PERSONA)) {
                ps.setInt(1, idPersona); ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            revertir(con, e, "Error al eliminar persona id=" + id);
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    // ── Lectura ───────────────────────────────────────────────────────────────

    @Override
    public List<Persona> listar(String texto, int totalPorPagina, int numPagina) {
        List<Persona> lista = new ArrayList<>();
        Connection con = conexion.conectar();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS)) {
            ps.setString(1, "%" + (texto == null ? "" : texto) + "%");
            ps.setInt(2, (numPagina - 1) * totalPorPagina);
            ps.setInt(3, totalPorPagina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar personas", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
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
    public boolean existe(String email) {
        Connection con = conexion.conectar();
        if (con == null) return false;

        try (PreparedStatement ps = con.prepareStatement(SQL_EXISTE)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia de email", e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public int total() {
        Connection con = conexion.conectar();
        if (con == null) return 0;

        try (PreparedStatement ps = con.prepareStatement(SQL_TOTAL);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar personas", e);
        } finally {
            conexion.desconectar();
        }
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    /**
     * Construye la instancia concreta correcta según nombre_rol:
     *   - ADMINISTRADOR → Administrador (usa contrasena_administrador)
     *   - ENTRENADOR    → Entrenador    (usa contraseña de usuarios + especialidad)  ← NUEVO
     *   - Cualquier otro → Usuario
     */
    private Persona mapear(ResultSet rs) throws SQLException {
        String rolBD = rs.getString("nombre_rol");

        if ("ADMINISTRADOR".equalsIgnoreCase(rolBD)) {
            Administrador admin = new Administrador(
                    rs.getInt("idusuario"),
                    rs.getString("nombre"),
                    rs.getString("email"),
                    rs.getString("contraseña_administrador"),
                    rs.getString("tipodocumento"),
                    rs.getString("numdocumento")
            );
            admin.setRolBD(rolBD);
            return admin;
        }

        if ("ENTRENADOR".equalsIgnoreCase(rolBD)) {
            Entrenador entrenador = new Entrenador(
                    rs.getString("nombre"),
                    rs.getString("email"),
                    rs.getString("especialidad"),   // disponible por el LEFT JOIN con entrenador
                    rs.getString("tipodocumento"),
                    rs.getString("numdocumento"),
                    rs.getInt("idusuario")
            );
            entrenador.setContraseña(rs.getString("contraseña")); // para el login
            entrenador.setRolBD(rolBD);
            return entrenador;
        }

        // Usuario regular (USUARIO) o cualquier otro rol
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

    // ── Utilidades ────────────────────────────────────────────────────────────

    private boolean ejecutarUpdateBool(String sql, int id) {
        Connection con = conexion.conectar();
        if (con == null) return false;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al ejecutar update id=" + id, e);
        } finally {
            conexion.desconectar();
        }
    }

    private void revertir(Connection con, SQLException error, String msg) {
        if (con != null) try { con.rollback(); }
        catch (SQLException ex) { LOGGER.log(Level.WARNING, "No se pudo revertir", ex); }
        throw new RuntimeException(msg, error);
    }

    private void restaurarAutoCommit(Connection con) {
        if (con != null) try { con.setAutoCommit(true); }
        catch (SQLException e) { LOGGER.log(Level.WARNING, "No se pudo restaurar autoCommit", e); }
    }
}

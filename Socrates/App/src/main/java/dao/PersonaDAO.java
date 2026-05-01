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
import entidades.Usuario;

/**
 * PersonaDAO — Acceso a datos de personas del sistema.
 *
 * Rediseño conceptual: la capa de aplicación usa el nombre PersonaDAO,
 * aunque la persistencia concreta sigue apoyándose en las tablas persona y usuarios.
 */
public class PersonaDAO implements IPersonaDAO {

    private static final Logger LOGGER = Logger.getLogger(PersonaDAO.class.getName());
    private final Conexion conexion = Conexion.getInstancia();

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO persona (nombre, email, tipodocumento, numdocumento) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_USUARIO =
            "INSERT INTO usuarios (id_persona, contraseña, categoria, esAfiliado) VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT u.idusuario, p.nombre, p.email, p.tipodocumento, p.numdocumento, " +
            "       u.contraseña, u.categoria, u.esAfiliado, r.nombre_rol " +
            "FROM usuarios u " +
            "JOIN persona p ON u.id_persona = p.id_persona " +
            "LEFT JOIN rol r ON u.id_rol = r.id_rol ";

    private static final String SQL_SELECT_POR_ID = SQL_SELECT_BASE + "WHERE u.idusuario = ?";
    private static final String SQL_SELECT_POR_EMAIL = SQL_SELECT_BASE + "WHERE p.email = ?";
    private static final String SQL_SELECT_TODOS = SQL_SELECT_BASE;
    private static final String SQL_SELECT_LOGIN = SQL_SELECT_BASE + "WHERE p.email = ? AND u.contraseña = ?";

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

    private static final String SQL_DELETE_USUARIO =
            "DELETE FROM usuarios WHERE idusuario = ?";

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
                    if (!keys.next()) {
                        throw new SQLException("No se generó id_persona.");
                    }
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
                    if (!keys.next()) {
                        throw new SQLException("No se generó idusuario.");
                    }
                    persona.setId(keys.getInt(1));
                }
            }

            con.commit();
        } catch (SQLException e) {
            revertirTransaccion(con, e, "Error al insertar persona (transacción revertida)");
        } finally {
            restaurarAutoCommit(con);
            conexion.desconectar();
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar persona por id=" + id, e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_POR_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar persona por email", e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> listarTodos() {
        Connection con = conexion.conectar();
        List<Usuario> lista = new ArrayList<>();
        if (con == null) return lista;

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar personas", e);
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

            try (PreparedStatement psPersona = con.prepareStatement(SQL_UPDATE_PERSONA)) {
                psPersona.setString(1, persona.getNombre());
                psPersona.setString(2, persona.getEmail());
                psPersona.setString(3, persona.getTipoDocumento());
                psPersona.setString(4, persona.getNumDocumento());
                psPersona.setInt(5, persona.getId());
                psPersona.executeUpdate();
            }

            try (PreparedStatement psUsuario = con.prepareStatement(SQL_UPDATE_USUARIO)) {
                psUsuario.setString(1, persona.getCategoria());
                psUsuario.setBoolean(2, persona.isEsAfiliado());
                psUsuario.setInt(3, persona.getId());
                psUsuario.executeUpdate();
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
            throw new RuntimeException("Error al actualizar contraseña de persona", e);
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
            LOGGER.log(Level.INFO, "Rol actualizado a {0} para usuario {1}", new Object[]{nombreRol, idPersona});
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar rol de persona: " + nombreRol, e);
        } finally {
            conexion.desconectar();
        }
    }

    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            int idPersona = -1;
            try (PreparedStatement psGetPersona = con.prepareStatement(
                    "SELECT id_persona FROM usuarios WHERE idusuario = ?")) {
                psGetPersona.setInt(1, id);
                try (ResultSet rs = psGetPersona.executeQuery()) {
                    if (rs.next()) {
                        idPersona = rs.getInt("id_persona");
                    }
                }
            }

            if (idPersona == -1) {
                con.rollback();
                return;
            }

            try (PreparedStatement psU = con.prepareStatement(SQL_DELETE_USUARIO)) {
                psU.setInt(1, id);
                psU.executeUpdate();
            }

            try (PreparedStatement psP = con.prepareStatement(
                    "DELETE FROM persona WHERE id_persona = ?")) {
                psP.setInt(1, idPersona);
                psP.executeUpdate();
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
    public Optional<Usuario> verificarCredenciales(String email, String contrasena) {
        Connection con = conexion.conectar();
        if (con == null) return Optional.empty();

        try (PreparedStatement ps = con.prepareStatement(SQL_SELECT_LOGIN)) {
            ps.setString(1, email);
            ps.setString(2, contrasena);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar credenciales de persona", e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
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
        // Establecer el rol desde la BD
        String rolBD = rs.getString("nombre_rol");
        usuario.setRolBD(rolBD);
        return usuario;
    }

    private void revertirTransaccion(Connection con, SQLException error, String mensaje) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException rollbackError) {
                LOGGER.log(Level.WARNING, "No se pudo revertir la transaccion: {0}", rollbackError.getMessage());
            }
        }
        throw new RuntimeException(mensaje, error);
    }

    private void restaurarAutoCommit(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "No se pudo restaurar autoCommit: {0}", e.getMessage());
            }
        }
    }
}

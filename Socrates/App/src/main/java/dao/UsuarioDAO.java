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
import entidades.Usuario;

/**
 * UsuarioDAO — Acceso a datos de usuarios en la base de datos.
 *
 * OPERACIONES:
 *   • insertar(): Crea un nuevo usuario guardando datos en persona y usuarios.
 *   • buscarPorId() / buscarPorEmail(): Obtiene un usuario específico.
 *   • listarTodos(): Obtiene todos los usuarios registrados.
 *   • actualizar(): Modifica datos del usuario.
 *   • actualizarContrasena(): Cambia solo la contraseña.
 *   • eliminar(): Borra un usuario de la base de datos.
 *   • verificarCredenciales(): Valida email y contraseña para login.
 *
 * ESTRUCTURA DE DATOS:
 *   Los usuarios se guardan en dos tablas: 'persona' (datos básicos) y 'usuarios' (contraseña, categoría).
 */
public class UsuarioDAO implements IUsuarioDAO {

    private final Conexion conexion = Conexion.getInstancia();

    // ── SQL ────────────────────────────────────────────────────────────────────

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO persona (nombre, email, tipodocumento, numdocumento) VALUES (?, ?, ?, ?)";

    private static final String SQL_INSERT_USUARIO =
            "INSERT INTO usuarios (id_persona, contraseña, categoria, esAfiliado) VALUES (?, ?, ?, ?)";

    private static final String SQL_SELECT_BASE =
            "SELECT u.idusuario, p.nombre, p.email, p.tipodocumento, p.numdocumento, " +
            "       u.contraseña, u.categoria, u.esAfiliado " +
            "FROM usuarios u " +
            "JOIN persona p ON u.id_persona = p.id_persona ";

    private static final String SQL_SELECT_POR_ID    = SQL_SELECT_BASE + "WHERE u.idusuario = ?";
    private static final String SQL_SELECT_POR_EMAIL = SQL_SELECT_BASE + "WHERE p.email = ?";
    private static final String SQL_SELECT_TODOS     = SQL_SELECT_BASE;
    private static final String SQL_SELECT_LOGIN     = SQL_SELECT_BASE + "WHERE p.email = ? AND u.contraseña = ?";

    private static final String SQL_UPDATE_PERSONA =
            "UPDATE persona p " +
            "JOIN usuarios u ON u.id_persona = p.id_persona " +
            "SET p.nombre = ?, p.email = ?, p.tipodocumento = ?, p.numdocumento = ? " +
            "WHERE u.idusuario = ?";

    private static final String SQL_UPDATE_USUARIO =
            "UPDATE usuarios SET categoria = ?, esAfiliado = ? WHERE idusuario = ?";

    private static final String SQL_UPDATE_CONTRASENA =
            "UPDATE usuarios SET contraseña = ? WHERE idusuario = ?";

    private static final String SQL_DELETE_USUARIO =
            "DELETE FROM usuarios WHERE idusuario = ?";

    private static final String SQL_DELETE_PERSONA =
            "DELETE FROM persona WHERE id_persona = " +
            "(SELECT id_persona FROM usuarios WHERE idusuario = ?)";

    // ── Implementación ─────────────────────────────────────────────────────────

    /**
     * Crea un nuevo usuario guardando sus datos en la base de datos.
     * Guarda primero los datos personales, luego los datos de acceso.
     * Si algo falla, revierte todos los cambios.
     */
    @Override
    public void insertar(Usuario usuario) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false); // ← inicio de transacción

            // Paso 1: insertar en persona
            int idPersonaGenerado;
            try (PreparedStatement psPersona = con.prepareStatement(
                    SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS)) {

                psPersona.setString(1, usuario.getNombre());
                psPersona.setString(2, usuario.getEmail());
                psPersona.setString(3, usuario.getTipoDocumento());
                psPersona.setString(4, usuario.getNumDocumento());
                psPersona.executeUpdate();

                try (ResultSet keys = psPersona.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó id_persona.");
                    idPersonaGenerado = keys.getInt(1);
                }
            }

            // Paso 2: insertar en usuarios usando el id de persona recién creado
            try (PreparedStatement psUsuario = con.prepareStatement(
                    SQL_INSERT_USUARIO, Statement.RETURN_GENERATED_KEYS)) {

                psUsuario.setInt(1, idPersonaGenerado);
                psUsuario.setString(2, usuario.getContraseña());
                psUsuario.setString(3, usuario.getCategoria());
                psUsuario.setBoolean(4, usuario.isEsAfiliado());
                psUsuario.executeUpdate();

                try (ResultSet keys = psUsuario.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No se generó idusuario.");
                    usuario.setId(keys.getInt(1)); // ← refleja el ID generado en el objeto
                }
            }

            con.commit(); // ← confirmar transacción

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Error al insertar usuario (transacción revertida)", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
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
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por id=" + id, e);
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
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario por email", e);
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

            while (rs.next()) lista.add(mapear(rs));

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar usuarios", e);
        } finally {
            conexion.desconectar();
        }
        return lista;
    }

    @Override
    public void actualizar(Usuario usuario) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            try (PreparedStatement psPersona = con.prepareStatement(SQL_UPDATE_PERSONA)) {
                psPersona.setString(1, usuario.getNombre());
                psPersona.setString(2, usuario.getEmail());
                psPersona.setString(3, usuario.getTipoDocumento());
                psPersona.setString(4, usuario.getNumDocumento());
                psPersona.setInt(5, usuario.getId());
                psPersona.executeUpdate();
            }

            try (PreparedStatement psUsuario = con.prepareStatement(SQL_UPDATE_USUARIO)) {
                psUsuario.setString(1, usuario.getCategoria());
                psUsuario.setBoolean(2, usuario.isEsAfiliado());
                psUsuario.setInt(3, usuario.getId());
                psUsuario.executeUpdate();
            }

            con.commit();

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Error al actualizar usuario", e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
            conexion.desconectar();
        }
    }

    @Override
    public void actualizarContrasena(int idUsuario, String nuevaContrasena) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE_CONTRASENA)) {
            ps.setString(1, nuevaContrasena);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar contraseña", e);
        } finally {
            conexion.desconectar();
        }
    }

    /**
     * Borra un usuario de la base de datos.
     * Primero elimina el registro del usuario, luego sus datos personales.
     * Si el usuario es administrador, esos datos se borran automáticamente.
     */
    @Override
    public void eliminar(int id) {
        Connection con = conexion.conectar();
        if (con == null) return;

        try {
            con.setAutoCommit(false);

            // Guardamos id_persona ANTES de borrar usuarios
            int idPersona = -1;
            try (PreparedStatement psGetPersona = con.prepareStatement(
                    "SELECT id_persona FROM usuarios WHERE idusuario = ?")) {
                psGetPersona.setInt(1, id);
                try (ResultSet rs = psGetPersona.executeQuery()) {
                    if (rs.next()) idPersona = rs.getInt("id_persona");
                }
            }
            if (idPersona == -1) { con.rollback(); return; }

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
            try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Error al eliminar usuario id=" + id, e);
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
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
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar credenciales", e);
        } finally {
            conexion.desconectar();
        }
        return Optional.empty();
    }

    // ── Mapeo ResultSet → Usuario ──────────────────────────────────────────────

    /**
     * Convierte una fila de la consulta en un objeto Usuario.
     * Carga todos los datos: id, nombre, email, contraseña, documento, categoría y afiliación.
     */
    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("idusuario"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("contraseña"),
                rs.getString("tipodocumento"),
                rs.getString("numdocumento"),
                rs.getBoolean("esAfiliado"),
                rs.getString("categoria")
        );
    }
}
package dao;

import java.util.List;
import java.util.Optional;

import entidades.Usuario;

/**
 * Contrato de acceso a datos para personas del sistema.
 *
 * En esta versión del proyecto, la entidad persistida en la base de datos
 * para usuarios y administradores sigue siendo la combinación de tablas
 * persona + usuarios.
 *
 * La intención de este contrato es que la capa de negocio trabaje sobre el
 * concepto Persona y no sobre un DAO llamado UsuarioDAO.
 */
public interface IPersonaDAO {

    void insertar(Usuario persona);

    Optional<Usuario> buscarPorId(int id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> listarTodos();

    void actualizar(Usuario persona);

    void actualizarContrasena(int idPersona, String nuevaContrasena);

    void actualizarRol(int idPersona, String nombreRol);

    void eliminar(int id);

    Optional<Usuario> verificarCredenciales(String email, String contrasena);
}

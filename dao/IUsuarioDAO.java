package dao;

import entidades.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para acceder a los datos de usuarios.
 * Define las operaciones que se pueden realizar: crear, buscar, listar, actualizar y eliminar usuarios.
 */
public interface IUsuarioDAO {

    /**
     * Crea un nuevo usuario en la base de datos.
     * Guarda todos los datos y asigna un ID al usuario creado.
     */
    void insertar(Usuario usuario);

    /**
     * Busca un usuario por su identificador (ID).
     * Retorna el usuario si existe, o vacío si no.
     */
    Optional<Usuario> buscarPorId(int id);

    /**
     * Busca un usuario por su email.
     * Útel para login y para verificar si un email ya está registrado.
     */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Obtiene todos los usuarios registrados.
     */
    List<Usuario> listarTodos();

    /**
     * Modifica los datos del usuario (nombre, email, documento, categoría, afiliación).
     */
    void actualizar(Usuario usuario);

    /**
     * Cambia la contraseña de un usuario.
     */
    void actualizarContrasena(int idUsuario, String nuevaContrasena);

    /**
     * Borra un usuario de la base de datos.
     */
    void eliminar(int id);

    /**
     * Valida email y contraseña para permitir el acceso (login).
     * Retorna el usuario si las credenciales son correctas, o vacío si no.
     */
    Optional<Usuario> verificarCredenciales(String email, String contrasena);
}


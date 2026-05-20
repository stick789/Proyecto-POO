package dao;

import java.util.List;
import java.util.Optional;

import entidades.Persona;
import entidades.Usuario;

/**
 * IPersonaDAO — Contrato de acceso a datos para personas del sistema.
 *
 * <p><b>Mejoras respecto a la versión original:</b></p>
 * <ol>
 *   <li><b>Polimorfismo en lectura</b>: los métodos de consulta retornan {@link Persona}
 *       (tipo padre), lo que permite que {@code buscarPorEmail} y {@code buscarPorId}
 *       devuelvan tanto {@link Usuario} como {@link entidades.Administrador} sin
 *       duplicar la lógica de autenticación.</li>
 *   <li><b>Paginación</b>: {@link #listar(String, int, int)} reemplaza al antiguo
 *       {@code listarTodos()} sin filtro ni límite, reduciendo la carga en BD
 *       cuando la tabla crece (mismo patrón de {@code CrudPaginadoInterface} del
 *       sistema de referencia comentado).</li>
 *   <li><b>{@link #existe(String)}</b>: verifica unicidad de email antes de insertar,
 *       evitando llegar a la BD con un duplicado que rompe el constraint.</li>
 *   <li><b>{@link #total()}</b>: cuenta registros para calcular el número de páginas
 *       en la capa de presentación.</li>
 *   <li><b>Soft delete ({@link #activar}/{@link #desactivar})</b>: baja lógica en lugar
 *       de DELETE físico; requiere la columna {@code activo TINYINT(1)} en
 *       {@code usuarios} (ver script SQL adjunto).</li>
 *   <li><b>Separación de la verificación de contraseña</b>: el antiguo
 *       {@code verificarCredenciales()} hacía el cotejo de clave directamente en SQL.
 *       Ahora la capa de negocio obtiene la persona con {@link #buscarPorEmail(String)}
 *       y realiza la verificación con PBKDF2 en {@code PersonaControl.login()},
 *       siguiendo el mismo patrón del sistema de referencia comentado
 *       ({@code UsuarioControl.login()}).</li>
 * </ol>
 *
 * <p><b>Métodos de escritura</b> usan {@link Usuario} porque el registro y la edición
 * de datos aplican solo a usuarios regulares (los admins se gestionan en BD).</p>
 *
 * <p><b>Métodos de lectura</b> retornan {@link Persona} para trabajar de forma
 * polimórfica con {@link Usuario} y {@link entidades.Administrador}.</p>
 */
public interface IPersonaDAO {

    // ---------------------------------------------------------------- ESCRITURA

    /**
     * Inserta un nuevo usuario (registros en {@code persona} + {@code usuarios}).
     * Verificar con {@link #existe(String)} antes de llamar este método.
     *
     * @param persona objeto {@link Usuario} con todos los datos, incluyendo la
     *                contraseña ya hasheada (responsabilidad de {@code PersonaControl}).
     */
    void insertar(Usuario persona);

    /**
     * Actualiza los datos personales y de usuario de un usuario regular.
     * No modifica la contraseña; para eso usar {@link #actualizarContrasena}.
     */
    void actualizar(Usuario persona);

    /**
     * Actualiza únicamente la contraseña almacenada (ya hasheada).
     * Se usa para migración de hashes legacy SHA-256 → PBKDF2.
     */
    void actualizarContraseña(int idPersona, String nuevaContrasena);

    /** Cambia el rol de una persona en la tabla {@code usuarios}. */
    void actualizarRol(int idPersona, String nombreRol);

    /**
     * Desactivación lógica: establece {@code activo = 0} en {@code usuarios}.
     * <b>Requiere columna {@code activo TINYINT(1)} en la tabla (ver script SQL).</b>
     *
     * @return {@code true} si se actualizó al menos una fila.
     */
    boolean desactivar(int id);

    /**
     * Reactiva una persona previamente desactivada: {@code activo = 1}.
     * <b>Requiere columna {@code activo TINYINT(1)} en la tabla (ver script SQL).</b>
     *
     * @return {@code true} si se actualizó al menos una fila.
     */
    boolean activar(int id);

    /**
     * Eliminación física del registro (persona + usuario).
     * Preferir {@link #desactivar} para auditoría. Usar {@code eliminar}
     * solo cuando se requiera borrar datos por normativa (GDPR, etc.).
     */
    void eliminar(int id);

    // ------------------------------------------------------------------ LECTURA

    /**
     * Lista personas de forma paginada filtrando por nombre.
     *
     * <p>Reemplaza al anterior {@code listarTodos()} sin filtro ni límite.
     * Cálculo de offset: {@code LIMIT (numPagina - 1) * totalPorPagina, totalPorPagina}</p>
     *
     * @param texto          fragmento de nombre para filtrar (vacío = todos).
     * @param totalPorPagina registros por página (ej: 10).
     * @param numPagina      página actual empezando en 1.
     * @return lista de {@link Persona} (puede ser {@link Usuario} o
     *         {@link entidades.Administrador} según el rol en BD).
     */
    List<Persona> listar(String texto, int totalPorPagina, int numPagina);

    /**
     * Busca por ID. Retorna {@link Persona}; puede ser {@link Usuario} o
     * {@link entidades.Administrador} según el rol almacenado en BD.
     */
    Optional<Persona> buscarPorId(int id);

    /**
     * Busca por email incluyendo la contraseña hasheada para que
     * {@code PersonaControl.login()} pueda verificar el hash PBKDF2.
     *
     * <p>Ya no existe {@code verificarCredenciales(email, contrasena)} que
     * cotejaba la clave en SQL. La verificación se hace en {@code PersonaControl},
     * igual que en el sistema de referencia ({@code UsuarioControl.login()}).</p>
     *
     * @return {@link Persona} con la contraseña cargada para verificar el hash,
     *         o {@code Optional.empty()} si no existe el email.
     */
    Optional<Persona> buscarPorEmail(String email);

    // --------------------------------------------------------------- AUXILIARES

    /**
     * Verifica si ya existe una persona con ese email.
     * Se llama antes de {@link #insertar} para validar unicidad.
     *
     * @return {@code true} si ya existe un registro con ese email.
     */
    boolean existe(String email);

    /**
     * Retorna el total de registros activos en {@code usuarios}.
     * Usado para calcular el número de páginas:
     * {@code totalPaginas = (int) Math.ceil((double) total() / totalPorPagina)}.
     */
    int total();
}

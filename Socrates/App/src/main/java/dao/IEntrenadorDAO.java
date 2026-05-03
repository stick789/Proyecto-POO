package dao;

import java.util.List;
import java.util.Optional;

import entidades.Entrenador;

/**
 * IEntrenadorDAO — Contrato de acceso a datos para entrenadores.
 *
 * <p>Los entrenadores se almacenan como una fila en {@code persona} + {@code usuarios}
 * (con {@code rol = 'ENTRENADOR'}) y adicionalmente en la tabla {@code entrenador}
 * que guarda la especialidad.</p>
 *
 * <p>Diseño inspirado en el patrón {@code CrudPaginadoInterface} del sistema de
 * referencia comentado: paginación, conteo y filtro por nombre.</p>
 */
public interface IEntrenadorDAO {

    // ---------------------------------------------------------------- ESCRITURA

    /**
     * Inserta un nuevo entrenador.
     * Crea registros en {@code persona}, {@code usuarios} (con rol ENTRENADOR)
     * y {@code entrenador} (especialidad) en una sola transacción.
     */
    void insertar(Entrenador entrenador, String contraseñaHash);

    /**
     * Actualiza los datos personales y la especialidad del entrenador.
     * No modifica la contraseña.
     */
    void actualizar(Entrenador entrenador);

    /**
     * Baja lógica: establece {@code activo = 0} en {@code usuarios}.
     *
     * @return {@code true} si se actualizó al menos una fila.
     */
    boolean desactivar(int idEntrenador);

    /**
     * Reactiva el entrenador: establece {@code activo = 1} en {@code usuarios}.
     *
     * @return {@code true} si se actualizó al menos una fila.
     */
    boolean activar(int idEntrenador);

    /** Eliminación física del entrenador (entrenador + usuario + persona). */
    void eliminar(int idEntrenador);

    // ------------------------------------------------------------------ LECTURA

    /**
     * Busca un entrenador por su ID (equivale al {@code idusuario} en {@code usuarios}).
     */
    Optional<Entrenador> buscarPorId(int id);

    /**
     * Lista entrenadores paginados y filtrados por nombre.
     *
     * @param texto          fragmento de nombre (vacío = todos).
     * @param totalPorPagina registros por página.
     * @param numPagina      página actual empezando en 1.
     */
    List<Entrenador> listar(String texto, int totalPorPagina, int numPagina);

    /**
     * Lista todos los entrenadores activos con la especialidad dada.
     *
     * @param especialidad "Natación" o "Gimnasio"
     */
    List<Entrenador> listarPorEspecialidad(String especialidad);

    /**
     * Retorna el total de entrenadores activos.
     * Usado para calcular el número de páginas:
     * {@code totalPaginas = (int) Math.ceil((double) total() / totalPorPagina)}
     */
    int total();
}

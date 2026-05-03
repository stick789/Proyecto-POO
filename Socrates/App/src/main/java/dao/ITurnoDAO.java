package dao;

import java.util.List;
import java.util.Optional;

import entidades.Turno;

/**
 * ITurnoDAO — Contrato de acceso a datos para turnos.
 *
 * <p><b>Cambios respecto a la versión anterior:</b></p>
 * <ul>
 *   <li>Se añade {@link #listarTodos()} para consultas administrativas.</li>
 *   <li>{@link #actualizarEntrenador(int, Integer)} ya existía; se documenta
 *       que acepta {@code null} para desasignar un entrenador.</li>
 * </ul>
 */
public interface ITurnoDAO {

    /**
     * Crea un nuevo turno en la base de datos.
     * Asigna el ID generado al objeto turno.
     */
    void insertar(Turno turno);

    /**
     * Busca un turno por su ID.
     */
    Optional<Turno> buscarPorId(int id);

    /**
     * Lista todos los turnos de un usuario, ordenados por fecha descendente.
     */
    List<Turno> listarPorUsuario(int idUsuario);

    /**
     * Lista todos los turnos de una instalación.
     */
    List<Turno> listarPorInstalacion(int idInstalacion);

    /**
     * Lista solo los turnos en estado RESERVADO de una instalación.
     */
    List<Turno> listarReservadosPorInstalacion(int idInstalacion);

    /**
     * Lista todos los turnos del sistema (para vistas de administrador).
     * Ordenados por fecha descendente.
     */
    List<Turno> listarTodos();

    /**
     * Actualiza el estado de un turno.
     *
     * @param idTurno    ID del turno.
     * @param nuevoEstado uno de: {@code RESERVADO}, {@code CANCELADO}, {@code COMPLETADO}.
     */
    void actualizarEstado(int idTurno, String nuevoEstado);

    /**
     * Actualiza el entrenador asociado a un turno.
     *
     * @param idTurno     ID del turno.
     * @param idEntrenador ID del entrenador, o {@code null} para desasignar.
     */
    void actualizarEntrenador(int idTurno, Integer idEntrenador);

    /**
     * Elimina físicamente un turno de la base de datos.
     */
    void eliminar(int id);
}

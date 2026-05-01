package dao;

import java.util.List;
import java.util.Optional;

import entidades.Turno;

/**
 * Contrato para acceder a los datos de turnos.
 * Define las operaciones que se pueden realizar: crear, buscar, listar, actualizar y eliminar turnos.
 */
public interface ITurnoDAO {
 
    /**
     * Crea un nuevo turno en la base de datos.
     * Asigna un ID al turno creado.
     */
    void insertar(Turno turno);
 
    /**
     * Busca un turno por su identificador.
     */
    Optional<Turno> buscarPorId(int id);

    /**
     * Obtiene todos los turnos de un usuario.
     */
    List<Turno> listarPorUsuario(int idUsuario);
 
    /**
     * Obtiene todos los turnos de una instalación.
     */
    List<Turno> listarPorInstalacion(int idInstalacion);
 
    /**
     * Obtiene solo los turnos reservados de una instalación.
     */
    List<Turno> listarReservadosPorInstalacion(int idInstalacion);
 
    /**
     * Cambia el estado de un turno (RESERVADO, CANCELADO, COMPLETADO).
     */
    void actualizarEstado(int idTurno, String nuevoEstado);
    /**
     * Actualiza el id del entrenador asociado a un turno.
     * Puede ser null si no hay entrenador asignado.
     */
    void actualizarEntrenador(int idTurno, Integer idEntrenador);
 
    /**
     * Borra un turno de la base de datos.
     */
    void eliminar(int id);
}

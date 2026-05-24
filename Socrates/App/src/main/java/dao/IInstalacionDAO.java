package dao;

import entidades.Instalacion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para acceder a los datos de instalaciones.
 */
public interface IInstalacionDAO {

    void insertar(Instalacion instalacion);

    Optional<Instalacion> buscarPorId(int id);

    List<Instalacion> listarTodos();

    List<Instalacion> listarGimnasios();

    List<Instalacion> listarPiscinas();

    /** Obtiene solo los gimnasios que pertenecen a una sede concreta. */
    List<Instalacion> listarGimnasiosPorSede(int idSede);

    /** Obtiene solo las piscinas que pertenecen a una sede concreta. */
    List<Instalacion> listarPiscinasPorSede(int idSede);

    void actualizar(Instalacion instalacion);

    void eliminar(int id);

    void actualizarAforo(int idInstalacion, int nuevoAforo);
}
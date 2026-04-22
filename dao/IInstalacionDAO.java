package dao;

import entidades.Instalacion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para acceder a los datos de instalaciones.
 * Define las operaciones que se pueden realizar: crear, buscar, listar, actualizar y eliminar instalaciones.
 */
public interface IInstalacionDAO {

    /**
     * Crea una nueva instalación en la base de datos.
     * Asigna un ID a la instalación creada.
     */
    void insertar(Instalacion instalacion);

    /**
     * Busca una instalación por su identificador.
     */
    Optional<Instalacion> buscarPorId(int id);

    /**
     * Obtiene todas las instalaciones registradas.
     */
    List<Instalacion> listarTodos();

    /**
     * Obtiene solo los gimnasios.
     */
    List<Instalacion> listarGimnasios();

    /**
     * Obtiene solo las piscinas.
     */
    List<Instalacion> listarPiscinas();

    /**
     * Modifica los datos de una instalación.
     */
    void actualizar (Instalacion instalacion);

    /**
     * Borra una instalación de la base de datos.
     */
    void eliminar(int id);

    /**
     * Actualiza los cupos disponibles de una instalación.
     */
    void actualizarAforo(int idInstalacion, int nuevoAforo);
}


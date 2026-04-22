package dao;

import entidades.Historial_citas;
import java.util.List;

/**
 * Contrato para acceder a los datos del historial de citas.
 * Permite registrar y consultar todos los eventos relacionados con turnos.
 */
public interface IHistorialCitasDAO {

    /**
     * Registra un evento en el historial de citas.
     * Guarda acciones como reservas, cancelaciones, reagendamientos y completaciones.
     */
    void insertar(Historial_citas historial);

    /**
     * Obtiene todos los eventos del historial de un usuario específico.
     * Los eventos se ordenan de más reciente a más antiguo.
     */
    List<Historial_citas> listarPorUsuario(int idUsuario);

    /**
     * Obtiene todos los eventos del historial de un turno específico.
     * Los eventos se ordenan de más reciente a más antiguo.
     */
    List<Historial_citas> listarPorTurno(int idTurno);

    /**
     * Obtiene todos los eventos del historial de citas registrados.
     */
    List<Historial_citas> listarTodos();
}

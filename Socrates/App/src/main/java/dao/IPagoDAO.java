package dao;

import java.util.List;
import java.util.Optional;

import entidades.Pago;

/**
 * IPagoDAO — Contrato de acceso a datos para pagos.
 *
 * <p>Gestiona el ciclo de vida de los pagos asociados a turnos reservados.
 * Un pago siempre referencia un turno existente y un usuario.</p>
 *
 * <p>Estados de pago permitidos: {@code PENDIENTE}, {@code COMPLETADO},
 * {@code FALLIDO}, {@code REEMBOLSADO}.</p>
 */
public interface IPagoDAO {

    // ---------------------------------------------------------------- ESCRITURA

    /**
     * Registra un nuevo pago en la base de datos.
     * El ID del pago generado se asigna automáticamente al objeto.
     *
     * @param pago objeto con todos los datos del pago (monto, método, estado, idTurno, idUsuario).
     */
    void insertar(Pago pago);

    /**
     * Actualiza el estado del pago.
     *
     * @param idPago      ID del pago a actualizar.
     * @param nuevoEstado nuevo estado ({@code PENDIENTE}, {@code COMPLETADO}, etc.).
     * @return {@code true} si se actualizó al menos una fila.
     */
    boolean actualizarEstado(long idPago, String nuevoEstado);

    // ------------------------------------------------------------------ LECTURA

    /**
     * Busca un pago por su ID.
     */
    Optional<Pago> buscarPorId(long id);

    /**
     * Lista todos los pagos de un usuario dado.
     *
     * @param idUsuario ID del usuario propietario de los pagos.
     */
    List<Pago> listarPorUsuario(int idUsuario);

    /**
     * Lista todos los pagos asociados a un turno específico.
     *
     * @param idTurno ID del turno.
     */
    List<Pago> listarPorTurno(int idTurno);

    /**
     * Retorna el total de pagos registrados en el sistema.
     * Útil para paginación en vistas de administrador.
     */
    int total();
}

package LogicaCita;

import dao.IInstalacionDAO;
import dao.ITurnoDAO;
import entidades.Instalacion;
import entidades.Turno;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ConsultaTurnos — servicio dedicado SOLO a consultas y filtros.
 *
 * POR QUÉ EXISTE:
 *   ServicioTurnos estaba mezclando operaciones de escritura (reservar, cancelar)
 *   con necesidades de lectura (¿hay cupos? ¿qué turnos tiene hoy?).
 *   Al separar las consultas aquí:
 *   - ServicioTurnos queda enfocado en transacciones.
 *   - La GUI puede llamar consultas sin riesgo de modificar estado.
 *
 * PRINCIPIO APLICADO: Command Query Separation (CQS) —
 *   los métodos que leen no modifican, los que modifican no devuelven datos.
 */
public class ConsultaTurnos {

    private final ITurnoDAO        turnoDAO;
    private final IInstalacionDAO  instalacionDAO;
    private final ValidadorTurno   validador;

    public ConsultaTurnos(ITurnoDAO turnoDAO, IInstalacionDAO instalacionDAO) {
        this.turnoDAO       = turnoDAO;
        this.instalacionDAO = instalacionDAO;
        this.validador      = new ValidadorTurno();
    }

    // ── Consultas por usuario ──────────────────────────────────────────────────

    /**
     * Devuelve los turnos RESERVADOS del usuario, ordenados de más próximo a más lejano.
     */
    public List<Turno> obtenerTurnosActivosPorUsuario(int idUsuario) {
        return turnoDAO.listarPorUsuario(idUsuario).stream()
                .filter(t -> Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                .sorted(Comparator.comparing(Turno::getFechaHora))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el próximo turno reservado del usuario (el más cercano en el tiempo).
     * Devuelve Optional.empty() si el usuario no tiene turnos futuros.
     */
    public Optional<Turno> obtenerProximoTurno(int idUsuario) {
        return obtenerTurnosActivosPorUsuario(idUsuario).stream().findFirst();
    }

    /**
     * Historial completo del usuario: cancelados + completados, más reciente primero.
     */
    public List<Turno> obtenerHistorialPorUsuario(int idUsuario) {
        return turnoDAO.listarPorUsuario(idUsuario).stream()
                .filter(t -> !Turno.ESTADO_RESERVADO.equals(t.getEstado()))
                .sorted(Comparator.comparing(Turno::getFechaHora).reversed())
                .collect(Collectors.toList());
    }

    // ── Consultas por instalación ──────────────────────────────────────────────

    /**
     * Turnos RESERVADOS de una instalación que ocurren HOY.
     * Útil para la vista de administrador del día.
     */
    public List<Turno> obtenerTurnosDeHoy(int idInstalacion) {
        LocalDate hoy = LocalDate.now();
        return turnoDAO.listarReservadosPorInstalacion(idInstalacion).stream()
                .filter(t -> t.getFechaHora().toLocalDate().equals(hoy))
                .sorted(Comparator.comparing(Turno::getFechaHora))
                .collect(Collectors.toList());
    }

    /**
     * Cuenta cuántos turnos RESERVADOS tiene una instalación en este momento.
     * Refleja la ocupación real (independiente del aforoActual del objeto en memoria).
     */
    public int contarReservasActivas(int idInstalacion) {
        return turnoDAO.listarReservadosPorInstalacion(idInstalacion).size();
    }

    /**
     * Verifica si una instalación tiene cupos disponibles para un horario dado.
     *
     * La comprobación es doble:
     *   1. aforoActual > 0 (hay capacidad general disponible)
     *   2. No hay solapamiento con turnos ya reservados en ese bloque horario
     *
     * @param idInstalacion id de la instalación a consultar
     * @param fechaHora     inicio del bloque que se quiere reservar
     * @param duracion      duración en minutos del bloque
     * @return true si el bloque está libre
     */
    public boolean estaDisponible(int idInstalacion, LocalDateTime fechaHora, int duracion) {
        Optional<Instalacion> optInst = instalacionDAO.buscarPorId(idInstalacion);
        if (optInst.isEmpty()) return false;

        Instalacion instalacion = optInst.get();
        if (instalacion.getAforoActual() <= 0) return false;

        List<Turno> reservasActivas = turnoDAO.listarReservadosPorInstalacion(idInstalacion);
        return !validador.haySolapamiento(reservasActivas, fechaHora, duracion);
    }

    /**
     * Devuelve los turnos de una instalación en un rango de fechas.
     * Útil para generar reportes semanales o mensuales.
     *
     * @param idInstalacion  instalación a consultar
     * @param desde          inicio del rango (inclusive)
     * @param hasta          fin del rango (inclusive)
     */
    public List<Turno> obtenerPorRangoFechas(int idInstalacion,
                                              LocalDateTime desde,
                                              LocalDateTime hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("Rango de fechas invalido.");
        }
        return turnoDAO.listarPorInstalacion(idInstalacion).stream()
                .filter(t -> !t.getFechaHora().isBefore(desde)
                          && !t.getFechaHora().isAfter(hasta))
                .sorted(Comparator.comparing(Turno::getFechaHora))
                .collect(Collectors.toList());
    }

    /**
     * Porcentaje de ocupación actual de una instalación.
     * 0.0 = vacía, 1.0 = llena al máximo.
     *
     * Fórmula: (capacidadMaxima - aforoActual) / capacidadMaxima
     */
    public double getPorcentajeOcupacion(int idInstalacion) {
        return instalacionDAO.buscarPorId(idInstalacion)
                .map(inst -> {
                    if (inst.getCapacidadMaxima() == 0) return 0.0;
                    double ocupados = inst.getCapacidadMaxima() - inst.getAforoActual();
                    return ocupados / inst.getCapacidadMaxima();
                })
                .orElse(0.0);
    }
}
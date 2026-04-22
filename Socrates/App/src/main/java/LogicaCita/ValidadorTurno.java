package LogicaCita;

import entidades.Instalacion;
import entidades.Turno;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ValidadorTurno — Centraliza todas las validaciones de reglas de negocio para turnos.
 *
 * FUNCIONALIDAD:
 *   • Valida fechas: asegura que las reservas se hagan con anticipación mínima.
 *   • Valida duraciones: verifica que estén dentro del rango permitido.
 *   • Valida aforo: comprueba que la instalación tenga cupos disponibles.
 *   • Detecta solapamientos: verifica que los turnos del usuario no se crucen.
 *   • Valida reservas completas: ejecuta todas las validaciones antes de persistir.
 *
 * PRINCIPIO:
 *   Esta clase es responsable solo de validar, no de persistir ni modificar datos.
 */
public class ValidadorTurno {

    // Constantes de reglas de negocio 
    public static final int DURACION_MINIMA_MINUTOS  = 15;
    public static final int DURACION_MAXIMA_MINUTOS  = 120;
    public static final int HORAS_RESERVA_ANTICIPADA = 1;

    // Validaciones individuales 

    /**
     * Valida que la fecha esté suficientemente en el futuro.
     * Requiere mínimo 1 hora de anticipación para hacer la reserva.
     */
    public void validarFechaFutura(LocalDateTime fechaHora) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora del turno no puede ser null.");
        }
        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(HORAS_RESERVA_ANTICIPADA);
        if (fechaHora.isBefore(limiteMinimo)) {
            throw new IllegalArgumentException(
                "La reserva debe hacerse con al menos " + HORAS_RESERVA_ANTICIPADA +
                " hora(s) de anticipacion. Fecha minima: " + limiteMinimo
            );
        }
    }

    /**
     * Valida que la duración esté dentro del rango permitido.
     */
    public void validarDuracion(int duracionMinutos) {
        if (duracionMinutos < DURACION_MINIMA_MINUTOS || duracionMinutos > DURACION_MAXIMA_MINUTOS) {
            throw new IllegalArgumentException(
                "La duracion debe estar entre " + DURACION_MINIMA_MINUTOS +
                " y " + DURACION_MAXIMA_MINUTOS + " minutos. Valor recibido: " + duracionMinutos
            );
        }
    }

    /**
     * Valida que la instalación tenga al menos un cupo disponible.
     */
    public void validarInstalacionConCupos(Instalacion instalacion) {
        if (instalacion == null) {
            throw new IllegalArgumentException("La instalacion no puede ser null.");
        }
        if (instalacion.getAforoActual() <= 0) {
            throw new IllegalStateException(
                "La instalacion '" + instalacion.getTipo() +
                "' no tiene cupos disponibles en este momento."
            );
        }
    }

    /**
     * Comprueba si dos turnos se solapan en tiempo.
     * Retorna true si el nuevo turno coincide con alguno existente.
     * Solo analiza turnos en estado RESERVADO.
     */
    public boolean haySolapamiento(List<Turno> turnosExistentes,
                                   LocalDateTime nuevaFechaHora,
                                   int nuevaDuracion) {
        if (turnosExistentes == null || turnosExistentes.isEmpty()) return false;

        LocalDateTime finNuevo = nuevaFechaHora.plusMinutes(nuevaDuracion);

        for (Turno existente : turnosExistentes) {
            if (!Turno.ESTADO_RESERVADO.equals(existente.getEstado())) continue;

            LocalDateTime inicioExistente = existente.getFechaHora();
            LocalDateTime finExistente    = inicioExistente.plusMinutes(existente.getDuracionMinutos());

            boolean seSolapan = nuevaFechaHora.isBefore(finExistente)
                             && finNuevo.isAfter(inicioExistente);

            if (seSolapan) return true;
        }
        return false;
    }

    /**
     * Verifica que no haya solapamiento con otros turnos.
     * Lanza excepción si el nuevo turno se cruza con alguno existente.
     */
    public void validarSinSolapamiento(List<Turno> turnosExistentes,
                                       LocalDateTime nuevaFechaHora,
                                       int nuevaDuracion) {
        if (haySolapamiento(turnosExistentes, nuevaFechaHora, nuevaDuracion)) {
            throw new IllegalStateException(
                "El usuario ya tiene un turno reservado que se solapa con el horario solicitado."
            );
        }
    }

    /**
     * Valida todas las condiciones juntas antes de persistir una reserva.
     */
    public void validarReserva(Instalacion instalacion,
                               LocalDateTime fechaHora,
                               int duracion,
                               List<Turno> turnosActivos) {
        validarFechaFutura(fechaHora);
        validarDuracion(duracion);
        validarInstalacionConCupos(instalacion);
        validarSinSolapamiento(turnosActivos, fechaHora, duracion);
    }
}
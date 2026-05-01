package LogicaCita;

import java.time.LocalDateTime;
import java.util.List;

import entidades.Instalacion;
import entidades.Turno;

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
     * Limita la cantidad de citas reservadas por usuario en un mismo día.
     */
    public void validarMaximoCitasPorDia(List<Turno> turnosActivosUsuario, LocalDateTime nuevaFechaHora) {
        if (nuevaFechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora del turno no puede ser null.");
        }

        int turnosDelDia = 0;
        if (turnosActivosUsuario != null) {
            for (Turno turno : turnosActivosUsuario) {
                if (turno != null
                        && turno.getFechaHora() != null
                        && turno.getFechaHora().toLocalDate().equals(nuevaFechaHora.toLocalDate())) {
                    turnosDelDia++;
                }
            }
        }

        if (turnosDelDia >= 3) {
            throw new IllegalStateException("Solo se permiten un máximo de 3 citas por día para un usuario.");
        }
    }
    // Valida disponibilidad y capacidad maxima de la instalacion.
    private void validarCapacidadGlobal(Instalacion instalacion, List<Turno> turnosActivos) {
        if (instalacion == null) {
            throw new IllegalArgumentException("La instalacion no puede ser null.");
        }
        // Verifica que la instalación tenga cupos disponibles (aforoActual > 0)
        if (instalacion.getAforoActual() <= 0) {
            throw new IllegalStateException(
                "La instalacion '" + instalacion.getTipo() +
                "' no tiene cupos disponibles en este momento."
            );
        }
        // Si la instalación tiene una capacidad máxima definida (>0), verifica que no se haya alcanzado.
        if (instalacion.getCapacidadMaxima() > 0) {
            int reservasActivas = turnosActivos == null ? 0 : turnosActivos.size();
            if (reservasActivas >= instalacion.getCapacidadMaxima()) {
                throw new IllegalStateException(
                    "La instalacion '" + instalacion.getTipo() +
                    "' ha alcanzado su capacidad maxima de " + instalacion.getCapacidadMaxima() + " reservas."
                );
            }
        }
    }
    


    /**
     * Valida todas las condiciones juntas antes de persistir una reserva.
     */
    public void validarReserva(Instalacion instalacion,
                               LocalDateTime fechaHora,
                               int duracion,
                               List<Turno> turnosActivosUsuario,
                               List<Turno> reservasActivasInstalacion) {
        validarFechaFutura(fechaHora);
        validarDuracion(duracion);
        validarSinSolapamiento(turnosActivosUsuario, fechaHora, duracion);
        validarMaximoCitasPorDia(turnosActivosUsuario, fechaHora);
        validarCapacidadGlobal(instalacion, reservasActivasInstalacion);
    }
        
}

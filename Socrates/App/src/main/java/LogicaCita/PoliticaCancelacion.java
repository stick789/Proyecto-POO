package LogicaCita;

import entidades.Turno;
import entidades.Usuario;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * PoliticaCancelacion — Define las reglas y restricciones para cancelar turnos.
 *
 * FUNCIONALIDAD:
 *   • Valida si un turno puede ser cancelado según fecha, estado y categoría del usuario.
 *   • Diferencia entre usuarios estándar y administradores (estos pueden cancelar sin restricciones).
 *   • Aplica tolerancia especial para usuarios de categoría A (premium).
 *   • Proporciona mensajes claros cuando una cancelación no es posible.
 *
 * REGLAS DE CANCELACIÓN:
 *   • Solo se pueden cancelar turnos en estado RESERVADO.
 *   • Usuario estándar: debe cancelar con mínimo 2 horas de anticipación.
 *   • Usuario categoría A: puede cancelar con 1 hora de anticipación (tiene descuento).
 *   • Administrador: puede cancelar cualquier turno RESERVADO sin restricción de tiempo.
 */
public class PoliticaCancelacion {

    /** Horas mínimas de anticipación para cancelar (usuario estándar). */
    public static final int HORAS_MINIMAS_CANCELACION = 2;

    /**
     * Reducción de horas para usuarios categoría A (afiliados premium).
     * Categoría A puede cancelar con (HORAS_MINIMAS - TOLERANCIA) horas de anticipación.
     */
    public static final int TOLERANCIA_CATEGORIA_A = 1;

    //API pública 

    /**
     * Devuelve true si el turno puede cancelarse según las reglas vigentes.
     */
    public boolean puedeCancelar(Turno turno) {
        if (turno == null) return false;
        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) return false;

        long horasRestantes = horasHastaElTurno(turno);
        int minimoRequerido = calcularMinimoHoras(turno.getUsuario());
        return horasRestantes >= minimoRequerido;
    }

    /**
     * Valida si el turno puede ser cancelado.
     * Si no es posible, lanza una excepción con el motivo.
     */
    public void validarCancelacion(Turno turno) {
        if (!puedeCancelar(turno)) {
            throw new IllegalStateException(motivoCancelacionBloqueada(turno));
        }
    }

    /**
     * Devuelve un mensaje explicando por qué no se puede cancelar.
     * Útil para mostrar en la GUI sin lanzar excepción.
     */
    public String motivoCancelacionBloqueada(Turno turno) {
        if (turno == null) return "El turno no puede ser null.";

        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            return "Solo se puede cancelar un turno en estado RESERVADO. Estado actual: " + turno.getEstado();
        }

        long horasRestantes = horasHastaElTurno(turno);
        int minimoRequerido = calcularMinimoHoras(turno.getUsuario());

        return "No se puede cancelar. Faltan " + horasRestantes +
               " hora(s) para el turno y se requieren al menos " + minimoRequerido +
               " hora(s) de anticipacion.";
    }

    /**
     * Horas que faltan hasta que comience el turno.
     * Valor negativo = el turno ya pasó.
     */
    public long horasHastaElTurno(Turno turno) {
        if (turno == null || turno.getFechaHora() == null) return -1;
        return Duration.between(LocalDateTime.now(), turno.getFechaHora()).toHours();
    }

    /**
     * Un administrador puede cancelar cualquier turno RESERVADO
     * sin restricción de tiempo.
     */
    public boolean puedeCancelarComoAdmin(Turno turno, boolean esAdministrador) {
        if (!esAdministrador) return puedeCancelar(turno);
        return turno != null && Turno.ESTADO_RESERVADO.equals(turno.getEstado());
    }

    // Privados 

    private int calcularMinimoHoras(Usuario usuario) {
        if (usuario != null && "A".equalsIgnoreCase(usuario.getCategoria())) {
            return Math.max(0, HORAS_MINIMAS_CANCELACION - TOLERANCIA_CATEGORIA_A);
        }
        return HORAS_MINIMAS_CANCELACION;
    }
}
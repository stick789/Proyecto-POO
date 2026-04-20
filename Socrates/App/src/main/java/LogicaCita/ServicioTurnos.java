package LogicaCita;
/*
 * Esta clase se encarga de gestionar la lógica de negocio relacionada con los turnos, incluyendo la reserva, cancelación y finalización de turnos. 
 * Se asegura de que las reglas de negocio se cumplan, como verificar el estado del turno antes de permitir ciertas acciones y actualizar el aforo de las instalaciones.
 */
import java.time.LocalDateTime;

import entidades.Instalacion;
import entidades.Turno;
import entidades.Usuario;

public class ServicioTurnos {
    /**
     * Datos nesesarios para el servicio de turnos
     * En este caso, no se necesitan atributos específicos para el servicio de turnos, ya que todas las operaciones 
     * se realizan a través de los métodos y se basan en los objetos Turno
     **/
    public Turno reservarTurno(String idTurno, LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion) {
      // Validamos que la instalación no sea null antes de intentar reservar un turno
        if (instalacion == null) {
            throw new IllegalArgumentException("La instalacion no puede ser null.");
        }

        instalacion.descontarCupo();// Descontamos un cupo en la instalación al reservar un turno
        return new Turno(idTurno, fechaHora, duracionMinutos, usuario, instalacion);// Creamos y devolvemos un nuevo turno con el estado inicial de RESERVADO
    }
// Método para cancelar un turno, validando que el turno esté en estado RESERVADO antes de permitir la cancelación
    public void cancelarTurno(Turno turno) {
        validarTurno(turno);

        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede cancelar un turno en estado RESERVADO.");
        }

        turno.setEstado(Turno.ESTADO_CANCELADO);// Cambia el estado del turno a CANCELADO
        turno.getInstalacion().liberarCupo();// Libera un cupo en la instalación al cancelar el turno
    }
// Método para completar un turno, validando que el turno esté en estado RESERVADO antes de permitir la finalización
    public void completarTurno(Turno turno) {
        validarTurno(turno);

        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede completar un turno en estado RESERVADO.");
        }

        turno.setEstado(Turno.ESTADO_COMPLETADO);// Cambia el estado del turno a COMPLETADO
        turno.getInstalacion().liberarCupo();// Libera un cupo en la instalación al completar el turno
    }
// Método privado para validar que el turno no sea null antes de realizar operaciones sobre él
    private void validarTurno(Turno turno) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }
    }

    
}

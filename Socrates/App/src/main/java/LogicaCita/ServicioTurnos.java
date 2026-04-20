package LogicaCita;
/*
 * Esta clase se encarga de gestionar la lógica de negocio relacionada con los turnos, incluyendo la reserva, cancelación y finalización de turnos. 
 * Se asegura de que las reglas de negocio se cumplan, como verificar el estado del turno antes de permitir ciertas acciones y actualizar el aforo de las instalaciones.
 */
import java.time.LocalDateTime;

import entidades.Instalacion;
import entidades.Picsina;
import entidades.Turno;
import entidades.Usuario;

public class ServicioTurnos {
    /*
    El atributo maxPersonasPorCarril se utiliza para limitar el número 
    de personas que pueden reservar un turno en un mismo carril de la piscina, 
    lo que ayuda a gestionar el aforo y garantizar una experiencia segura y cómoda para los usuarios.

    */
    private int maxPersonasPorCarril;
// Constructor por defecto que establece un valor predeterminado para el máximo de personas por carril
    public ServicioTurnos() {
        this(3);
    }
// Constructor que permite configurar el máximo de personas por carril al crear una instancia del servicio de turnos
    public ServicioTurnos(int maxPersonasPorCarril) {
        this.maxPersonasPorCarril = validarMaxPersonasPorCarril(maxPersonasPorCarril);
    }
// Getters y Setters para el atributo maxPersonasPorCarril, con validación para asegurar que el valor sea mayor a 0
    public int getMaxPersonasPorCarril() {
        return maxPersonasPorCarril;
    }

    public void setMaxPersonasPorCarril(int maxPersonasPorCarril) {
        this.maxPersonasPorCarril = validarMaxPersonasPorCarril(maxPersonasPorCarril);
    }

    private int validarMaxPersonasPorCarril(int maxPersonasPorCarril) {
        if (maxPersonasPorCarril <= 0) {
            throw new IllegalArgumentException("El maximo de personas por carril debe ser mayor a 0.");
        }
        return maxPersonasPorCarril;
    }

    /**
     * Datos nesesarios para el servicio de turnos
     * En este caso, no se necesitan atributos específicos para el servicio de turnos, ya que todas las operaciones 
     * se realizan a través de los métodos y se basan en los objetos Turno
     **/
    public Turno reservarTurno(String idTurno, LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion) {

/*
Si la instalación es una piscina, se asigna automáticamente un carril disponible utilizando 
el método asignarCarrilAutomatico. Si la instalación no es una piscina, el carril asignado se mantiene 
como null, ya que no aplica para gimnasios.
*/
        Integer carrilAsignado = null;// Para turnos de piscina, se asigna automáticamente un carril disponible
        if (instalacion instanceof Picsina) {
            Picsina picsina = (Picsina) instalacion;
            carrilAsignado = asignarCarrilAutomatico(picsina);
        }

        return reservarTurno(idTurno, fechaHora, duracionMinutos, usuario, instalacion, carrilAsignado);
    }

    public Turno reservarTurno(String idTurno, LocalDateTime fechaHora, int duracionMinutos,
                               Usuario usuario, Instalacion instalacion, Integer numeroCarril) {
      // Validamos que la instalación no sea null antes de intentar reservar un turno
        if (instalacion == null) {
            throw new IllegalArgumentException("La instalacion no puede ser null.");
        }

        Integer carrilValidado = validarCarril(instalacion, numeroCarril);
        instalacion.descontarCupo();// Descontamos un cupo en la instalación al reservar un turno
        Turno turno = new Turno(idTurno, fechaHora, duracionMinutos, usuario, instalacion);// Creamos y devolvemos un nuevo turno con el estado inicial de RESERVADO
        turno.setNumeroCarrilAsignado(carrilValidado);
        instalacion.getTurnos().add(turno);
        return turno;
    }
// Método para cancelar un turno, validando que el turno esté en estado RESERVADO antes de permitir la cancelación
    public void cancelarTurno(Turno turno) {
        validarTurno(turno);

        validarTransicionDesdeReservado(turno, "cancelar");

        turno.setEstado(Turno.ESTADO_CANCELADO);// Cambia el estado del turno a CANCELADO
        HistorialCitasServicio.desdeTurno(turno, "Turno cancelado.");
        turno.getInstalacion().liberarCupo();// Libera un cupo en la instalación al cancelar el turno
    }
// Método para completar un turno, validando que el turno esté en estado RESERVADO antes de permitir la finalización
    public void completarTurno(Turno turno) {
        validarTurno(turno);

        validarTransicionDesdeReservado(turno, "completar");

        turno.setEstado(Turno.ESTADO_COMPLETADO);// Cambia el estado del turno a COMPLETADO
        HistorialCitasServicio.desdeTurno(turno, "Turno completado.");
        turno.getInstalacion().liberarCupo();// Libera un cupo en la instalación al completar el turno
    }
// Método privado para validar que el turno no sea null antes de realizar operaciones sobre él
    private void validarTurno(Turno turno) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }
    }

    private void validarTransicionDesdeReservado(Turno turno, String accion) {
        if (!Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
            throw new IllegalStateException("Solo se puede " + accion + " un turno en estado RESERVADO.");
        }
    }
// Método privado para validar que el número de carril asignado sea válido para la instalación, asegurando que solo se asignen carriles en instalaciones tipo piscina y que el número de carril esté dentro del rango permitido
    private Integer validarCarril(Instalacion instalacion, Integer numeroCarril) {
        if (instalacion instanceof Picsina) {// Solo se asignan carriles para instalaciones tipo piscina
            Picsina picsina = (Picsina) instalacion;
            if (numeroCarril == null) {
                throw new IllegalArgumentException("Debe asignar un carril para reservas de piscina.");
            }

            if (numeroCarril <= 0 || numeroCarril > picsina.getNumeroCarriles()) {
                throw new IllegalArgumentException("Carril invalido. Debe estar entre 1 y " + picsina.getNumeroCarriles() + ".");
            }

            if (contarReservasActivasEnCarril(picsina, numeroCarril) >= maxPersonasPorCarril) {
                throw new IllegalStateException("El carril " + numeroCarril + " ya alcanzo el maximo de " + maxPersonasPorCarril + " personas.");
            }

            return numeroCarril;
        }

        if (numeroCarril != null) {
            throw new IllegalArgumentException("Solo se puede asignar carril en instalaciones tipo piscina.");
        }

        return null;
    }
// Método privado para asignar automáticamente un carril en una piscina, utilizando el aforo actual para determinar el siguiente carril disponible de manera cíclica
    private int asignarCarrilAutomatico(Picsina picsina) {
        int carriles = picsina.getNumeroCarriles();
        if (carriles <= 0) {
            throw new IllegalStateException("La piscina no tiene carriles configurados.");
        }

        for (int carril = 1; carril <= carriles; carril++) {
            if (contarReservasActivasEnCarril(picsina, carril) < maxPersonasPorCarril) {
                return carril;
            }
        }

        throw new IllegalStateException("No hay carriles disponibles. Todos alcanzaron el maximo de " + maxPersonasPorCarril + " personas.");
    }

    private int contarReservasActivasEnCarril(Picsina picsina, int numeroCarril) {
        int reservas = 0;
        for (Turno turno : picsina.getTurnos()) {
            Integer carril = turno.getNumeroCarrilAsignado();
            if (carril != null
                    && carril == numeroCarril
                    && Turno.ESTADO_RESERVADO.equals(turno.getEstado())) {
                reservas++;
            }
        }
        return reservas;
    }

    
}

package entidades;

import java.util.ArrayList;
import java.util.List;
public abstract class Instalacion {
    private String idInstalacion;
    private String tipo; // "Piscina" o "Gimnasio"
    private int capacidadMaxima;
    private int aforoActual;
    private final List<Turno> turnos = new ArrayList<>(); // Lista de turnos asociados a esta instalación

    public Instalacion() {
    }

    public Instalacion(String idInstalacion, String tipo, int capacidadMaxima, int aforoActual) {
        this.idInstalacion = idInstalacion;
        this.tipo = validarTipo(tipo);
        this.capacidadMaxima = capacidadMaxima;
        this.aforoActual = aforoActual;
    }



//Getters
    public String getIdInstalacion() { 
        return idInstalacion;
     }
    public String getTipo() { 
        return tipo; 
    }
    public int getCapacidadMaxima() { 
        return capacidadMaxima; 
    } 
    public int getAforoActual() {
        return aforoActual;
    }
    public List<Turno> getTurnos() { 
        return new ArrayList<>(turnos);
     }

    public void registrarTurno(Turno turno) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }
        turnos.add(turno);
    }
//Setters
    public void setIdInstalacion(String idInstalacion) { 
        this.idInstalacion = idInstalacion; 
    }
    public void setTipo(String tipo) { 
        this.tipo = validarTipo(tipo); 
    }
    // El setter de capacidadMaxima valida que no sea negativo y que no sea menor al aforo actual
    public void setCapacidadMaxima(int capacidadMaxima) { 
        if (capacidadMaxima < 0) {
            throw new IllegalArgumentException("La capacidad maxima no puede ser negativa.");
        }
        if (aforoActual > capacidadMaxima) {
            throw new IllegalArgumentException("La capacidad maxima no puede ser menor al aforo actual.");
        }
        this.capacidadMaxima = capacidadMaxima; 
    }
    // El setter de aforoActual valida que no sea negativo y que no sea mayor a la capacidad máxima
    public void setAforoActual(int aforoActual) {
        if (aforoActual < 0 || aforoActual > capacidadMaxima) {
            throw new IllegalArgumentException("El aforo actual debe estar entre 0 y la capacidad maxima.");
        }
        this.aforoActual = aforoActual;
    }
// Método para saber cuántos turnos están disponibles 
    public int getTurnosDisponibles() {
        return aforoActual;
    }
// Método para descontar un cupo cuando se reserva un turno 
    public void descontarCupo() {
        if (aforoActual <= 0) {
            throw new IllegalStateException("No hay cupos disponibles en la instalacion.");
        }
        aforoActual--;
    }
// Método para liberar un cupo cuando se cancela un turno
    public void liberarCupo() {
        if (aforoActual >= capacidadMaxima) {
            throw new IllegalStateException("El aforo actual ya esta en su capacidad maxima.");
        }
        aforoActual++;
    }
// Método para asegurar que el tipo de instalación es Gimnasio o Piscina
    private String validarTipo(String tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de instalacion no puede ser null. Valores permitidos: GIMNASIO o PISCINA.");
        }

        String tipoNormalizado = tipo.trim().toUpperCase();
        if (!"GIMNASIO".equals(tipoNormalizado) && !"PISCINA".equals(tipoNormalizado)) {
            throw new IllegalArgumentException("Tipo de instalacion invalido: " + tipo + ". Valores permitidos: GIMNASIO o PISCINA.");
        }

        return tipoNormalizado;
    }
public abstract int calcularAforoActual(); // Método abstracto para calcular el aforo actual según la instalación
    // Método para mostrar información de la instalación
@Override
    public String toString() {
        return tipo + " | Aforo máx: " + capacidadMaxima + 
               " | Reservados: " + (capacidadMaxima - aforoActual) +
               " | Disponibles: " + aforoActual;
    }
}

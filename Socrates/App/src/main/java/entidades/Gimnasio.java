package entidades;

public class Gimnasio extends Instalacion {
// Constructor sin parámetros para facilitar la creación de objetos Gimnasio sin necesidad de proporcionar todos los detalles de inmediato
    public Gimnasio() {
        super();
    }

    public Gimnasio(String idInstalacion, String tipo, int capacidadMaxima, int aforoActual) {
        super(idInstalacion, tipo, capacidadMaxima, aforoActual);// Llamada al constructor de la clase padre (instalacion)
    }

    // Método para calcular el aforo actual específico para el gimnasio
    @Override
    public int calcularAforoActual(){
        return getAforoActual();
    }

 @Override
public String toString() {
        return "Instalacion: " + getTipo() + 
               " | Capacidad Maxima: " + getCapacidadMaxima() + 
               " | Aforo Actual: " + getAforoActual();
    }
}

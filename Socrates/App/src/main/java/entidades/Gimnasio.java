package entidades;

/**
 * Gimnasio — Subclase concreta de Instalacion.
 *
 * CORRECCIÓN: constructor actualizado de String a int para idInstalacion,
 * siguiendo la corrección de la clase padre Instalacion.
 */
public class Gimnasio extends Instalacion {

    public Gimnasio() {
        super();
    }

    public Gimnasio(int idInstalacion, String tipo, int capacidadMaxima, int aforoActual) {
        super(idInstalacion, tipo, capacidadMaxima, aforoActual);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

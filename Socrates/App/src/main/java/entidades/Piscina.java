package entidades;

/**
 * Piscina — Subclase concreta de Instalacion.
 *
 * CORRECCIÓN: constructor actualizado de String a int para idInstalacion,
 * siguiendo la corrección de la clase padre Instalacion.
 */
public class Piscina extends Instalacion {

    private int    numeroCarriles;
    private double profundidad;     // En metros

    public Piscina() {
        super();
    }

    public Piscina(int idInstalacion, String tipo, int capacidadMaxima, int aforoActual,
                   int numeroCarriles, double profundidad) {
        super(idInstalacion, tipo, capacidadMaxima, aforoActual);
        this.numeroCarriles = numeroCarriles;
        this.profundidad    = profundidad;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getNumeroCarriles() { return numeroCarriles; }
    public double getProfundidad()    { return profundidad; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setNumeroCarriles(int numeroCarriles) { this.numeroCarriles = numeroCarriles; }
    public void setProfundidad(double profundidad)    { this.profundidad = profundidad; }

    @Override
    public String toString() {
        return "Instalacion: " + getTipo() +
               " | Carriles: " + numeroCarriles +
               " | Profundidad: " + profundidad + "m";
    }
}

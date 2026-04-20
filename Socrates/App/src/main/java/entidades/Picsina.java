package entidades;

public class Picsina extends Instalacion {
    private int numeroCarriles;
    private double profundidad; // En metros

    public Picsina() {
        super();
    }

    public Picsina(String idInstalacion, String tipo, int capacidadMaxima, int aforoActual,
                     int numeroCarriles, double profundidad) {
        super(idInstalacion, tipo, capacidadMaxima, aforoActual);
        this.numeroCarriles = numeroCarriles;
        this.profundidad = profundidad;
    }

    // Getters
    public int getNumeroCarriles() {
        return numeroCarriles;
    }

    public double getProfundidad() {
        return profundidad;
    }

    // Setters
    public void setNumeroCarriles(int numeroCarriles) {
        this.numeroCarriles = numeroCarriles;
    }

    public void setProfundidad(double profundidad) {
        this.profundidad = profundidad;
    }
    
    @Override
    public int calcularAforoActual(){
        return getAforoActual();
    }

 @Override

public String toString() {
        return "Instalacion: " + getTipo() + 
               " | Carriles: " + numeroCarriles + 
               " | Profundidad: " + profundidad + "m";
    }

}

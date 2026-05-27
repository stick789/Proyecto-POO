package entidades;

/**
 * Instalacion — Clase abstracta base para Gimnasio y Piscina.
 *
 * CAMBIOS (sede + nombre):
 *  - Añadidos campos nombre e idSede, que ya existían en la BD pero no se mapeaban.
 *  - toString() muestra el nombre cuando está disponible.
 *  - El constructor base de 4 parámetros se conserva para compatibilidad con
 *    subclases existentes (Gimnasio, Piscina); nombre e idSede se setean vía setter
 *    desde el DAO tras construir el objeto.
 */
public abstract class Instalacion {

    private int    idInstalacion;
    private String tipo;            // "GIMNASIO" o "PISCINA"
    private int    capacidadMaxima;
    private int    aforoActual;     // cupos DISPONIBLES (no ocupados)
    private String nombre;          // columna `nombre` de la BD
    private int    idSede;          // columna `idSede` de la BD
    private String nombreSede;      // ← s.nombre del JOIN con sede
    private String direccionSede;   // ← s.direccion del JOIN con sede

    public Instalacion() {}

    public Instalacion(int idInstalacion, String tipo, int capacidadMaxima, int aforoActual) {
        this.idInstalacion   = idInstalacion;
        this.tipo            = validarTipo(tipo);
        this.capacidadMaxima = capacidadMaxima;
        this.aforoActual     = aforoActual;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getIdInstalacion()  { return idInstalacion; }
    public String getTipo()           { return tipo; }
    public int    getCapacidadMaxima(){ return capacidadMaxima; }
    public int    getAforoActual()    { return aforoActual; }
    public String getNombre()         { return nombre; }
    public int    getIdSede()         { return idSede; }
    public String getNombreSede()     { return nombreSede; }
    public String getDireccionSede()  { return direccionSede; }

    // ── Setters con validación ────────────────────────────────────────────────

    public void setIdInstalacion(int idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public void setTipo(String tipo) {
        this.tipo = validarTipo(tipo);
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        if (capacidadMaxima < 0)
            throw new IllegalArgumentException("La capacidad máxima no puede ser negativa.");
        if (capacidadMaxima > 30)
            throw new IllegalArgumentException("Capacidad máxima permitida: 30 personas. Se ingresó: " + capacidadMaxima);
        if (aforoActual > capacidadMaxima)
            throw new IllegalArgumentException("La capacidad máxima no puede ser menor al aforo actual.");
        this.capacidadMaxima = capacidadMaxima;
    }

    public void setAforoActual(int aforoActual) {
        if (aforoActual < 0 || aforoActual > capacidadMaxima)
            throw new IllegalArgumentException(
                "El aforo actual debe estar entre 0 y la capacidad máxima (" + capacidadMaxima + ").");
        this.aforoActual = aforoActual;
    }

    public void setNombre(String nombre)             { this.nombre = nombre; }
    public void setIdSede(int idSede)                { this.idSede = idSede; }
    public void setNombreSede(String nombreSede)     { this.nombreSede = nombreSede; }
    public void setDireccionSede(String direccion)   { this.direccionSede = direccion; }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    /** Cuántos cupos quedan disponibles para reservar. */
    public int getCuposDisponibles() {
        return aforoActual;
    }

    public void descontarCupo() {
        if (aforoActual <= 0)
            throw new IllegalStateException("No hay cupos disponibles en la instalación.");
        aforoActual--;
    }

    public void liberarCupo() {
        if (aforoActual >= capacidadMaxima)
            throw new IllegalStateException("El aforo actual ya está en su capacidad máxima.");
        aforoActual++;
    }

    public int calcularAforoActual() {
        return aforoActual;
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private String validarTipo(String tipo) {
        if (tipo == null)
            throw new IllegalArgumentException(
                "El tipo de instalación no puede ser null. Valores: GIMNASIO, PISCINA.");
        String normalizado = tipo.trim().toUpperCase();
        if (!"GIMNASIO".equals(normalizado) && !"PISCINA".equals(normalizado))
            throw new IllegalArgumentException(
                "Tipo inválido: " + tipo + ". Valores permitidos: GIMNASIO, PISCINA.");
        return normalizado;
    }

    /**
     * Muestra nombre + sede. Usado por ChoiceDialog para mostrar opciones legibles.
     */
    @Override
    public String toString() {
        String label = (nombre != null && !nombre.isBlank()) ? nombre : tipo;
        String sede  = (nombreSede != null && !nombreSede.isBlank()) ? " · " + nombreSede : "";
        return label + sede + " | Máx: " + capacidadMaxima + " | Disponibles: " + aforoActual;
    }
}
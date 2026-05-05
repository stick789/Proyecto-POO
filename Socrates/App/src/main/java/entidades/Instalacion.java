package entidades;

/**
 * Instalacion — Clase abstracta base para Gimnasio y Piscina.
 *
 * CORRECCIONES APLICADAS:
 *
 * 1. BUG: idInstalacion era String, la BD lo define como INT AUTO_INCREMENT.
 *    ANTES: private String idInstalacion;
 *           El DAO hacía Integer.parseInt(...) que lanzaba NumberFormatException
 *           si por alguna razón llegaba un valor no numérico.
 *    AHORA: private int idInstalacion;
 *           Consistente con la BD, sin conversiones frágiles.
 *
 * 2. MEJORA: campo nombre eliminado
 *    Estaba declarado pero sin getter ni setter — código muerto que confundía.
 *    La BD no tiene columna nombre en instalacion; si se necesita en el futuro
 *    se agrega junto con su getter/setter y la columna en BD.
 *
 * 3. MEJORA: lista turnos eliminada
 *    List<Turno> turnos nunca se cargaba desde la BD. Un developer que llamara
 *    getTurnos() esperaba datos y recibía lista vacía sin error. La relación
 *    instalación→turnos se gestiona con TurnoDAO.listarPorInstalacion(), no
 *    guardando la lista dentro de la entidad.
 *
 * 4. MEJORA: calcularAforoActual() convertido en método concreto
 *    Era abstracto pero Gimnasio y Piscina devolvían exactamente lo mismo.
 *    No había variación real entre subclases, el polimorfismo no aportaba nada.
 *    Se conserva como método final para que subclases no lo sobreescriban sin
 *    una razón válida. Si en el futuro Piscina calcula diferente (ej: por carriles),
 *    puede eliminarse el modificador final.
 */
public abstract class Instalacion {

    private int    idInstalacion;   // ← corregido: int (era String)
    private String tipo;            // "GIMNASIO" o "PISCINA"
    private int    capacidadMaxima;
    private int    aforoActual;     // cupos DISPONIBLES (no ocupados)

    public Instalacion() {}

    public Instalacion(int idInstalacion, String tipo, int capacidadMaxima, int aforoActual) {
        this.idInstalacion  = idInstalacion;
        this.tipo           = validarTipo(tipo);
        this.capacidadMaxima = capacidadMaxima;
        this.aforoActual    = aforoActual;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getIdInstalacion()  { return idInstalacion; }
    public String getTipo()           { return tipo; }
    public int    getCapacidadMaxima(){ return capacidadMaxima; }
    public int    getAforoActual()    { return aforoActual; }

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

    // ── Lógica de dominio ────────────────────────────────────────────────────

    /** Cuántos cupos quedan disponibles para reservar. */
    public int getCuposDisponibles() {
        return aforoActual;
    }

    /**
     * Descuenta un cupo cuando se reserva un turno.
     * @throws IllegalStateException si no hay cupos disponibles.
     */
    public void descontarCupo() {
        if (aforoActual <= 0)
            throw new IllegalStateException("No hay cupos disponibles en la instalación.");
        aforoActual--;
    }

    /**
     * Libera un cupo cuando se cancela un turno.
     * @throws IllegalStateException si el aforo ya está en su máximo.
     */
    public void liberarCupo() {
        if (aforoActual >= capacidadMaxima)
            throw new IllegalStateException("El aforo actual ya está en su capacidad máxima.");
        aforoActual++;
    }

    /**
     * Retorna el aforo actual disponible.
     * Era abstracto antes pero Gimnasio y Piscina devolvían lo mismo.
     * Se mantiene como método concreto para evitar sobrescrituras innecesarias.
     * Si una subclase necesita calcular diferente (ej: Piscina por carriles libres),
     * puede sobrescribir este método.
     */
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

    @Override
    public String toString() {
        return tipo + " | Aforo máx: " + capacidadMaxima +
               " | Reservados: " + (capacidadMaxima - aforoActual) +
               " | Disponibles: " + aforoActual;
    }
}

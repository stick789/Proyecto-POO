package entidades;

import java.time.LocalDateTime;

/**
 * Turno — Reserva de un usuario en una instalación.
 *
 * CORRECCIÓN: idTurno cambiado de String a int.
 *   ANTES: private String idTurno;
 *          El DAO hacía setIdTurno(String.valueOf(keys.getInt(1))) y
 *          ServicioAsignarEntrenador hacía Integer.parseInt(turno.getIdTurno()),
 *          lo que era una conversión int→String→int innecesaria y frágil.
 *   AHORA: private int idTurno;
 *          Consistente con la BD (idTurno INT AUTO_INCREMENT). Todos los
 *          DAOs y servicios que usaban parseInt() ahora usan el int directamente.
 */
public class Turno {

    public static final String ESTADO_RESERVADO  = "RESERVADO";
    public static final String ESTADO_CANCELADO  = "CANCELADO";
    public static final String ESTADO_COMPLETADO = "COMPLETADO";

    private int           idTurno;                 // ← corregido: int (era String)
    private LocalDateTime fechaHora;
    private int           duracionMinutos;
    private Usuario       usuario;
    private Instalacion   instalacion;
    private Integer       numeroCarrilAsignado;    // null para gimnasios
    private String        estado;
    private Persona       entrenador;              // Persona (puede ser Entrenador)

    public Turno() {
        this.estado = ESTADO_RESERVADO;
    }

    public Turno(int idTurno, LocalDateTime fechaHora, int duracionMinutos,
                 Usuario usuario, Instalacion instalacion) {
        if (instalacion == null)
            throw new IllegalArgumentException("La instalación no puede ser null.");
        this.idTurno         = idTurno;
        this.fechaHora       = fechaHora;
        this.duracionMinutos = duracionMinutos;
        this.usuario         = usuario;
        this.instalacion     = instalacion;
        this.estado          = ESTADO_RESERVADO;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int           getIdTurno()              { return idTurno; }
    public LocalDateTime getFechaHora()            { return fechaHora; }
    public int           getDuracionMinutos()       { return duracionMinutos; }
    public Usuario       getUsuario()              { return usuario; }
    public Instalacion   getInstalacion()          { return instalacion; }
    public Integer       getNumeroCarrilAsignado() { return numeroCarrilAsignado; }
    public String        getEstado()               { return estado; }
    public Persona       getEntrenador()           { return entrenador; }

    /** Helper: retorna el ID del entrenador, o null si no hay uno asignado. */
    public Integer getIdEntrenador() {
        return entrenador != null ? entrenador.getId() : null;
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setIdTurno(int idTurno)                       { this.idTurno = idTurno; }
    public void setFechaHora(LocalDateTime fechaHora)         { this.fechaHora = fechaHora; }
    public void setDuracionMinutos(int duracionMinutos)       { this.duracionMinutos = duracionMinutos; }
    public void setUsuario(Usuario usuario)                   { this.usuario = usuario; }
    public void setInstalacion(Instalacion instalacion)       { this.instalacion = instalacion; }
    public void setEntrenador(Persona entrenador)             { this.entrenador = entrenador; }

    public void setNumeroCarrilAsignado(Integer carril) {
        if (carril != null && carril <= 0)
            throw new IllegalArgumentException("El carril debe ser mayor a 0.");
        this.numeroCarrilAsignado = carril;
    }

    public void setEstado(String estado) {
        if (estado == null)
            throw new IllegalArgumentException("El estado no puede ser null.");
        String norm = estado.trim().toUpperCase();
        if (!ESTADO_RESERVADO.equals(norm)
                && !ESTADO_CANCELADO.equals(norm)
                && !ESTADO_COMPLETADO.equals(norm))
            throw new IllegalArgumentException(
                "Estado inválido: " + estado + ". Valores: RESERVADO, CANCELADO, COMPLETADO.");
        this.estado = norm;
    }

    @Override
    public String toString() {
        String carrilStr = numeroCarrilAsignado != null
                ? " | Carril: " + numeroCarrilAsignado : "";
        return "Turno " + idTurno + " | " + fechaHora +
               " | " + instalacion.getTipo() + carrilStr +
               " | Estado: " + estado;
    }
}

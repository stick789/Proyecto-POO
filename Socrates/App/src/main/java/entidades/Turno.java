package entidades;

import java.time.LocalDateTime;

public class Turno {
// Definición de constantes para los estados permitidos del turno
    public static final String ESTADO_RESERVADO = "RESERVADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_COMPLETADO = "COMPLETADO";
    
    private String idTurno;
    private LocalDateTime fechaHora;        // Fecha y hora exacta del turno
    private int duracionMinutos;
    private Usuario usuario;                // Usuario que reservó el turno
    private Instalacion instalacion;        // Piscina o Gimnasio
    private String estado;                  // "Reservado", "Cancelado", "Completado"

    public Turno() {
        this.estado = ESTADO_RESERVADO;
    }

    // Constructor
    public Turno(String idTurno, LocalDateTime fechaHora, int duracionMinutos, 
                 Usuario usuario, Instalacion instalacion) {
        
        this.idTurno = idTurno;
        this.fechaHora = fechaHora;
        this.duracionMinutos = duracionMinutos;
        this.usuario = usuario;
        if (instalacion == null) {
            throw new IllegalArgumentException("La instalacion no puede ser null.");
        }
        this.instalacion = instalacion;
        this.estado = ESTADO_RESERVADO;
    }

    // Getters y Setters
    public String getIdTurno() { return idTurno; }
    public void setIdTurno(String idTurno) { this.idTurno = idTurno; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Instalacion getInstalacion() { return instalacion; }
    public void setInstalacion(Instalacion instalacion) { this.instalacion = instalacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) {
        cambiarEstado(estado);
    }
//Metodo para proteger el cambio de estado del turno, validando que solo se puedan asignar los estados permitidos
    private void cambiarEstado(String nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado no puede ser null.");
        }
        String normalizado = nuevoEstado.trim().toUpperCase();
        if (!ESTADO_RESERVADO.equals(normalizado)
                && !ESTADO_CANCELADO.equals(normalizado)
                && !ESTADO_COMPLETADO.equals(normalizado)) {
            throw new IllegalArgumentException("Estado invalido. Valores permitidos: RESERVADO, CANCELADO, COMPLETADO");
        }

        this.estado = normalizado;
    }

    @Override
    public String toString() {
        return "Turno " + idTurno + " | " + fechaHora + " | " + instalacion.getTipo() +
               " | Estado: " + estado;
    }
}

package entidades;

import java.time.LocalDateTime;

public class Historial_citas {

    private int  idHistorial;
    private String idTurno;
    private String estado;
    private LocalDateTime fechaEvento;
    private Integer idUsuario;
    private String idInstalacion;
    private String detalle;
// Constructor vacío para permitir la creación de instancias sin necesidad de proporcionar todos los datos de inmediato
    public Historial_citas() {
    }
// Constructor completo para crear instancias de historial de citas
    public Historial_citas(int idHistorial, String idTurno, String estado, LocalDateTime fechaEvento,
                           Integer idUsuario, String idInstalacion, String detalle) {
        this.idHistorial = idHistorial;
        this.idTurno = idTurno;
        this.estado = validarEstado(estado);
        this.fechaEvento = fechaEvento != null ? fechaEvento : LocalDateTime.now();
        this.idUsuario = idUsuario;
        this.idInstalacion = idInstalacion;
        this.detalle = detalle;
    }
// Método para crear una instancia de historial de citas a partir de un turno
    public static Historial_citas desdeTurno(Turno turno, String detalle) {
        if (turno == null) {
            throw new IllegalArgumentException("El turno no puede ser null.");
        }
         // Crear un historial a partir de un turno.
        return new Historial_citas(
                0,
                turno.getIdTurno(),
                turno.getEstado(),
                LocalDateTime.now(),
                turno.getUsuario() != null ? turno.getUsuario().getId() : null,
                turno.getInstalacion() != null ? turno.getInstalacion().getIdInstalacion() : null,
                detalle
        );
    }
// Getters y Setters para cada atributo del historial de citas
    public int getIdHistorial() {
        return idHistorial;
    }
    public void setIdHistorial(int idHistorial) {
        this.idHistorial = idHistorial;
    }

    public String getIdTurno() {
        return idTurno;
    }
    public void setIdTurno(String idTurno) {
        this.idTurno = idTurno;
    }

    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = validarEstado(estado);
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }
    public void setFechaEvento(LocalDateTime fechaEvento) {
        this.fechaEvento = fechaEvento;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdInstalacion() {
        return idInstalacion;
    }
    public void setIdInstalacion(String idInstalacion) {
        this.idInstalacion = idInstalacion;
    }

    public String getDetalle() {
        return detalle;
    }
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
// Método para validar que el estado del turno sea uno de los valores permitidos (RESERVADO, CANCELADO, COMPLETADO)
    private String validarEstado(String estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado no puede ser null.");
        }

        String normalizado = estado.trim().toUpperCase();
        if (!Turno.ESTADO_RESERVADO.equals(normalizado)
                && !Turno.ESTADO_CANCELADO.equals(normalizado)
                && !Turno.ESTADO_COMPLETADO.equals(normalizado)) {
            throw new IllegalArgumentException("Estado invalido. Valores permitidos: RESERVADO, CANCELADO, COMPLETADO");
        }

        return normalizado;
    }
}
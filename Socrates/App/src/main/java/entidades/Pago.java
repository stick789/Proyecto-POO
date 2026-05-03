package entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago — Entidad que representa un pago asociado a un turno reservado.
 *
 * <p>Diseño: clase independiente (no extiende {@link Historial_citas}) porque
 * un pago es un concepto financiero distinto al historial de eventos de una
 * cita. Esta separación sigue el principio de responsabilidad única (SRP).</p>
 *
 * <p><b>Estados de pago permitidos:</b>
 * {@code PENDIENTE}, {@code COMPLETADO}, {@code FALLIDO}, {@code REEMBOLSADO}.</p>
 *
 * <p>El campo {@code fechaPago} se asigna automáticamente en BD
 * ({@code DEFAULT NOW()}), y se carga desde la BD al recuperar el registro.</p>
 */
public class Pago {

    public static final String ESTADO_PENDIENTE    = "PENDIENTE";
    public static final String ESTADO_COMPLETADO   = "COMPLETADO";
    public static final String ESTADO_FALLIDO      = "FALLIDO";
    public static final String ESTADO_REEMBOLSADO  = "REEMBOLSADO";

    private long           idPago;
    private int            idTurno;
    private int            idUsuario;
    private BigDecimal     monto;
    private String         metodoPago;   // "EFECTIVO", "TARJETA", "TRANSFERENCIA"
    private String         estadoPago;
    private LocalDateTime  fechaPago;    // asignado por BD; null al insertar

    // ---------------------------------------------------------------- Constructores

    /** Constructor para crear un pago nuevo (antes de persistir). */
    public Pago(int idTurno, int idUsuario, BigDecimal monto,
                String metodoPago, String estadoPago) {
        this.idTurno    = idTurno;
        this.idUsuario  = idUsuario;
        this.monto      = validarMonto(monto);
        this.metodoPago = validarMetodoPago(metodoPago);
        this.estadoPago = validarEstado(estadoPago);
    }

    /** Constructor completo para recuperar un pago desde la BD. */
    public Pago(long idPago, int idTurno, int idUsuario,
                BigDecimal monto, String metodoPago,
                String estadoPago, LocalDateTime fechaPago) {
        this.idPago     = idPago;
        this.idTurno    = idTurno;
        this.idUsuario  = idUsuario;
        this.monto      = validarMonto(monto);
        this.metodoPago = validarMetodoPago(metodoPago);
        this.estadoPago = validarEstado(estadoPago);
        this.fechaPago  = fechaPago;
    }

    // ---------------------------------------------------------------- Getters / Setters

    public long getIdPago()           { return idPago; }
    public void setIdPago(long id)    { this.idPago = id; }

    public int getIdTurno()           { return idTurno; }
    public void setIdTurno(int id)    { this.idTurno = id; }

    public int getIdUsuario()         { return idUsuario; }
    public void setIdUsuario(int id)  { this.idUsuario = id; }

    public BigDecimal getMonto()      { return monto; }
    public void setMonto(BigDecimal m){ this.monto = validarMonto(m); }

    public String getMetodoPago()     { return metodoPago; }
    public void setMetodoPago(String m){ this.metodoPago = validarMetodoPago(m); }

    public String getEstadoPago()     { return estadoPago; }
    public void setEstadoPago(String e){ this.estadoPago = validarEstado(e); }

    public LocalDateTime getFechaPago()         { return fechaPago; }
    public void setFechaPago(LocalDateTime f)   { this.fechaPago = f; }

    // ---------------------------------------------------------------- Validaciones

    /**
     * Valida y normaliza un estado de pago.
     * Se declara {@code public static} para que {@link dao.PagoDAO} pueda reutilizarla
     * al actualizar el estado directamente desde la capa de datos.
     */
    public static String validarEstado(String estado) {
        if (estado == null) throw new IllegalArgumentException("El estado del pago no puede ser null.");
        String norm = estado.trim().toUpperCase();
        if (!ESTADO_PENDIENTE.equals(norm) && !ESTADO_COMPLETADO.equals(norm)
                && !ESTADO_FALLIDO.equals(norm) && !ESTADO_REEMBOLSADO.equals(norm)) {
            throw new IllegalArgumentException(
                "Estado de pago inválido. Valores permitidos: " +
                "PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO. Recibido: " + estado);
        }
        return norm;
    }

    private static BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a 0.");
        }
        return monto;
    }

    private static String validarMetodoPago(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            throw new IllegalArgumentException("El método de pago no puede estar vacío.");
        }
        return metodo.trim().toUpperCase();
    }

    // ---------------------------------------------------------------- toString

    @Override
    public String toString() {
        return "Pago #" + idPago +
               " | Turno: " + idTurno +
               " | Usuario: " + idUsuario +
               " | $" + monto +
               " (" + metodoPago + ")" +
               " | Estado: " + estadoPago +
               (fechaPago != null ? " | Fecha: " + fechaPago : "");
    }
}

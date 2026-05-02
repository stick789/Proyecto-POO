package entidades;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class Pagos extends Historial_citas {
    private Long idPago;
    private BigDecimal monto;
    private String metodoPago;
    private String estadoPago;
    private LocalDateTime fechaPago;
   

    public Pagos(Long idPago, BigDecimal monto, String metodoPago, String estadoPago, String idTurno, Integer idUsuario) {
        super(0, idTurno, null, null, idUsuario, null, null);
        this.idPago = idPago;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estadoPago = estadoPago;
        this.fechaPago = LocalDateTime.now();
    }
     public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }
    
}
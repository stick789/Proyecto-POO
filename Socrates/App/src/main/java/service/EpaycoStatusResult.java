package service;

public class EpaycoStatusResult {
    private final String estado;
    private final String refPayco;

    public EpaycoStatusResult(String estado, String refPayco) {
        this.estado = estado;
        this.refPayco = refPayco;
    }

    public String getEstado() {
        return estado;
    }

    public String getRefPayco() {
        return refPayco;
    }
}

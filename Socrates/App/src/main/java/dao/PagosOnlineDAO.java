
package dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import entidades.Historial_citas;
import entidades.Instalacion;
import entidades.Pago;
import entidades.Turno;
import service.EpaycoService;
import socratesGui.SesionActual;
import util.ConfigLoader;

public class PagosOnlineDAO {
    private final EpaycoService epaycoService;
    private final PagoDAO pagoDAO;
    private final TurnoDAO turnoDAO;
    private final InstalacionDAO instalacionDAO;

    public PagosOnlineDAO(String publicKey, String privateKey, PagoDAO pagoDAO) {
        this.epaycoService = new EpaycoService(publicKey, privateKey);
        this.pagoDAO = pagoDAO;
        this.turnoDAO = new TurnoDAO(new PersonaDAO(), new InstalacionDAO(), new EntrenadorDAO());
        this.instalacionDAO = new InstalacionDAO();
    }



    public String iniciarPagoConTarjeta(int idTurno, double montoTotal, String descripcion) throws Exception {
        // 1. Crear el pago en BD con estado PENDIENTE
        // Tomar el usuario autenticado para que el pago quede visible en "Mis Pagos".
        int idUsuario = SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 0;
        Pago pago = new Pago(idTurno, idUsuario, BigDecimal.valueOf(montoTotal), "TARJETA_ONLINE", Pago.ESTADO_PENDIENTE);
        pagoDAO.insertar(pago);
        int idPago = (int) pago.getIdPago();

        // 2. Preparar datos para ePayco
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("checkout_version", "2");
        // El API de ePayco espera `amount` como número y valida rangos (ej: 5000..200000)
        double minimo = 5000.0;
        double maximo = 200000.0;
        if (montoTotal < minimo || montoTotal > maximo) {
            throw new IllegalArgumentException("El monto debe estar entre " + (int)minimo + " y " + (int)maximo + " COP. Monto actual: " + montoTotal);
        }
        // Enviar como número (Double) para que el serializador genere JSON numérico
        sessionData.put("amount", Double.valueOf(montoTotal));
        sessionData.put("currency", "COP");
        sessionData.put("description", descripcion != null ? descripcion.trim() : "Pago turno #" + idTurno);
        sessionData.put("internal_reference", String.valueOf(idPago));
        sessionData.put("invoice", "PAGO-" + idPago);
        sessionData.put("country", "CO");
        sessionData.put("lang", "es");
        // ePayco suele requerir URLs válidas de retorno y confirmación
        sessionData.put("response", getOrDefault(ConfigLoader.get("epayco.responseUrl"), "https://checkout.epayco.co"));
        sessionData.put("confirmation", getOrDefault(ConfigLoader.get("epayco.confirmationUrl"), "https://checkout.epayco.co"));
        // Datos del pagador: si no hay config, usamos placeholders válidos para completar la sesión
        sessionData.put("name", getOrDefault(ConfigLoader.get("epayco.customerName"), "Cliente Prueba"));
        sessionData.put("email", getOrDefault(ConfigLoader.get("epayco.customerEmail"), "cliente@prueba.com"));
       

        // 3. Crear sesión en ePayco
        String sessionId = epaycoService.createPaymentSession(sessionData);

        // 4. Actualizar el registro del pago con el sessionId
        pagoDAO.actualizarSessionId(idPago, sessionId);

        // 5. Devolver el sessionId para construir la URL de pago
        return sessionId;
        
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
    // Método para verificar el estado del pago consultando ePayco
    public String verificarEstado(int idPago) throws Exception {
        Pago pago = pagoDAO.buscarPorId(idPago)
                .orElseThrow(() -> new Exception("No existe un pago con id " + idPago));

        if (pago.getEpaycoSessionId() == null || pago.getEpaycoSessionId().isBlank()) {
            marcarPagoComoFallido(pago, idPago, "No hay sesión de pago asociada");
            return "FALLIDO";
        }

        String estado;
        try {
            estado = epaycoService.consultarEstado(pago.getEpaycoSessionId());
        } catch (Exception ex) {
            marcarPagoComoFallido(pago, idPago, "Error consultando ePayco: " + ex.getMessage());
            return "FALLIDO";
        }

        String normalizado = estado != null ? estado.trim().toLowerCase() : "";

        if ("aprobado".equals(normalizado)) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_COMPLETADO);
            // Registrar evento en historial: pago aprobado
            try {
                HistorialCitasDAO histDao = new HistorialCitasDAO();
                Historial_citas h = new Historial_citas();
                h.setIdTurno(String.valueOf(pago.getIdTurno()));
                h.setIdUsuario(pago.getIdUsuario());
                h.setIdInstalacion("0");
                h.setEstado(Turno.ESTADO_COMPLETADO);
                h.setDetalle("Pago APROBADO (idPago=" + idPago + ")");
                histDao.insertar(h);
            } catch (Exception ignore) {
                // no interrumpimos el flujo por fallos al registrar el historial
            }
        } else if (normalizado.contains("rechaz") || normalizado.contains("fail") || normalizado.contains("error") || normalizado.contains("desconoc")) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
            cancelarTurnoAsociado(pago, idPago, "Pago rechazado por ePayco");
            // Registrar evento en historial: pago fallido
            try {
                HistorialCitasDAO histDao = new HistorialCitasDAO();
                Historial_citas h = new Historial_citas();
                h.setIdTurno(String.valueOf(pago.getIdTurno()));
                h.setIdUsuario(pago.getIdUsuario());
                h.setIdInstalacion("0");
                h.setEstado("FALLIDO");
                h.setDetalle("Pago FALLIDO (idPago=" + idPago + ")");
                histDao.insertar(h);
            } catch (Exception ignore) {
                // no interrumpimos el flujo por fallos al registrar el historial
            }
            return "FALLIDO";
        } else if (!normalizado.contains("pend")) {
            marcarPagoComoFallido(pago, idPago, "Estado no reconocido por ePayco: " + estado);
            return "FALLIDO";
        }

        return estado;
    }

    private void marcarPagoComoFallido(Pago pago, int idPago, String detalle) {
        try {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
            cancelarTurnoAsociado(pago, idPago, detalle);
            HistorialCitasDAO histDao = new HistorialCitasDAO();
            Historial_citas h = new Historial_citas();
            h.setIdTurno(String.valueOf(pago.getIdTurno()));
            h.setIdUsuario(pago.getIdUsuario());
            h.setIdInstalacion("0");
            h.setEstado("FALLIDO");
            h.setDetalle("Pago FALLIDO (idPago=" + idPago + ") - " + detalle);
            histDao.insertar(h);
        } catch (Exception ignore) {
            // no interrumpimos el flujo por fallos al registrar el historial
        }
    }

    private void cancelarTurnoAsociado(Pago pago, int idPago, String detalle) {
        try {
            Turno turno = turnoDAO.buscarPorId(pago.getIdTurno()).orElse(null);
            if (turno == null) {
                return;
            }

            if (!Turno.ESTADO_CANCELADO.equalsIgnoreCase(turno.getEstado())) {
                turno.setEstado(Turno.ESTADO_CANCELADO);
                turnoDAO.actualizarEstado(turno.getIdTurno(), Turno.ESTADO_CANCELADO);

                Instalacion instalacion = turno.getInstalacion();
                if (instalacion != null) {
                    instalacion.liberarCupo();
                    instalacionDAO.actualizarAforo(instalacion.getIdInstalacion(), instalacion.getAforoActual());
                }

                HistorialCitasDAO histDao = new HistorialCitasDAO();
                Historial_citas h = new Historial_citas();
                h.setIdTurno(String.valueOf(turno.getIdTurno()));
                h.setIdUsuario(turno.getUsuario() != null ? turno.getUsuario().getId() : pago.getIdUsuario());
                h.setIdInstalacion(turno.getInstalacion() != null ? String.valueOf(turno.getInstalacion().getIdInstalacion()) : "0");
                h.setEstado(Turno.ESTADO_CANCELADO);
                h.setDetalle("Turno cancelado automáticamente por fallo de pago (idPago=" + idPago + "). " + detalle);
                histDao.insertar(h);
            }
        } catch (Exception ignore) {
            // no interrumpimos el flujo por fallos al cancelar el turno
        }
    }
}

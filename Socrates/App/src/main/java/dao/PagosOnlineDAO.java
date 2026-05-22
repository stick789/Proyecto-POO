
package dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import entidades.Pago;
import service.EpaycoService;
import util.ConfigLoader;

public class PagosOnlineDAO {
    private final EpaycoService epaycoService;
    private final PagoDAO pagoDAO;

    public PagosOnlineDAO(String publicKey, String privateKey, PagoDAO pagoDAO) {
        this.epaycoService = new EpaycoService(publicKey, privateKey);
        this.pagoDAO = pagoDAO;
    }



    public String iniciarPagoConTarjeta(int idTurno, double montoTotal, String descripcion) throws Exception {
        // 1. Crear el pago en BD con estado PENDIENTE
        // Usar el constructor para crear un pago nuevo: (idTurno, idUsuario, monto, metodoPago, estadoPago)
        // No tenemos idUsuario aquí, lo inicializamos a 0 (usuario desconocido en este contexto).
        Pago pago = new Pago(idTurno, 0, BigDecimal.valueOf(montoTotal), "TARJETA_ONLINE", Pago.ESTADO_PENDIENTE);
        int idPago = pagoDAO.registrarPago(pago); // este método debe devolver el ID generado

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
            throw new Exception("No hay sesión de pago asociada");
        }

        String estado = epaycoService.consultarEstado(pago.getEpaycoSessionId());

        if ("Aprobado".equalsIgnoreCase(estado)) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_COMPLETADO);
        } else if ("Rechazado".equalsIgnoreCase(estado)) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
        }

        return estado;
    }
}

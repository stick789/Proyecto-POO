
package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import database.Conexion;
import entidades.Historial_citas;
import entidades.Instalacion;
import entidades.Pago;
import entidades.Turno;
import service.EpaycoService;
import service.EpaycoStatusResult;
import socratesGui.SesionActual;
import util.ConfigLoader;

public class PagosOnlineDAO {
    private final Conexion conexion = Conexion.getInstancia();
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
        return iniciarPagoConTarjeta(idTurno, montoTotal, descripcion, null, null);
    }

    public String iniciarPagoConTarjeta(int idTurno, double montoTotal, String descripcion,
                                        String responseUrlOverride, String confirmationUrlOverride) throws Exception {
        // 1. Crear el pago en BD con estado PENDIENTE
        // Tomar el usuario autenticado para que el pago quede visible en "Mis Pagos".
        int idUsuario = SesionActual.getUsuario() != null ? SesionActual.getUsuario().getId() : 0;
        Pago pago = new Pago(idTurno, idUsuario, BigDecimal.valueOf(montoTotal), "TARJETA_ONLINE", Pago.ESTADO_PENDIENTE);
        pagoDAO.insertar(pago);
        int idPago = (int) pago.getIdPago();
        System.out.println("📌 Pago insertado con id=" + idPago);

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
        Map<String, Object> extras = new HashMap<>();
        extras.put("extra1", String.valueOf(idPago));
        extras.put("extra2", String.valueOf(idTurno));
        sessionData.put("extras", extras);
        // ePayco suele requerir URLs válidas de retorno y confirmación
        String responseUrl = (responseUrlOverride != null && !responseUrlOverride.isBlank())
            ? responseUrlOverride.trim()
            : getOrDefault(ConfigLoader.get("epayco.responseUrl"), "https://checkout.epayco.co");
        String confirmationUrl = (confirmationUrlOverride != null && !confirmationUrlOverride.isBlank())
            ? confirmationUrlOverride.trim()
            : getOrDefault(ConfigLoader.get("epayco.confirmationUrl"), "https://checkout.epayco.co");
        responseUrl = responseUrl.replace("{idPago}", String.valueOf(idPago));
        confirmationUrl = confirmationUrl.replace("{idPago}", String.valueOf(idPago));
        sessionData.put("response", responseUrl);
        sessionData.put("confirmation", confirmationUrl);
        // Datos del pagador: si no hay config, usamos placeholders válidos para completar la sesión
        sessionData.put("name", getOrDefault(ConfigLoader.get("epayco.customerName"), "Cliente Prueba"));
        sessionData.put("email", getOrDefault(ConfigLoader.get("epayco.customerEmail"), "cliente@prueba.com"));
       

        // 3. Crear sesión en ePayco (backend) y recibir sessionId + posible URL
        java.util.Map<String, String> epayResp = epaycoService.createPaymentSession(sessionData);
        String sessionId = epayResp.get("sessionId");
        String paymentUrl = epayResp.get("url");
        System.out.println("[PagosOnlineDAO] crear sesión ePayco para idPago=" + idPago + " -> sessionId=" + sessionId + " url=" + paymentUrl);

        // 4. Actualizar el registro del pago con el sessionId (si está disponible)
        if (sessionId != null) {
    pagoDAO.actualizarSessionId(idPago, sessionId);
    
    // Bloque de verificación (lo que preguntas)
    try (Connection conn = conexion.conectar();
         PreparedStatement ps = conn.prepareStatement("SELECT epayco_session_id FROM pagos WHERE idPago = ?")) {
        ps.setInt(1, idPago);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            System.out.println("🔍 Verificación en BD: sessionId=" + rs.getString("epayco_session_id"));
        } else {
            System.out.println("⚠️ No se encontró el pago recién actualizado");
        }
    } catch (SQLException e) {
        System.out.println("❌ Error verificando sessionId en BD: " + e.getMessage());
    }
}


        // 5. Devolver la URL si está disponible (para que el frontend abra exactamente esa sesión),
        //    de lo contrario devolver el sessionId para compatibilidad.
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        return paymentUrl;

     


        
    }

    public String aplicarResultadoRealDesdeCallback(int idPago, String codigoRespuesta, String textoRespuesta,
                                                    String refPayco) throws Exception {
        Pago pago = pagoDAO.buscarPorId(idPago)
                .orElseThrow(() -> new Exception("No existe un pago con id " + idPago));

        if (refPayco != null && !refPayco.isBlank()) {
            try {
                pagoDAO.actualizarRefPayco(idPago, refPayco.trim());
            } catch (SQLException e) {
                System.out.println("[PagosOnlineDAO] No se pudo guardar ref_payco desde callback: " + e.getMessage());
            }
        }

        String estado = mapearEstadoDesdeCallback(codigoRespuesta, textoRespuesta);
        if (Pago.ESTADO_COMPLETADO.equalsIgnoreCase(estado)) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_COMPLETADO);
            return Pago.ESTADO_COMPLETADO;
        }

        if (Pago.ESTADO_FALLIDO.equalsIgnoreCase(estado)) {
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
            cancelarTurnoAsociado(pago, idPago, "Pago rechazado/fallido según callback de ePayco");
            return Pago.ESTADO_FALLIDO;
        }

        pagoDAO.actualizarEstado(idPago, Pago.ESTADO_PENDIENTE);
        return Pago.ESTADO_PENDIENTE;
    }

    public String aplicarResultadoRealDesdeReferencia(int idPago, String refPayco) throws Exception {
        // ref_payco de ePayco es siempre numérico — rechazar sessionIds hexadecimales
        if (refPayco == null || refPayco.isBlank() || !refPayco.matches("[0-9]{5,12}")) {
            System.out.println("[PagosOnlineDAO] refPayco inválido o es sessionId, se omite: " + refPayco);
            return Pago.ESTADO_PENDIENTE;
        }
        // ePayco puede tardar unos segundos en reflejar el estado final por referencia.
        // Reintentamos brevemente antes de dejarlo en PENDIENTE para evitar falsos pendientes.
        EpaycoStatusResult statusResult = null;
        String estado = null;
        for (int intento = 1; intento <= 4; intento++) {
            statusResult = epaycoService.consultarEstadoPorReferencia(refPayco.trim());
            estado = statusResult != null ? statusResult.getEstado() : null;
            if (estado == null) {
                estado = Pago.ESTADO_PENDIENTE;
            }

            String e = estado.trim().toLowerCase();
            boolean esFinal = e.contains("aprob")
                || e.contains("acept")
                || e.contains("approved")
                || e.contains("completed")
                || e.contains("success")
                || e.contains("rechaz")
                || e.contains("fall")
                || e.contains("failed")
                || e.contains("declin")
                || e.contains("denied");
            if (esFinal) {
                break;
            }

            if (intento < 4) {
                try {
                    Thread.sleep(1500L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (estado == null) estado = Pago.ESTADO_PENDIENTE;
        String estadoMapeado = normalizarEstadoExterno(estado);

        if (Pago.ESTADO_COMPLETADO.equalsIgnoreCase(estadoMapeado)) {
            pagoDAO.actualizarRefPayco(idPago, refPayco.trim());
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_COMPLETADO);
            return Pago.ESTADO_COMPLETADO;
        }
        if (Pago.ESTADO_FALLIDO.equalsIgnoreCase(estadoMapeado)) {
            pagoDAO.actualizarRefPayco(idPago, refPayco.trim());
            Pago pago = pagoDAO.buscarPorId(idPago).orElseThrow(() -> new Exception("No existe un pago con id " + idPago));
            pagoDAO.actualizarEstado(idPago, Pago.ESTADO_FALLIDO);
            cancelarTurnoAsociado(pago, idPago, "Pago rechazado/fallido según validation endpoint de ePayco");
            return Pago.ESTADO_FALLIDO;
        }

        pagoDAO.actualizarRefPayco(idPago, refPayco.trim());
        pagoDAO.actualizarEstado(idPago, Pago.ESTADO_PENDIENTE);
        return Pago.ESTADO_PENDIENTE;
    }

    private String normalizarEstadoExterno(String estadoExterno) {
        if (estadoExterno == null || estadoExterno.isBlank()) {
            return Pago.ESTADO_PENDIENTE;
        }
        String normalizado = estadoExterno.trim().toLowerCase();
        boolean isAprobado = normalizado.contains("aprob")
            || normalizado.contains("acept")
            || normalizado.contains("accepted")
            || normalizado.contains("approved")
            || normalizado.contains("success")
            || normalizado.contains("exito")
            || normalizado.contains("successful")
            || normalizado.contains("complet")
            || normalizado.contains("completed")
            || normalizado.contains("paid")
            || normalizado.contains("authorized");
        if (isAprobado) return Pago.ESTADO_COMPLETADO;

        boolean isRechazado = normalizado.contains("rechaz")
            || normalizado.contains("reject")
            || normalizado.contains("denied")
            || normalizado.contains("declin")
            || normalizado.contains("cancel")
            || normalizado.contains("canceled")
            || normalizado.contains("cancelled")
            || normalizado.contains("failed")
            || normalizado.contains("fallid");
        if (isRechazado) return Pago.ESTADO_FALLIDO;

        return Pago.ESTADO_PENDIENTE;
    }

    private String mapearEstadoDesdeCallback(String codigoRespuesta, String textoRespuesta) {
        String cod = codigoRespuesta != null ? codigoRespuesta.trim() : "";
        String txt = textoRespuesta != null ? textoRespuesta.trim().toLowerCase() : "";

        // Códigos típicos ePayco: 1=aceptada, 2=rechazada, 3=pending, 4=fallida
        if ("1".equals(cod)) return Pago.ESTADO_COMPLETADO;
        if ("2".equals(cod) || "4".equals(cod)) return Pago.ESTADO_FALLIDO;
        if ("3".equals(cod)) return Pago.ESTADO_PENDIENTE;

        if (txt.contains("aprob") || txt.contains("acept")) return Pago.ESTADO_COMPLETADO;
        if (txt.contains("rechaz") || txt.contains("fall") || txt.contains("error")) return Pago.ESTADO_FALLIDO;
        return Pago.ESTADO_PENDIENTE;
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
    // Método para verificar el estado del pago consultando ePayco
    public String verificarEstado(int idPago) throws Exception {
        Pago pago = pagoDAO.buscarPorId(idPago)
                .orElseThrow(() -> new Exception("No existe un pago con id " + idPago));

        if (pago.getEpaycoSessionId() == null || pago.getEpaycoSessionId().isBlank()) {
            // Puede ocurrir una carrera breve entre creación del pago y actualización de sessionId.
            // No cancelar ni marcar fallido automáticamente en este punto.
            System.out.println("[PagosOnlineDAO] idPago=" + idPago + " aún sin sessionId. Se deja en PENDIENTE.");
            return "PENDIENTE";
        }

        String raw = "";
        EpaycoStatusResult statusResult = null;
        try {
            System.out.println("[PagosOnlineDAO] consultarEstado usando sessionId=" + pago.getEpaycoSessionId() + " (idPago=" + idPago + ")");
            statusResult = epaycoService.consultarEstado(pago.getEpaycoSessionId());
            raw = statusResult != null && statusResult.getEstado() != null ? statusResult.getEstado().trim() : "";
            System.out.println("[PagosOnlineDAO] estado crudo recibido de ePayco para idPago=" + idPago + ": " + raw);
            // Si la respuesta incluye ref_payco, guardarla
           if (statusResult != null && statusResult.getRefPayco() != null
                    && statusResult.getRefPayco().matches("[0-9]{5,12}")) {
                try {
                    pagoDAO.actualizarRefPayco(idPago, statusResult.getRefPayco());
                } catch (Exception e) {
                    System.out.println("[PagosOnlineDAO] No se pudo actualizar epayco_ref_payco en BD: " + e.getMessage());
                }
            }
        } catch (Exception ex) {
            // Error de red/API no implica rechazo real del pago.
            System.out.println("[PagosOnlineDAO] Error consultando ePayco para idPago=" + idPago + ": " + ex.getMessage() + ". Se deja en PENDIENTE.");
            return "PENDIENTE";
        }

        boolean tieneRefPayco = statusResult != null
                && statusResult.getRefPayco() != null
                && !statusResult.getRefPayco().isBlank();

        // Guardrail anti-falso positivo:
        // si llega "COMPLETADO" pero no hay ref_payco, no se confirma pago.
        // Esto evita aprobar por inferencias ambiguas del endpoint de sesión.
        if ("COMPLETADO".equalsIgnoreCase(raw) && !tieneRefPayco) {
            System.out.println("[PagosOnlineDAO] COMPLETADO sin ref_payco (idPago=" + idPago + ") -> se fuerza PENDIENTE para evitar falso aprobado.");
            raw = "PENDIENTE";
        }

        if (raw.startsWith("{")) {
            System.out.println("[PagosOnlineDAO] La respuesta de ePayco es JSON crudo; se aplicará clasificación por contenido.");
        }
        String normalizado = raw.toLowerCase();

        // Normalización robusta: aceptar variantes en español e inglés y diferentes palabras clave
        boolean isAprobado = normalizado.contains("aprob")
            || normalizado.contains("acept")
            || normalizado.contains("accepted")
            || normalizado.contains("approved")
            || normalizado.contains("success")
            || normalizado.contains("exito")
            || normalizado.contains("successful")
            || normalizado.contains("complet")
            || normalizado.contains("completed")
            || normalizado.contains("paid")
            || normalizado.contains("authorized");
        boolean isRechazado = normalizado.contains("rechaz")
            || normalizado.contains("reject")
            || normalizado.contains("denied")
            || normalizado.contains("declin")
            || normalizado.contains("cancel")
            || normalizado.contains("canceled")
            || normalizado.contains("cancelled")
            || normalizado.contains("failed");
        boolean isPendiente = normalizado.contains("pend")
            || normalizado.contains("pending")
            || normalizado.contains("created")
            || normalizado.contains("init")
            || normalizado.contains("process")
            || normalizado.contains("waiting");

        if (isAprobado) {
            System.out.println("[PagosOnlineDAO] Clasificación final idPago=" + idPago + " -> COMPLETADO | motivo=coincidencia de palabras clave en la respuesta.");
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
            return Pago.ESTADO_COMPLETADO;
        } else if (isRechazado) {
            System.out.println("[PagosOnlineDAO] Clasificación final idPago=" + idPago + " -> FALLIDO | motivo=coincidencia de palabras clave de rechazo.");
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
        } else if (!isPendiente) {
            // Cualquier estado no reconocido se considera pendiente para evitar falsos negativos.
            System.out.println("[PagosOnlineDAO] Estado no categorizado (PENDIENTE): " + raw + " (idPago=" + idPago + ")");
            return "PENDIENTE";
        }
        System.out.println("[PagosOnlineDAO] Clasificación final idPago=" + idPago + " -> PENDIENTE | motivo=no hubo coincidencia clara de aprobado/rechazado.");
        return Pago.ESTADO_PENDIENTE;
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

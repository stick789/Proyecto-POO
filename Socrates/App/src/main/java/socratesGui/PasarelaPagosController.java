package socratesGui;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dao.EntrenadorDAO;
import dao.InstalacionDAO;
import dao.PagoDAO;
import dao.PagosOnlineDAO;
import dao.PersonaDAO;
import dao.TurnoDAO;
import entidades.Pago;
import entidades.Turno;
import entidades.Usuario;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import util.ConfigLoader;

public class PasarelaPagosController {

    @FXML private Label lblIdTurno;
    @FXML private Label lblTotal;
    @FXML private Label lblDescripcion;
    @FXML private Button btnPagar;
    @FXML private Button btnVerificar;
    @FXML private Button btnCerrar;
    @FXML private TextArea txtLog;

    private PagoDAO pagoDAO;
    private PagosOnlineDAO pagosOnlineDAO;
    private HostServices hostServices;
    private String publicKey;
    private String privateKey;
    private Turno turnoActual;
    private Integer idPagoGenerado = null;
    private double totalTurno = 0.0;
    private HttpServer checkoutServer;
    private Timer verificationTimer;
    private boolean pagoFinalizado = false;
    private boolean pagoIniciado = false;
    private static final int PUERTO_LOCAL_EPAYCO = 8080;

    private void iniciarServidorCheckoutSiNecesario() throws IOException {
        if (checkoutServer != null) return;

        try {
            checkoutServer = HttpServer.create(new InetSocketAddress("127.0.0.1", PUERTO_LOCAL_EPAYCO), 0);
        } catch (IOException ex) {
            txtLog.appendText("⚠️ No se pudo usar el puerto 8080; intentando puerto dinámico.\n");
            checkoutServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        }
        checkoutServer.createContext("/checkout", (HttpExchange exchange) -> {
            String sessionId = valorParam(parseQueryParams(exchange.getRequestURI() != null ? exchange.getRequestURI().getRawQuery() : null), "sessionId");
            byte[] response = construirHtmlCheckout(sessionId).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        checkoutServer.createContext("/epayco-response", (HttpExchange exchange) -> {
            String query = exchange.getRequestURI() != null ? exchange.getRequestURI().getRawQuery() : null;
            try {
                String headersDump = headersToText(exchange);
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String dump = "[RESPONSE] QUERY=" + (query != null ? query : "") + "\nHEADERS=" + headersDump + "\nBODY=" + body + "\n";
                appendRawPayloadLog("epayco-response", dump);
            } catch (Exception ex) {
                System.out.println("[PasarelaPagosController] No se pudo volcar payload response: " + ex.getMessage());
            }
            Map<String, String> params = parseQueryParams(query);
            String ref = valorParam(params, "ref_payco", "x_ref_payco", "ref");
            String idPagoTxt = valorParam(params, "idPago", "paymentId", "id_pago");
            Integer idPagoCb = parseIdPago(idPagoTxt);
            if ((idPagoCb == null || idPagoCb <= 0) && ref != null && !ref.isBlank()) {
                try {
                    idPagoCb = pagoDAO.buscarPorRefPayco(ref.trim()).map(Pago::getIdPago).map(Long::intValue).orElse(null);
                } catch (Exception ex) {
                    System.out.println("[PasarelaPagosController] No se pudo resolver idPago desde ref_payco: " + ex.getMessage());
                }
            }

            String estadoFinal = Pago.ESTADO_PENDIENTE;
            if (idPagoCb != null && idPagoCb > 0 && ref != null && !ref.isBlank()) {
                try {
                    estadoFinal = pagosOnlineDAO.aplicarResultadoRealDesdeReferencia(idPagoCb, ref.trim());
                } catch (Exception ex) {
                    estadoFinal = Pago.ESTADO_PENDIENTE;
                    System.out.println("[PasarelaPagosController] Error aplicando respuesta ePayco por referencia: " + ex.getMessage());
                }
            }

            final Integer idPagoFinal = idPagoCb;
            final String estadoUi = estadoFinal;
            final String refUi = ref;
            Platform.runLater(() -> {
                txtLog.appendText("📩 Respuesta ePayco recibida | idPago=" + (idPagoFinal != null ? idPagoFinal : "N/A") + ", ref=" + (refUi != null ? refUi : "N/A") + ", estado=" + estadoUi + "\n");
                if (Pago.ESTADO_COMPLETADO.equalsIgnoreCase(estadoUi)) {
                    pagoFinalizado = true;
                    mostrarAlerta("Éxito", "Pago confirmado por respuesta de ePayco.");
                    cerrarVentana();
                } else if (Pago.ESTADO_FALLIDO.equalsIgnoreCase(estadoUi)) {
                    cerrarPorFallo("Pago rechazado/fallido según respuesta de ePayco.");
                }
            });

            String html = "<!doctype html><html><head><meta charset='utf-8'><title>Resultado de pago</title></head>"
                    + "<body style='font-family:Arial,sans-serif;padding:24px'>"
                    + "<h3>Resultado recibido</h3>"
                    + "<p>Estado: <strong>" + escapeHtml(estadoFinal) + "</strong></p>"
                    + "<p>Ref ePayco: <strong>" + escapeHtml(ref != null ? ref : "N/A") + "</strong></p>"
                    + "<p>Puedes volver a la aplicación.</p>"
                    + "</body></html>";

            byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        checkoutServer.createContext("/epayco-confirmation", (HttpExchange exchange) -> {
            Map<String, String> params = new HashMap<>();
            String body = "";
            try {
                String headersDump = headersToText(exchange);
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String dump = "[CONFIRMATION] QUERY=" + (exchange.getRequestURI() != null ? exchange.getRequestURI().getRawQuery() : "") + "\nHEADERS=" + headersDump + "\nBODY=" + body + "\n";
                appendRawPayloadLog("epayco-confirmation", dump);
            } catch (Exception ex) {
                System.out.println("[PasarelaPagosController] No se pudo volcar payload confirmation: " + ex.getMessage());
            }
            params.putAll(parseQueryParams(exchange.getRequestURI() != null ? exchange.getRequestURI().getRawQuery() : null));
            params.putAll(parseQueryParams(body));

            String ref = valorParam(params, "ref_payco", "x_ref_payco");
            String cod = valorParam(params, "x_cod_response", "cod_response");
            String resp = valorParam(params, "x_response", "response");
            String extra1 = valorParam(params, "x_extra1", "extra1");
            String invoice = valorParam(params, "x_invoice", "invoice", "x_id_invoice");

            Integer idPagoCb = parseIdPago(extra1);
            if ((idPagoCb == null || idPagoCb <= 0)) idPagoCb = parseIdPago(invoice);
            if ((idPagoCb == null || idPagoCb <= 0) && ref != null && !ref.isBlank()) {
                try {
                    idPagoCb = pagoDAO.buscarPorRefPayco(ref.trim()).map(Pago::getIdPago).map(Long::intValue).orElse(null);
                } catch (Exception ex) {
                    System.out.println("[PasarelaPagosController] No se pudo resolver idPago desde webhook ref_payco: " + ex.getMessage());
                }
            }

            String estadoFinal = Pago.ESTADO_PENDIENTE;
            if (idPagoCb != null && idPagoCb > 0) {
                try {
                    estadoFinal = pagosOnlineDAO.aplicarResultadoRealDesdeCallback(idPagoCb, cod, resp, ref);
                } catch (Exception ex) {
                    estadoFinal = Pago.ESTADO_PENDIENTE;
                    System.out.println("[PasarelaPagosController] Error procesando webhook ePayco: " + ex.getMessage());
                }
            }

            final Integer idPagoFinal = idPagoCb;
            final String estadoUi = estadoFinal;
            Platform.runLater(() -> txtLog.appendText("📡 Webhook ePayco recibido | idPago=" + (idPagoFinal != null ? idPagoFinal : "N/A") + ", estado=" + estadoUi + "\n"));

            byte[] ok = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });
        checkoutServer.setExecutor(null);
        checkoutServer.start();
    }

    private void appendRawPayloadLog(String label, String content) {
        try {
            Path p = Paths.get("logs/epayco_raw_payloads.log");
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            String entry = java.time.Instant.now().toString() + " " + label + "\n" + content + "\n----\n";
            Files.write(p, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Platform.runLater(() -> txtLog.appendText("📝 Payload guardado en logs/epayco_raw_payloads.log: " + label + "\n"));
        } catch (Exception e) {
            System.out.println("[PasarelaPagosController] Error al guardar payload: " + e.getMessage());
        }
    }

    private String headersToText(HttpExchange exchange) {
        StringBuilder sb = new StringBuilder();
        sb.append("METHOD=").append(exchange.getRequestMethod()).append("; ");
        exchange.getRequestHeaders().forEach((k, v) -> sb.append(k).append("=").append(String.join("|", v)).append("; "));
        return sb.toString();
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(k, v);
        }
        return params;
    }

    private String valorParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            if (params.containsKey(key)) return params.get(key);
        }
        return null;
    }

    private void cerrarPorFallo(String motivo) {
        pagoFinalizado = true;
        txtLog.appendText("❌ " + motivo + "\n");
        mostrarAlerta("Pago fallido", motivo);
        cerrarVentana();
    }
 

    public void setTurno(Turno turno) {
        this.turnoActual = turno;
        if (turno != null) {
            lblIdTurno.setText(String.valueOf(turno.getIdTurno()));
            double total = calcularTotalTurno();
            this.totalTurno = total;
            lblTotal.setText(formatearPesos(total));
            lblDescripcion.setText("Pago turno #" + turno.getIdTurno());
            txtLog.appendText("ℹ️ Turno cargado. Presiona 'PAGAR CON TARJETA' para abrir ePayco.\n");
        }
    }
//Metodo para verificar el pago cada cierto tiempo
    private void iniciarVerificacionAutomatica(){
        if (verificationTimer != null) {
            verificationTimer.cancel();
        }
        verificationTimer = new Timer(true);
        verificationTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (pagoFinalizado) {
                    verificationTimer.cancel();
                    return;
                }
                try {
                    if (idPagoGenerado == null) {
                        idPagoGenerado = obtenerIdPagoPorTurno(turnoActual.getIdTurno());
                    }
                    if(idPagoGenerado != null && idPagoGenerado != -1) {
                        String estado = pagosOnlineDAO.verificarEstado(idPagoGenerado);
                        Platform.runLater(() -> {
                            txtLog.appendText("🔄 Verificación automática: " + estado + "\n");
                                if (Pago.ESTADO_COMPLETADO.equalsIgnoreCase(estado)) {
                                    pagoFinalizado = true;
                                    txtLog.appendText("✅ Pago confirmado por verificación automática.\n");
                                    mostrarAlerta("Éxito", "El pago ha sido APROBADO.");
                                    cerrarVentana();
                                } else if (Pago.ESTADO_FALLIDO.equalsIgnoreCase(estado)) {
                                    cerrarPorFallo("El pago fue rechazado por verificación automática.");
                                }
                        });
                    }
                    // Si no hay pago generado aún, o si el estado es pendiente, seguimos esperando.
                }catch (Exception e) {
                    Platform.runLater(() -> cerrarPorFallo("Error en verificación automática: " + e.getMessage())); 
                }
            }
        }, 3000, 3000); // Verificar cada 3 segundos
    }



    @FXML
    public void initialize() {
        publicKey = ConfigLoader.get("epayco.publicKey");
        privateKey = ConfigLoader.get("epayco.privateKey");
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("Faltan las llaves de ePayco en config.properties (epayco.publicKey / epayco.privateKey)");
        }
        pagoDAO = new PagoDAO();
        pagosOnlineDAO = new PagosOnlineDAO(publicKey, privateKey, pagoDAO);
        btnPagar.setOnAction(event -> iniciarPago());
        btnVerificar.setOnAction(event -> verificarPago());
        btnCerrar.setOnAction(event -> cerrarVentana());
        txtLog.appendText("✅ Controlador listo. Esperando acción...\n");

        if (turnoActual != null) {
            try {
                TurnoDAO turnoDAO = new TurnoDAO(new PersonaDAO(), new InstalacionDAO(), new EntrenadorDAO());
                Optional<Turno> opt = turnoDAO.buscarPorId(turnoActual.getIdTurno());
                if (opt.isPresent()) {
                    setTurno(opt.get());
                    txtLog.appendText("🔎 Turno de prueba cargado: id=" + turnoActual.getIdTurno() + "\n");
                } else {
                    txtLog.appendText("⚠️ No se encontró turno de prueba id=" + turnoActual.getIdTurno() + " en BD.\n");
                }
            } catch (Exception e) {
                txtLog.appendText("🔴 Error cargando turno de prueba: " + e.getMessage() + "\n");
            }
        }
    }

    private void iniciarPago() {
        try {
            if (pagoIniciado) {
                txtLog.appendText("⚠️ El pago ya ha sido iniciado para este turno.\n");
                return;
            }
            if (turnoActual == null) {
                mostrarAlerta("Error", "No hay un turno seleccionado.");
                return;
            }

            double total = this.totalTurno;
            if (total <= 0) {
                mostrarAlerta("Error", "El total a pagar debe ser mayor a cero.");
                return;
            }

            iniciarServidorCheckoutSiNecesario();
            String localBaseUrl = "http://127.0.0.1:" + checkoutServer.getAddress().getPort();
            String responseUrl = ConfigLoader.get("epayco.responseUrl");
            if (responseUrl == null || responseUrl.isBlank() || responseUrl.contains(":8080/")) {
                responseUrl = localBaseUrl + "/epayco-response?idPago={idPago}";
            }
            String confirmationUrl = ConfigLoader.get("epayco.confirmationUrl");
            if (confirmationUrl == null || confirmationUrl.isBlank() || confirmationUrl.contains(":8080/")) {
                confirmationUrl = localBaseUrl + "/epayco-confirmation?idPago={idPago}";
                txtLog.appendText("⚠️ No hay confirmationUrl pública en config.properties; usando local solo para pruebas.\n");
            }
            txtLog.appendText("🔗 Response ePayco: " + responseUrl + "\n");
            txtLog.appendText("🔗 Confirmation ePayco: " + confirmationUrl + "\n");

            String descripcion = lblDescripcion.getText();
            String sessionOrUrl = pagosOnlineDAO.iniciarPagoConTarjeta(
                    turnoActual.getIdTurno(), total, descripcion, responseUrl, confirmationUrl);
            pagoIniciado = true;
            idPagoGenerado = obtenerIdPagoPorTurno(turnoActual.getIdTurno());

            String sessionId = sessionOrUrl;
            String checkoutUrl = "http://127.0.0.1:" + checkoutServer.getAddress().getPort() + "/checkout?sessionId=" + sessionId;
            txtLog.appendText("🟢 Sesión creada. sessionId=" + sessionId + "\n");
            if (hostServices != null) {
                hostServices.showDocument(checkoutUrl);
            } else {
                txtLog.appendText("🔗 Abra manualmente: " + checkoutUrl + "\n");
                mostrarAlerta("Información", "No pude abrir el navegador automáticamente. Copia y pega la URL:\n" + checkoutUrl);
            }

            txtLog.appendText("🟢 Checkout abierto en navegador.\n");
            mostrarAlerta("Información", "La pasarela se abrió en tu navegador. Completa el pago y espera la redirección de ePayco.");
            iniciarVerificacionAutomatica();
        } catch (Exception e) {
            txtLog.appendText("🔴 Error al iniciar pago: " + e.getMessage() + "\n");
            mostrarAlerta("Error", "No se pudo iniciar el pago: " + e.getMessage());
        }
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    private void verificarPago() {
        try {
            if (turnoActual == null) {
                mostrarAlerta("Error", "No hay turno seleccionado.");
                return;
            }

            if (idPagoGenerado == null) {
                idPagoGenerado = obtenerIdPagoPorTurno(turnoActual.getIdTurno());
            }
            if (idPagoGenerado == null || idPagoGenerado == -1) {
                txtLog.appendText("⚠️ No hay registro de pago online para este turno.\n");
                mostrarAlerta("Información", "Aún no se ha iniciado ningún pago para este turno.");
                return;
            }

            String estado = pagosOnlineDAO.verificarEstado(idPagoGenerado);
            txtLog.appendText("🔄 Estado consultado: " + estado + "\n");
            if (Pago.ESTADO_COMPLETADO.equalsIgnoreCase(estado)) {
                txtLog.appendText("✅ ¡Pago confirmado! Estado actualizado en BD.\n");
                mostrarAlerta("Éxito", "El pago ha sido APROBADO.");
            } else if (Pago.ESTADO_FALLIDO.equalsIgnoreCase(estado)) {
                cerrarPorFallo("El pago fue rechazado.");
            } else {
                txtLog.appendText("⏳ Pago pendiente. Intente más tarde.\n");
                mostrarAlerta("Pendiente", "El pago aún no ha sido confirmado.");
            }
        } catch (RuntimeException e) {
            cerrarPorFallo("Error al verificar (runtime): " + e.getMessage());
        } catch (Exception e) {
            cerrarPorFallo("Error al verificar: " + e.getMessage());
        }
    }

    private void cerrarVentana() {
        if (verificationTimer != null) {
            verificationTimer.cancel();
            verificationTimer = null;
        }
        if (checkoutServer != null) {
            checkoutServer.stop(0);
            checkoutServer = null;
        }
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    private Integer parseIdPago(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.trim();
            if (cleaned.toUpperCase().startsWith("PAGO-")) {
                cleaned = cleaned.substring(5).trim();
            }
            return Integer.valueOf(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String formatearPesos(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        formato.setMaximumFractionDigits(0);
        formato.setMinimumFractionDigits(0);
        return "$ " + formato.format(Math.round(valor));
    }

private String construirHtmlCheckout(String sessionId) {
    String session = escapeHtml(sessionId != null ? sessionId : "");

    return "<!DOCTYPE html>\n" +
            "<html lang=\"es\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Checkout ePayco</title>\n" +
            "    <script src=\"https://checkout.epayco.co/checkout-v2.js\"></script>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; padding: 24px; max-width: 760px; margin: 0 auto; background: #fafafa; color: #111; }\n" +
            "        .card { background: white; border: 1px solid #ddd; border-radius: 12px; padding: 20px; margin-bottom: 16px; }\n" +
            "        button { background-color: #16a34a; color: white; padding: 12px 18px; border: none; border-radius: 8px; cursor: pointer; font-weight: 700; }\n" +
            "        button:hover { background-color: #15803d; }\n" +
            "        .hint { color: #444; font-size: 0.95rem; line-height: 1.45; }\n" +
            "        code { background:#f3f4f6; padding:2px 4px; border-radius:4px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "    <h2>Checkout ePayco</h2>\n" +
            "    <div class=\"card\">\n" +
            "        <div class=\"hint\">\n" +
            "            Se abre el checkout oficial con <code>sessionId</code> y <code>type: standard</code>, como indica la documentación.\n" +
            "            Si la redirección final no llega a la app, revisa la URL <code>response</code> y el webhook <code>confirmation</code>.\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        var sessionId = \"" + session + "\";\n" +
            "        function abrirCheckout() {\n" +
            "            var checkout = ePayco.checkout.configure({\n" +
            "                sessionId: sessionId,\n" +
            "                type: 'standard',\n" +
            "                test: true\n" +
            "            });\n" +
            "            checkout.onCreated(() => console.log('Checkout creado'));\n" +
            "            checkout.onErrors((e) => console.error('Checkout error', e));\n" +
            "            checkout.onClosed(() => console.log('Checkout cerrado'));\n" +
            "            checkout.open();\n" +
            "        }\n" +
            "    </script>\n" +
            "\n" +
            "    <div class=\"card\">\n" +
            "        <p class=\"hint\">SessionId: <code>" + session + "</code></p>\n" +
            "        <button onclick=\"abrirCheckout()\">Abrir checkout ePayco</button>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"card\">\n" +
            "        <h3>Tarjetas de Crédito de Pruebas</h3>\n" +
            "        <p class=\"hint\"><strong>Aceptada</strong>: Visa 4575623182290326, expiración 12/2027, CVV 123.</p>\n" +
            "        <p class=\"hint\"><strong>Fondos insuficientes</strong>: Visa 4151611527583283, expiración 12/2027, CVV 123.</p>\n" +
            "        <p class=\"hint\"><strong>Fallida</strong>: Mastercard 5170394090379427, expiración 12/2027, CVV 123.</p>\n" +
            "        <p class=\"hint\"><strong>Pendiente</strong>: American Express 373118856457642, expiración 12/2027, CVV 123.</p>\n" +
            "    </div>\n" +
            "\n" +
            "</body>\n" +
            "</html>";
}


    private double calcularTotalTurno() {
        if (turnoActual == null) {
            txtLog.appendText("⚠️ No hay turno para calcular total.\n");
            return 0.0;
        }
        Usuario usuario = turnoActual.getUsuario();
        if(usuario == null) {
            txtLog.appendText("⚠️ El turno no tiene un usuario asociado para calcular total.\n");
            return 0.0;
        }
        double precioBase = 25000.0; // Precio base por turno en COP
        /*
        *Se verifica que el Rol sea Usuario
        *La categoria No Afiliado es == a Null
        */
        try{
            String rol = usuario.getRolBD();
            String categoria = usuario.getCategoria();
            String categoriaNormalizada = categoria != null ? categoria.trim().toUpperCase() : "";

            boolean esUsuario = "Usuario".equalsIgnoreCase(rol);
            boolean categoriaValida = categoria != null && !categoria.trim().isEmpty() && !categoria.trim().equalsIgnoreCase("No Afiliado");

            if (esUsuario && categoriaValida) {
                double descuento;
                switch (categoriaNormalizada) {
                    case "ESTUDIANTE":
                        descuento = 1.00; // 100% de descuento
                        break;
                    case "A":
                        descuento = 0.40; // 40% de descuento
                        break;
                    case "B":
                        descuento = 0.20; // 20% de descuento
                        break;
                    case "C":
                        descuento = 0.10; // 10% de descuento
                        break;
                    default:
                        descuento = 0.0; // Sin descuento para categorías no reconocidas
                        break;
                }
                double totalConDescuento = precioBase * (1 - descuento);
                totalConDescuento = Math.round(totalConDescuento); // Redondear a COP sin decimales
                txtLog.appendText("ℹ️ Usuario"+ usuario.getNombre() + " (Categoría: " + categoria + ")'. Precio base: " + formatearPesos(precioBase) + ", descuento aplicado: " + (descuento * 100) + "%, total a pagar: " + formatearPesos(totalConDescuento) + "\n");
                return totalConDescuento;
            } else {
                txtLog.appendText("ℹ️ Usuario"+ usuario.getNombre() + " no cumple con los requisitos para aplicar descuentos. Se aplica precio base sin descuentos.\n");
                return precioBase;
            }
        }catch (Exception e) {
            txtLog.appendText("🔴 Error calculando total con descuento: " + e.getMessage() + ". Se aplicará precio base sin descuentos.\n");
            return precioBase;
        }
    }


    private int obtenerIdPagoPorTurno(int idTurno) {
        try {
            if (pagoDAO == null) {
                pagoDAO = new PagoDAO();
            }
            java.util.List<Pago> lista = pagoDAO.listarPorTurno(idTurno);
            if (lista == null || lista.isEmpty()) {
                return -1;
            }
            Pago pago = lista.get(0);
            return (int) pago.getIdPago();
        } catch (Exception e) {
            return -1;
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}

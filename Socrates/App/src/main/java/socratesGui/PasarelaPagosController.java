package socratesGui;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
            lblTotal.setText("$ " + String.format("%,.2f", total));
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

            String descripcion = lblDescripcion.getText();
            String sessionOrUrl = pagosOnlineDAO.iniciarPagoConTarjeta(turnoActual.getIdTurno(), total, descripcion);
            pagoIniciado = true;
            idPagoGenerado = obtenerIdPagoPorTurno(turnoActual.getIdTurno());

            // Si el backend devolvió una URL de pago, abrirla directamente (evita que JS cree otra sesión distinta)
            if (sessionOrUrl != null && sessionOrUrl.toLowerCase().startsWith("http")) {
                txtLog.appendText("🟢 Abriendo URL de pago provista por backend: " + sessionOrUrl + "\n");
                if (hostServices != null) {
                    hostServices.showDocument(sessionOrUrl);
                } else {
                    txtLog.appendText("🔗 Abra manualmente: " + sessionOrUrl + "\n");
                    mostrarAlerta("Información", "No pude abrir el navegador automáticamente. Copia y pega la URL:\n" + sessionOrUrl);
                }
            } else {
                // fallback: servir página local que usaba JS para crear la sesión (mantener compatibilidad)
                String html = construirHtmlCheckout(total, publicKey, idPagoGenerado);
                try {
                    abrirCheckoutLocal(html);
                    String uri = "http://127.0.0.1:" + checkoutServer.getAddress().getPort() + "/checkout";
                    if (hostServices != null) {
                        hostServices.showDocument(uri);
                    } else {
                        txtLog.appendText("🔗 Abra manualmente: " + uri + "\n");
                        mostrarAlerta("Información", "No pude abrir el navegador automáticamente. Copia y pega la URL:\n" + uri);
                    }
                } catch (IOException e) {
                    txtLog.appendText("🔴 Error preparando checkout local: " + e.getMessage() + "\n");
                    mostrarAlerta("Error", "No pude preparar la pasarela local: " + e.getMessage());
                }
            }

            txtLog.appendText("🟢 Pasarela abierta en navegador. Session/URL: " + sessionOrUrl + "\n");
            mostrarAlerta("Información", "La pasarela se abrió en tu navegador.\nCompleta el pago y luego presiona 'VERIFICAR PAGO'.");
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

    private void abrirCheckoutLocal(String html) throws IOException {
        if (checkoutServer != null) {
            checkoutServer.stop(0);
            checkoutServer = null;
        }

        checkoutServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Contexto principal que sirve la página de checkout
        checkoutServer.createContext("/checkout", (HttpExchange exchange) -> {
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });

       
        checkoutServer.setExecutor(null);
        checkoutServer.start();
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

private String construirHtmlCheckout(double total, String publicKey, Integer backendPagoId) {
    String amount = String.valueOf(Math.round(total));
    String clientKey = escapeHtml(publicKey);

    return "<!DOCTYPE html>\n" +
            "<html lang=\"es\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Prueba de Pago ePayco Sandbox</title>\n" +
            "    <script src=\"https://checkout.epayco.co/checkout-v2.js\"></script>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; padding: 20px; max-width: 760px; margin: 0 auto; background: #121212; color: #f5f5f5; }\n" +
            "        .card { background: #1e1e1e; border: 1px solid #333; border-radius: 12px; padding: 20px; margin-bottom: 16px; }\n" +
            "        .form-group { margin-bottom: 15px; }\n" +
            "        label { display: block; margin-bottom: 5px; font-weight: bold; }\n" +
            "        input { width: 100%; padding: 10px; box-sizing: border-box; border-radius: 8px; border: 1px solid #444; background: #111; color: #fff; }\n" +
            "        button { background-color: #ff7a00; color: white; padding: 10px 15px; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; }\n" +
            "        button:hover { background-color: #e56f00; }\n" +
            "        #resultado { margin-top: 20px; padding: 10px; border: 1px solid #333; background: #111; border-radius: 8px; display: none; }\n" +
            "        .hint { color: #cfcfcf; font-size: 0.95rem; line-height: 1.4; }\n" +
            "        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "    <h2>Formulario de Prueba ePayco Sandbox</h2>\n" +
            "    <div class=\"card\">\n" +
            "        <div class=\"hint\">\n" +
            "            Este flujo usa <strong>checkout.epayco.co</strong> con <strong>test: true</strong>, que es el modo de pruebas real del checkout v2.\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        var client_key = \"" + clientKey + "\";\n" +
            "        window.ePayco.checkout.configure({ key: client_key, test: true });\n" +
            "    </script>\n" +
            "\n" +
            "    <div class=\"card\">\n" +
            "    <div class=\"grid\">\n" +
            "        <div class=\"form-group\">\n" +
            "            <label>Nombre del Titular:</label>\n" +
            "            <input type=\"text\" id=\"nombre\" value=\"PRUEBA USUARIO\">\n" +
            "        </div>\n" +
            "        <div class=\"form-group\">\n" +
            "            <label>Correo:</label>\n" +
            "            <input type=\"text\" id=\"email\" value=\"test@prueba.com\">\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"form-group\">\n" +
            "        <label>Descripción:</label>\n" +
            "        <input type=\"text\" id=\"descripcion\" value=\"Prueba de tarjeta en sandbox\">\n" +
            "    </div>\n" +
            "\n" +
            "    <div class=\"grid\">\n" +
            "        <div class=\"form-group\">\n" +
            "            <label>Documento:</label>\n" +
            "            <input type=\"text\" id=\"documento\" value=\"123456789\">\n" +
            "        </div>\n" +
            "        <div class=\"form-group\">\n" +
            "            <label>Moneda:</label>\n" +
            "            <input type=\"text\" id=\"moneda\" value=\"COP\">\n" +
            "        </div>\n" +
            "    </div>\n" +
            "\n" +
            "    <button onclick=\"realizarPago()\">Abrir checkout sandbox por $" + amount + " COP</button>\n" +
            "\n" +
            "    <div id=\"resultado\"></div>\n" +
            "\n" +
            "    <script>\n" +
            "        function realizarPago() {\n" +
            "            var nombre = document.getElementById(\"nombre\").value.trim();\n" +
            "            var email = document.getElementById(\"email\").value.trim();\n" +
            "            var descripcion = document.getElementById(\"descripcion\").value.trim();\n" +
            "            var documento = document.getElementById(\"documento\").value.trim();\n" +
            "            var moneda = document.getElementById(\"moneda\").value.trim() || 'COP';\n" +
            "            if (!nombre || !email || !descripcion || !documento) {\n" +
            "                alert(\"Completa los datos básicos para abrir el checkout sandbox.\");\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            var transaction = {\n" +
            "                type: 'checkout',\n" +
            "                key: client_key,\n" +
            "                test: true,\n" +
             "                ip: '0.0.0.0',\n" +
            "                name: nombre,\n" +
            "                description: descripcion,\n" +
            "                currency: moneda,\n" +
            "                amount: '" + amount + "',\n" +
            "                lang: 'es',\n" +
            "                checkoutType: 'standard',\n" +
            "                emailBilling: email,\n" +
            "                nameBilling: nombre,\n" +
            "                typeDocBilling: 'CC',\n" +
            "                numberDocBilling: documento,\n" +
            "                toApiPayload: function() {\n" +
            "                    return {\n" +
            "                        transaction: {\n" +
            "                            epaycoKey: this.key,\n" +
            "                            epaycoTest: true,\n" +
            "                            epaycoIp: this.ip,\n" +
            "                            epaycoName: this.name,\n" +
            "                            epaycoDescription: this.description,\n" +
            "                            epaycoCurrency: this.currency,\n" +
            "                            epaycoAmount: Number(this.amount),\n" +
            "                            epaycoLang: this.lang,\n" +
            "                            epaycoMethod: 'POST',\n" +
            "                            epaycoCheckoutType: this.checkoutType,\n" +
            "                            epaycoBilling: {\n" +
            "                                email: this.emailBilling,\n" +
            "                                name: this.nameBilling,\n" +
            "                                typeDoc: this.typeDocBilling,\n" +
            "                                numberDoc: this.numberDocBilling\n" +
            "                            }\n" +
            "                        }\n" +
            "                    };\n" +
            "                }\n" +
            "            };\n" +
            "\n" +
            "            window.ePayco.checkout._checkoutModule.createTransaction(transaction, 'standard').then(function(respuesta) {\n" +
            "                document.getElementById(\"resultado\").style.display = \"block\";\n" +
            "                document.getElementById(\"resultado\").innerHTML = \"<strong>Sesión sandbox creada:</strong> \" + (respuesta.sessionId || 'sin id') + \"<br><strong>URL:</strong> \" + (respuesta.url || 'sin url');\n" +
            "                // Notificar al backend local con la sessionId real para sincronizar estados\n" +
            "                try {\n" +
            "                    var sid = respuesta.sessionId || '';\n" +
            "                    var idPago = " + (backendPagoId != null ? backendPagoId.toString() : "null") + ";\n" +
            "                    if (sid && idPago) {\n" +
            "                        fetch('/checkout/callback?sessionId=' + encodeURIComponent(sid) + '&idPago=' + encodeURIComponent(idPago)).then(function(r){ return r.text(); }).then(function(txt){ console.log('Callback backend:', txt); });\n" +
            "                    }\n" +
            "                } catch(e) { console.log('Error notificando backend', e); }\n" +
            "                if (respuesta && respuesta.url) {\n" +
            "                    window.open(respuesta.url, '_blank');\n" +
            "                }\n" +
            "            }).catch(function(err) {\n" +
            "                console.log(err);\n" +
            "                document.getElementById(\"resultado\").style.display = \"block\";\n" +
            "                document.getElementById(\"resultado\").innerHTML = \"<strong>Error del sandbox:</strong> \" + (err && err.message ? err.message : err);\n" +
            "            });\n" +
            "        }\n" +
            "    </script>\n" +
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
        return 15000.0;
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

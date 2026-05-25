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
            double total = calcularTotalTurno(turno.getIdTurno());
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
                            if ("Aprobado".equals(estado)) {
                                pagoFinalizado = true;
                                txtLog.appendText("✅ Pago confirmado por verificación automática.\n");
                                mostrarAlerta("Éxito", "El pago ha sido APROBADO.");
                                cerrarVentana();
                            } else if ("Rechazado".equals(estado) || "FALLIDO".equalsIgnoreCase(estado)) {
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
        String publicKey = ConfigLoader.get("epayco.publicKey");
        String privateKey = ConfigLoader.get("epayco.privateKey");
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
            String sessionId = pagosOnlineDAO.iniciarPagoConTarjeta(turnoActual.getIdTurno(), total, descripcion);
            pagoIniciado = true;
            String miepaycoUrl = ConfigLoader.get("epayco.miepaycoUrl");
            if (miepaycoUrl == null || miepaycoUrl.isBlank()) {
                miepaycoUrl = "https://dquintero1582228.epayco.me";
            }

            idPagoGenerado = obtenerIdPagoPorTurno(turnoActual.getIdTurno());
            String html = construirHtmlCheckout(total, miepaycoUrl);

            try {
                abrirCheckoutLocal(html);
                String uri = "http://127.0.0.1:" + checkoutServer.getAddress().getPort() + "/checkout";
                if (hostServices != null) {
                    hostServices.showDocument(uri);
                } else {
                    txtLog.appendText("🔗 Abra manualmente: " + uri + "\n");
                    mostrarAlerta("Información", "No pude abrir el navegador automáticamente. Copia y pega la URL:\n" + uri);
                }
            } catch (Exception e) {
                txtLog.appendText("🔴 Error preparando checkout local: " + e.getMessage() + "\n");
                mostrarAlerta("Error", "No pude preparar la pasarela local: " + e.getMessage());
            }

            txtLog.appendText("🟢 Pasarela abierta en navegador. Session ID: " + sessionId + "\n");
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
            if ("Aprobado".equals(estado)) {
                txtLog.appendText("✅ ¡Pago confirmado! Estado actualizado en BD.\n");
                mostrarAlerta("Éxito", "El pago ha sido APROBADO.");
            } else if ("Rechazado".equals(estado) || "FALLIDO".equalsIgnoreCase(estado)) {
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

 private String formatAmountForEpaycoInput(double amount) {
    // Redondear a entero
    long rounded = Math.round(amount);
    // Formatear con separador de miles = coma
    java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
    symbols.setGroupingSeparator(',');
    java.text.DecimalFormat formatter = new java.text.DecimalFormat("$#,##0", symbols);
    return formatter.format(rounded);
}

private String construirHtmlCheckout(double total, String miepaycoUrl) {
    String amount = formatAmountForEpaycoInput(total);
    String url = escapeHtml(miepaycoUrl);

    return "<!doctype html>\n" +
            "<html lang=\"es\">\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "  <title>Pago ePayco.me</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div id=\"miepayco\">\n" +
            "    <script defer type=\"text/javascript\"\n" +
            "      src=\"https://mi-epayco.s3.amazonaws.com/embed.js\"\n" +
            "      miepaycoUrl=\"" + url + "\">\n" +
            "    </script>\n" +
            "  </div>\n" +
            "  <script>\n" +
            "    (function() {\n" +
            "      const targetAmount = '" + amount + "';\n" +
            "      console.log('Insertando monto:', targetAmount);\n" +
            "      function simulateTyping(el, text) {\n" +
            "        el.focus();\n" +
            "        el.value = '';\n" +
            "        for (let ch of text) {\n" +
            "          el.dispatchEvent(new KeyboardEvent('keydown', { key: ch, bubbles: true }));\n" +
            "          el.dispatchEvent(new KeyboardEvent('keypress', { key: ch, bubbles: true }));\n" +
            "          el.value += ch;\n" +
            "          el.dispatchEvent(new Event('input', { bubbles: true }));\n" +
            "          el.dispatchEvent(new KeyboardEvent('keyup', { key: ch, bubbles: true }));\n" +
            "        }\n" +
            "        el.dispatchEvent(new Event('change', { bubbles: true }));\n" +
            "        el.blur();\n" +
            "      }\n" +
            "      function findAndFill() {\n" +
            "        let input = document.querySelector('#miepayco input[placeholder*=\"$0\"]');\n" +
            "        if (!input) input = document.querySelector('#miepayco input[type=\"text\"]');\n" +
            "        if (!input) input = document.querySelector('#miepayco input');\n" +
            "        if (input && !input.dataset.filled) {\n" +
            "          simulateTyping(input, targetAmount);\n" +
            "          input.dataset.filled = 'true';\n" +
            "          return true;\n" +
            "        }\n" +
            "        return false;\n" +
            "      }\n" +
            "      setTimeout(findAndFill, 500);\n" +
            "      const observer = new MutationObserver(() => {\n" +
            "        if (findAndFill()) observer.disconnect();\n" +
            "      });\n" +
            "      observer.observe(document.getElementById('miepayco'), { childList: true, subtree: true });\n" +
            "      let attempts = 0;\n" +
            "      const interval = setInterval(() => {\n" +
            "        if (findAndFill() || attempts++ > 20) clearInterval(interval);\n" +
            "      }, 500);\n" +
            "    })();\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>";
}


    private double calcularTotalTurno(int idTurno) {
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
            e.printStackTrace();
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

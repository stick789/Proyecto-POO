package service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.EpaycoEstadoParser;

public class EpaycoService {
    private static final String API_BASE_URL = "https://apify.epayco.co";
    private static final String AUTH_URL = "https://apify.epayco.co/login";
    private static final String TRANSACTION_BASE_URL = "https://ms-checkout-create-transaction.epayco.co";

    private final ApiService apiService;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final String privateKey;

    public EpaycoService(String publicKey, String privateKey) {
        this.apiService = new ApiService();
        this.objectMapper = new ObjectMapper();
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    private String getAuthToken() throws Exception {
        Map<String, String> headers = new HashMap<>();
        String basicAuth = Base64.getEncoder().encodeToString((publicKey + ":" + privateKey).getBytes(StandardCharsets.UTF_8));
        headers.put("Authorization", "Basic " + basicAuth);

        String response = apiService.post(AUTH_URL, Map.of(), headers);
        JsonNode json = objectMapper.readTree(response);
        if (json.has("token")) return json.get("token").asText();
        if (json.has("access_token")) return json.get("access_token").asText();
        throw new RuntimeException("No se obtuvo access_token del servidor ePayco. Respuesta: " + response);
    }

    public java.util.Map<String, String> createPaymentSession(Map<String, Object> sessionData) throws Exception {
        String token = getAuthToken();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);

        String response = apiService.post(API_BASE_URL + "/payment/session/create", sessionData, headers);
        System.out.println("[EpaycoService] createPaymentSession raw response: " + response);
        JsonNode json = objectMapper.readTree(response);

        String sessionId = null;
        String url = null;

        if (json.has("data")) {
            JsonNode data = json.get("data");
            if (data.has("sessionId")) sessionId = data.get("sessionId").asText();
            if (data.has("url")) url = data.get("url").asText();
            if (data.has("paymentUrl")) url = url == null ? data.get("paymentUrl").asText() : url;
            if (data.has("redirect_url")) url = url == null ? data.get("redirect_url").asText() : url;
        }
        if (sessionId == null) {
            if (json.has("sessionId")) sessionId = json.get("sessionId").asText();
            if (json.has("session") && json.get("session").has("id")) sessionId = json.get("session").get("id").asText();
        }

        java.util.Map<String, String> out = new java.util.HashMap<>();
        if (sessionId != null) out.put("sessionId", sessionId);
        if (url != null) out.put("url", url);
        if (out.isEmpty()) {
            throw new RuntimeException("No se obtuvo sessionId/URL de ePayco. Respuesta: " + response);
        }
        return out;
    }

    public EpaycoStatusResult consultarEstado(String sessionId) throws Exception {
        String token = getAuthToken();
        Map<String, String> headers = Map.of("Authorization", "Bearer " + token);

        String respV2 = null;
        String respLegado = null;
        try {
            respV2 = apiService.get(TRANSACTION_BASE_URL + "/transactions/" + sessionId, headers);
        } catch (Exception ex) {
            System.out.println("[EpaycoService] error consultando transacción V2 para sessionId=" + sessionId + ": " + ex.getMessage() + ". Probando endpoint legado.");
        }

        try {
            respLegado = apiService.get(API_BASE_URL + "/payment/session/status/" + sessionId, headers);
        } catch (Exception ex) {
            System.out.println("[EpaycoService] error consultando estado legado para sessionId=" + sessionId + ": " + ex.getMessage());
        }

        String estadoV2 = EpaycoEstadoParser.extraerEstado(respV2);
        String estadoLegado = EpaycoEstadoParser.extraerEstado(respLegado);

        System.out.println("[EpaycoService] consultarEstado raw response V2 for sessionId=" + sessionId + ": " + respV2);
        System.out.println("[EpaycoService] consultarEstado raw response legado for sessionId=" + sessionId + ": " + respLegado);

        String mejorEstado = EpaycoEstadoParser.priorizarEstado(estadoV2, estadoLegado);

        // Extraer ref_payco si está presente en alguna de las respuestas
        String refV2 = extractRefPayco(respV2);
        String refLegado = extractRefPayco(respLegado);
        String mejorRef = refV2 != null ? refV2 : refLegado;

        if (mejorEstado != null) return new EpaycoStatusResult(mejorEstado, mejorRef);

        if (respV2 != null && !respV2.isBlank()) return new EpaycoStatusResult(respV2, mejorRef);
        if (respLegado != null && !respLegado.isBlank()) return new EpaycoStatusResult(respLegado, mejorRef);

        return new EpaycoStatusResult("PENDIENTE", mejorRef);
    }

    /**
     * Consulta el detalle real de la transacción por `ref_payco` usando el endpoint
     * de validación documentado por ePayco.
     */
    public EpaycoStatusResult consultarEstadoPorReferencia(String refPayco) throws Exception {
        if (refPayco == null || refPayco.isBlank()) {
            throw new IllegalArgumentException("refPayco no puede ser vacío");
        }

        String response = apiService.get("https://secure.epayco.co/validation/v1/reference/" + refPayco.trim(), null);
        System.out.println("[EpaycoService] consultarEstadoPorReferencia raw response for ref_payco=" + refPayco + ": " + response);

        String estado = EpaycoEstadoParser.extraerEstado(response);
        if (estado == null || estado.isBlank()) {
            estado = "PENDIENTE";
        }

        return new EpaycoStatusResult(estado, refPayco.trim());
    }

    private String extractRefPayco(String response) {
        if (response == null || response.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(response);
            // Solo buscar claves explícitas — NO "ref", "reference" ni búsqueda amplia,
            // ya que podrían capturar el sessionId hexadecimal u otros valores no numéricos
            String r = findKeyRecursive(root, "ref_payco");
            if (r == null) r = findKeyRecursive(root, "x_ref_payco");
            if (r == null) r = findKeyRecursive(root, "refPayco");
            if (r == null) return null;

            // El ref_payco de ePayco es SIEMPRE puramente numérico (ej: 100000123)
            // Los sessionIds son hexadecimales (ej: 6a165b65...) — se descartan aquí
            if (r.matches("[0-9]{5,12}")) return r;
            return null;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private String findKeyRecursive(JsonNode node, String key) {
        if (node == null) return null;
        if (node.isObject()) {
            if (node.has(key) && node.get(key).isTextual()) return node.get(key).asText();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String f = it.next();
                String r = findKeyRecursive(node.get(f), key);
                if (r != null) return r;
            }
        } else if (node.isArray()) {
            for (JsonNode e : node) {
                String r = findKeyRecursive(e, key);
                if (r != null) return r;
            }
        }
        return null;
    }

}

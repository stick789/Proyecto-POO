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

    private String extractRefPayco(String response) {
        if (response == null || response.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(response);
            // buscar claves típicas y variantes: ref_payco, refPayco, ref, reference, epaycoReference, pay_reference
            String r = findKeyRecursive(root, "ref_payco");
            if (r == null) r = findKeyRecursive(root, "refPayco");
            if (r == null) r = findKeyRecursive(root, "ref");
            if (r == null) r = findKeyRecursive(root, "reference");
            if (r == null) r = findKeyRecursive(root, "epaycoReference");
            if (r == null) r = findKeyRecursive(root, "pay_reference");
            // búsqueda más amplia: claves que parezcan referencia/recibo
            if (r == null) r = findRefLikeRecursive(root);
            if (r == null) return null;

            // Normalizar: extraer dígitos representativos del recibo (5-12 dígitos)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]{5,12})").matcher(r);
            if (m.find()) return m.group(1);
            // Si no hay dígitos, descartamos para no tomar descripciones por error
            return null;
        } catch (Exception e) {
            // si no es JSON, intentar buscar patrones numéricos comunes en el HTML/texto
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("(Referencia ePayco|Referencia|Recibo|ref_payco|refPayco|ref)[^0-9]{0,10}([0-9]{5,12})", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = p.matcher(response);
                if (m.find()) return m.group(2);
            } catch (Exception ex) {
                // ignore
            }
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

    private String findRefLikeRecursive(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String f = it.next();
                String lower = f.toLowerCase();
                boolean keyLooksLikeRef = lower.contains("ref") || lower.contains("reference") || lower.contains("recibo");
                JsonNode v = node.get(f);
                if (keyLooksLikeRef && v != null) {
                    // Prefer numeric values (the recibo/ref suele ser numérico)
                    if (v.isNumber()) return v.asText();
                    if (v.isTextual()) {
                        String txt = v.asText().trim();
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9]{5,12})").matcher(txt);
                        if (m.find()) return m.group(1);
                    }
                }
                String r = findRefLikeRecursive(v);
                if (r != null) return r;
            }
        } else if (node.isArray()) {
            for (JsonNode e : node) {
                String r = findRefLikeRecursive(e);
                if (r != null) return r;
            }
        }
        return null;
    }

}

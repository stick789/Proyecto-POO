package service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

 

public class EpaycoService {
    private static final String API_BASE_URL = "https://apify.epayco.co";
    private static final String AUTH_URL = "https://apify.epayco.co/login";
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

    public String createPaymentSession(Map<String, Object> sessionData) throws Exception {
        String token = getAuthToken();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);

        String response = apiService.post(API_BASE_URL + "/payment/session/create", sessionData, headers);
        JsonNode json = objectMapper.readTree(response);
        if (json.has("data") && json.get("data").has("sessionId")) {
            return json.get("data").get("sessionId").asText();
        }
        if (json.has("sessionId")) {
            return json.get("sessionId").asText();
        }
        if (json.has("session") && json.get("session").has("id")) {
            return json.get("session").get("id").asText();
        }
        throw new RuntimeException("No se obtuvo sessionId de ePayco. Respuesta: " + response);
    }
      public String consultarEstado(String sessionId) throws Exception {
        String token = getAuthToken();
        Map<String, String> headers = Map.of("Authorization", "Bearer " + token);
        String resp = apiService.get(API_BASE_URL + "/payment/session/" + sessionId, headers);
        JsonNode root = objectMapper.readTree(resp);
        if (root.has("data") && root.get("data").has("status")) {
            return root.get("data").get("status").asText();
        }
        if (root.has("status")) {
            return root.get("status").asText();
        }
        return "Desconocido";
    }
}

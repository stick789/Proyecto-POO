package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
public class ApiService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String post (String url, Map<String, Object> body, Map<String, String> headers) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        HttpRequest request = requestBuilder.build();
        System.out.println("[ApiService] POST " + url + "\nBody: " + jsonBody + "\nHeaders: " + (headers==null?"":headers));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[ApiService] Response " + response.statusCode() + "\n" + response.body());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("Error en la solicitud: " + response.statusCode() + " - " + response.body());
        }

    }

    public String get (String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET().header("Accept","application/json");
        
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        HttpRequest request = requestBuilder.build();
        System.out.println("[ApiService] GET " + url + "\nHeaders: " + (headers==null?"":headers));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[ApiService] Response " + response.statusCode() + "\n" + response.body());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("Error en la solicitud: " + response.statusCode() + " - " + response.body());
        }
    }

    public String postForm(String url, Map<String, String> formBody, Map<String, String> headers) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : formBody.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"));
            sb.append('=');
            sb.append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        String body = sb.toString();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json");

        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        HttpRequest request = requestBuilder.build();
        System.out.println("[ApiService] POST-FORM " + url + "\nBody: " + body + "\nHeaders: " + (headers==null?"":headers));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[ApiService] Response " + response.statusCode() + "\n" + response.body());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("Error en la solicitud: " + response.statusCode() + " - " + response.body());
        }
    }

}

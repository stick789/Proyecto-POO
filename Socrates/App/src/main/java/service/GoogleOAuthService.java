package service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javafx.application.HostServices;
import javafx.application.Platform;

/**
 * Flujo OAuth2 para Google con PKCE y servidor local de callback.
 *
 * Diseñado para aplicaciones de escritorio o web confiable: abre el navegador
 * del sistema y usa client_secret cuando está configurado.
 */
public class GoogleOAuthService {

    private static final String CLIENT_ID = "161773198624-rvb1huvn8v9niorcnenj1qp3lm1263rc.apps.googleusercontent.com";
     private static final String CLIENT_SECRET = cargarClientSecret();
    private static final int CALLBACK_PORT = 55555;
    private static final String REDIRECT_URI = "http://127.0.0.1:" + CALLBACK_PORT + "/callback";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private static final String SCOPES = "openid email profile";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleOAuthService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public GoogleProfile autenticar(HostServices hostServices) throws Exception {
        Objects.requireNonNull(hostServices, "HostServices es requerido para abrir el navegador.");

        String codeVerifier = generarCodeVerifier();
        String codeChallenge = generarCodeChallenge(codeVerifier);
        CompletableFuture<String> codeFuture = new CompletableFuture<>();

        HttpServer server = crearServidorCallback(codeFuture);
        try {
            server.start();

            String authUrl = construirUrlAutorizacion(codeChallenge);
            Platform.runLater(() -> hostServices.showDocument(authUrl));

            String code = codeFuture.get(5, TimeUnit.MINUTES);
            TokenResponse tokenResponse = intercambiarCodigoPorToken(code, codeVerifier);
            return obtenerPerfil(tokenResponse.accessToken, tokenResponse.refreshToken);
        } finally {
            server.stop(0);
        }
    }

    private HttpServer crearServidorCallback(CompletableFuture<String> codeFuture) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", CALLBACK_PORT), 0);
        server.createContext("/callback", exchange -> manejarCallback(exchange, codeFuture));
        server.setExecutor(Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "google-oauth-callback");
            thread.setDaemon(true);
            return thread;
        }));
        return server;
    }

    private void manejarCallback(HttpExchange exchange, CompletableFuture<String> codeFuture) throws IOException {
        String response;
        int status = 200;
        try {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            String error = params.get("error");
            String code = params.get("code");

            if (error != null && !error.isBlank()) {
                response = "<html><body><h2>Inicio de sesión cancelado</h2><p>Puedes volver a la aplicación.</p></body></html>";
                status = 200;
                codeFuture.completeExceptionally(new IllegalStateException("Google respondió con error: " + error));
            } else if (code == null || code.isBlank()) {
                response = "<html><body><h2>Respuesta inválida</h2><p>Falta el código de autorización.</p></body></html>";
                status = 400;
                codeFuture.completeExceptionally(new IllegalStateException("No se recibió el código de autorización de Google."));
            } else {
                response = "<html><body><h2>Sesión completada</h2><p>Ya puedes cerrar esta ventana y volver a la aplicación.</p></body></html>";
                codeFuture.complete(code);
            }
        } catch (Exception ex) {
            response = "<html><body><h2>Error</h2><p>No se pudo procesar la respuesta de Google.</p></body></html>";
            status = 500;
            codeFuture.completeExceptionally(ex);
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private TokenResponse intercambiarCodigoPorToken(String code, String codeVerifier) throws Exception {
        if (CLIENT_SECRET == null || CLIENT_SECRET.isBlank()) {
            throw new IllegalStateException(
                "Falta google.clientSecret en src/main/resources/properties/config.properties " +
                "o en la variable de entorno GOOGLE_CLIENT_SECRET. " +
                "Si tu cliente OAuth es de tipo Web, Google exige ese secreto para intercambiar el código.");
        }

        String body = formEncode(mapOf(
                "client_id", CLIENT_ID,
            "client_secret", CLIENT_SECRET,
                "code", code,
                "code_verifier", codeVerifier,
                "grant_type", "authorization_code",
                "redirect_uri", REDIRECT_URI));

        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("No se pudo intercambiar el código por tokens. Respuesta: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String accessToken = json.path("access_token").asText(null);
        String refreshToken = json.path("refresh_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Google no devolvió access_token.");
        }

        return new TokenResponse(accessToken, refreshToken);
    }

    private GoogleProfile obtenerPerfil(String accessToken, String refreshToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("No se pudo obtener el perfil de Google. Respuesta: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String email = json.path("email").asText(null);
        String nombre = json.path("name").asText(null);
        String googleId = json.path("sub").asText(null);

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Google no devolvió un email válido.");
        }
        if (googleId == null || googleId.isBlank()) {
            googleId = email;
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = email;
        }

        return new GoogleProfile(googleId, nombre, email, accessToken, refreshToken);
    }

    private String construirUrlAutorizacion(String codeChallenge) {
        return AUTH_URL +
                "?client_id=" + encode(CLIENT_ID) +
                "&redirect_uri=" + encode(REDIRECT_URI) +
                "&response_type=code" +
                "&scope=" + encode(SCOPES) +
                "&code_challenge=" + encode(codeChallenge) +
                "&code_challenge_method=S256" +
                "&access_type=offline" +
                "&prompt=consent";
    }

    private String generarCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generarCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formEncode(Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (builder.length() > 0) builder.append('&');
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private Map<String, String> mapOf(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static String cargarClientSecret() {
        String property = System.getProperty("google.clientSecret");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        String env = System.getenv("GOOGLE_CLIENT_SECRET");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        Properties properties = new Properties();
        try (InputStream input = GoogleOAuthService.class.getResourceAsStream("/properties/config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
        }

        String value = properties.getProperty("google.clientSecret");
        return value != null ? value.trim() : "";
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return values;
        String[] parts = rawQuery.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static final class TokenResponse {
        private final String accessToken;
        private final String refreshToken;

        private TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public static final class GoogleProfile {
        private final String googleId;
        private final String name;
        private final String email;
        private final String accessToken;
        private final String refreshToken;

        public GoogleProfile(String googleId, String name, String email, String accessToken, String refreshToken) {
            this.googleId = googleId;
            this.name = name;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getGoogleId() { return googleId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
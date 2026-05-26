package dao;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EpaycoEstadoParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private EpaycoEstadoParser() {
    }

    public static String priorizarEstado(String estadoV2, String estadoLegado) {
        String normalizadoV2 = normalizarEstado(estadoV2);
        String normalizadoLegado = normalizarEstado(estadoLegado);

        if (esEstadoFinal(normalizadoV2)) return estadoV2;
        if (esEstadoFinal(normalizadoLegado)) return estadoLegado;
        if (normalizadoV2 != null) return estadoV2;
        if (normalizadoLegado != null) return estadoLegado;
        return null;
    }

    public static String extraerEstado(String response) {
        if (response == null || response.isBlank()) return null;

        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);

            String encontrado = leerEstadoPreferente(root);
            if (encontrado != null) return encontrado;

            if (root.has("status") && root.get("status").isTextual()) return root.get("status").asText();

            String found = findStatusRecursive(root);
            if (found != null) return found;

            System.out.println("[EpaycoEstadoParser] Respuesta sin estado textual explícito; se devuelve el JSON crudo para análisis.");
            return response.trim();
        } catch (java.io.IOException ex) {
            return response.trim();
        }
    }

    private static String leerEstadoPreferente(JsonNode root) {
        if (root == null) return null;

        if (root.has("data")) {
            JsonNode data = root.get("data");
            String candidate = leerEstadoDesdeNodo(data);
            if (candidate != null) return candidate;
        }

        String candidate = leerEstadoDesdeNodo(root);
        if (candidate != null) return candidate;

        return null;
    }

    private static String leerEstadoDesdeNodo(JsonNode node) {
        if (node == null || !node.isObject()) return null;

        if (node.has("payment") && node.get("payment").has("status") && node.get("payment").get("status").isTextual()) {
            return node.get("payment").get("status").asText();
        }
        if (node.has("x_cod_response")) {
            String cod = node.get("x_cod_response").asText("").trim();
            if ("1".equals(cod)) return "Aceptada";
            if ("2".equals(cod)) return "Rechazada";
            if ("3".equals(cod)) return "Pendiente";
            if ("4".equals(cod)) return "Fallida";
            if (!cod.isBlank()) return cod;
        }
        if (node.has("transaction") && node.get("transaction").has("status") && node.get("transaction").get("status").isTextual()) {
            return node.get("transaction").get("status").asText();
        }
        if (node.has("x_response") && node.get("x_response").isTextual()) {
            return node.get("x_response").asText();
        }
        if (node.has("x_transaction_state") && node.get("x_transaction_state").isTextual()) {
            return node.get("x_transaction_state").asText();
        }
        if (node.has("x_response_reason_text") && node.get("x_response_reason_text").isTextual()) {
            return node.get("x_response_reason_text").asText();
        }
        if (node.has("session") && node.get("session").has("status") && node.get("session").get("status").isTextual()) {
            return node.get("session").get("status").asText();
        }
        if (node.has("status") && node.get("status").isTextual()) {
            return node.get("status").asText();
        }
        return null;
    }

    private static String findStatusRecursive(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if ("status".equalsIgnoreCase(field) || "state".equalsIgnoreCase(field) || "result".equalsIgnoreCase(field)) {
                    JsonNode v = node.get(field);
                    if (v != null && v.isTextual()) return v.asText();
                }
                if ("x_cod_response".equalsIgnoreCase(field)) {
                    JsonNode v = node.get(field);
                    if (v != null) {
                        String cod = v.asText("").trim();
                        if ("1".equals(cod)) return "Aceptada";
                        if ("2".equals(cod)) return "Rechazada";
                        if ("3".equals(cod)) return "Pendiente";
                        if ("4".equals(cod)) return "Fallida";
                        if (v.isTextual()) return v.asText();
                    }
                }
                if ("x_response".equalsIgnoreCase(field) || "x_transaction_state".equalsIgnoreCase(field) || "x_response_reason_text".equalsIgnoreCase(field)) {
                    JsonNode v = node.get(field);
                    if (v != null && v.isTextual()) return v.asText();
                }
                String r = findStatusRecursive(node.get(field));
                if (r != null) return r;
            }
        } else if (node.isArray()) {
            for (JsonNode el : node) {
                String r = findStatusRecursive(el);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static String normalizarEstado(String estado) {
        return estado == null ? null : estado.trim().toLowerCase();
    }

    private static boolean esEstadoFinal(String estado) {
        if (estado == null) return false;
        return estado.contains("aprob")
            || estado.contains("acept")
            || estado.contains("accepted")
            || estado.contains("approved")
            || estado.contains("success")
            || estado.contains("exito")
            || estado.contains("successful")
            || estado.contains("complet")
            || estado.contains("completed")
            || estado.contains("paid")
            || estado.contains("authorized")
            || estado.contains("rechaz")
            || estado.contains("reject")
            || estado.contains("denied")
            || estado.contains("declin")
            || estado.contains("cancel")
            || estado.contains("canceled")
            || estado.contains("cancelled")
            || estado.contains("failed");
    }
}
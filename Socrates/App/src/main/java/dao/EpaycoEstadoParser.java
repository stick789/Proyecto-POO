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
            return found != null ? found : response.trim();
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

        // Heurística para V2: si hay un nodo 'transaction' con epaycoRemainingAmount == 0
        // y epaycoAmount > 0, lo consideramos COMPLETADO.
        if (node.has("transaction") && node.get("transaction").isObject()) {
            JsonNode tx = node.get("transaction");
            try {
                if (tx.has("epaycoRemainingAmount") && tx.has("epaycoAmount")) {
                    double remaining = tx.get("epaycoRemainingAmount").asDouble(-1);
                    double amount = tx.get("epaycoAmount").asDouble(-1);
                    if (amount > 0 && remaining == 0) return "COMPLETADO";
                }
            } catch (Exception ignore) {
            }
        }

        if (node.has("payment") && node.get("payment").has("status") && node.get("payment").get("status").isTextual()) {
            return node.get("payment").get("status").asText();
        }
        if (node.has("transaction") && node.get("transaction").has("status") && node.get("transaction").get("status").isTextual()) {
            return node.get("transaction").get("status").asText();
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
package com.example.externalapi.infrastructure.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Set;

public final class MaskUtils {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "authorization",
            "secret",
            "key",
            "privatekey",
            "idcard",
            "phone",
            "mobile",
            "email",
            "bankcard"
    );

    private MaskUtils() {
    }

    public static String maskJson(ObjectMapper objectMapper, String body) {
        if (body == null || body.isBlank()) {
            return "-";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            maskNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return body;
        }
    }

    private static void maskNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(field -> {
                JsonNode child = objectNode.get(field);
                if (SENSITIVE_FIELDS.contains(field.toLowerCase(Locale.ROOT))) {
                    objectNode.put(field, "***");
                } else {
                    maskNode(child);
                }
            });
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(MaskUtils::maskNode);
        }
    }
}

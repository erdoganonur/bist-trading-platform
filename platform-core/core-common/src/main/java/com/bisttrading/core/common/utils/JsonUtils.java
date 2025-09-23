package com.bisttrading.core.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility class for JSON operations using Jackson ObjectMapper.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    /**
     * Creates and configures the ObjectMapper instance.
     *
     * @return Configured ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java Time module for LocalDateTime, ZonedDateTime etc.
        mapper.registerModule(new JavaTimeModule());

        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Configure deserialization
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        return mapper;
    }

    /**
     * Gets the configured ObjectMapper instance.
     *
     * @return ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * Converts an object to JSON string.
     *
     * @param object The object to convert
     * @return JSON string representation
     * @throws RuntimeException if conversion fails
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON: {}", e.getMessage());
            throw new RuntimeException("JSON dönüştürme hatası", e);
        }
    }

    /**
     * Converts an object to pretty-printed JSON string.
     *
     * @param object The object to convert
     * @return Pretty-printed JSON string
     * @throws RuntimeException if conversion fails
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to pretty JSON: {}", e.getMessage());
            throw new RuntimeException("JSON dönüştürme hatası", e);
        }
    }

    /**
     * Converts JSON string to an object of specified type.
     *
     * @param json  The JSON string
     * @param clazz The target class type
     * @param <T>   The type parameter
     * @return Converted object
     * @throws RuntimeException if conversion fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to object: {}", e.getMessage());
            throw new RuntimeException("JSON ayrıştırma hatası", e);
        }
    }

    /**
     * Converts JSON string to an object using TypeReference.
     *
     * @param json          The JSON string
     * @param typeReference The type reference
     * @param <T>           The type parameter
     * @return Converted object
     * @throws RuntimeException if conversion fails
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to object: {}", e.getMessage());
            throw new RuntimeException("JSON ayrıştırma hatası", e);
        }
    }

    /**
     * Safely converts JSON string to an object, returning null if conversion fails.
     *
     * @param json  The JSON string
     * @param clazz The target class type
     * @param <T>   The type parameter
     * @return Converted object or null if conversion fails
     */
    public static <T> T fromJsonSafely(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.warn("Failed to convert JSON safely: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely converts JSON string to an object using TypeReference, returning null if conversion fails.
     *
     * @param json          The JSON string
     * @param typeReference The type reference
     * @param <T>           The type parameter
     * @return Converted object or null if conversion fails
     */
    public static <T> T fromJsonSafely(String json, TypeReference<T> typeReference) {
        try {
            return fromJson(json, typeReference);
        } catch (Exception e) {
            log.warn("Failed to convert JSON safely: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Converts JSON string to a Map.
     *
     * @param json The JSON string
     * @return Map representation
     * @throws RuntimeException if conversion fails
     */
    public static Map<String, Object> toMap(String json) {
        return fromJson(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Safely converts JSON string to a Map, returning null if conversion fails.
     *
     * @param json The JSON string
     * @return Map representation or null if conversion fails
     */
    public static Map<String, Object> toMapSafely(String json) {
        return fromJsonSafely(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Converts JSON string to a List.
     *
     * @param json The JSON string
     * @return List representation
     * @throws RuntimeException if conversion fails
     */
    public static List<Object> toList(String json) {
        return fromJson(json, new TypeReference<List<Object>>() {});
    }

    /**
     * Safely converts JSON string to a List, returning null if conversion fails.
     *
     * @param json The JSON string
     * @return List representation or null if conversion fails
     */
    public static List<Object> toListSafely(String json) {
        return fromJsonSafely(json, new TypeReference<List<Object>>() {});
    }

    /**
     * Converts an object to another object type via JSON serialization/deserialization.
     *
     * @param source The source object
     * @param target The target class type
     * @param <T>    The type parameter
     * @return Converted object
     * @throws RuntimeException if conversion fails
     */
    public static <T> T convert(Object source, Class<T> target) {
        if (source == null) {
            return null;
        }

        try {
            String json = toJson(source);
            return fromJson(json, target);
        } catch (Exception e) {
            log.error("Failed to convert object: {}", e.getMessage());
            throw new RuntimeException("Nesne dönüştürme hatası", e);
        }
    }

    /**
     * Safely converts an object to another object type, returning null if conversion fails.
     *
     * @param source The source object
     * @param target The target class type
     * @param <T>    The type parameter
     * @return Converted object or null if conversion fails
     */
    public static <T> T convertSafely(Object source, Class<T> target) {
        try {
            return convert(source, target);
        } catch (Exception e) {
            log.warn("Failed to convert object safely: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses JSON string to JsonNode.
     *
     * @param json The JSON string
     * @return JsonNode representation
     * @throws RuntimeException if parsing fails
     */
    public static JsonNode parseToNode(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to node: {}", e.getMessage());
            throw new RuntimeException("JSON ayrıştırma hatası", e);
        }
    }

    /**
     * Safely parses JSON string to JsonNode, returning null if parsing fails.
     *
     * @param json The JSON string
     * @return JsonNode representation or null if parsing fails
     */
    public static JsonNode parseToNodeSafely(String json) {
        try {
            return parseToNode(json);
        } catch (Exception e) {
            log.warn("Failed to parse JSON to node safely: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a string is valid JSON.
     *
     * @param json The string to validate
     * @return true if valid JSON
     */
    public static boolean isValidJson(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }

        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Merges two JSON objects.
     *
     * @param mainJson   The main JSON object
     * @param updateJson The update JSON object
     * @return Merged JSON string
     * @throws RuntimeException if merging fails
     */
    public static String mergeJson(String mainJson, String updateJson) {
        try {
            JsonNode mainNode = parseToNode(mainJson);
            JsonNode updateNode = parseToNode(updateJson);

            if (mainNode == null) {
                return updateJson;
            }
            if (updateNode == null) {
                return mainJson;
            }

            JsonNode mergedNode = merge(mainNode, updateNode);
            return OBJECT_MAPPER.writeValueAsString(mergedNode);
        } catch (Exception e) {
            log.error("Failed to merge JSON: {}", e.getMessage());
            throw new RuntimeException("JSON birleştirme hatası", e);
        }
    }

    /**
     * Merges two JsonNodes.
     *
     * @param mainNode   The main node
     * @param updateNode The update node
     * @return Merged JsonNode
     */
    private static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        try {
            return OBJECT_MAPPER.readerForUpdating(mainNode).readValue(updateNode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge JSON nodes", e);
        }
    }

    /**
     * Extracts a value from JSON using JSONPath-like syntax.
     *
     * @param json The JSON string
     * @param path The path to extract (e.g., "user.name", "items[0].id")
     * @return Extracted value as string or null if not found
     */
    public static String extractValue(String json, String path) {
        try {
            JsonNode node = parseToNode(json);
            if (node == null) {
                return null;
            }

            String[] pathParts = path.split("\\.");
            JsonNode current = node;

            for (String part : pathParts) {
                if (part.contains("[") && part.contains("]")) {
                    // Handle array access
                    String arrayName = part.substring(0, part.indexOf("["));
                    String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                    int index = Integer.parseInt(indexStr);

                    current = current.get(arrayName);
                    if (current == null || !current.isArray() || current.size() <= index) {
                        return null;
                    }
                    current = current.get(index);
                } else {
                    current = current.get(part);
                }

                if (current == null) {
                    return null;
                }
            }

            return current.asText();
        } catch (Exception e) {
            log.warn("Failed to extract value from JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates a deep copy of an object using JSON serialization/deserialization.
     *
     * @param object The object to copy
     * @param clazz  The class type
     * @param <T>    The type parameter
     * @return Deep copy of the object
     * @throws RuntimeException if copying fails
     */
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }

        try {
            String json = toJson(object);
            return fromJson(json, clazz);
        } catch (Exception e) {
            log.error("Failed to create deep copy: {}", e.getMessage());
            throw new RuntimeException("Derin kopyalama hatası", e);
        }
    }

    /**
     * Safely creates a deep copy of an object, returning null if copying fails.
     *
     * @param object The object to copy
     * @param clazz  The class type
     * @param <T>    The type parameter
     * @return Deep copy of the object or null if copying fails
     */
    public static <T> T deepCopySafely(T object, Class<T> clazz) {
        try {
            return deepCopy(object, clazz);
        } catch (Exception e) {
            log.warn("Failed to create deep copy safely: {}", e.getMessage());
            return null;
        }
    }
}
package com.bisttrading.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

/**
 * JPA AttributeConverter for PostgreSQL JSONB column type.
 * Converts between Java Map objects and JSON strings for database storage.
 */
@Slf4j
@Converter
public class JsonbConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new ParameterNamesModule());
        objectMapper.findAndRegisterModules();
    }

    /**
     * Converts the entity attribute value to database column representation.
     * Serializes Map to JSON string.
     *
     * @param attribute Entity attribute value (Map)
     * @return Database column value (JSON string)
     */
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("JSON dönüştürme: Map -> String, size: {}", attribute.size());
            return json;
        } catch (JsonProcessingException e) {
            log.error("Map to JSON dönüştürme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert Map to JSON", e);
        }
    }

    /**
     * Converts the database column value to entity attribute representation.
     * Deserializes JSON string to Map.
     *
     * @param dbData Database column value (JSON string)
     * @return Entity attribute value (Map)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> map = objectMapper.readValue(dbData, typeRef);
            log.debug("JSON dönüştürme: String -> Map, size: {}", map != null ? map.size() : 0);
            return map;
        } catch (JsonProcessingException e) {
            log.error("JSON to Map dönüştürme hatası: {}", e.getMessage(), e);
            // Return null instead of throwing exception to handle corrupt data gracefully
            return null;
        }
    }

    /**
     * Gets the configured ObjectMapper instance.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Validates if a string is valid JSON.
     *
     * @param jsonString String to validate
     * @return true if valid JSON
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Converts any object to JSON string.
     *
     * @param object Object to convert
     * @return JSON string
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Object to JSON dönüştürme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }

    /**
     * Converts JSON string to object of specified type.
     *
     * @param json JSON string
     * @param valueType Target class type
     * @param <T> Type parameter
     * @return Converted object
     */
    public static <T> T fromJson(String json, Class<T> valueType) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            log.error("JSON to Object dönüştürme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }

    /**
     * Converts JSON string to object using TypeReference.
     *
     * @param json JSON string
     * @param typeReference Type reference for complex types
     * @param <T> Type parameter
     * @return Converted object
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON to Object dönüştürme hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }
}
package com.bisttrading.broker.algolab.util;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.exception.AlgoLabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SHA-256 checker hash utility for AlgoLab API request integrity.
 *
 * Formula: SHA256(APIKEY + HOSTNAME + ENDPOINT + JSON_BODY_NO_WHITESPACE)
 */
@Component
@Slf4j
public class AlgoLabCheckerUtil {

    private final String apiKey;
    private final String apiHostname;
    private final ObjectMapper objectMapper;

    public AlgoLabCheckerUtil(AlgoLabProperties properties, ObjectMapper objectMapper) {
        this.apiKey = properties.getApi().getKey();
        this.apiHostname = properties.getApi().getHostname();
        this.objectMapper = objectMapper;

        log.debug("AlgoLab checker utility initialized with hostname: {}", apiHostname);
    }

    /**
     * Creates SHA-256 checker hash for API request integrity.
     *
     * @param endpoint API endpoint path (e.g., "/api/GetEquityInfo")
     * @param payload Request payload (Map, DTO, or any object)
     * @return SHA-256 hex string (64 characters)
     * @throws AlgoLabException if checker creation fails
     */
    public String makeChecker(String endpoint, Object payload) {
        try {
            String body = "";

            if (payload != null && !isEmpty(payload)) {
                // Serialize to JSON and remove all whitespace
                String json = objectMapper.writeValueAsString(payload);
                body = json.replaceAll("\\s+", "");
            }

            // Concatenate: APIKEY + HOSTNAME + ENDPOINT + BODY
            String data = apiKey + apiHostname + endpoint + body;

            // SHA-256 hash
            String checker = DigestUtils.sha256Hex(data);

            log.trace("Checker created for endpoint {} with body length: {}", endpoint, body.length());

            return checker;

        } catch (Exception e) {
            log.error("Failed to create checker hash for endpoint: {}", endpoint, e);
            throw new AlgoLabException("Failed to create checker hash", e);
        }
    }

    /**
     * Checks if payload is empty.
     */
    private boolean isEmpty(Object payload) {
        if (payload instanceof Map) {
            return ((Map<?, ?>) payload).isEmpty();
        }
        return false;
    }
}

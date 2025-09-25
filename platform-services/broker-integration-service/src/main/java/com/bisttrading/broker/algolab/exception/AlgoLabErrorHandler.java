package com.bisttrading.broker.algolab.exception;

import com.bisttrading.broker.algolab.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

@Slf4j
public class AlgoLabErrorHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AlgoLabException handleException(Exception e) {
        if (e instanceof HttpClientErrorException clientError) {
            return handleClientError(clientError);
        } else if (e instanceof HttpServerErrorException serverError) {
            return handleServerError(serverError);
        } else if (e instanceof RestClientException restError) {
            return handleRestClientError(restError);
        } else {
            return new AlgoLabException("Unexpected error: " + e.getMessage(), e);
        }
    }

    private static AlgoLabException handleClientError(HttpClientErrorException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        ErrorResponse errorResponse = parseErrorResponse(e.getResponseBodyAsString());

        String message = errorResponse != null && errorResponse.getMessage() != null
            ? errorResponse.getMessage()
            : "Client error: " + e.getMessage();

        String errorCode = errorResponse != null && errorResponse.getErrorCode() != null
            ? errorResponse.getErrorCode()
            : "CLIENT_ERROR";

        return switch (status) {
            case BAD_REQUEST -> {
                if (errorResponse != null && errorResponse.hasDetails()) {
                    yield new AlgoLabValidationException(message, errorResponse.getDetails());
                }
                yield new AlgoLabValidationException(message);
            }
            case UNAUTHORIZED -> new AlgoLabAuthenticationException(message);
            case FORBIDDEN -> new AlgoLabAuthenticationException("Access forbidden: " + message);
            case NOT_FOUND -> new AlgoLabException(message, errorCode, status.value());
            case UNPROCESSABLE_ENTITY -> new AlgoLabOrderException(message);
            case TOO_MANY_REQUESTS -> new AlgoLabException("Rate limit exceeded: " + message, "RATE_LIMIT", status.value());
            default -> new AlgoLabException(message, errorCode, status.value());
        };
    }

    private static AlgoLabException handleServerError(HttpServerErrorException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        ErrorResponse errorResponse = parseErrorResponse(e.getResponseBodyAsString());

        String message = errorResponse != null && errorResponse.getMessage() != null
            ? errorResponse.getMessage()
            : "Server error: " + e.getMessage();

        String errorCode = errorResponse != null && errorResponse.getErrorCode() != null
            ? errorResponse.getErrorCode()
            : "SERVER_ERROR";

        return switch (status) {
            case INTERNAL_SERVER_ERROR -> new AlgoLabException("Internal server error: " + message, errorCode, status.value());
            case BAD_GATEWAY -> new AlgoLabException("Bad gateway: " + message, errorCode, status.value());
            case SERVICE_UNAVAILABLE -> new AlgoLabMarketDataException("Service unavailable: " + message);
            case GATEWAY_TIMEOUT -> new AlgoLabException("Gateway timeout: " + message, errorCode, status.value());
            default -> new AlgoLabException(message, errorCode, status.value());
        };
    }

    private static AlgoLabException handleRestClientError(RestClientException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("timeout")) {
            return new AlgoLabException("Request timeout: " + message, "TIMEOUT_ERROR", 408, e);
        } else if (message != null && message.toLowerCase().contains("connection")) {
            return new AlgoLabException("Connection error: " + message, "CONNECTION_ERROR", 0, e);
        } else {
            return new AlgoLabException("Network error: " + message, "NETWORK_ERROR", 0, e);
        }
    }

    private static ErrorResponse parseErrorResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, ErrorResponse.class);
        } catch (Exception e) {
            log.debug("Failed to parse error response: {}", responseBody, e);
            return null;
        }
    }

    public static void validateOrderRequest(com.bisttrading.broker.algolab.model.OrderRequest request) {
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            throw new AlgoLabValidationException("Order validation failed: " + e.getMessage(), e);
        }
    }

    public static void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new AlgoLabValidationException("Symbol cannot be null or empty");
        }

        if (!symbol.matches("^[A-Z0-9.]+$")) {
            throw new AlgoLabValidationException("Invalid symbol format: " + symbol);
        }
    }

    public static void validateAuthentication(boolean authenticated) {
        if (!authenticated) {
            throw new AlgoLabAuthenticationException("Not authenticated. Please call authenticate() first.");
        }
    }
}
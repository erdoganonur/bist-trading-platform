package com.bisttrading.oms.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for Order Management Service.
 *
 * Provides consistent error responses across all endpoints with:
 * - Proper HTTP status codes
 * - Detailed error information
 * - Field validation errors
 * - Security-aware error messages
 * - Request correlation for tracing
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =====================================
    // BUSINESS EXCEPTIONS
    // =====================================

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, WebRequest request) {
        log.warn("Order not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Order Not Found")
            .message(ex.getMessage())
            .path(getPath(request))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(OrderManagementException.class)
    public ResponseEntity<ErrorResponse> handleOrderManagementException(
            OrderManagementException ex, WebRequest request) {
        log.error("Order management error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Order Management Error")
            .message(ex.getMessage())
            .path(getPath(request))
            .details(extractExceptionDetails(ex))
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ErrorResponse> handleOrderValidationException(
            OrderValidationException ex, WebRequest request) {
        log.warn("Order validation failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error("Order Validation Failed")
            .message(ex.getMessage())
            .path(getPath(request))
            .validationErrors(ex.getValidationErrors())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // =====================================
    // VALIDATION EXCEPTIONS
    // =====================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed for request: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .path(getPath(request))
            .validationErrors(fieldErrors)
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> validationErrors = ex.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> getPropertyPath(violation),
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Constraint Violation")
            .message("Request parameter validation failed")
            .path(getPath(request))
            .validationErrors(validationErrors)
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch for parameter '{}': expected {} but got '{}'",
                ex.getName(), ex.getRequiredType(), ex.getValue());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Type Mismatch")
            .message(message)
            .path(getPath(request))
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Malformed Request")
            .message("Request body is malformed or missing required fields")
            .path(getPath(request))
            .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    // =====================================
    // SECURITY EXCEPTIONS
    // =====================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to access this resource")
            .path(getPath(request))
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // =====================================
    // ASYNC EXCEPTIONS
    // =====================================

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<ErrorResponse> handleCompletionException(
            CompletionException ex, WebRequest request) {
        log.error("Async operation failed: {}", ex.getMessage(), ex);

        Throwable cause = ex.getCause();
        if (cause instanceof OrderManagementException) {
            return handleOrderManagementException((OrderManagementException) cause, request);
        } else if (cause instanceof OrderNotFoundException) {
            return handleOrderNotFoundException((OrderNotFoundException) cause, request);
        } else if (cause instanceof OrderValidationException) {
            return handleOrderValidationException((OrderValidationException) cause, request);
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred while processing your request")
            .path(getPath(request))
            .build();

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    // =====================================
    // GENERIC EXCEPTIONS
    // =====================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please try again later.")
            .path(getPath(request))
            .build();

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    // =====================================
    // UTILITY METHODS
    // =====================================

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String getPropertyPath(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        return propertyPath.isEmpty() ? "request" : propertyPath;
    }

    private Map<String, Object> extractExceptionDetails(Throwable ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("exceptionType", ex.getClass().getSimpleName());

        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage());
        }

        return details;
    }

    // =====================================
    // ERROR RESPONSE DTO
    // =====================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> validationErrors;
        private Map<String, Object> details;
        private String traceId;

        public boolean hasValidationErrors() {
            return validationErrors != null && !validationErrors.isEmpty();
        }

        public boolean hasDetails() {
            return details != null && !details.isEmpty();
        }
    }
}
package com.bisttrading.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for user management service.
 * Handles all exceptions and converts them to appropriate HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message("Geçersiz istek verisi")
            .details(fieldErrors)
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("Validation error for request {}: {}", extractPath(request), fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles user service specific business logic errors.
     */
    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceException(
            UserServiceException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("USER_SERVICE_ERROR")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("User service error for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles authentication failures.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("INVALID_CREDENTIALS")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("Authentication failure for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles user not found errors.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UsernameNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("USER_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("User not found for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("INVALID_ARGUMENT")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("Invalid argument for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles database constraint violations and similar data access errors.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {

        String message = "Veri bütünlüğü hatası";

        // Check for common constraint violations
        String errorMessage = ex.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("email")) {
                message = "Bu e-posta adresi zaten kullanımda";
            } else if (errorMessage.contains("username")) {
                message = "Bu kullanıcı adı zaten kullanımda";
            } else if (errorMessage.contains("tc_kimlik")) {
                message = "Bu TC Kimlik No zaten kayıtlı";
            } else if (errorMessage.contains("phone_number")) {
                message = "Bu telefon numarası zaten kullanımda";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("DATA_INTEGRITY_VIOLATION")
            .message(message)
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("Data integrity violation for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("ACCESS_DENIED")
            .message("Bu işlem için yetkiniz bulunmuyor")
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.warn("Access denied for request {}: {}", extractPath(request), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        ErrorResponse errorResponse = ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("Beklenmeyen bir hata oluştu")
            .timestamp(Instant.now())
            .path(extractPath(request))
            .build();

        log.error("Unexpected error for request {}: {}", extractPath(request), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extracts the request path from WebRequest.
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
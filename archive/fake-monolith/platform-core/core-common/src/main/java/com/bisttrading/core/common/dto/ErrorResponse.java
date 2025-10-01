package com.bisttrading.core.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response structure for API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * HTTP error reason phrase.
     */
    private String error;

    /**
     * Application-specific error code.
     */
    private String code;

    /**
     * Human-readable error message in Turkish.
     */
    private String message;

    /**
     * API path where the error occurred.
     */
    private String path;

    /**
     * Detailed validation errors (if applicable).
     */
    private List<ValidationError> validationErrors;

    /**
     * Additional error details for debugging.
     */
    private Object details;

    /**
     * Request ID for tracing.
     */
    private String requestId;

    /**
     * Stack trace (only in development).
     */
    private String stackTrace;

    /**
     * Creates a simple error response.
     *
     * @param status  HTTP status code
     * @param error   HTTP error reason
     * @param message Error message
     * @param path    Request path
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates an error response with error code.
     *
     * @param status  HTTP status code
     * @param error   HTTP error reason
     * @param code    Application error code
     * @param message Error message
     * @param path    Request path
     * @return ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String code, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .code(code)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Creates a validation error response.
     *
     * @param status           HTTP status code
     * @param error            HTTP error reason
     * @param code             Application error code
     * @param message          Error message
     * @param path             Request path
     * @param validationErrors List of validation errors
     * @return ErrorResponse
     */
    public static ErrorResponse validation(int status, String error, String code, String message,
                                         String path, List<ValidationError> validationErrors) {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status);
        response.setError(error);
        response.setCode(code);
        response.setMessage(message);
        response.setPath(path);
        response.setValidationErrors(validationErrors);
        return response;
    }

    /**
     * Adds validation errors to the response.
     *
     * @param validationErrors List of validation errors
     * @return This ErrorResponse for method chaining
     */
    public ErrorResponse withValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    /**
     * Adds additional details to the response.
     *
     * @param details Additional error details
     * @return This ErrorResponse for method chaining
     */
    public ErrorResponse withDetails(Object details) {
        this.details = details;
        return this;
    }

    /**
     * Adds request ID to the response.
     *
     * @param requestId Request ID for tracing
     * @return This ErrorResponse for method chaining
     */
    public ErrorResponse withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Adds stack trace to the response (for development only).
     *
     * @param stackTrace Stack trace string
     * @return This ErrorResponse for method chaining
     */
    public ErrorResponse withStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    /**
     * Checks if the response has validation errors.
     *
     * @return true if validation errors exist
     */
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    /**
     * Checks if the response has additional details.
     *
     * @return true if details exist
     */
    public boolean hasDetails() {
        return details != null;
    }

    /**
     * Gets the number of validation errors.
     *
     * @return Number of validation errors
     */
    public int getValidationErrorCount() {
        return validationErrors != null ? validationErrors.size() : 0;
    }

    /**
     * Represents a single field validation error.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {

        /**
         * The field name that failed validation.
         */
        private String field;

        /**
         * The validation error message in Turkish.
         */
        private String message;

        /**
         * The rejected value that caused the validation error.
         */
        private Object rejectedValue;

        /**
         * The validation rule that was violated.
         */
        private String rule;

        /**
         * Additional context about the validation error.
         */
        private Object context;

        /**
         * Constructor for commonly used validation error with field, message, and rejected value.
         */
        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        /**
         * Creates a simple validation error.
         *
         * @param field   Field name
         * @param message Error message
         * @return ValidationError
         */
        public static ValidationError of(String field, String message) {
            ValidationError error = new ValidationError();
            error.setField(field);
            error.setMessage(message);
            return error;
        }

        /**
         * Creates a validation error with rejected value.
         *
         * @param field         Field name
         * @param message       Error message
         * @param rejectedValue The value that was rejected
         * @return ValidationError
         */
        public static ValidationError of(String field, String message, Object rejectedValue) {
            ValidationError error = new ValidationError();
            error.setField(field);
            error.setMessage(message);
            error.setRejectedValue(rejectedValue);
            return error;
        }

        /**
         * Creates a validation error with rule and rejected value.
         *
         * @param field         Field name
         * @param message       Error message
         * @param rejectedValue The value that was rejected
         * @param rule          The validation rule
         * @return ValidationError
         */
        public static ValidationError of(String field, String message, Object rejectedValue, String rule) {
            ValidationError error = new ValidationError();
            error.setField(field);
            error.setMessage(message);
            error.setRejectedValue(rejectedValue);
            error.setRule(rule);
            return error;
        }

        /**
         * Adds context to the validation error.
         *
         * @param context Additional context
         * @return This ValidationError for method chaining
         */
        public ValidationError withContext(Object context) {
            this.context = context;
            return this;
        }
    }
}
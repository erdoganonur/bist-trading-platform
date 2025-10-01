package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;
import com.bisttrading.core.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Global exception handler for the BIST Trading Platform.
 * Handles all exceptions and converts them to appropriate HTTP responses with Turkish error messages.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles BaseException and its subclasses.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        log.error("Base exception occurred: {}", ex.getMessage(), ex);

        HttpStatus status = determineHttpStatus(ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(ex.getErrorCodeString())
                .message(ex.getUserMessage())
                .path(request.getRequestURI())
                .build();

        // Add validation errors if this is a ValidationException
        if (ex instanceof ValidationException validationEx) {
            List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
            for (ValidationException.ValidationError error : validationEx.getValidationErrors()) {
                validationErrors.add(new ErrorResponse.ValidationError(
                        error.getField(),
                        error.getMessage(),
                        error.getRejectedValue()
                ));
            }
            errorResponse.setValidationErrors(validationErrors);
        }

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.error("Authentication exception occurred: {}", ex.getMessage(), ex);

        ErrorCodes errorCode = ErrorCodes.UNAUTHORIZED;
        if (ex instanceof BadCredentialsException) {
            errorCode = ErrorCodes.INVALID_CREDENTIALS;
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles Spring Security access denied exceptions.
     */
    @ExceptionHandler({org.springframework.security.access.AccessDeniedException.class, java.nio.file.AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception ex, HttpServletRequest request) {
        log.error("Access denied exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code(ErrorCodes.FORBIDDEN.getCode())
                .message(ErrorCodes.FORBIDDEN.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation exception occurred: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ErrorResponse.ValidationError(
                    error.getField(),
                    error.getDefaultMessage(),
                    error.getRejectedValue()
            ));
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.VALIDATION_ERROR.getCode())
                .message(ErrorCodes.VALIDATION_ERROR.getMessage())
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles binding exceptions.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        log.error("Bind exception occurred: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ErrorResponse.ValidationError(
                    error.getField(),
                    error.getDefaultMessage(),
                    error.getRejectedValue()
            ));
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.VALIDATION_ERROR.getCode())
                .message(ErrorCodes.VALIDATION_ERROR.getMessage())
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.error("Constraint violation exception occurred: {}", ex.getMessage());

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            validationErrors.add(new ErrorResponse.ValidationError(
                    fieldName,
                    violation.getMessage(),
                    violation.getInvalidValue()
            ));
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.VALIDATION_ERROR.getCode())
                .message(ErrorCodes.VALIDATION_ERROR.getMessage())
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles missing request parameter exceptions.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.error("Missing request parameter: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST.getCode())
                .message(String.format("Zorunlu parametre eksik: %s", ex.getParameterName()))
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles method argument type mismatch exceptions.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.error("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Parametre tipi hatalı: %s", ex.getName());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles HTTP message not readable exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.error("HTTP message not readable: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST.getCode())
                .message("Geçersiz JSON formatı")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles HTTP request method not supported exceptions.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.error("HTTP method not supported: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .code(ErrorCodes.METHOD_NOT_ALLOWED.getCode())
                .message(ErrorCodes.METHOD_NOT_ALLOWED.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handles HTTP media type not supported exceptions.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.error("HTTP media type not supported: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .code(ErrorCodes.BAD_REQUEST.getCode())
                .message("Desteklenmeyen medya tipi")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handles no handler found exceptions.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("No handler found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .code(ErrorCodes.NOT_FOUND.getCode())
                .message(ErrorCodes.NOT_FOUND.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles all other unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code(ErrorCodes.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCodes.INTERNAL_SERVER_ERROR.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Determines the appropriate HTTP status code based on the exception type.
     */
    private HttpStatus determineHttpStatus(BaseException ex) {
        return switch (ex) {
            case ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case UnauthorizedException ignored -> {
                ErrorCodes errorCode = ex.getErrorCode();
                if (errorCode == ErrorCodes.FORBIDDEN || errorCode == ErrorCodes.PORTFOLIO_ACCESS_DENIED) {
                    yield HttpStatus.FORBIDDEN;
                }
                yield HttpStatus.UNAUTHORIZED;
            }
            case BusinessException ignored -> {
                // Most business exceptions are bad requests, but some might be conflicts
                ErrorCodes errorCode = ex.getErrorCode();
                if (errorCode == ErrorCodes.USER_ALREADY_EXISTS ||
                    errorCode == ErrorCodes.EMAIL_ALREADY_EXISTS ||
                    errorCode == ErrorCodes.CONFLICT) {
                    yield HttpStatus.CONFLICT;
                }
                yield HttpStatus.BAD_REQUEST;
            }
            case TechnicalException ignored -> {
                ErrorCodes errorCode = ex.getErrorCode();
                if (errorCode == ErrorCodes.SERVICE_UNAVAILABLE) {
                    yield HttpStatus.SERVICE_UNAVAILABLE;
                }
                if (errorCode == ErrorCodes.TOO_MANY_REQUESTS) {
                    yield HttpStatus.TOO_MANY_REQUESTS;
                }
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
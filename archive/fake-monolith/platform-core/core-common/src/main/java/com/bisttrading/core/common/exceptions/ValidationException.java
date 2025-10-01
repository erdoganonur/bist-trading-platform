package com.bisttrading.core.common.exceptions;

import com.bisttrading.core.common.constants.ErrorCodes;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when validation errors occur.
 * Can contain multiple validation errors for different fields.
 */
@Getter
public class ValidationException extends BaseException {

    private final List<ValidationError> validationErrors;

    /**
     * Creates a validation exception with error code.
     *
     * @param errorCode The error code
     */
    public ValidationException(ErrorCodes errorCode) {
        super(errorCode);
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Creates a validation exception with error code and custom message.
     *
     * @param errorCode   The error code
     * @param userMessage Custom user-friendly message
     */
    public ValidationException(ErrorCodes errorCode, String userMessage) {
        super(errorCode, userMessage);
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Creates a validation exception with error code and validation errors.
     *
     * @param errorCode        The error code
     * @param validationErrors List of validation errors
     */
    public ValidationException(ErrorCodes errorCode, List<ValidationError> validationErrors) {
        super(errorCode);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    /**
     * Creates a validation exception with error code, custom message and validation errors.
     *
     * @param errorCode        The error code
     * @param userMessage      Custom user-friendly message
     * @param validationErrors List of validation errors
     */
    public ValidationException(ErrorCodes errorCode, String userMessage, List<ValidationError> validationErrors) {
        super(errorCode, userMessage);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    /**
     * Creates a validation exception with single field error.
     *
     * @param errorCode The error code
     * @param field     The field name
     * @param message   The validation message
     */
    public ValidationException(ErrorCodes errorCode, String field, String message) {
        super(errorCode);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(new ValidationError(field, message));
    }

    /**
     * Adds a validation error to the exception.
     *
     * @param field   The field name
     * @param message The validation message
     * @return This ValidationException for method chaining
     */
    public ValidationException addError(String field, String message) {
        this.validationErrors.add(new ValidationError(field, message));
        return this;
    }

    /**
     * Adds a validation error to the exception.
     *
     * @param validationError The validation error
     * @return This ValidationException for method chaining
     */
    public ValidationException addError(ValidationError validationError) {
        this.validationErrors.add(validationError);
        return this;
    }

    /**
     * Returns an unmodifiable list of validation errors.
     *
     * @return List of validation errors
     */
    public List<ValidationError> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    /**
     * Checks if there are any validation errors.
     *
     * @return true if has validation errors
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * Returns the number of validation errors.
     *
     * @return Number of validation errors
     */
    public int getErrorCount() {
        return validationErrors.size();
    }

    /**
     * Represents a single validation error for a specific field.
     */
    @Getter
    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public ValidationError(String field, String message) {
            this(field, message, null);
        }

        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        @Override
        public String toString() {
            return String.format("ValidationError[field=%s, message=%s, rejectedValue=%s]",
                    field, message, rejectedValue);
        }
    }

    // Convenience factory methods for common validation exceptions

    /**
     * Creates a validation exception for required field.
     *
     * @param field The field name
     * @return ValidationException
     */
    public static ValidationException requiredField(String field) {
        return new ValidationException(ErrorCodes.FIELD_REQUIRED, field,
                String.format("%s alanı zorunludur", field));
    }

    /**
     * Creates a validation exception for field too long.
     *
     * @param field     The field name
     * @param maxLength Maximum allowed length
     * @return ValidationException
     */
    public static ValidationException fieldTooLong(String field, int maxLength) {
        return new ValidationException(ErrorCodes.FIELD_TOO_LONG, field,
                String.format("%s alanı en fazla %d karakter olabilir", field, maxLength));
    }

    /**
     * Creates a validation exception for field too short.
     *
     * @param field     The field name
     * @param minLength Minimum required length
     * @return ValidationException
     */
    public static ValidationException fieldTooShort(String field, int minLength) {
        return new ValidationException(ErrorCodes.FIELD_TOO_SHORT, field,
                String.format("%s alanı en az %d karakter olmalıdır", field, minLength));
    }

    /**
     * Creates a validation exception for invalid email format.
     *
     * @param email The invalid email
     * @return ValidationException
     */
    public static ValidationException invalidEmailFormat(String email) {
        return new ValidationException(ErrorCodes.INVALID_EMAIL_FORMAT, "email",
                "Geçersiz e-posta formatı");
    }

    /**
     * Creates a validation exception for invalid TC Kimlik.
     *
     * @param tcKimlik The invalid TC Kimlik
     * @return ValidationException
     */
    public static ValidationException invalidTCKimlik(String tcKimlik) {
        return new ValidationException(ErrorCodes.INVALID_TC_KIMLIK, "tcKimlik",
                "Geçersiz TC Kimlik numarası");
    }

    /**
     * Creates a validation exception for invalid IBAN.
     *
     * @param iban The invalid IBAN
     * @return ValidationException
     */
    public static ValidationException invalidIBAN(String iban) {
        return new ValidationException(ErrorCodes.INVALID_IBAN, "iban",
                "Geçersiz IBAN numarası");
    }

    /**
     * Creates a validation exception for invalid phone number.
     *
     * @param phoneNumber The invalid phone number
     * @return ValidationException
     */
    public static ValidationException invalidPhoneNumber(String phoneNumber) {
        return new ValidationException(ErrorCodes.INVALID_PHONE_NUMBER, "phoneNumber",
                "Geçersiz telefon numarası");
    }

    /**
     * Creates a validation exception for invalid date format.
     *
     * @param field The field name
     * @param value The invalid date value
     * @return ValidationException
     */
    public static ValidationException invalidDateFormat(String field, String value) {
        return new ValidationException(ErrorCodes.INVALID_DATE_FORMAT, field,
                String.format("Geçersiz tarih formatı: %s", value));
    }

    /**
     * Creates a validation exception for invalid number format.
     *
     * @param field The field name
     * @param value The invalid number value
     * @return ValidationException
     */
    public static ValidationException invalidNumberFormat(String field, String value) {
        return new ValidationException(ErrorCodes.INVALID_NUMBER_FORMAT, field,
                String.format("Geçersiz sayı formatı: %s", value));
    }

    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, message=%s, errorCount=%d]",
                getClass().getSimpleName(),
                getErrorCode().getCode(),
                getUserMessage(),
                getErrorCount());
    }
}
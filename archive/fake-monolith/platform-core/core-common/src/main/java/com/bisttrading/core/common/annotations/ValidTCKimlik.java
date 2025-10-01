package com.bisttrading.core.common.annotations;

import com.bisttrading.core.common.utils.ValidationUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for Turkish TC Kimlik numbers.
 * Validates that the string is a valid TC Kimlik number according to official algorithm.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTCKimlik.TCKimlikValidator.class)
@Documented
public @interface ValidTCKimlik {

    /**
     * Default validation error message.
     */
    String message() default "Geçersiz TC Kimlik numarası";

    /**
     * Validation groups.
     */
    Class<?>[] groups() default {};

    /**
     * Payload for additional metadata.
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow null values.
     */
    boolean allowNull() default true;

    /**
     * Whether to allow empty strings.
     */
    boolean allowEmpty() default false;

    /**
     * Whether to normalize the input (remove spaces, dashes).
     */
    boolean normalize() default true;

    /**
     * Custom error message for null values.
     */
    String nullMessage() default "TC Kimlik numarası boş olamaz";

    /**
     * Custom error message for empty values.
     */
    String emptyMessage() default "TC Kimlik numarası boş olamaz";

    /**
     * Custom error message for invalid format.
     */
    String formatMessage() default "TC Kimlik numarası 11 haneli olmalıdır";

    /**
     * Custom error message for invalid algorithm.
     */
    String algorithmMessage() default "TC Kimlik numarası geçersiz";

    /**
     * Validator implementation for ValidTCKimlik annotation.
     */
    class TCKimlikValidator implements ConstraintValidator<ValidTCKimlik, String> {

        private boolean allowNull;
        private boolean allowEmpty;
        private boolean normalize;
        private String nullMessage;
        private String emptyMessage;
        private String formatMessage;
        private String algorithmMessage;

        @Override
        public void initialize(ValidTCKimlik annotation) {
            this.allowNull = annotation.allowNull();
            this.allowEmpty = annotation.allowEmpty();
            this.normalize = annotation.normalize();
            this.nullMessage = annotation.nullMessage();
            this.emptyMessage = annotation.emptyMessage();
            this.formatMessage = annotation.formatMessage();
            this.algorithmMessage = annotation.algorithmMessage();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Handle null values
            if (value == null) {
                if (!allowNull) {
                    addConstraintViolation(context, nullMessage);
                    return false;
                }
                return true;
            }

            // Normalize input if requested
            String processedValue = normalize ? normalizeInput(value) : value;

            // Handle empty values
            if (processedValue.isEmpty()) {
                if (!allowEmpty) {
                    addConstraintViolation(context, emptyMessage);
                    return false;
                }
                return true;
            }

            // Validate format first
            if (!isValidFormat(processedValue)) {
                addConstraintViolation(context, formatMessage);
                return false;
            }

            // Validate using official algorithm
            if (!ValidationUtils.isValidTCKimlik(processedValue)) {
                addConstraintViolation(context, algorithmMessage);
                return false;
            }

            return true;
        }

        /**
         * Normalizes the input by removing spaces, dashes, and other non-digit characters.
         */
        private String normalizeInput(String value) {
            if (value == null) {
                return null;
            }
            return value.replaceAll("[^0-9]", "");
        }

        /**
         * Checks if the format is valid (11 digits, not starting with 0).
         */
        private boolean isValidFormat(String value) {
            // Must be exactly 11 digits
            if (!value.matches("^\\d{11}$")) {
                return false;
            }

            // Cannot start with 0
            return !value.startsWith("0");
        }

        /**
         * Adds a custom constraint violation message.
         */
        private void addConstraintViolation(ConstraintValidatorContext context, String message) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
        }
    }

    /**
     * List constraint for validating arrays or collections.
     */
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        ValidTCKimlik[] value();
    }
}
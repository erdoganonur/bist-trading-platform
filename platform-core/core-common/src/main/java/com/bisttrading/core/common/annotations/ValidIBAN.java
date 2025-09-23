package com.bisttrading.core.common.annotations;

import com.bisttrading.core.common.utils.ValidationUtils;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for IBAN numbers.
 * Supports both Turkish and international IBAN validation.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidIBAN.IBANValidator.class)
@Documented
public @interface ValidIBAN {

    /**
     * Default validation error message.
     */
    String message() default "Geçersiz IBAN numarası";

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
     * Whether to validate only Turkish IBANs.
     */
    boolean turkishOnly() default false;

    /**
     * Whether to normalize the input (remove spaces).
     */
    boolean normalize() default true;

    /**
     * Custom error message for null values.
     */
    String nullMessage() default "IBAN numarası boş olamaz";

    /**
     * Custom error message for empty values.
     */
    String emptyMessage() default "IBAN numarası boş olamaz";

    /**
     * Custom error message for invalid format.
     */
    String formatMessage() default "IBAN numarası geçersiz formatda";

    /**
     * Custom error message for invalid checksum.
     */
    String checksumMessage() default "IBAN numarası geçersiz";

    /**
     * Custom error message for non-Turkish IBAN when turkishOnly is true.
     */
    String nonTurkishMessage() default "Sadece Türk bankalarının IBAN numaraları kabul edilir";

    /**
     * Validator implementation for ValidIBAN annotation.
     */
    class IBANValidator implements ConstraintValidator<ValidIBAN, String> {

        private boolean allowNull;
        private boolean allowEmpty;
        private boolean turkishOnly;
        private boolean normalize;
        private String nullMessage;
        private String emptyMessage;
        private String formatMessage;
        private String checksumMessage;
        private String nonTurkishMessage;

        @Override
        public void initialize(ValidIBAN annotation) {
            this.allowNull = annotation.allowNull();
            this.allowEmpty = annotation.allowEmpty();
            this.turkishOnly = annotation.turkishOnly();
            this.normalize = annotation.normalize();
            this.nullMessage = annotation.nullMessage();
            this.emptyMessage = annotation.emptyMessage();
            this.formatMessage = annotation.formatMessage();
            this.checksumMessage = annotation.checksumMessage();
            this.nonTurkishMessage = annotation.nonTurkishMessage();
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

            // Check if Turkish only validation is required
            if (turkishOnly) {
                if (!isTurkishIBAN(processedValue)) {
                    addConstraintViolation(context, nonTurkishMessage);
                    return false;
                }

                // Validate Turkish IBAN
                if (!ValidationUtils.isValidTurkishIBAN(processedValue)) {
                    addConstraintViolation(context, checksumMessage);
                    return false;
                }
            } else {
                // Validate any IBAN
                if (!ValidationUtils.isValidIBAN(processedValue)) {
                    // Determine if it's a format or checksum issue
                    if (!isValidIBANFormat(processedValue)) {
                        addConstraintViolation(context, formatMessage);
                    } else {
                        addConstraintViolation(context, checksumMessage);
                    }
                    return false;
                }
            }

            return true;
        }

        /**
         * Normalizes the input by removing spaces and converting to uppercase.
         */
        private String normalizeInput(String value) {
            if (value == null) {
                return null;
            }
            return value.replaceAll("\\s", "").toUpperCase();
        }

        /**
         * Checks if the IBAN is Turkish (starts with TR).
         */
        private boolean isTurkishIBAN(String iban) {
            return iban != null && iban.toUpperCase().startsWith("TR");
        }

        /**
         * Checks basic IBAN format without checksum validation.
         */
        private boolean isValidIBANFormat(String iban) {
            if (iban == null || iban.length() < 15 || iban.length() > 34) {
                return false;
            }

            // First two characters must be letters (country code)
            if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))) {
                return false;
            }

            // Next two characters must be digits (check digits)
            if (!Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3))) {
                return false;
            }

            // Rest must be alphanumeric
            for (int i = 4; i < iban.length(); i++) {
                char c = iban.charAt(i);
                if (!Character.isLetterOrDigit(c)) {
                    return false;
                }
            }

            return true;
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
        ValidIBAN[] value();
    }
}
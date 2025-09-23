package com.bisttrading.core.common.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Locale;

/**
 * Validation annotation that ensures a string is properly formatted in Turkish uppercase.
 * This annotation validates that the string contains only uppercase letters using Turkish locale rules.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TurkishUpperCase.TurkishUpperCaseValidator.class)
@Documented
public @interface TurkishUpperCase {

    /**
     * Default validation error message.
     */
    String message() default "Alan Türkçe büyük harf formatında olmalıdır";

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
    boolean allowEmpty() default true;

    /**
     * Whether to allow numeric characters.
     */
    boolean allowNumbers() default false;

    /**
     * Whether to allow spaces.
     */
    boolean allowSpaces() default true;

    /**
     * Whether to allow special characters.
     */
    boolean allowSpecialChars() default false;

    /**
     * Custom allowed characters pattern.
     */
    String allowedCharsPattern() default "";

    /**
     * Validator implementation for TurkishUpperCase annotation.
     */
    class TurkishUpperCaseValidator implements ConstraintValidator<TurkishUpperCase, String> {

        private static final Locale TURKISH_LOCALE = new Locale("tr", "TR");

        private boolean allowNull;
        private boolean allowEmpty;
        private boolean allowNumbers;
        private boolean allowSpaces;
        private boolean allowSpecialChars;
        private String allowedCharsPattern;

        @Override
        public void initialize(TurkishUpperCase annotation) {
            this.allowNull = annotation.allowNull();
            this.allowEmpty = annotation.allowEmpty();
            this.allowNumbers = annotation.allowNumbers();
            this.allowSpaces = annotation.allowSpaces();
            this.allowSpecialChars = annotation.allowSpecialChars();
            this.allowedCharsPattern = annotation.allowedCharsPattern();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Handle null values
            if (value == null) {
                return allowNull;
            }

            // Handle empty values
            if (value.isEmpty()) {
                return allowEmpty;
            }

            // Check if the string is properly uppercase in Turkish locale
            String upperCaseValue = value.toUpperCase(TURKISH_LOCALE);
            if (!value.equals(upperCaseValue)) {
                addConstraintViolation(context, "Metin Türkçe büyük harf formatında olmalıdır");
                return false;
            }

            // Validate character composition
            return validateCharacters(value, context);
        }

        /**
         * Validates the characters in the string based on configuration.
         */
        private boolean validateCharacters(String value, ConstraintValidatorContext context) {
            // If custom pattern is provided, use it
            if (!allowedCharsPattern.isEmpty()) {
                if (!value.matches(allowedCharsPattern)) {
                    addConstraintViolation(context, "Metin belirtilen karakter desenine uymalıdır");
                    return false;
                }
                return true;
            }

            // Check each character
            for (char c : value.toCharArray()) {
                if (!isCharacterAllowed(c)) {
                    addConstraintViolation(context,
                        String.format("Geçersiz karakter: '%c'", c));
                    return false;
                }
            }

            return true;
        }

        /**
         * Checks if a character is allowed based on configuration.
         */
        private boolean isCharacterAllowed(char c) {
            // Always allow Turkish uppercase letters
            if (isTurkishUpperCaseLetter(c)) {
                return true;
            }

            // Check numbers
            if (Character.isDigit(c)) {
                return allowNumbers;
            }

            // Check spaces
            if (Character.isWhitespace(c)) {
                return allowSpaces;
            }

            // Check special characters
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                return allowSpecialChars;
            }

            return false;
        }

        /**
         * Checks if a character is a Turkish uppercase letter.
         */
        private boolean isTurkishUpperCaseLetter(char c) {
            // Turkish uppercase letters including special characters
            return (c >= 'A' && c <= 'Z') ||
                   c == 'Ç' || c == 'Ğ' || c == 'I' ||
                   c == 'İ' || c == 'Ö' || c == 'Ş' || c == 'Ü';
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
        TurkishUpperCase[] value();
    }
}
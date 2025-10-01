package com.bisttrading.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Turkish validation utilities for BIST Trading Platform.
 * Handles validation of Turkish-specific data like TC Kimlik, phone numbers, etc.
 */
@Slf4j
@Component
public class TurkishValidationUtil {

    // Turkish phone number pattern: +90 5XX XXX XX XX
    private static final Pattern TURKISH_PHONE_PATTERN = Pattern.compile(
        "^(\\+90|0)?\\s?5\\d{2}\\s?\\d{3}\\s?\\d{2}\\s?\\d{2}$"
    );

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Password pattern: at least 8 chars, 1 uppercase, 1 lowercase, 1 digit
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&çğıöşüÇĞIİÖŞÜ]{8,}$"
    );

    // TC Kimlik multipliers for checksum calculation
    private static final int[] TC_KIMLIK_MULTIPLIERS_1 = {1, 3, 5, 7, 9, 1, 3, 5, 7};
    private static final int[] TC_KIMLIK_MULTIPLIERS_2 = {2, 4, 6, 8, 2, 4, 6, 8};

    /**
     * Validates Turkish TC Kimlik number using official algorithm.
     *
     * @param tcKimlik TC Kimlik number as string
     * @return true if valid, false otherwise
     */
    public boolean isValidTcKimlik(String tcKimlik) {
        if (tcKimlik == null || tcKimlik.trim().isEmpty()) {
            return false;
        }

        // Remove any spaces or dashes
        tcKimlik = tcKimlik.replaceAll("[\\s-]", "");

        // Must be exactly 11 digits
        if (!tcKimlik.matches("^\\d{11}$")) {
            log.debug("TC Kimlik invalid format: {}", tcKimlik);
            return false;
        }

        // First digit cannot be 0
        if (tcKimlik.charAt(0) == '0') {
            log.debug("TC Kimlik starts with 0: {}", tcKimlik);
            return false;
        }

        // Convert to integer array
        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = Character.getNumericValue(tcKimlik.charAt(i));
        }

        // Apply TC Kimlik validation algorithm
        try {
            // Calculate first checksum (10th digit)
            int sum1 = 0;
            int sum2 = 0;

            for (int i = 0; i < 9; i++) {
                sum1 += digits[i] * TC_KIMLIK_MULTIPLIERS_1[i];
                sum2 += digits[i] * TC_KIMLIK_MULTIPLIERS_2[i];
            }

            int checksum1 = (sum1 % 10);
            if (checksum1 != digits[9]) {
                log.debug("TC Kimlik checksum1 failed: expected {}, got {}", checksum1, digits[9]);
                return false;
            }

            // Calculate second checksum (11th digit)
            int checksum2 = (sum1 + sum2 + digits[9]) % 10;
            if (checksum2 != digits[10]) {
                log.debug("TC Kimlik checksum2 failed: expected {}, got {}", checksum2, digits[10]);
                return false;
            }

            log.debug("TC Kimlik validation successful: {}", tcKimlik);
            return true;

        } catch (Exception e) {
            log.error("Error validating TC Kimlik: {}", tcKimlik, e);
            return false;
        }
    }

    /**
     * Validates Turkish phone number format.
     * Accepts formats like: +905551234567, 05551234567, 5551234567
     *
     * @param phoneNumber Phone number to validate
     * @return true if valid Turkish mobile number
     */
    public boolean isValidTurkishPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and dashes for validation
        String cleanPhone = phoneNumber.replaceAll("[\\s-]", "");

        boolean isValid = TURKISH_PHONE_PATTERN.matcher(cleanPhone).matches();

        if (isValid) {
            // Additional check: ensure it's a mobile number (starts with 5)
            String normalized = normalizePhoneNumber(cleanPhone);
            isValid = normalized.length() == 13 && normalized.startsWith("+905");
        }

        log.debug("Phone number validation for {}: {}", phoneNumber, isValid);
        return isValid;
    }

    /**
     * Normalizes Turkish phone number to international format.
     *
     * @param phoneNumber Phone number to normalize
     * @return Normalized phone number in +90XXXXXXXXXX format
     */
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        // Handle different formats
        if (cleaned.startsWith("+90")) {
            return cleaned;
        } else if (cleaned.startsWith("90") && cleaned.length() == 12) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return "+9" + cleaned;
        } else if (cleaned.length() == 10 && cleaned.startsWith("5")) {
            return "+90" + cleaned;
        }

        log.warn("Could not normalize phone number: {}", phoneNumber);
        return phoneNumber; // Return original if can't normalize
    }

    /**
     * Validates email address format.
     *
     * @param email Email address to validate
     * @return true if valid email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim().toLowerCase();
        boolean isValid = EMAIL_PATTERN.matcher(trimmedEmail).matches();

        log.debug("Email validation for {}: {}", email, isValid);
        return isValid;
    }

    /**
     * Validates password strength according to Turkish regulations.
     * - At least 8 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     * - Supports Turkish characters
     *
     * @param password Password to validate
     * @return true if password meets strength requirements
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        boolean isValid = PASSWORD_PATTERN.matcher(password).matches();

        log.debug("Password validation: {} characters, valid: {}",
            password != null ? password.length() : 0, isValid);
        return isValid;
    }

    /**
     * Validates Turkish postal code format.
     * Turkish postal codes are 5 digits.
     *
     * @param postalCode Postal code to validate
     * @return true if valid Turkish postal code
     */
    public boolean isValidTurkishPostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return false;
        }

        String cleaned = postalCode.trim();
        boolean isValid = cleaned.matches("^\\d{5}$");

        log.debug("Postal code validation for {}: {}", postalCode, isValid);
        return isValid;
    }

    /**
     * Validates Turkish IBAN format.
     * Turkish IBAN: TR followed by 2 check digits and 22 more digits
     *
     * @param iban IBAN to validate
     * @return true if valid Turkish IBAN format
     */
    public boolean isValidTurkishIban(String iban) {
        if (iban == null || iban.trim().isEmpty()) {
            return false;
        }

        String cleanedIban = iban.replaceAll("[\\s-]", "").toUpperCase();

        // Check basic format
        if (!cleanedIban.matches("^TR\\d{24}$")) {
            log.debug("IBAN invalid format: {}", iban);
            return false;
        }

        // Validate IBAN checksum using mod-97 algorithm
        try {
            String rearranged = cleanedIban.substring(4) + "2927" + cleanedIban.substring(2, 4);

            // Convert letters to numbers and calculate mod 97
            StringBuilder numericString = new StringBuilder();
            for (char c : rearranged.toCharArray()) {
                if (Character.isDigit(c)) {
                    numericString.append(c);
                } else {
                    numericString.append(Character.getNumericValue(c));
                }
            }

            // Calculate mod 97 for large numbers
            long remainder = 0;
            for (char digit : numericString.toString().toCharArray()) {
                remainder = (remainder * 10 + Character.getNumericValue(digit)) % 97;
            }

            boolean isValid = remainder == 1;
            log.debug("IBAN validation for {}: {}", iban, isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Error validating IBAN: {}", iban, e);
            return false;
        }
    }

    /**
     * Validates Turkish tax number (Vergi Kimlik Numarası).
     * Tax numbers are 10 digits with a specific algorithm.
     *
     * @param taxNumber Tax number to validate
     * @return true if valid Turkish tax number
     */
    public boolean isValidTurkishTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.trim().isEmpty()) {
            return false;
        }

        String cleaned = taxNumber.replaceAll("[\\s-]", "");

        // Must be exactly 10 digits
        if (!cleaned.matches("^\\d{10}$")) {
            log.debug("Tax number invalid format: {}", taxNumber);
            return false;
        }

        try {
            int[] digits = new int[10];
            for (int i = 0; i < 10; i++) {
                digits[i] = Character.getNumericValue(cleaned.charAt(i));
            }

            // Apply Turkish tax number validation algorithm
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                int temp = (digits[i] + (9 - i)) % 10;
                sum += (temp * Math.pow(2, 9 - i)) % 9;
            }

            int checksum = (10 - (sum % 10)) % 10;
            boolean isValid = checksum == digits[9];

            log.debug("Tax number validation for {}: {}", taxNumber, isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Error validating tax number: {}", taxNumber, e);
            return false;
        }
    }

    /**
     * Validates if a name contains only valid Turkish characters.
     * Allows Turkish letters, spaces, apostrophes, and hyphens.
     *
     * @param name Name to validate
     * @return true if name contains only valid characters
     */
    public boolean isValidTurkishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmed = name.trim();

        // Check length (2-50 characters)
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            return false;
        }

        // Allow Turkish letters, spaces, apostrophes, and hyphens
        boolean isValid = trimmed.matches("^[a-zA-ZçğıöşüÇĞIİÖŞÜ\\s'-]+$");

        log.debug("Turkish name validation for {}: {}", name, isValid);
        return isValid;
    }

    /**
     * Formats TC Kimlik for display (XXX XX XXX XX X).
     *
     * @param tcKimlik TC Kimlik number
     * @return Formatted TC Kimlik or original if invalid
     */
    public String formatTcKimlik(String tcKimlik) {
        if (!isValidTcKimlik(tcKimlik)) {
            return tcKimlik;
        }

        String cleaned = tcKimlik.replaceAll("[\\s-]", "");
        return String.format("%s %s %s %s %s",
            cleaned.substring(0, 3),
            cleaned.substring(3, 5),
            cleaned.substring(5, 8),
            cleaned.substring(8, 10),
            cleaned.substring(10, 11)
        );
    }

    /**
     * Formats Turkish phone number for display (+90 5XX XXX XX XX).
     *
     * @param phoneNumber Phone number to format
     * @return Formatted phone number or original if invalid
     */
    public String formatPhoneNumber(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        if (normalized == null || !isValidTurkishPhoneNumber(normalized)) {
            return phoneNumber;
        }

        // Format as +90 5XX XXX XX XX
        return String.format("%s %s %s %s %s",
            normalized.substring(0, 3),   // +90
            normalized.substring(3, 6),   // 5XX
            normalized.substring(6, 9),   // XXX
            normalized.substring(9, 11),  // XX
            normalized.substring(11, 13)  // XX
        );
    }
}
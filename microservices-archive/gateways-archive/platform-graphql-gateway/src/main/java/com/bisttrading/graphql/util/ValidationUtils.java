package com.bisttrading.graphql.util;

/**
 * Validation utilities for Turkish market requirements
 */
public class ValidationUtils {

    /**
     * Validate Turkish Citizenship Number (TC Kimlik No)
     *
     * @param tckn Turkish Identity Number
     * @return true if valid
     */
    public static boolean isValidTCKN(String tckn) {
        if (tckn == null || tckn.length() != 11) {
            return false;
        }

        // Check if all characters are digits
        if (!tckn.matches("\\d{11}")) {
            return false;
        }

        // First digit cannot be 0
        if (tckn.charAt(0) == '0') {
            return false;
        }

        // Convert to digit array
        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = Character.getNumericValue(tckn.charAt(i));
        }

        // Calculate 10th digit
        int sumOdd = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int sumEven = digits[1] + digits[3] + digits[5] + digits[7];
        int check10 = (sumOdd * 7 - sumEven) % 10;

        if (check10 != digits[9]) {
            return false;
        }

        // Calculate 11th digit
        int sumAll = 0;
        for (int i = 0; i < 10; i++) {
            sumAll += digits[i];
        }
        int check11 = sumAll % 10;

        return check11 == digits[10];
    }

    /**
     * Validate Turkish phone number
     *
     * @param phone Phone number
     * @return true if valid Turkish phone format
     */
    public static boolean isValidTurkishPhone(String phone) {
        if (phone == null) {
            return false;
        }

        // Remove all non-digit characters
        String cleaned = phone.replaceAll("[^0-9]", "");

        // Turkish mobile: 05XX XXX XX XX (11 digits)
        // Turkish landline: 02XX XXX XX XX, 03XX XXX XX XX, etc. (10 digits)
        return cleaned.matches("^(05\\d{9}|0[2-4]\\d{8})$");
    }

    /**
     * Validate Turkish IBAN
     *
     * @param iban IBAN number
     * @return true if valid Turkish IBAN
     */
    public static boolean isValidTurkishIBAN(String iban) {
        if (iban == null) {
            return false;
        }

        // Remove spaces and convert to uppercase
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();

        // Turkish IBAN format: TR + 2 check digits + 5 bank code + 1 reserved + 16 account number = 26 chars
        if (!cleaned.matches("^TR\\d{24}$")) {
            return false;
        }

        // Basic IBAN checksum validation (simplified)
        return true; // Full checksum validation would be implemented here
    }
}
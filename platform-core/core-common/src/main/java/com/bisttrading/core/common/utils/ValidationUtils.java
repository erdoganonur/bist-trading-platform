package com.bisttrading.core.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * Utility class for validation operations with Turkish-specific validations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtils {

    /**
     * Pattern for Turkish IBAN validation.
     */
    private static final Pattern TURKISH_IBAN_PATTERN = Pattern.compile("^TR\\d{2}\\d{5}\\d{1}\\d{16}$");

    /**
     * Pattern for international IBAN validation.
     */
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}([A-Z0-9]?){0,16}$");

    /**
     * Validates a Turkish TC Kimlik number.
     *
     * @param tcKimlik The TC Kimlik number to validate
     * @return true if valid TC Kimlik number
     */
    public static boolean isValidTCKimlik(String tcKimlik) {
        if (StringUtils.isBlank(tcKimlik)) {
            return false;
        }

        // Remove any spaces or special characters
        String cleaned = tcKimlik.replaceAll("[^0-9]", "");

        // TC Kimlik must be 11 digits
        if (cleaned.length() != 11) {
            return false;
        }

        // Cannot start with 0
        if (cleaned.startsWith("0")) {
            return false;
        }

        // Convert to integer array
        int[] digits = new int[11];
        try {
            for (int i = 0; i < 11; i++) {
                digits[i] = Integer.parseInt(String.valueOf(cleaned.charAt(i)));
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // Validate using TC Kimlik algorithm
        return validateTCKimlikAlgorithm(digits);
    }

    /**
     * Validates TC Kimlik using the official algorithm.
     *
     * @param digits Array of 11 digits
     * @return true if valid according to algorithm
     */
    private static boolean validateTCKimlikAlgorithm(int[] digits) {
        // First 10 digits algorithm
        int sumOdd = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int sumEven = digits[1] + digits[3] + digits[5] + digits[7];

        int check10 = ((sumOdd * 7) - sumEven) % 10;
        if (check10 != digits[9]) {
            return false;
        }

        // All 11 digits algorithm
        int sumAll = 0;
        for (int i = 0; i < 10; i++) {
            sumAll += digits[i];
        }

        int check11 = sumAll % 10;
        return check11 == digits[10];
    }

    /**
     * Validates a Turkish IBAN number.
     *
     * @param iban The IBAN to validate
     * @return true if valid Turkish IBAN
     */
    public static boolean isValidTurkishIBAN(String iban) {
        if (StringUtils.isBlank(iban)) {
            return false;
        }

        // Remove spaces and convert to uppercase
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();

        // Check format
        if (!TURKISH_IBAN_PATTERN.matcher(cleaned).matches()) {
            return false;
        }

        // Validate using IBAN checksum algorithm
        return validateIBANChecksum(cleaned);
    }

    /**
     * Validates any IBAN number.
     *
     * @param iban The IBAN to validate
     * @return true if valid IBAN
     */
    public static boolean isValidIBAN(String iban) {
        if (StringUtils.isBlank(iban)) {
            return false;
        }

        // Remove spaces and convert to uppercase
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();

        // Check basic format
        if (!IBAN_PATTERN.matcher(cleaned).matches()) {
            return false;
        }

        // Validate using IBAN checksum algorithm
        return validateIBANChecksum(cleaned);
    }

    /**
     * Validates IBAN using mod-97 checksum algorithm.
     *
     * @param iban The IBAN to validate
     * @return true if checksum is valid
     */
    private static boolean validateIBANChecksum(String iban) {
        try {
            // Move first 4 characters to the end
            String rearranged = iban.substring(4) + iban.substring(0, 4);

            // Replace letters with numbers (A=10, B=11, ..., Z=35)
            StringBuilder numeric = new StringBuilder();
            for (char c : rearranged.toCharArray()) {
                if (Character.isLetter(c)) {
                    numeric.append(c - 'A' + 10);
                } else {
                    numeric.append(c);
                }
            }

            // Calculate mod 97
            String numericString = numeric.toString();
            int remainder = calculateMod97(numericString);

            return remainder == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates mod 97 for large numbers.
     *
     * @param numericString The numeric string
     * @return Remainder after mod 97
     */
    private static int calculateMod97(String numericString) {
        int remainder = 0;
        for (char digit : numericString.toCharArray()) {
            remainder = (remainder * 10 + (digit - '0')) % 97;
        }
        return remainder;
    }

    /**
     * Validates a Turkish tax number (Vergi No).
     *
     * @param taxNumber The tax number to validate
     * @return true if valid tax number
     */
    public static boolean isValidTurkishTaxNumber(String taxNumber) {
        if (StringUtils.isBlank(taxNumber)) {
            return false;
        }

        String cleaned = taxNumber.replaceAll("[^0-9]", "");

        // Tax number must be 10 digits
        if (cleaned.length() != 10) {
            return false;
        }

        try {
            int[] digits = new int[10];
            for (int i = 0; i < 10; i++) {
                digits[i] = Integer.parseInt(String.valueOf(cleaned.charAt(i)));
            }

            return validateTaxNumberAlgorithm(digits);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates tax number using the official algorithm.
     *
     * @param digits Array of 10 digits
     * @return true if valid according to algorithm
     */
    private static boolean validateTaxNumberAlgorithm(int[] digits) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int temp = (digits[i] + (10 - i)) % 10;
            sum += (temp * Math.pow(2, 10 - i)) % 9;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == digits[9];
    }

    /**
     * Validates a Turkish passport number.
     *
     * @param passportNumber The passport number to validate
     * @return true if valid format
     */
    public static boolean isValidTurkishPassport(String passportNumber) {
        if (StringUtils.isBlank(passportNumber)) {
            return false;
        }

        String cleaned = passportNumber.replaceAll("\\s", "").toUpperCase();

        // Turkish passport: 1 letter + 8 digits
        return cleaned.matches("^[A-Z]\\d{8}$");
    }

    /**
     * Validates a Turkish driving license number.
     *
     * @param licenseNumber The license number to validate
     * @return true if valid format
     */
    public static boolean isValidTurkishDrivingLicense(String licenseNumber) {
        if (StringUtils.isBlank(licenseNumber)) {
            return false;
        }

        String cleaned = licenseNumber.replaceAll("[^0-9]", "");

        // Turkish driving license: 6 digits
        return cleaned.matches("^\\d{6}$");
    }

    /**
     * Validates a Turkish postal code.
     *
     * @param postalCode The postal code to validate
     * @return true if valid Turkish postal code
     */
    public static boolean isValidTurkishPostalCode(String postalCode) {
        if (StringUtils.isBlank(postalCode)) {
            return false;
        }

        String cleaned = postalCode.replaceAll("[^0-9]", "");

        // Turkish postal code: 5 digits, first digit between 01-81
        if (!cleaned.matches("^\\d{5}$")) {
            return false;
        }

        int firstTwoDigits = Integer.parseInt(cleaned.substring(0, 2));
        return firstTwoDigits >= 1 && firstTwoDigits <= 81;
    }

    /**
     * Validates a credit card number using Luhn algorithm.
     *
     * @param cardNumber The card number to validate
     * @return true if valid according to Luhn algorithm
     */
    public static boolean isValidCreditCardNumber(String cardNumber) {
        if (StringUtils.isBlank(cardNumber)) {
            return false;
        }

        String cleaned = cardNumber.replaceAll("[^0-9]", "");

        // Credit card must be between 13-19 digits
        if (cleaned.length() < 13 || cleaned.length() > 19) {
            return false;
        }

        return validateLuhnAlgorithm(cleaned);
    }

    /**
     * Validates using Luhn algorithm.
     *
     * @param number The number to validate
     * @return true if valid according to Luhn algorithm
     */
    private static boolean validateLuhnAlgorithm(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(String.valueOf(number.charAt(i)));

            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }

            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * Validates if a string represents a valid age (between 0-150).
     *
     * @param ageString The age string to validate
     * @return true if valid age
     */
    public static boolean isValidAge(String ageString) {
        if (StringUtils.isBlank(ageString)) {
            return false;
        }

        try {
            int age = Integer.parseInt(ageString.trim());
            return age >= 0 && age <= 150;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if a person is above the legal age (18 years).
     *
     * @param ageString The age string to validate
     * @return true if person is 18 or older
     */
    public static boolean isLegalAge(String ageString) {
        if (StringUtils.isBlank(ageString)) {
            return false;
        }

        try {
            int age = Integer.parseInt(ageString.trim());
            return age >= 18;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates a password strength.
     *
     * @param password The password to validate
     * @return true if password meets strength requirements
     */
    public static boolean isStrongPassword(String password) {
        if (StringUtils.isBlank(password)) {
            return false;
        }

        // At least 8 characters
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        // Must have at least 3 of the 4 character types
        int score = 0;
        if (hasUpper) score++;
        if (hasLower) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;

        return score >= 3;
    }

    /**
     * Validates if a string contains only alphanumeric characters and Turkish letters.
     *
     * @param text The text to validate
     * @return true if contains only valid characters
     */
    public static boolean isValidTextInput(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        return text.matches("^[a-zA-ZçğıöşüÇĞIÖŞÜ0-9\\s.-]+$");
    }

    /**
     * Validates if a string is a valid URL.
     *
     * @param url The URL to validate
     * @return true if valid URL format
     */
    public static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * Validates if a string represents a valid decimal number.
     *
     * @param numberString The number string to validate
     * @return true if valid decimal number
     */
    public static boolean isValidDecimal(String numberString) {
        if (StringUtils.isBlank(numberString)) {
            return false;
        }

        try {
            Double.parseDouble(numberString.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if a string represents a valid integer.
     *
     * @param numberString The number string to validate
     * @return true if valid integer
     */
    public static boolean isValidInteger(String numberString) {
        if (StringUtils.isBlank(numberString)) {
            return false;
        }

        try {
            Integer.parseInt(numberString);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
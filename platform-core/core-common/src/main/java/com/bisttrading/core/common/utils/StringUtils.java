package com.bisttrading.core.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for string operations with Turkish character support.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtils {

    /**
     * Turkish locale for string operations.
     */
    public static final Locale TURKISH_LOCALE = new Locale("tr", "TR");

    /**
     * Turkish characters mapping for normalization.
     */
    private static final String TURKISH_CHARS = "çğıöşüÇĞIÖŞÜ";
    private static final String LATIN_CHARS = "cgiosuCGIOSU";

    /**
     * Pattern for Turkish phone number validation.
     */
    private static final Pattern TURKISH_PHONE_PATTERN = Pattern.compile("^(\\+90|0)?[1-9][0-9]{9}$");

    /**
     * Pattern for email validation.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * Pattern for alphanumeric characters (including Turkish).
     */
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-ZçğıöşüÇĞIÖŞÜ0-9]+$");

    /**
     * Pattern for letters only (including Turkish).
     */
    private static final Pattern LETTERS_ONLY_PATTERN = Pattern.compile("^[a-zA-ZçğıöşüÇĞIÖŞÜ\\s]+$");

    /**
     * Checks if a string is null or empty.
     *
     * @param str The string to check
     * @return true if string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is not null and not empty.
     *
     * @param str The string to check
     * @return true if string is not null and not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * @param str The string to check
     * @return true if string is blank
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a string is not blank.
     *
     * @param str The string to check
     * @return true if string is not blank
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Trims a string, handling null values.
     *
     * @param str The string to trim
     * @return Trimmed string or null if input is null
     */
    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }

    /**
     * Trims a string and returns empty string if null.
     *
     * @param str The string to trim
     * @return Trimmed string or empty string if null
     */
    public static String trimToEmpty(String str) {
        return str != null ? str.trim() : "";
    }

    /**
     * Trims a string and returns null if the result is empty.
     *
     * @param str The string to trim
     * @return Trimmed string or null if empty
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }

    /**
     * Converts string to uppercase using Turkish locale.
     *
     * @param str The string to convert
     * @return Uppercase string or null if input is null
     */
    public static String toUpperCaseTurkish(String str) {
        return str != null ? str.toUpperCase(TURKISH_LOCALE) : null;
    }

    /**
     * Converts string to lowercase using Turkish locale.
     *
     * @param str The string to convert
     * @return Lowercase string or null if input is null
     */
    public static String toLowerCaseTurkish(String str) {
        return str != null ? str.toLowerCase(TURKISH_LOCALE) : null;
    }

    /**
     * Capitalizes the first letter of each word using Turkish locale.
     *
     * @param str The string to capitalize
     * @return Capitalized string or null if input is null
     */
    public static String toTitleCaseTurkish(String str) {
        if (isBlank(str)) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Removes Turkish characters and replaces them with Latin equivalents.
     *
     * @param str The string to normalize
     * @return Normalized string or null if input is null
     */
    public static String removeTurkishChars(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            int index = TURKISH_CHARS.indexOf(c);
            if (index >= 0) {
                result.append(LATIN_CHARS.charAt(index));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Removes accents and diacritics from string.
     *
     * @param str The string to normalize
     * @return Normalized string or null if input is null
     */
    public static String removeAccents(String str) {
        if (str == null) {
            return null;
        }

        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    /**
     * Masks a string, showing only the first and last characters.
     *
     * @param str        The string to mask
     * @param maskChar   The character to use for masking
     * @param showLength Number of characters to show at start and end
     * @return Masked string
     */
    public static String mask(String str, char maskChar, int showLength) {
        if (isBlank(str) || str.length() <= showLength * 2) {
            return str;
        }

        StringBuilder masked = new StringBuilder();
        masked.append(str, 0, showLength);

        for (int i = showLength; i < str.length() - showLength; i++) {
            masked.append(maskChar);
        }

        masked.append(str.substring(str.length() - showLength));
        return masked.toString();
    }

    /**
     * Masks an email address.
     *
     * @param email The email to mask
     * @return Masked email (e.g., "jo***@example.com")
     */
    public static String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 2) {
            return email;
        }

        String maskedLocal = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return maskedLocal + "@" + domainPart;
    }

    /**
     * Masks a phone number.
     *
     * @param phoneNumber The phone number to mask
     * @return Masked phone number (e.g., "905******1234")
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return phoneNumber;
        }

        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 8) {
            return phoneNumber;
        }

        return cleaned.substring(0, 3) + "******" + cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Validates a Turkish phone number.
     *
     * @param phoneNumber The phone number to validate
     * @return true if valid Turkish phone number
     */
    public static boolean isValidTurkishPhoneNumber(String phoneNumber) {
        if (isBlank(phoneNumber)) {
            return false;
        }

        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        return TURKISH_PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Validates an email address.
     *
     * @param email The email to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Checks if a string contains only alphanumeric characters (including Turkish).
     *
     * @param str The string to check
     * @return true if alphanumeric
     */
    public static boolean isAlphanumeric(String str) {
        return isNotBlank(str) && ALPHANUMERIC_PATTERN.matcher(str).matches();
    }

    /**
     * Checks if a string contains only letters (including Turkish).
     *
     * @param str The string to check
     * @return true if contains only letters
     */
    public static boolean isLettersOnly(String str) {
        return isNotBlank(str) && LETTERS_ONLY_PATTERN.matcher(str).matches();
    }

    /**
     * Checks if a string contains only digits.
     *
     * @param str The string to check
     * @return true if contains only digits
     */
    public static boolean isNumeric(String str) {
        if (isBlank(str)) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pads a string to the left with the specified character.
     *
     * @param str     The string to pad
     * @param length  The target length
     * @param padChar The character to pad with
     * @return Padded string
     */
    public static String padLeft(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }

        if (str.length() >= length) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - str.length(); i++) {
            sb.append(padChar);
        }
        sb.append(str);

        return sb.toString();
    }

    /**
     * Pads a string to the right with the specified character.
     *
     * @param str     The string to pad
     * @param length  The target length
     * @param padChar The character to pad with
     * @return Padded string
     */
    public static String padRight(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }

        if (str.length() >= length) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < length - str.length(); i++) {
            sb.append(padChar);
        }

        return sb.toString();
    }

    /**
     * Truncates a string to the specified length.
     *
     * @param str    The string to truncate
     * @param length The maximum length
     * @return Truncated string
     */
    public static String truncate(String str, int length) {
        if (str == null || str.length() <= length) {
            return str;
        }

        return str.substring(0, length);
    }

    /**
     * Truncates a string and adds ellipsis if needed.
     *
     * @param str    The string to truncate
     * @param length The maximum length (including ellipsis)
     * @return Truncated string with ellipsis if needed
     */
    public static String truncateWithEllipsis(String str, int length) {
        if (str == null || str.length() <= length) {
            return str;
        }

        if (length <= 3) {
            return truncate(str, length);
        }

        return str.substring(0, length - 3) + "...";
    }

    /**
     * Splits a string by delimiter and trims each part.
     *
     * @param str       The string to split
     * @param delimiter The delimiter
     * @return List of trimmed parts
     */
    public static List<String> splitAndTrim(String str, String delimiter) {
        if (isBlank(str)) {
            return List.of();
        }

        return Arrays.stream(str.split(delimiter))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .toList();
    }

    /**
     * Joins strings with a delimiter, ignoring null and empty strings.
     *
     * @param delimiter The delimiter
     * @param strings   The strings to join
     * @return Joined string
     */
    public static String joinNonEmpty(String delimiter, String... strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        return Arrays.stream(strings)
                     .filter(StringUtils::isNotBlank)
                     .reduce((a, b) -> a + delimiter + b)
                     .orElse("");
    }

    /**
     * Returns the string if it's not null, otherwise returns the default value.
     *
     * @param str          The string to check
     * @param defaultValue The default value
     * @return The string or default value
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str != null ? str : defaultValue;
    }

    /**
     * Returns the string if it's not blank, otherwise returns the default value.
     *
     * @param str          The string to check
     * @param defaultValue The default value
     * @return The string or default value
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isNotBlank(str) ? str : defaultValue;
    }

    /**
     * Generates a random string of specified length using alphanumeric characters.
     *
     * @param length The length of the random string
     * @return Random string
     */
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    /**
     * Counts the number of words in a string.
     *
     * @param str The string to count words in
     * @return Number of words
     */
    public static int wordCount(String str) {
        if (isBlank(str)) {
            return 0;
        }

        return str.trim().split("\\s+").length;
    }

    /**
     * Reverses a string.
     *
     * @param str The string to reverse
     * @return Reversed string or null if input is null
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }

        return new StringBuilder(str).reverse().toString();
    }
}
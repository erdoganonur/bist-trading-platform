package com.bisttrading.core.security.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password utility for password validation, generation, and security checks.
 * Provides comprehensive password security features for the BIST Trading Platform.
 */
@Slf4j
@Component
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    // Password complexity patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    private static final Pattern TURKISH_CHAR_PATTERN = Pattern.compile(".*[ğĞıİöÖüÜşŞçÇ].*");

    // Common weak passwords (partial list)
    private static final List<String> COMMON_WEAK_PASSWORDS = List.of(
        "password", "123456", "123456789", "qwerty", "abc123", "password123",
        "admin", "letmein", "welcome", "monkey", "dragon", "master",
        "şifre", "parola", "sifre", "123456", "qwerty", "asdfgh"
    );

    // Character sets for password generation
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    public PasswordUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Validates password complexity according to BIST security requirements.
     *
     * @param password Password to validate
     * @return PasswordValidationResult with validation details
     */
    public PasswordValidationResult validatePassword(String password) {
        PasswordValidationResult result = new PasswordValidationResult();

        if (password == null) {
            result.addError("Şifre boş olamaz");
            return result;
        }

        // Length check
        if (password.length() < 8) {
            result.addError("Şifre en az 8 karakter olmalıdır");
        }
        if (password.length() > 128) {
            result.addError("Şifre en fazla 128 karakter olabilir");
        }

        // Complexity checks
        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            result.addError("Şifre en az 1 büyük harf içermelidir");
        }
        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            result.addError("Şifre en az 1 küçük harf içermelidir");
        }
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            result.addError("Şifre en az 1 rakam içermelidir");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            result.addError("Şifre en az 1 özel karakter içermelidir");
        }

        // Check for common weak passwords
        if (isCommonWeakPassword(password)) {
            result.addError("Bu şifre çok yaygın kullanılan bir şifredir, daha güvenli bir şifre seçiniz");
        }

        // Check for repeated characters
        if (hasRepeatedCharacters(password, 3)) {
            result.addError("Şifre üst üste 3 veya daha fazla aynı karakter içeremez");
        }

        // Check for sequential characters
        if (hasSequentialCharacters(password, 3)) {
            result.addError("Şifre üst üste 3 veya daha fazla ardışık karakter içeremez");
        }

        // Check for Turkish characters (informational)
        if (TURKISH_CHAR_PATTERN.matcher(password).matches()) {
            result.addWarning("Şifrenizde Türkçe karakter bulunuyor, bazı sistemlerde sorun yaşayabilirsiniz");
        }

        // Calculate password strength
        result.setStrength(calculatePasswordStrength(password));

        log.debug("Şifre doğrulama tamamlandı - güçlü: {}, hata sayısı: {}",
            result.isValid(), result.getErrors().size());

        return result;
    }

    /**
     * Generates a secure random password with specified length and complexity.
     *
     * @param length Password length (minimum 8, maximum 128)
     * @param includeSpecialChars Whether to include special characters
     * @return Generated password
     */
    public String generateSecurePassword(int length, boolean includeSpecialChars) {
        if (length < 8 || length > 128) {
            throw new IllegalArgumentException("Şifre uzunluğu 8-128 karakter arası olmalıdır");
        }

        StringBuilder password = new StringBuilder();
        List<Character> allChars = new ArrayList<>();

        // Add character sets
        addCharsToList(allChars, UPPERCASE_CHARS);
        addCharsToList(allChars, LOWERCASE_CHARS);
        addCharsToList(allChars, DIGIT_CHARS);

        if (includeSpecialChars) {
            addCharsToList(allChars, SPECIAL_CHARS);
        }

        // Ensure at least one character from each required set
        password.append(getRandomChar(UPPERCASE_CHARS));
        password.append(getRandomChar(LOWERCASE_CHARS));
        password.append(getRandomChar(DIGIT_CHARS));

        if (includeSpecialChars) {
            password.append(getRandomChar(SPECIAL_CHARS));
        }

        // Fill remaining length with random characters
        int remainingLength = length - password.length();
        for (int i = 0; i < remainingLength; i++) {
            password.append(allChars.get(secureRandom.nextInt(allChars.size())));
        }

        // Shuffle the password characters
        return shuffleString(password.toString());
    }

    /**
     * Generates a secure password with default settings (12 characters, with special chars).
     *
     * @return Generated password
     */
    public String generateSecurePassword() {
        return generateSecurePassword(12, true);
    }

    /**
     * Encodes a password using the configured password encoder.
     *
     * @param rawPassword Raw password
     * @return Encoded password
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Şifre null olamaz");
        }

        log.debug("Şifre hashleniyor");
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies a raw password against an encoded password.
     *
     * @param rawPassword Raw password
     * @param encodedPassword Encoded password
     * @return true if passwords match
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        log.debug("Şifre doğrulama yapılıyor");
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Checks if a password needs to be updated (for security policy changes).
     *
     * @param encodedPassword Encoded password to check
     * @return true if password needs update
     */
    public boolean needsUpgrade(String encodedPassword) {
        if (encodedPassword == null) {
            return true;
        }

        // Check if password uses an old encoding format
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }

    /**
     * Calculates password strength score (0-100).
     *
     * @param password Password to analyze
     * @return Strength score
     */
    public int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // Length scoring
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;

        // Character variety scoring
        if (UPPERCASE_PATTERN.matcher(password).matches()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score += 10;
        if (DIGIT_PATTERN.matcher(password).matches()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score += 15;

        // Uniqueness scoring
        if (!isCommonWeakPassword(password)) score += 10;
        if (!hasRepeatedCharacters(password, 3)) score += 5;
        if (!hasSequentialCharacters(password, 3)) score += 5;

        return Math.min(100, score);
    }

    /**
     * Checks if password is in common weak passwords list.
     *
     * @param password Password to check
     * @return true if password is weak
     */
    private boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return COMMON_WEAK_PASSWORDS.stream()
            .anyMatch(weak -> lowerPassword.contains(weak.toLowerCase()));
    }

    /**
     * Checks for repeated characters in password.
     *
     * @param password Password to check
     * @param maxRepeats Maximum allowed repeats
     * @return true if has excessive repeats
     */
    private boolean hasRepeatedCharacters(String password, int maxRepeats) {
        for (int i = 0; i <= password.length() - maxRepeats; i++) {
            char c = password.charAt(i);
            boolean allSame = true;
            for (int j = 1; j < maxRepeats; j++) {
                if (password.charAt(i + j) != c) {
                    allSame = false;
                    break;
                }
            }
            if (allSame) return true;
        }
        return false;
    }

    /**
     * Checks for sequential characters in password.
     *
     * @param password Password to check
     * @param maxSequential Maximum allowed sequential chars
     * @return true if has excessive sequential chars
     */
    private boolean hasSequentialCharacters(String password, int maxSequential) {
        for (int i = 0; i <= password.length() - maxSequential; i++) {
            boolean isSequential = true;
            for (int j = 1; j < maxSequential; j++) {
                if (password.charAt(i + j) != password.charAt(i) + j) {
                    isSequential = false;
                    break;
                }
            }
            if (isSequential) return true;
        }
        return false;
    }

    /**
     * Adds characters from string to list.
     *
     * @param list Target list
     * @param chars Characters to add
     */
    private void addCharsToList(List<Character> list, String chars) {
        for (char c : chars.toCharArray()) {
            list.add(c);
        }
    }

    /**
     * Gets a random character from string.
     *
     * @param chars String of characters
     * @return Random character
     */
    private char getRandomChar(String chars) {
        return chars.charAt(secureRandom.nextInt(chars.length()));
    }

    /**
     * Shuffles a string randomly.
     *
     * @param input Input string
     * @return Shuffled string
     */
    private String shuffleString(String input) {
        List<Character> chars = new ArrayList<>();
        for (char c : input.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars, secureRandom);

        StringBuilder shuffled = new StringBuilder();
        for (char c : chars) {
            shuffled.append(c);
        }
        return shuffled.toString();
    }

    /**
     * Password validation result class.
     */
    public static class PasswordValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private int strength = 0;

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public int getStrength() {
            return strength;
        }

        public void setStrength(int strength) {
            this.strength = strength;
        }

        public String getStrengthDescription() {
            if (strength < 30) return "Çok Zayıf";
            if (strength < 50) return "Zayıf";
            if (strength < 70) return "Orta";
            if (strength < 90) return "Güçlü";
            return "Çok Güçlü";
        }
    }
}
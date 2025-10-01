package com.bisttrading.core.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PasswordUtil.
 * Tests password validation, generation, and security features.
 */
@ExtendWith(MockitoExtension.class)
class PasswordUtilTest {

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    private PasswordUtil passwordUtil;
    private PasswordEncoder realPasswordEncoder;

    @BeforeEach
    void setUp() {
        realPasswordEncoder = new BCryptPasswordEncoder(12);
        passwordUtil = new PasswordUtil(realPasswordEncoder);
    }

    @Test
    void shouldValidateStrongPassword() {
        String strongPassword = "StrongP@ssw0rd123!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(strongPassword);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getStrength() > 70);
        assertEquals("Güçlü", result.getStrengthDescription());
    }

    @Test
    void shouldRejectWeakPassword() {
        String weakPassword = "weak";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(weakPassword);

        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getStrength() < 50);
    }

    @Test
    void shouldRejectPasswordWithoutUppercase() {
        String password = "lowercase123!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("büyük harf")));
    }

    @Test
    void shouldRejectPasswordWithoutLowercase() {
        String password = "UPPERCASE123!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("küçük harf")));
    }

    @Test
    void shouldRejectPasswordWithoutDigit() {
        String password = "NoDigitsHere!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("rakam")));
    }

    @Test
    void shouldRejectPasswordWithoutSpecialCharacter() {
        String password = "NoSpecialChars123";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("özel karakter")));
    }

    @Test
    void shouldRejectTooShortPassword() {
        String shortPassword = "Abc1!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(shortPassword);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("en az 8 karakter")));
    }

    @Test
    void shouldRejectTooLongPassword() {
        String longPassword = "A".repeat(130) + "bc1!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(longPassword);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("en fazla 128 karakter")));
    }

    @Test
    void shouldRejectCommonWeakPassword() {
        String commonPassword = "password123";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(commonPassword);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("yaygın kullanılan")));
    }

    @Test
    void shouldRejectPasswordWithRepeatedCharacters() {
        String repeatedPassword = "Passssword123!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(repeatedPassword);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("aynı karakter")));
    }

    @Test
    void shouldRejectPasswordWithSequentialCharacters() {
        String sequentialPassword = "Password123!abc";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(sequentialPassword);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("ardışık karakter")));
    }

    @Test
    void shouldWarnAboutTurkishCharacters() {
        String turkishPassword = "Şifrè123!";

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(turkishPassword);

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("Türkçe karakter")));
    }

    @Test
    void shouldHandleNullPassword() {
        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(null);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
            .anyMatch(error -> error.contains("boş olamaz")));
    }

    @Test
    void shouldGenerateSecurePasswordWithCorrectLength() {
        String password = passwordUtil.generateSecurePassword(16, true);

        assertNotNull(password);
        assertEquals(16, password.length());
    }

    @Test
    void shouldGeneratePasswordWithRequiredComplexity() {
        String password = passwordUtil.generateSecurePassword(12, true);

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

        assertTrue(result.isValid());
        assertTrue(result.getStrength() > 50);
    }

    @Test
    void shouldGeneratePasswordWithoutSpecialChars() {
        String password = passwordUtil.generateSecurePassword(12, false);

        assertNotNull(password);
        assertFalse(password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"));
    }

    @Test
    void shouldGenerateDefaultSecurePassword() {
        String password = passwordUtil.generateSecurePassword();

        assertNotNull(password);
        assertEquals(12, password.length());

        PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);
        assertTrue(result.isValid());
    }

    @Test
    void shouldRejectInvalidLengthForGeneration() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.generateSecurePassword(5, true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.generateSecurePassword(150, true);
        });
    }

    @Test
    void shouldEncodePassword() {
        String rawPassword = "TestPassword123!";

        String encodedPassword = passwordUtil.encodePassword(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$"));
    }

    @Test
    void shouldVerifyCorrectPassword() {
        String rawPassword = "TestPassword123!";
        String encodedPassword = passwordUtil.encodePassword(rawPassword);

        assertTrue(passwordUtil.verifyPassword(rawPassword, encodedPassword));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String rawPassword = "TestPassword123!";
        String wrongPassword = "WrongPassword123!";
        String encodedPassword = passwordUtil.encodePassword(rawPassword);

        assertFalse(passwordUtil.verifyPassword(wrongPassword, encodedPassword));
    }

    @Test
    void shouldHandleNullPasswordVerification() {
        String encodedPassword = passwordUtil.encodePassword("TestPassword123!");

        assertFalse(passwordUtil.verifyPassword(null, encodedPassword));
        assertFalse(passwordUtil.verifyPassword("TestPassword123!", null));
        assertFalse(passwordUtil.verifyPassword(null, null));
    }

    @Test
    void shouldRejectNullPasswordEncoding() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordUtil.encodePassword(null);
        });
    }

    @Test
    void shouldCalculatePasswordStrength() {
        String weakPassword = "weak";
        String strongPassword = "VeryStrongP@ssw0rd123!";

        int weakStrength = passwordUtil.calculatePasswordStrength(weakPassword);
        int strongStrength = passwordUtil.calculatePasswordStrength(strongPassword);

        assertTrue(weakStrength < strongStrength);
        assertTrue(weakStrength < 50);
        assertTrue(strongStrength > 70);
    }

    @Test
    void shouldReturnZeroStrengthForNullPassword() {
        int strength = passwordUtil.calculatePasswordStrength(null);
        assertEquals(0, strength);
    }

    @Test
    void shouldReturnZeroStrengthForEmptyPassword() {
        int strength = passwordUtil.calculatePasswordStrength("");
        assertEquals(0, strength);
    }

    @Test
    void shouldIdentifyNonExpiredPasswords() {
        String encodedPassword = passwordUtil.encodePassword("TestPassword123!");

        // BCrypt passwords don't expire by algorithm, so this should return false
        assertFalse(passwordUtil.needsUpgrade(encodedPassword));
    }

    @Test
    void shouldIdentifyNullPasswordAsNeedingUpgrade() {
        assertTrue(passwordUtil.needsUpgrade(null));
    }

    @Test
    void shouldValidatePasswordComplexityCorrectly() {
        // Test the isPasswordComplex method indirectly through validation
        String complexPassword = "ComplexP@ssw0rd123!";
        String simplePassword = "simple";

        PasswordUtil.PasswordValidationResult complexResult = passwordUtil.validatePassword(complexPassword);
        PasswordUtil.PasswordValidationResult simpleResult = passwordUtil.validatePassword(simplePassword);

        assertTrue(complexResult.isValid());
        assertFalse(simpleResult.isValid());
    }

    @Test
    void shouldGenerateUniquePasswords() {
        String password1 = passwordUtil.generateSecurePassword(12, true);
        String password2 = passwordUtil.generateSecurePassword(12, true);

        assertNotEquals(password1, password2);
    }

    @Test
    void shouldProvideCorrectStrengthDescriptions() {
        assertEquals("Çok Zayıf", getResultWithStrength(20).getStrengthDescription());
        assertEquals("Zayıf", getResultWithStrength(40).getStrengthDescription());
        assertEquals("Orta", getResultWithStrength(60).getStrengthDescription());
        assertEquals("Güçlü", getResultWithStrength(80).getStrengthDescription());
        assertEquals("Çok Güçlü", getResultWithStrength(95).getStrengthDescription());
    }

    private PasswordUtil.PasswordValidationResult getResultWithStrength(int strength) {
        PasswordUtil.PasswordValidationResult result = new PasswordUtil.PasswordValidationResult();
        result.setStrength(strength);
        return result;
    }

    @Test
    void shouldHandleEdgeCasesInPasswordGeneration() {
        // Test minimum length
        String minPassword = passwordUtil.generateSecurePassword(8, true);
        assertEquals(8, minPassword.length());

        // Test maximum length
        String maxPassword = passwordUtil.generateSecurePassword(128, true);
        assertEquals(128, maxPassword.length());
    }

    @Test
    void shouldValidateGeneratedPasswordsConsistently() {
        // Generate multiple passwords and ensure they all pass validation
        for (int i = 0; i < 10; i++) {
            String password = passwordUtil.generateSecurePassword(12, true);
            PasswordUtil.PasswordValidationResult result = passwordUtil.validatePassword(password);

            assertTrue(result.isValid(), "Generated password should be valid: " + password);
            assertTrue(result.getStrength() > 50, "Generated password should be strong");
        }
    }
}
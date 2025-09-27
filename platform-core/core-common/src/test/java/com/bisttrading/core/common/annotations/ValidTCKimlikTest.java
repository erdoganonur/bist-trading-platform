package com.bisttrading.core.common.annotations;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ValidTCKimlik annotation.
 */
class ValidTCKimlikTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Data
    static class TestObject {
        @ValidTCKimlik
        private String tcKimlik;

        @ValidTCKimlik(allowNull = false)
        private String requiredTcKimlik;

        @ValidTCKimlik(normalize = false)
        private String exactTcKimlik;
    }

    @Test
    void shouldValidateCorrectTCKimlik() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("10000000146"); // Valid test TC Kimlik

        Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectInvalidTCKimlik() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("12345678900"); // Invalid TC Kimlik

        Set<ConstraintViolation<TestObject>> violations = validator.validate(obj);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("geçersiz");
    }

    @Test
    void shouldAllowNullByDefault() {
        TestObject obj = new TestObject();
        obj.setTcKimlik(null);

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "tcKimlik");

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNullWhenNotAllowed() {
        TestObject obj = new TestObject();
        obj.setRequiredTcKimlik(null);

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "requiredTcKimlik");

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("boş olamaz");
    }

    @Test
    void shouldNormalizeInputByDefault() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("100 000 001 46"); // Valid TC with spaces

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "tcKimlik");

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldNotNormalizeWhenDisabled() {
        TestObject obj = new TestObject();
        obj.setExactTcKimlik("100 000 001 46"); // With spaces

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "exactTcKimlik");

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectTooShortNumbers() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("1234567890"); // 10 digits

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "tcKimlik");

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("11 haneli");
    }

    @Test
    void shouldRejectNumbersStartingWithZero() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("01111111116"); // Starts with 0

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "tcKimlik");

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectNonNumericCharacters() {
        TestObject obj = new TestObject();
        obj.setTcKimlik("1111111111a"); // Contains letter

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "tcKimlik");

        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldRejectEmptyString() {
        TestObject obj = new TestObject();
        obj.setRequiredTcKimlik("");

        Set<ConstraintViolation<TestObject>> violations = validator.validateProperty(obj, "requiredTcKimlik");

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("boş olamaz");
    }
}
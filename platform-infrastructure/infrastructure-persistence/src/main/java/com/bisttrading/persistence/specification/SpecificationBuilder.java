package com.bisttrading.persistence.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Specification builder for dynamic query construction
 * Provides fluent API for building JPA Criteria queries
 *
 * @param <T> Entity type
 */
public class SpecificationBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    private SpecificationBuilder() {}

    /**
     * Create a new specification builder
     *
     * @param <T> Entity type
     * @return New specification builder instance
     */
    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /**
     * Add custom specification
     *
     * @param spec Custom specification
     * @return This builder instance
     */
    public SpecificationBuilder<T> with(Specification<T> spec) {
        if (spec != null) {
            specifications.add(spec);
        }
        return this;
    }

    /**
     * Add equals condition
     *
     * @param field Field name
     * @param value Field value
     * @return This builder instance
     */
    public SpecificationBuilder<T> equals(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * Add not equals condition
     *
     * @param field Field name
     * @param value Field value
     * @return This builder instance
     */
    public SpecificationBuilder<T> notEquals(String field, Object value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get(field), value));
        }
        return this;
    }

    /**
     * Add like condition (case-insensitive)
     *
     * @param field Field name
     * @param value Search value
     * @return This builder instance
     */
    public SpecificationBuilder<T> like(String field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(field)),
                    "%" + value.toLowerCase() + "%"
                ));
        }
        return this;
    }

    /**
     * Add starts with condition
     *
     * @param field Field name
     * @param value Prefix value
     * @return This builder instance
     */
    public SpecificationBuilder<T> startsWith(String field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(field)),
                    value.toLowerCase() + "%"
                ));
        }
        return this;
    }

    /**
     * Add IN condition
     *
     * @param field Field name
     * @param values Collection of values
     * @return This builder instance
     */
    public SpecificationBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add((root, query, criteriaBuilder) ->
                root.get(field).in(values));
        }
        return this;
    }

    /**
     * Add NOT IN condition
     *
     * @param field Field name
     * @param values Collection of values
     * @return This builder instance
     */
    public SpecificationBuilder<T> notIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.not(root.get(field).in(values)));
        }
        return this;
    }

    /**
     * Add greater than condition
     *
     * @param field Field name
     * @param value Comparison value
     * @return This builder instance
     */
    @SuppressWarnings("unchecked")
    public SpecificationBuilder<T> greaterThan(String field, Comparable value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get(field), value));
        }
        return this;
    }

    /**
     * Add greater than or equal condition
     *
     * @param field Field name
     * @param value Comparison value
     * @return This builder instance
     */
    @SuppressWarnings("unchecked")
    public SpecificationBuilder<T> greaterThanOrEqual(String field, Comparable value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * Add less than condition
     *
     * @param field Field name
     * @param value Comparison value
     * @return This builder instance
     */
    @SuppressWarnings("unchecked")
    public SpecificationBuilder<T> lessThan(String field, Comparable value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get(field), value));
        }
        return this;
    }

    /**
     * Add less than or equal condition
     *
     * @param field Field name
     * @param value Comparison value
     * @return This builder instance
     */
    @SuppressWarnings("unchecked")
    public SpecificationBuilder<T> lessThanOrEqual(String field, Comparable value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get(field), value));
        }
        return this;
    }

    /**
     * Add between condition
     *
     * @param field Field name
     * @param start Start value
     * @param end End value
     * @return This builder instance
     */
    @SuppressWarnings("unchecked")
    public SpecificationBuilder<T> between(String field, Comparable start, Comparable end) {
        if (start != null && end != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get(field), start, end));
        }
        return this;
    }

    /**
     * Add IS NULL condition
     *
     * @param field Field name
     * @return This builder instance
     */
    public SpecificationBuilder<T> isNull(String field) {
        specifications.add((root, query, criteriaBuilder) ->
            criteriaBuilder.isNull(root.get(field)));
        return this;
    }

    /**
     * Add IS NOT NULL condition
     *
     * @param field Field name
     * @return This builder instance
     */
    public SpecificationBuilder<T> isNotNull(String field) {
        specifications.add((root, query, criteriaBuilder) ->
            criteriaBuilder.isNotNull(root.get(field)));
        return this;
    }

    /**
     * Add date range condition
     *
     * @param field Date field name
     * @param startDate Start date
     * @param endDate End date
     * @return This builder instance
     */
    public SpecificationBuilder<T> dateRange(String field, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get(field), startDate, endDate));
        } else if (startDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get(field), startDate));
        } else if (endDate != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get(field), endDate));
        }
        return this;
    }

    /**
     * Add timestamp range condition
     *
     * @param field Timestamp field name
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return This builder instance
     */
    public SpecificationBuilder<T> timestampRange(String field, Instant startTime, Instant endTime) {
        if (startTime != null && endTime != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.get(field), startTime, endTime));
        } else if (startTime != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get(field), startTime));
        } else if (endTime != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get(field), endTime));
        }
        return this;
    }

    /**
     * Add active records only condition
     *
     * @return This builder instance
     */
    public SpecificationBuilder<T> activeOnly() {
        specifications.add((root, query, criteriaBuilder) -> {
            try {
                return criteriaBuilder.equal(root.get("isActive"), true);
            } catch (Exception e) {
                // If isActive field doesn't exist, return always true condition
                return criteriaBuilder.conjunction();
            }
        });
        return this;
    }

    /**
     * Add inactive records only condition
     *
     * @return This builder instance
     */
    public SpecificationBuilder<T> inactiveOnly() {
        specifications.add((root, query, criteriaBuilder) -> {
            try {
                return criteriaBuilder.equal(root.get("isActive"), false);
            } catch (Exception e) {
                // If isActive field doesn't exist, return always false condition
                return criteriaBuilder.disjunction();
            }
        });
        return this;
    }

    /**
     * Add join condition
     *
     * @param joinField Join field name
     * @param targetField Target field name in joined entity
     * @param value Value to compare
     * @return This builder instance
     */
    public SpecificationBuilder<T> join(String joinField, String targetField, Object value) {
        if (value != null) {
            specifications.add((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.join(joinField).get(targetField), value));
        }
        return this;
    }

    /**
     * Build the final specification
     *
     * @return Combined specification
     */
    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return null;
        }

        Specification<T> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = result.and(specifications.get(i));
        }

        return result;
    }

    /**
     * Build OR specification instead of AND
     *
     * @return Combined specification with OR logic
     */
    public Specification<T> buildOr() {
        if (specifications.isEmpty()) {
            return null;
        }

        Specification<T> result = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            result = result.or(specifications.get(i));
        }

        return result;
    }

    /**
     * Create a custom specification
     *
     * @param spec Custom specification function
     * @param <T> Entity type
     * @return Custom specification
     */
    public static <T> Specification<T> custom(TriFunction<Root<T>, CriteriaQuery<?>, CriteriaBuilder, Predicate> spec) {
        return spec::apply;
    }

    /**
     * Functional interface for custom specifications
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
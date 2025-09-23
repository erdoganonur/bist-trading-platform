package com.bisttrading.core.domain.specifications;

/**
 * Base specification interface following the Specification pattern.
 * Encapsulates business rules that can be combined and reused.
 *
 * @param <T> The type being evaluated
 */
public interface Specification<T> {

    /**
     * Checks if the candidate satisfies the specification.
     *
     * @param candidate The object to evaluate
     * @return true if the specification is satisfied
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * Creates a specification that represents the logical AND of this and another specification.
     *
     * @param other The other specification
     * @return Combined AND specification
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /**
     * Creates a specification that represents the logical OR of this and another specification.
     *
     * @param other The other specification
     * @return Combined OR specification
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * Creates a specification that represents the logical NOT of this specification.
     *
     * @return Negated specification
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }

    /**
     * AND specification implementation.
     */
    record AndSpecification<T>(Specification<T> left, Specification<T> right) implements Specification<T> {
        @Override
        public boolean isSatisfiedBy(T candidate) {
            return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate);
        }
    }

    /**
     * OR specification implementation.
     */
    record OrSpecification<T>(Specification<T> left, Specification<T> right) implements Specification<T> {
        @Override
        public boolean isSatisfiedBy(T candidate) {
            return left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate);
        }
    }

    /**
     * NOT specification implementation.
     */
    record NotSpecification<T>(Specification<T> specification) implements Specification<T> {
        @Override
        public boolean isSatisfiedBy(T candidate) {
            return !specification.isSatisfiedBy(candidate);
        }
    }
}
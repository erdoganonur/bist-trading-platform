package com.bisttrading.broker.algolab.exception;

import java.util.List;

public class AlgoLabValidationException extends AlgoLabException {

    private final List<String> validationErrors;

    public AlgoLabValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
        this.validationErrors = null;
    }

    public AlgoLabValidationException(String message, List<String> validationErrors) {
        super(message, "VALIDATION_ERROR", 400);
        this.validationErrors = validationErrors;
    }

    public AlgoLabValidationException(String message, Throwable cause) {
        super(message, "VALIDATION_ERROR", 400, cause);
        this.validationErrors = null;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());

        if (hasValidationErrors()) {
            sb.append(" [ValidationErrors: ");
            sb.append(String.join(", ", validationErrors));
            sb.append("]");
        }

        return sb.toString();
    }
}
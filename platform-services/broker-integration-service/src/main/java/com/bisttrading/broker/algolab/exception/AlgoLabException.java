package com.bisttrading.broker.algolab.exception;

public class AlgoLabException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public AlgoLabException(String message) {
        super(message);
        this.errorCode = null;
        this.httpStatus = 0;
    }

    public AlgoLabException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatus = 0;
    }

    public AlgoLabException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 0;
    }

    public AlgoLabException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public AlgoLabException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.trim().isEmpty();
    }

    public boolean hasHttpStatus() {
        return httpStatus > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());

        if (hasErrorCode()) {
            sb.append(" [ErrorCode: ").append(errorCode).append("]");
        }

        if (hasHttpStatus()) {
            sb.append(" [HttpStatus: ").append(httpStatus).append("]");
        }

        return sb.toString();
    }
}
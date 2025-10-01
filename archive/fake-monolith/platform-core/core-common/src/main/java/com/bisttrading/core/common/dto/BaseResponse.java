package com.bisttrading.core.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base response wrapper for all API responses.
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * Indicates if the operation was successful.
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Human-readable message describing the result.
     */
    private String message;

    /**
     * The actual data payload.
     */
    private T data;

    /**
     * Timestamp when the response was created.
     */
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Optional metadata about the response.
     */
    private ResponseMetadata metadata;

    /**
     * Creates a successful response with data.
     *
     * @param data The response data
     * @param <T>  The type of data
     * @return BaseResponse with success=true
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage("İşlem başarılı");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates a successful response with data and custom message.
     *
     * @param data    The response data
     * @param message Custom success message
     * @param <T>     The type of data
     * @return BaseResponse with success=true
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates a successful response with only a message.
     *
     * @param message Success message
     * @param <T>     The type of data
     * @return BaseResponse with success=true and no data
     */
    public static <T> BaseResponse<T> success(String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates a successful response with default message.
     *
     * @param <T> The type of data
     * @return BaseResponse with success=true and default message
     */
    public static <T> BaseResponse<T> success() {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(true);
        response.setMessage("İşlem başarılı");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates a failed response with error message.
     *
     * @param message Error message
     * @param <T>     The type of data
     * @return BaseResponse with success=false
     */
    public static <T> BaseResponse<T> error(String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates a failed response with error message and data.
     *
     * @param message Error message
     * @param data    Error data (e.g., validation errors)
     * @param <T>     The type of data
     * @return BaseResponse with success=false
     */
    public static <T> BaseResponse<T> error(String message, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Adds metadata to the response.
     *
     * @param metadata Response metadata
     * @return This BaseResponse for method chaining
     */
    public BaseResponse<T> withMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Checks if the response indicates success.
     *
     * @return true if success is true
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if the response indicates failure.
     *
     * @return true if success is false
     */
    public boolean isError() {
        return !success;
    }

    /**
     * Checks if the response has data.
     *
     * @return true if data is not null
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Checks if the response has metadata.
     *
     * @return true if metadata is not null
     */
    public boolean hasMetadata() {
        return metadata != null;
    }

    /**
     * Metadata class for additional response information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseMetadata {

        /**
         * Request ID for tracing.
         */
        private String requestId;

        /**
         * Processing time in milliseconds.
         */
        private Long processingTimeMs;

        /**
         * API version.
         */
        private String version;

        /**
         * Server instance identifier.
         */
        private String serverInstance;

        /**
         * Additional custom metadata.
         */
        private Object additionalData;

        /**
         * Creates metadata with request ID.
         *
         * @param requestId The request ID
         * @return ResponseMetadata
         */
        public static ResponseMetadata withRequestId(String requestId) {
            ResponseMetadata metadata = new ResponseMetadata();
            metadata.setRequestId(requestId);
            return metadata;
        }

        /**
         * Creates metadata with processing time.
         *
         * @param processingTimeMs Processing time in milliseconds
         * @return ResponseMetadata
         */
        public static ResponseMetadata withProcessingTime(Long processingTimeMs) {
            ResponseMetadata metadata = new ResponseMetadata();
            metadata.setProcessingTimeMs(processingTimeMs);
            return metadata;
        }

        /**
         * Creates metadata with request ID and processing time.
         *
         * @param requestId        The request ID
         * @param processingTimeMs Processing time in milliseconds
         * @return ResponseMetadata
         */
        public static ResponseMetadata of(String requestId, Long processingTimeMs) {
            ResponseMetadata metadata = new ResponseMetadata();
            metadata.setRequestId(requestId);
            metadata.setProcessingTimeMs(processingTimeMs);
            return metadata;
        }
    }
}
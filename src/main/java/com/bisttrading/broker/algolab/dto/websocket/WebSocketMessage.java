package com.bisttrading.broker.algolab.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic WebSocket message wrapper from AlgoLab.
 * Contains message type and payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessage<T> {

    /**
     * Message type (tick, orderbook, trade, error, ping, pong, etc.)
     */
    @JsonProperty("type")
    private String type;

    /**
     * Channel or topic
     */
    @JsonProperty("channel")
    private String channel;

    /**
     * Message payload
     */
    @JsonProperty("data")
    private T data;

    /**
     * Error message (if type is error)
     */
    @JsonProperty("error")
    private String error;

    /**
     * Request ID for correlation
     */
    @JsonProperty("requestId")
    private String requestId;

    /**
     * Message types
     */
    public static class Type {
        public static final String TICK = "tick";
        public static final String ORDER_BOOK = "orderbook";
        public static final String TRADE = "trade";
        public static final String ERROR = "error";
        public static final String PING = "ping";
        public static final String PONG = "pong";
        public static final String SUBSCRIBE = "subscribe";
        public static final String UNSUBSCRIBE = "unsubscribe";
        public static final String AUTH = "auth";
        public static final String AUTH_SUCCESS = "auth_success";
        public static final String AUTH_FAILURE = "auth_failure";
    }

    /**
     * Channel types
     */
    public static class Channel {
        public static final String TICK = "tick";
        public static final String ORDER_BOOK = "orderbook";
        public static final String TRADE = "trade";
        public static final String ORDER_UPDATE = "order_update";
    }
}

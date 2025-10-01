package com.bisttrading.broker.websocket.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.Instant;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MarketDataMessage.class, name = "market_data"),
    @JsonSubTypes.Type(value = OrderUpdateMessage.class, name = "order_update"),
    @JsonSubTypes.Type(value = PortfolioUpdateMessage.class, name = "portfolio_update"),
    @JsonSubTypes.Type(value = TradeMessage.class, name = "trade"),
    @JsonSubTypes.Type(value = HeartbeatMessage.class, name = "pong"),
    @JsonSubTypes.Type(value = ErrorMessage.class, name = "error"),
    @JsonSubTypes.Type(value = SubscriptionConfirmationMessage.class, name = "subscription_confirmed"),
    @JsonSubTypes.Type(value = UnsubscriptionConfirmationMessage.class, name = "unsubscription_confirmed")
})
public abstract class WebSocketMessage {

    @JsonProperty("type")
    private String type;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("channel")
    private String channel;
}
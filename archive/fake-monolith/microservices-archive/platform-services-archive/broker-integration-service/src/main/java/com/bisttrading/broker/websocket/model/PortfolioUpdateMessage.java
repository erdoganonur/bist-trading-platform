package com.bisttrading.broker.websocket.model;

import com.bisttrading.broker.algolab.model.AccountInfo;
import com.bisttrading.broker.algolab.model.Position;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioUpdateMessage extends WebSocketMessage {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("account_info")
    private AccountInfo accountInfo;

    @JsonProperty("positions")
    private List<Position> positions;

    @JsonProperty("update_type")
    private String updateType; // "balance_change", "position_change", "pnl_change", etc.
}
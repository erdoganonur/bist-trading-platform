package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
public abstract class PortfolioEvent {
    private String userId;
    private Instant timestamp;
}
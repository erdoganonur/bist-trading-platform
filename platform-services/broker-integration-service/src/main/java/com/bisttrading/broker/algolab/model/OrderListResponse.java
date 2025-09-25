package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class OrderListResponse {

    @JsonProperty("orders")
    private List<OrderResponse> orders;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("page_size")
    private Integer pageSize;

    @JsonProperty("has_more")
    private Boolean hasMore;
}
package com.bisttrading.broker.controller;

import com.bisttrading.broker.adapter.BrokerAdapter;
import com.bisttrading.broker.dto.*;
import com.bisttrading.broker.service.AlgoLabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Broker Integration REST Controller for BIST Trading Platform.
 * Handles trading operations, order management, and portfolio operations through AlgoLab integration.
 *
 * Base URL: /api/v1/broker
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/broker")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Broker Integration", description = "AlgoLab broker entegrasyonu ve işlem yönetimi")
public class BrokerController {

    private final AlgoLabService algoLabService;
    private final BrokerAdapter brokerAdapter;

    /**
     * Places a new order through the broker.
     *
     * @param orderRequest Order details
     * @param authentication Current user's authentication
     * @return Order response with broker order ID
     */
    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('trading:place')")
    @Operation(
        summary = "Yeni emir ver",
        description = "AlgoLab aracılığıyla yeni bir alım/satım emri verir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Emir başarıyla verildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AlgoLabResponse.class),
                examples = @ExampleObject(
                    name = "Başarılı emir",
                    value = """
                    {
                        "success": true,
                        "content": {
                            "orderId": "ORD-123456789",
                            "brokerOrderId": "ALG-987654321",
                            "symbol": "AKBNK",
                            "side": "BUY",
                            "quantity": 1000,
                            "price": 15.75,
                            "status": "SUBMITTED"
                        },
                        "message": "Emir başarıyla verildi",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Geçersiz emir verisi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                    {
                        "success": false,
                        "error": "VALIDATION_ERROR",
                        "message": "Hisse senedi sembolü geçersiz",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "402",
            description = "Yetersiz bakiye",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Insufficient balance",
                    value = """
                    {
                        "success": false,
                        "error": "INSUFFICIENT_BALANCE",
                        "message": "Yetersiz hesap bakiyesi",
                        "details": "Gerekli: 15,750.00 TL, Mevcut: 10,000.00 TL",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Emir verme yetkisi yok",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> placeOrder(
            @Valid @RequestBody
            @Parameter(description = "Emir detayları", required = true)
            SendOrderRequest orderRequest,
            Authentication authentication) {

        log.info("Order placement request from user: {} for symbol: {}",
            authentication.getName(), orderRequest.getSymbol());

        // For now, call the stub implementation
        AlgoLabResponse<Object> response = algoLabService.sendOrder(
            orderRequest.getSymbol(),
            orderRequest.getDirection(),
            orderRequest.getPriceType(),
            orderRequest.getPrice(),
            orderRequest.getLot(),
            orderRequest.getSms(),
            orderRequest.getEmail(),
            orderRequest.getSubAccount()
        );

        if (response.isSuccess()) {
            log.info("Order placed successfully for user: {}", authentication.getName());
            return ResponseEntity.status(201).body(response);
        } else {
            log.warn("Order placement failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Modifies an existing order.
     *
     * @param orderId Order ID to modify
     * @param modifyRequest Modification details
     * @param authentication Current user's authentication
     * @return Modified order response
     */
    @PutMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:modify')")
    @Operation(
        summary = "Emri değiştir",
        description = "Mevcut bir emrin fiyat ve/veya miktarını değiştirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Emir başarıyla değiştirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AlgoLabResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Emir bulunamadı",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Order not found",
                    value = """
                    {
                        "success": false,
                        "error": "ORDER_NOT_FOUND",
                        "message": "Emir bulunamadı veya değiştirilemez durumda",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> modifyOrder(
            @PathVariable
            @Parameter(description = "Değiştirilecek emir ID'si", required = true)
            String orderId,
            @Valid @RequestBody
            @Parameter(description = "Emir değiştirme detayları", required = true)
            ModifyOrderRequest modifyRequest,
            Authentication authentication) {

        log.info("Order modification request from user: {} for order: {}",
            authentication.getName(), orderId);

        AlgoLabResponse<Object> response = algoLabService.modifyOrder(
            orderId,
            modifyRequest.getPrice(),
            modifyRequest.getLot(),
            modifyRequest.getViop(),
            modifyRequest.getSubAccount()
        );

        if (response.isSuccess()) {
            log.info("Order modified successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Order modification failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancels an existing order.
     *
     * @param orderId Order ID to cancel
     * @param authentication Current user's authentication
     * @return Cancellation response
     */
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:cancel')")
    @Operation(
        summary = "Emri iptal et",
        description = "Bekleyen bir emri iptal eder"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Emir başarıyla iptal edildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Order cancelled",
                    value = """
                    {
                        "success": true,
                        "content": {
                            "orderId": "ORD-123456789",
                            "status": "CANCELLED",
                            "cancelledAt": "2024-09-24T10:45:00Z"
                        },
                        "message": "Emir başarıyla iptal edildi",
                        "timestamp": "2024-09-24T10:45:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Emir bulunamadı",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Emir iptal edilemez durumda",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Cannot cancel",
                    value = """
                    {
                        "success": false,
                        "error": "CANNOT_CANCEL",
                        "message": "Emir zaten gerçekleştirilmiş veya iptal edilemiyor",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> cancelOrder(
            @PathVariable
            @Parameter(description = "İptal edilecek emir ID'si", required = true)
            String orderId,
            Authentication authentication) {

        log.info("Order cancellation request from user: {} for order: {}",
            authentication.getName(), orderId);

        AlgoLabResponse<Object> response = algoLabService.deleteOrder(orderId, null);

        if (response.isSuccess()) {
            log.info("Order cancelled successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Order cancellation failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Gets user's portfolio positions.
     *
     * @param authentication Current user's authentication
     * @return Portfolio positions
     */
    @GetMapping("/portfolio")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "Portföy pozisyonlarını getir",
        description = "Kullanıcının mevcut portföy pozisyonlarını ve değerlerini getirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Portföy bilgileri başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Portfolio data",
                    value = """
                    {
                        "success": true,
                        "content": {
                            "totalValue": 125750.50,
                            "totalCost": 120000.00,
                            "totalPnl": 5750.50,
                            "totalPnlPercent": 4.79,
                            "cashBalance": 25000.00,
                            "positions": [
                                {
                                    "symbol": "AKBNK",
                                    "quantity": 1000,
                                    "averagePrice": 15.50,
                                    "currentPrice": 15.75,
                                    "marketValue": 15750.00,
                                    "pnl": 250.00,
                                    "pnlPercent": 1.61
                                }
                            ]
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<List<Map<String, Object>>> getPortfolio(Authentication authentication) {
        log.debug("Portfolio request from user: {}", authentication.getName());

        List<Map<String, Object>> positions = brokerAdapter.getPositions();

        log.debug("Portfolio data retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(positions);
    }

    /**
     * Gets user's transaction history.
     *
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param symbol Symbol filter (optional)
     * @param authentication Current user's authentication
     * @return Transaction history
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "İşlem geçmişini getir",
        description = "Kullanıcının gerçekleşen işlem geçmişini getirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "İşlem geçmişi başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Transaction history",
                    value = """
                    {
                        "success": true,
                        "content": {
                            "transactions": [
                                {
                                    "transactionId": "TXN-789123456",
                                    "orderId": "ORD-123456789",
                                    "symbol": "AKBNK",
                                    "type": "BUY",
                                    "quantity": 1000,
                                    "price": 15.75,
                                    "amount": 15750.00,
                                    "commission": 7.87,
                                    "netAmount": 15757.87,
                                    "timestamp": "2024-09-24T10:35:00Z"
                                }
                            ],
                            "total": 15,
                            "summary": {
                                "totalBuyAmount": 95000.00,
                                "totalSellAmount": 0.00,
                                "totalCommissions": 47.50
                            }
                        }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> getTransactions(
            @RequestParam(required = false)
            @Parameter(description = "Başlangıç tarihi (YYYY-MM-DD)")
            String startDate,
            @RequestParam(required = false)
            @Parameter(description = "Bitiş tarihi (YYYY-MM-DD)")
            String endDate,
            @RequestParam(required = false)
            @Parameter(description = "Hisse senedi sembolü filtresi")
            String symbol,
            Authentication authentication) {

        log.debug("Transaction history request from user: {}", authentication.getName());

        AlgoLabResponse<Object> response = algoLabService.getTodaysTransactions(null);

        log.debug("Transaction history retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets instant positions (real-time portfolio).
     *
     * @param authentication Current user's authentication
     * @return Real-time position data
     */
    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "Anlık pozisyonları getir",
        description = "Kullanıcının anlık portföy pozisyonlarını getirir"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Anlık pozisyonlar başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AlgoLabResponse.class)
            )
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> getInstantPositions(Authentication authentication) {
        log.debug("Instant positions request from user: {}", authentication.getName());

        AlgoLabResponse<Object> response = algoLabService.getInstantPosition(null);

        log.debug("Instant positions retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets broker connection status.
     *
     * @param authentication Current user's authentication
     * @return Broker connection status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Broker bağlantı durumu",
        description = "AlgoLab broker bağlantısının durumunu kontrol eder"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bağlantı durumu başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Connection status",
                    value = """
                    {
                        "connected": true,
                        "authenticated": true,
                        "lastCheckTime": "2024-09-24T10:30:00Z",
                        "sessionExpiresAt": "2024-09-25T10:30:00Z",
                        "brokerName": "AlgoLab"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getBrokerStatus(Authentication authentication) {
        log.debug("Broker status request from user: {}", authentication.getName());

        boolean isConnected = brokerAdapter.isConnected();
        boolean isAuthenticated = algoLabService.isAuthenticated();

        Map<String, Object> status = Map.of(
            "connected", isConnected,
            "authenticated", isAuthenticated,
            "lastCheckTime", java.time.Instant.now(),
            "brokerName", "AlgoLab"
        );

        log.debug("Broker status retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(status);
    }

    /**
     * Gets available Turkish stock symbols.
     *
     * @return List of available BIST symbols
     */
    @GetMapping("/symbols")
    @PreAuthorize("hasAuthority('market:read')")
    @Operation(
        summary = "Mevcut hisse senetleri",
        description = "İşlem yapılabilecek Türk hisse senetlerini listeler"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Hisse senetleri başarıyla listelendi",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Available symbols",
                    value = """
                    {
                        "symbols": [
                            {
                                "symbol": "AKBNK",
                                "name": "Akbank T.A.Ş.",
                                "exchange": "BIST",
                                "sector": "Banking",
                                "currency": "TRY"
                            },
                            {
                                "symbol": "THYAO",
                                "name": "Türk Hava Yolları A.O.",
                                "exchange": "BIST",
                                "sector": "Transportation",
                                "currency": "TRY"
                            }
                        ]
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getAvailableSymbols() {
        log.debug("Available symbols request");

        // Mock Turkish stock symbols for now
        List<Map<String, String>> symbols = List.of(
            Map.of("symbol", "AKBNK", "name", "Akbank T.A.Ş.", "exchange", "BIST", "sector", "Banking", "currency", "TRY"),
            Map.of("symbol", "THYAO", "name", "Türk Hava Yolları A.O.", "exchange", "BIST", "sector", "Transportation", "currency", "TRY"),
            Map.of("symbol", "GARAN", "name", "Türkiye Garanti Bankası A.Ş.", "exchange", "BIST", "sector", "Banking", "currency", "TRY"),
            Map.of("symbol", "ISCTR", "name", "Türkiye İş Bankası A.Ş.", "exchange", "BIST", "sector", "Banking", "currency", "TRY"),
            Map.of("symbol", "SAHOL", "name", "Hacı Ömer Sabancı Holding A.Ş.", "exchange", "BIST", "sector", "Holding", "currency", "TRY")
        );

        Map<String, Object> response = Map.of(
            "symbols", symbols,
            "total", symbols.size(),
            "exchange", "BIST"
        );

        return ResponseEntity.ok(response);
    }
}
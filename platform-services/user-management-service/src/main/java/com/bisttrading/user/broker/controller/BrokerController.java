package com.bisttrading.user.broker.controller;

import com.bisttrading.user.broker.dto.AlgoLabResponse;
import com.bisttrading.user.broker.dto.ModifyOrderRequest;
import com.bisttrading.user.broker.dto.SendOrderRequest;
import com.bisttrading.user.broker.service.SimplifiedBrokerService;
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
 * Simplified Broker Integration REST Controller for BIST Trading Platform.
 * Handles trading operations, order management, and portfolio operations.
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

    private final SimplifiedBrokerService brokerService;

    /**
     * Places a new order through the broker.
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
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> placeOrder(
            @Valid @RequestBody
            @Parameter(description = "Emir detayları", required = true)
            SendOrderRequest orderRequest,
            Authentication authentication) {

        log.info("Order placement request from user: {} for symbol: {}",
            authentication.getName(), orderRequest.getSymbol());

        AlgoLabResponse<Object> response = brokerService.sendOrder(
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
     */
    @PutMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:modify')")
    @Operation(
        summary = "Emri değiştir",
        description = "Mevcut bir emrin fiyat ve/veya miktarını değiştirir"
    )
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

        AlgoLabResponse<Object> response = brokerService.modifyOrder(
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
     */
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:cancel')")
    @Operation(
        summary = "Emri iptal et",
        description = "Bekleyen bir emri iptal eder"
    )
    public ResponseEntity<AlgoLabResponse<Object>> cancelOrder(
            @PathVariable
            @Parameter(description = "İptal edilecek emir ID'si", required = true)
            String orderId,
            Authentication authentication) {

        log.info("Order cancellation request from user: {} for order: {}",
            authentication.getName(), orderId);

        AlgoLabResponse<Object> response = brokerService.deleteOrder(orderId, null);

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
     */
    @GetMapping("/portfolio")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "Portföy pozisyonlarını getir",
        description = "Kullanıcının mevcut portföy pozisyonlarını ve değerlerini getirir"
    )
    public ResponseEntity<List<Map<String, Object>>> getPortfolio(Authentication authentication) {
        log.debug("Portfolio request from user: {}", authentication.getName());

        List<Map<String, Object>> positions = brokerService.getPositions();

        log.debug("Portfolio data retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(positions);
    }

    /**
     * Gets user's transaction history.
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "İşlem geçmişini getir",
        description = "Kullanıcının gerçekleşen işlem geçmişini getirir"
    )
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

        AlgoLabResponse<Object> response = brokerService.getTodaysTransactions(null);

        log.debug("Transaction history retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets instant positions (real-time portfolio).
     */
    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('portfolio:read')")
    @Operation(
        summary = "Anlık pozisyonları getir",
        description = "Kullanıcının anlık portföy pozisyonlarını getirir"
    )
    public ResponseEntity<AlgoLabResponse<Object>> getInstantPositions(Authentication authentication) {
        log.debug("Instant positions request from user: {}", authentication.getName());

        AlgoLabResponse<Object> response = brokerService.getInstantPosition(null);

        log.debug("Instant positions retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets broker connection status.
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Broker bağlantı durumu",
        description = "AlgoLab broker bağlantısının durumunu kontrol eder"
    )
    public ResponseEntity<Map<String, Object>> getBrokerStatus(Authentication authentication) {
        log.debug("Broker status request from user: {}", authentication.getName());

        boolean isConnected = brokerService.isConnected();
        boolean isAuthenticated = brokerService.isAuthenticated();

        Map<String, Object> status = Map.of(
            "connected", isConnected,
            "authenticated", isAuthenticated,
            "lastCheckTime", java.time.Instant.now(),
            "brokerName", "AlgoLab (Simplified)"
        );

        log.debug("Broker status retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(status);
    }

    /**
     * Gets available Turkish stock symbols.
     */
    @GetMapping("/symbols")
    @PreAuthorize("hasAuthority('market:read')")
    @Operation(
        summary = "Mevcut hisse senetleri",
        description = "İşlem yapılabilecek Türk hisse senetlerini listeler"
    )
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
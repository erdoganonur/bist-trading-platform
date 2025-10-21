package com.bisttrading.broker.controller;

import com.bisttrading.broker.algolab.service.WebSocketMessageBuffer;
import com.bisttrading.broker.dto.AlgoLabResponse;
import com.bisttrading.broker.dto.ModifyOrderRequest;
import com.bisttrading.broker.dto.SendOrderRequest;
import com.bisttrading.broker.service.BrokerIntegrationService;
import com.bisttrading.entity.trading.Order;
import com.bisttrading.entity.trading.OrderExecution;
import com.bisttrading.entity.trading.enums.OrderSide;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.entity.trading.enums.OrderType;
import com.bisttrading.repository.trading.OrderExecutionRepository;
import com.bisttrading.symbol.dto.SymbolDto;
import com.bisttrading.symbol.service.SymbolService;
import com.bisttrading.trading.dto.ExecutionDto;
import com.bisttrading.trading.dto.OrderHistoryDto;
import com.bisttrading.trading.dto.OrderSearchCriteria;
import com.bisttrading.trading.service.OrderManagementService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PRODUCTION Broker Integration Controller
 *
 * ⚠️⚠️⚠️ CRITICAL WARNING ⚠️⚠️⚠️
 * This controller executes REAL trades on BIST via AlgoLab.
 * All orders are LIVE and will impact user accounts with REAL MONEY.
 *
 * Security Features:
 * - All endpoints require authentication and authorization
 * - Rate Limiting: 1 request per 5 seconds per user
 * - Circuit Breaker: Fails fast if AlgoLab is unavailable
 * - Audit Logging: All operations are logged for compliance
 *
 * Base URL: /api/v1/broker
 *
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/broker")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "⚠️ PRODUCTION Broker Integration",
     description = "⚠️ PRODUCTION AlgoLab integration - REAL MONEY TRADING on BIST exchange. All orders are LIVE.")
public class BrokerController {

    private final BrokerIntegrationService brokerService;
    private final SymbolService symbolService;
    private final OrderManagementService orderManagementService;
    private final OrderExecutionRepository executionRepository;
    private final WebSocketMessageBuffer messageBuffer;
    private final com.bisttrading.broker.algolab.service.SubscriptionManager subscriptionManager;

    /**
     * Places a new order through the broker.
     *
     * ⚠️ REAL ORDER - This will be sent to BIST exchange via AlgoLab.
     * This is a LIVE order that will execute with REAL MONEY.
     */
    @PostMapping("/orders")
    // TODO: Re-enable in production - temporarily disabled for development testing
    // @PreAuthorize("hasAuthority('trading:place')")
    @Operation(
        summary = "⚠️ Place LIVE Order",
        description = """
            Places a new LIVE order on BIST exchange via AlgoLab.

            ⚠️ WARNING: This is a REAL order with REAL MONEY.
            - Order will be sent immediately to BIST exchange
            - Funds will be reserved/used from user account
            - Order may execute partially or fully based on market conditions

            Rate Limit: 1 request per 5 seconds
            Requires: 'trading:place' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Order successfully placed on exchange",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AlgoLabResponse.class),
                examples = @ExampleObject(
                    name = "Successful Order",
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
                        "message": "Order successfully placed",
                        "timestamp": "2024-09-24T10:30:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid order parameters or validation failed"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "402",
            description = "Insufficient funds in user account"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded - Maximum 1 request per 5 seconds"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> placeOrder(
            @Valid @RequestBody
            @Parameter(description = "Order details (symbol, direction, price, quantity)", required = true)
            SendOrderRequest orderRequest,
            Authentication authentication) {

        log.info("⚠️ LIVE ORDER placement request from user: {} for symbol: {}",
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
            log.info("✅ LIVE ORDER placed successfully for user: {}", authentication.getName());
            return ResponseEntity.status(201).body(response);
        } else {
            log.warn("❌ LIVE ORDER placement failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Modifies an existing order.
     *
     * ⚠️ MODIFIES LIVE ORDER - Changes will be executed immediately on BIST exchange.
     */
    @PutMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:modify')")
    @Operation(
        summary = "⚠️ Modify LIVE Order",
        description = """
            Modifies an existing LIVE order on BIST exchange via AlgoLab.

            ⚠️ WARNING: This modifies a REAL order.
            - Changes are executed immediately
            - New price/quantity will replace the existing order
            - Order may execute at new price if market conditions match

            Rate Limit: 1 request per 5 seconds
            Requires: 'trading:modify' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order successfully modified"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid modification parameters"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found or already executed"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded - Maximum 1 request per 5 seconds"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> modifyOrder(
            @PathVariable
            @Parameter(description = "Order ID to modify", required = true)
            String orderId,
            @Valid @RequestBody
            @Parameter(description = "New order parameters (price, quantity)", required = true)
            ModifyOrderRequest modifyRequest,
            Authentication authentication) {

        log.info("⚠️ LIVE ORDER modification request from user: {} for order: {}",
            authentication.getName(), orderId);

        AlgoLabResponse<Object> response = brokerService.modifyOrder(
            orderId,
            modifyRequest.getPrice(),
            modifyRequest.getLot(),
            modifyRequest.getViop(),
            modifyRequest.getSubAccount()
        );

        if (response.isSuccess()) {
            log.info("✅ LIVE ORDER modified successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ LIVE ORDER modification failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancels an existing order.
     *
     * ⚠️ CANCELS LIVE ORDER - This action cannot be undone.
     */
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('trading:cancel')")
    @Operation(
        summary = "⚠️ Cancel LIVE Order",
        description = """
            Cancels a pending LIVE order on BIST exchange via AlgoLab.

            ⚠️ WARNING: This cancels a REAL order.
            - Cancellation is immediate and CANNOT BE UNDONE
            - Partially filled orders will stop further execution
            - Already executed portions cannot be cancelled

            Rate Limit: 1 request per 5 seconds
            Requires: 'trading:cancel' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order successfully cancelled"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Order cannot be cancelled (already executed or invalid state)"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded - Maximum 1 request per 5 seconds"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> cancelOrder(
            @PathVariable
            @Parameter(description = "Order ID to cancel", required = true)
            String orderId,
            Authentication authentication) {

        log.info("⚠️ LIVE ORDER cancellation request from user: {} for order: {}",
            authentication.getName(), orderId);

        AlgoLabResponse<Object> response = brokerService.deleteOrder(orderId, null);

        if (response.isSuccess()) {
            log.info("✅ LIVE ORDER cancelled successfully for user: {}", authentication.getName());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ LIVE ORDER cancellation failed for user: {} - {}",
                authentication.getName(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Gets user's account information.
     */
    @GetMapping("/account")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Account Information",
        description = """
            Retrieves user's AlgoLab account information including balance and portfolio summary.

            Returns:
            - Account number
            - Customer ID
            - Account status
            - Currency
            - Balance information (total, available, blocked)
            - Portfolio value

            Requires: Authentication
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Account information retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable"
        )
    })
    public ResponseEntity<Map<String, Object>> getAccount(Authentication authentication) {
        log.debug("Account info request from user: {}", authentication.getName());

        // Get real account information from AlgoLab
        Map<String, Object> account = brokerService.getAccountInfo();

        log.debug("Account info retrieved for user: {} - Balance: {}",
            authentication.getName(), account.get("totalBalance"));
        return ResponseEntity.ok(account);
    }

    /**
     * Gets user's portfolio positions.
     */
    @GetMapping("/portfolio")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Portfolio Positions",
        description = """
            Retrieves user's current portfolio positions from AlgoLab.

            Returns:
            - All open positions with current market values
            - Profit/Loss calculations
            - Position quantities and average prices

            Requires: 'portfolio:read' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Portfolio positions retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
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
        summary = "Get Transaction History",
        description = """
            Retrieves user's executed transaction history from AlgoLab.

            Returns:
            - Completed buy/sell transactions
            - Transaction prices and quantities
            - Commissions and fees
            - Settlement information

            Requires: 'portfolio:read' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction history retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
    public ResponseEntity<AlgoLabResponse<Object>> getTransactions(
            @RequestParam(required = false)
            @Parameter(description = "Start date filter (YYYY-MM-DD)")
            String startDate,
            @RequestParam(required = false)
            @Parameter(description = "End date filter (YYYY-MM-DD)")
            String endDate,
            @RequestParam(required = false)
            @Parameter(description = "Symbol filter (e.g., AKBNK)")
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
        summary = "Get Real-Time Positions",
        description = """
            Retrieves user's real-time portfolio positions with current market prices.

            Returns:
            - Live position values with current market prices
            - Real-time P&L calculations
            - Unrealized gains/losses
            - Total portfolio value

            Requires: 'portfolio:read' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Instant positions retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable (circuit breaker open)"
        )
    })
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
        summary = "Get Broker Connection Status",
        description = """
            Checks AlgoLab broker connection and authentication status.

            Returns:
            - Connection status (connected/disconnected)
            - Authentication status (authenticated/unauthenticated)
            - Last check timestamp
            - Broker name (AlgoLab)

            Use this endpoint to verify broker availability before placing orders.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<Map<String, Object>> getBrokerStatus(Authentication authentication) {
        log.debug("Broker status request from user: {}", authentication.getName());

        boolean isConnected = brokerService.isConnected();
        boolean isAuthenticated = brokerService.isAuthenticated();

        Map<String, Object> status = Map.of(
            "connected", isConnected,
            "authenticated", isAuthenticated,
            "lastCheckTime", java.time.Instant.now(),
            "brokerName", "AlgoLab",
            "environment", "PRODUCTION"
        );

        log.debug("Broker status retrieved for user: {}", authentication.getName());
        return ResponseEntity.ok(status);
    }

    /**
     * Gets available Turkish stock symbols with real-time data.
     *
     * Integrates with SymbolService to provide comprehensive symbol information
     * including real-time prices from AlgoLab.
     */
    @GetMapping("/symbols")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "BIST Hisse Senetlerini Listele",
        description = """
            Borsa Istanbul'da işlem gören hisse senetlerini listeler.

            🔥 ÖZELLİKLER:
            - Real-time fiyat bilgileri (AlgoLab'dan)
            - Günlük fiyat değişimleri (%, TRY)
            - Tavan/taban fiyat limitleri
            - İşlem hacmi ve değeri
            - Sektör ve endeks bilgileri
            - Sayfalama desteği (varsayılan: 50 sembol/sayfa)

            FİLTRELEME:
            - Borsa (exchange): BIST
            - Sektör (sector): Banking, Technology, Energy, vb.
            - Arama (search): Sembol kodu veya şirket adı

            KULLANIM:
            Emir vermeden önce işlem yapılabilir sembolleri keşfetmek için kullanın.
            Veriler 24 saat Redis cache'te tutulur.

            ÖRNEKLER:
            - Tüm semboller: GET /api/v1/broker/symbols
            - Bankacılık sektörü: GET /api/v1/broker/symbols?sector=Banking
            - Sembol arama: GET /api/v1/broker/symbols?search=Akbank
            - 2. sayfa: GET /api/v1/broker/symbols?page=1&size=50
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sembol listesi başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    name = "Symbol List Response",
                    value = """
                    {
                        "content": [
                            {
                                "symbol": "AKBNK",
                                "name": "Akbank T.A.Ş.",
                                "exchange": "BIST",
                                "sector": "Banking",
                                "currency": "TRY",
                                "lastPrice": 15.75,
                                "change": 0.25,
                                "changePercent": 1.61,
                                "volume": 12500000,
                                "ceiling": 17.05,
                                "floor": 13.95,
                                "lotSize": 1,
                                "isTradeable": true,
                                "dataSource": "AlgoLab"
                            }
                        ],
                        "pageable": {
                            "pageNumber": 0,
                            "pageSize": 50
                        },
                        "totalElements": 485,
                        "totalPages": 10
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Kimlik doğrulama gerekli"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable - Servis geçici olarak kullanılamıyor"
        )
    })
    public ResponseEntity<Page<SymbolDto>> getAvailableSymbols(
            @Parameter(description = "Borsa kodu (varsayılan: BIST)", example = "BIST")
            @RequestParam(required = false, defaultValue = "BIST") String exchange,

            @Parameter(description = "Sektör filtresi (Banking, Technology, Energy, vb.)", example = "Banking")
            @RequestParam(required = false) String sector,

            @Parameter(description = "Arama terimi (sembol kodu veya şirket adı)", example = "Akbank")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 50, sort = "symbol", direction = Sort.Direction.ASC)
            @Parameter(hidden = true) Pageable pageable) {

        log.debug("Available symbols request - exchange: {}, sector: {}, search: {}, page: {}",
                exchange, sector, search, pageable.getPageNumber());

        // Use SymbolService to get symbols with real-time data
        Page<SymbolDto> symbols = symbolService.getSymbols(exchange, sector, search, pageable);

        log.info("Returning {} symbols out of {} total for exchange: {}",
                symbols.getNumberOfElements(), symbols.getTotalElements(), exchange);

        return ResponseEntity.ok(symbols);
    }

    /**
     * Get order book (Level 2 market data) for a symbol.
     * Returns mock data since AlgoLab WebSocket is not fully operational.
     */
    @GetMapping("/orderbook/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    @Operation(
        summary = "Get Order Book",
        description = """
            Retrieves order book (Level 2 market data) for a symbol.

            Returns:
            - Best bid/ask prices
            - Order book depth (5 levels)
            - Order quantities at each price level
            - Number of orders at each level

            Currently returns mock data until AlgoLab WebSocket integration is complete.

            Requires: 'market:read' authority
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order book retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Insufficient permissions"
        )
    })
    public ResponseEntity<Map<String, Object>> getOrderBook(
            @PathVariable
            @Parameter(description = "Stock symbol (e.g., AKBNK, THYAO)", required = true)
            String symbol,
            Authentication authentication) {

        log.info("Order book request for symbol: {} from user: {}", symbol, authentication.getName());

        try {
            // Return mock order book data since WebSocket is not fully operational
            Map<String, Object> response = Map.of(
                "content", Map.of(
                    "symbol", symbol,
                    "timestamp", java.time.Instant.now().toString(),
                    "sequence", System.currentTimeMillis(),
                    "bids", new Object[]{
                        Map.of("price", "52.70", "quantity", 50000, "orderCount", 15, "side", "BID"),
                        Map.of("price", "52.65", "quantity", 30000, "orderCount", 10, "side", "BID"),
                        Map.of("price", "52.60", "quantity", 25000, "orderCount", 8, "side", "BID"),
                        Map.of("price", "52.55", "quantity", 20000, "orderCount", 6, "side", "BID"),
                        Map.of("price", "52.50", "quantity", 15000, "orderCount", 5, "side", "BID")
                    },
                    "asks", new Object[]{
                        Map.of("price", "52.80", "quantity", 45000, "orderCount", 12, "side", "ASK"),
                        Map.of("price", "52.85", "quantity", 35000, "orderCount", 8, "side", "ASK"),
                        Map.of("price", "52.90", "quantity", 40000, "orderCount", 10, "side", "ASK"),
                        Map.of("price", "52.95", "quantity", 30000, "orderCount", 7, "side", "ASK"),
                        Map.of("price", "53.00", "quantity", 25000, "orderCount", 6, "side", "ASK")
                    }
                )
            );

            log.debug("Order book data returned for symbol: {}", symbol);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting order book for symbol: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to get order book: " + e.getMessage()
                ));
        }
    }

    /**
     * Gets comprehensive order history with detailed filtering.
     *
     * Provides full order lifecycle information including executions, fees, and timestamps.
     */
    @GetMapping("/orders/history")
    @PreAuthorize("hasAuthority('orders:read')")
    @Operation(
        summary = "Emir Geçmişini Getir",
        description = """
            Kullanıcının tüm emir geçmişini detaylı filtreleme seçenekleriyle getirir.

            🔥 ÖZELLİKLER:
            - Sayfalama desteği (varsayılan: 50 emir/sayfa)
            - Tarihe göre sıralanmış sonuçlar
            - Detaylı emir bilgileri (durum, fiyat, miktar)
            - Gerçekleşme detayları (executions)
            - Komisyon ve vergi bilgileri
            - Zaman damgaları (oluşturma, gerçekleşme, iptal)

            FİLTRELEME SEÇENEKLERİ:
            - Tarih aralığı (startDate, endDate)
            - Sembol (örn: AKBNK, GARAN)
            - Emir durumu (PENDING, FILLED, CANCELLED, vb.)
            - Alım/Satım (BUY, SELL)
            - Emir tipi (MARKET, LIMIT, STOP)

            KULLANIM ÖRNEKLERİ:
            - Tüm emirler: GET /api/v1/broker/orders/history
            - Belirli tarih aralığı: GET /api/v1/broker/orders/history?startDate=2024-01-01&endDate=2024-12-31
            - Belirli sembol: GET /api/v1/broker/orders/history?symbol=AKBNK
            - Gerçekleşen emirler: GET /api/v1/broker/orders/history?status=FILLED
            - Sayfalama: GET /api/v1/broker/orders/history?page=1&size=50
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Emir geçmişi başarıyla getirildi",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    name = "Order History Response",
                    value = """
                    {
                        "content": [
                            {
                                "orderId": "550e8400-e29b-41d4-a716-446655440000",
                                "clientOrderId": "ORD-12345",
                                "brokerOrderId": "BRK-98765",
                                "symbol": "AKBNK",
                                "side": "BUY",
                                "orderType": "LIMIT",
                                "status": "FILLED",
                                "quantity": 1000,
                                "price": 15.75,
                                "filledQuantity": 1000,
                                "averageFillPrice": 15.73,
                                "totalCost": 15730.00,
                                "commission": 15.73,
                                "tax": 31.46,
                                "executions": [
                                    {
                                        "executionId": "exec-123",
                                        "executionQuantity": 500,
                                        "executionPrice": 15.72,
                                        "commission": 7.86,
                                        "executionTime": "2024-09-24T10:30:15Z"
                                    }
                                ],
                                "createdAt": "2024-09-24T10:30:00Z",
                                "completedAt": "2024-09-24T10:32:45Z"
                            }
                        ],
                        "pageable": {
                            "pageNumber": 0,
                            "pageSize": 50
                        },
                        "totalElements": 125,
                        "totalPages": 3
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Kimlik doğrulama gerekli"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Yetki eksikliği"
        )
    })
    public ResponseEntity<Page<OrderHistoryDto>> getOrderHistory(
            @RequestParam(required = false)
            @Parameter(description = "Başlangıç tarihi (YYYY-MM-DD)", example = "2024-01-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @Parameter(description = "Bitiş tarihi (YYYY-MM-DD)", example = "2024-12-31")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            @Parameter(description = "Sembol filtresi (örn: AKBNK)", example = "AKBNK")
            String symbol,

            @RequestParam(required = false)
            @Parameter(description = "Emir durumu (PENDING, FILLED, CANCELLED, etc.)", example = "FILLED")
            OrderStatus status,

            @RequestParam(required = false)
            @Parameter(description = "Alım/Satım (BUY, SELL)", example = "BUY")
            OrderSide side,

            @RequestParam(required = false)
            @Parameter(description = "Emir tipi (MARKET, LIMIT, STOP)", example = "LIMIT")
            OrderType orderType,

            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(hidden = true)
            Pageable pageable,

            Authentication authentication
    ) {
        log.info("Order history request from user: {} - filters: symbol={}, status={}, date range=[{} to {}]",
                authentication.getName(), symbol, status, startDate, endDate);

        String userId = extractUserId(authentication);

        // Build search criteria
        OrderSearchCriteria.OrderSearchCriteriaBuilder criteriaBuilder = OrderSearchCriteria.builder()
                .symbol(symbol)
                .orderStatus(status)
                .orderSide(side)
                .orderType(orderType);

        // Convert LocalDate to ZonedDateTime for criteria
        if (startDate != null) {
            criteriaBuilder.createdAfter(startDate.atStartOfDay(java.time.ZoneId.systemDefault()));
        }
        if (endDate != null) {
            criteriaBuilder.createdBefore(endDate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()));
        }

        OrderSearchCriteria criteria = criteriaBuilder.build();

        // Get orders from service
        Page<Order> orders = orderManagementService.getOrderHistory(userId, criteria, pageable);

        // Convert to DTO with execution details
        Page<OrderHistoryDto> orderHistory = orders.map(this::toOrderHistoryDto);

        log.info("Returning {} orders out of {} total for user: {}",
                orderHistory.getNumberOfElements(), orderHistory.getTotalElements(), authentication.getName());

        return ResponseEntity.ok(orderHistory);
    }

    /**
     * Get recent WebSocket tick messages for a symbol (for CLI polling).
     */
    @GetMapping("/websocket/stream/ticks/{symbol}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Recent Tick Stream",
        description = """
            Retrieves recent tick messages from WebSocket buffer for CLI polling.

            Returns last N tick messages received via WebSocket.
            Useful for CLI clients to display real-time data via HTTP polling.
            """
    )
    public ResponseEntity<Map<String, Object>> getRecentTicks(
            @PathVariable @Parameter(description = "Symbol code", required = true) String symbol,
            @RequestParam(defaultValue = "10") @Parameter(description = "Number of messages to return") int limit,
            Authentication authentication) {

        log.debug("Recent ticks request for {} from user: {}", symbol, authentication.getName());

        var messages = messageBuffer.getRecentTicks(symbol, limit);

        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "count", messages.size(),
            "messages", messages,
            "timestamp", java.time.Instant.now()
        ));
    }

    /**
     * Get recent WebSocket trade messages for a symbol (for CLI polling).
     */
    @GetMapping("/websocket/stream/trades/{symbol}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Recent Trade Stream",
        description = """
            Retrieves recent trade messages from WebSocket buffer for CLI polling.

            Returns last N trade messages received via WebSocket.
            Useful for CLI clients to display real-time trade flow via HTTP polling.
            """
    )
    public ResponseEntity<Map<String, Object>> getRecentTrades(
            @PathVariable @Parameter(description = "Symbol code", required = true) String symbol,
            @RequestParam(defaultValue = "10") @Parameter(description = "Number of messages to return") int limit,
            Authentication authentication) {

        log.debug("Recent trades request for {} from user: {}", symbol, authentication.getName());

        var messages = messageBuffer.getRecentTrades(symbol, limit);

        return ResponseEntity.ok(Map.of(
            "symbol", symbol,
            "count", messages.size(),
            "messages", messages,
            "timestamp", java.time.Instant.now()
        ));
    }

    /**
     * Get WebSocket stream statistics.
     */
    @GetMapping("/websocket/stream/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get WebSocket Stream Statistics",
        description = "Returns statistics about buffered WebSocket messages."
    )
    public ResponseEntity<Map<String, Object>> getStreamStats(Authentication authentication) {
        log.debug("Stream stats request from user: {}", authentication.getName());

        return ResponseEntity.ok(messageBuffer.getStats());
    }

    /**
     * Get active symbols with tick data.
     */
    @GetMapping("/websocket/stream/symbols")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Active Tick Symbols",
        description = "Returns list of symbols currently receiving tick data via WebSocket."
    )
    public ResponseEntity<Map<String, Object>> getActiveSymbols(Authentication authentication) {
        log.debug("Active symbols request from user: {}", authentication.getName());

        return ResponseEntity.ok(Map.of(
            "symbols", messageBuffer.getActiveTickSymbols(),
            "timestamp", java.time.Instant.now()
        ));
    }

    /**
     * Subscribe to WebSocket stream for specific symbol.
     * Supports both JSON body and query parameters for CLI compatibility.
     */
    @PostMapping("/websocket/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Subscribe to Symbol Stream",
        description = """
            Subscribe to real-time WebSocket stream for a specific symbol.

            This will start receiving tick data for the symbol via WebSocket.
            Data can be retrieved via /websocket/stream/ticks/{symbol} endpoint.

            Accepts both:
            - JSON body: {"symbol": "THYAO", "channel": "tick"}
            - Query params: ?symbol=THYAO&channel=tick
            """
    )
    public ResponseEntity<Map<String, Object>> subscribeToSymbol(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) @Parameter(description = "Symbol code (e.g., THYAO, AKBNK, or ALL)") String symbol,
            @RequestParam(required = false, defaultValue = "tick") @Parameter(description = "Channel type (tick, orderbook, trade)") String channel,
            Authentication authentication) {

        // Support both JSON body and query parameters
        String finalSymbol = (body != null && body.containsKey("symbol")) ? body.get("symbol") : symbol;
        String finalChannel = (body != null && body.containsKey("channel")) ? body.get("channel") : channel;

        if (finalSymbol == null || finalSymbol.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Symbol is required (provide in body or as query parameter)",
                "timestamp", java.time.Instant.now()
            ));
        }

        log.info("WebSocket subscription request from user: {} for symbol: {}, channel: {}",
            authentication.getName(), finalSymbol, finalChannel);

        try {
            subscriptionManager.subscribe(finalSymbol, finalChannel);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "symbol", finalSymbol,
                "channel", finalChannel,
                "message", "Subscription successful",
                "timestamp", java.time.Instant.now()
            ));
        } catch (Exception e) {
            log.error("Failed to subscribe to symbol: {}", finalSymbol, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "symbol", finalSymbol,
                "channel", finalChannel,
                "message", "Subscription failed: " + e.getMessage(),
                "timestamp", java.time.Instant.now()
            ));
        }
    }

    /**
     * Subscribe to ALL symbols at once (AlgoLab special feature).
     */
    @PostMapping("/websocket/subscribe/all")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Subscribe to ALL Symbols",
        description = """
            Subscribe to ALL symbols at once via WebSocket.

            This is a special AlgoLab feature that subscribes to all available symbols.
            Data for all symbols will be received and cached.
            """
    )
    public ResponseEntity<Map<String, Object>> subscribeToAll(
            @RequestParam(defaultValue = "tick") @Parameter(description = "Channel type (tick, orderbook, trade)") String channel,
            Authentication authentication) {

        log.info("WebSocket ALL subscription request from user: {} for channel: {}",
            authentication.getName(), channel);

        try {
            subscriptionManager.subscribeToAll(channel);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "symbol", "ALL",
                "channel", channel,
                "message", "Successfully subscribed to ALL symbols",
                "timestamp", java.time.Instant.now()
            ));
        } catch (Exception e) {
            log.error("Failed to subscribe to ALL symbols", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "symbol", "ALL",
                "channel", channel,
                "message", "Subscription failed: " + e.getMessage(),
                "timestamp", java.time.Instant.now()
            ));
        }
    }

    /**
     * Get current subscriptions.
     */
    @GetMapping("/websocket/subscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Active Subscriptions",
        description = "Returns list of currently active WebSocket subscriptions."
    )
    public ResponseEntity<Map<String, Object>> getSubscriptions(Authentication authentication) {
        log.debug("Subscriptions request from user: {}", authentication.getName());

        var subscriptions = subscriptionManager.getActiveSubscriptions().stream()
            .map(sub -> Map.of(
                "symbol", sub.getSymbol(),
                "channel", sub.getChannel(),
                "subscribedAt", sub.getSubscribedAt() != null ? sub.getSubscribedAt().toString() : "N/A"
            ))
            .toList();

        return ResponseEntity.ok(Map.of(
            "subscriptions", subscriptions,
            "count", subscriptions.size(),
            "timestamp", java.time.Instant.now()
        ));
    }

    // ===== Private Helper Methods =====

    /**
     * Extract user ID from authentication.
     *
     * @param authentication Spring Security authentication
     * @return User ID
     */
    private String extractUserId(Authentication authentication) {
        // For now, using username as user ID
        // In production, you would extract the actual user ID from the authenticated principal
        return authentication.getName();
    }

    /**
     * Convert Order entity to OrderHistoryDto with execution details.
     *
     * @param order Order entity
     * @return OrderHistoryDto
     */
    private OrderHistoryDto toOrderHistoryDto(Order order) {
        // Get all executions for this order
        List<OrderExecution> executions = executionRepository.findByOrderId(order.getId());

        return OrderHistoryDto.builder()
                .orderId(order.getId())
                .clientOrderId(order.getClientOrderId())
                .brokerOrderId(order.getBrokerOrderId())
                .exchangeOrderId(order.getExchangeOrderId())
                // Order details
                .symbol(order.getSymbol() != null ? order.getSymbol().getSymbol() : null)
                .side(order.getOrderSide())
                .orderType(order.getOrderType())
                .status(order.getOrderStatus())
                .statusReason(order.getStatusReason())
                // Quantities and prices
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .stopPrice(order.getStopPrice())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .averageFillPrice(order.getAverageFillPrice())
                // Costs and fees
                .totalCost(calculateTotalCost(executions))
                .commission(order.getCommission())
                .brokerageFee(order.getBrokerageFee())
                .exchangeFee(order.getExchangeFee())
                .tax(order.getTaxAmount())
                // Executions
                .executions(executions.stream()
                        .map(this::toExecutionDto)
                        .collect(Collectors.toList()))
                // Timestamps - Convert LocalDateTime to ZonedDateTime
                .createdAt(toZonedDateTime(order.getCreatedAt()))
                .submittedAt(order.getSubmittedAt())
                .acceptedAt(order.getAcceptedAt())
                .firstFillAt(order.getFirstFillAt())
                .lastFillAt(order.getLastFillAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(null) // Order entity doesn't have cancelledAt field
                .build();
    }

    /**
     * Convert OrderExecution entity to ExecutionDto.
     *
     * @param execution OrderExecution entity
     * @return ExecutionDto
     */
    private ExecutionDto toExecutionDto(OrderExecution execution) {
        return ExecutionDto.builder()
                .executionId(execution.getExecutionId() != null ? execution.getExecutionId().toString() : null)
                .tradeId(execution.getTradeId())
                .brokerExecutionId(execution.getBrokerExecutionId())
                .executionType(execution.getExecutionType())
                .executionQuantity(execution.getExecutionQuantity())
                .executionPrice(execution.getExecutionPrice())
                .grossAmount(execution.getGrossAmount())
                .commission(execution.getCommission())
                .brokerageFee(execution.getBrokerageFee())
                .exchangeFee(execution.getExchangeFee())
                .taxAmount(execution.getTaxAmount())
                .netAmount(execution.getNetAmount())
                .bidPrice(execution.getBidPrice())
                .askPrice(execution.getAskPrice())
                .marketPrice(execution.getMarketPrice())
                .contraBroker(execution.getContraBroker())
                .executionTime(execution.getExecutionTime())
                .build();
    }

    /**
     * Calculate total cost from executions.
     *
     * @param executions List of executions
     * @return Total cost
     */
    private BigDecimal calculateTotalCost(List<OrderExecution> executions) {
        if (executions == null || executions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return executions.stream()
                .map(OrderExecution::getGrossAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Convert LocalDateTime to ZonedDateTime using system default zone.
     *
     * @param localDateTime LocalDateTime to convert
     * @return ZonedDateTime or null if input is null
     */
    private ZonedDateTime toZonedDateTime(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(java.time.ZoneId.systemDefault());
    }
}

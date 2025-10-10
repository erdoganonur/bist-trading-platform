package com.bisttrading.symbol.controller;

import com.bisttrading.symbol.dto.SymbolDto;
import com.bisttrading.symbol.service.SymbolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Symbol operations.
 *
 * Provides endpoints for querying BIST symbols with real-time market data.
 * All symbol data is cached with 24-hour TTL and enriched with AlgoLab real-time prices.
 *
 * Base URL: /api/v1/symbols
 *
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/v1/symbols")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Symbol Management", description = "BIST symbol information with real-time market data from AlgoLab")
public class SymbolController {

    private final SymbolService symbolService;

    /**
     * Get paginated list of symbols with optional filtering.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Symbols (Paginated)",
        description = """
            Retrieves a paginated list of BIST symbols with optional filtering.

            Features:
            - Pagination support (default: 20 per page)
            - Filter by exchange (e.g., BIST)
            - Filter by sector (e.g., Banking)
            - Full-text search across symbol code and names
            - Real-time price data from AlgoLab
            - 24-hour cache with Redis

            Use this endpoint for symbol listing and discovery.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Symbols retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable"
        )
    })
    public ResponseEntity<Page<SymbolDto>> getSymbols(
            @Parameter(description = "Filter by exchange (e.g., BIST)")
            @RequestParam(required = false) String exchange,

            @Parameter(description = "Filter by sector (e.g., Banking, Technology)")
            @RequestParam(required = false) String sector,

            @Parameter(description = "Search query (searches symbol, name, localName)")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "symbol", direction = Sort.Direction.ASC)
            @Parameter(hidden = true) Pageable pageable) {

        log.debug("GET /api/v1/symbols - exchange: {}, sector: {}, search: {}, page: {}",
                exchange, sector, search, pageable.getPageNumber());

        Page<SymbolDto> symbols = symbolService.getSymbols(exchange, sector, search, pageable);

        log.debug("Returning {} symbols out of {} total", symbols.getNumberOfElements(), symbols.getTotalElements());

        return ResponseEntity.ok(symbols);
    }

    /**
     * Get single symbol by code.
     */
    @GetMapping("/{symbol}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Symbol Details",
        description = """
            Retrieves detailed information for a specific symbol.

            Returns:
            - Basic symbol information from database
            - Real-time market data from AlgoLab
            - Price limits (ceiling/floor)
            - Trading rules (lot size, min/max order)
            - Financial metrics (market cap, dividend yield)

            Data is cached for 24 hours. Use /symbols/{symbol}/refresh for real-time update.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Symbol found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SymbolDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Symbol not found"
        )
    })
    public ResponseEntity<SymbolDto> getSymbol(
            @Parameter(description = "Symbol code (e.g., AKBNK, GARAN)", required = true)
            @PathVariable String symbol) {

        log.debug("GET /api/v1/symbols/{}", symbol);

        Optional<SymbolDto> symbolDto = symbolService.getSymbol(symbol.toUpperCase());

        if (symbolDto.isEmpty()) {
            log.warn("Symbol not found: {}", symbol);
            return ResponseEntity.notFound().build();
        }

        log.debug("Symbol details retrieved: {}", symbol);
        return ResponseEntity.ok(symbolDto.get());
    }

    /**
     * Search symbols by query string.
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Search Symbols",
        description = """
            Search symbols by query string.

            Searches across:
            - Symbol code (e.g., "AKBNK")
            - Company name (e.g., "Akbank")
            - Local name (Turkish name)

            Returns up to 50 matching symbols sorted by relevance.
            Results are not cached - use for autocomplete/typeahead.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid search query"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<List<SymbolDto>> searchSymbols(
            @Parameter(description = "Search query (minimum 1 character)", required = true)
            @RequestParam String q) {

        log.debug("GET /api/v1/symbols/search?q={}", q);

        if (q == null || q.trim().isEmpty()) {
            log.warn("Empty search query");
            return ResponseEntity.badRequest().build();
        }

        List<SymbolDto> results = symbolService.searchSymbols(q);

        log.debug("Search returned {} results for query: {}", results.size(), q);
        return ResponseEntity.ok(results);
    }

    /**
     * Get symbols by exchange.
     */
    @GetMapping("/exchange/{exchange}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Symbols by Exchange",
        description = """
            Retrieves all symbols for a specific exchange.

            Supported exchanges:
            - BIST (Borsa Istanbul)

            Results are cached for 24 hours.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Symbols retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<List<SymbolDto>> getSymbolsByExchange(
            @Parameter(description = "Exchange code (e.g., BIST)", required = true)
            @PathVariable String exchange) {

        log.debug("GET /api/v1/symbols/exchange/{}", exchange);

        List<SymbolDto> symbols = symbolService.getSymbolsByExchange(exchange.toUpperCase());

        log.debug("Found {} symbols for exchange: {}", symbols.size(), exchange);
        return ResponseEntity.ok(symbols);
    }

    /**
     * Get symbols by sector.
     */
    @GetMapping("/sector/{sector}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Symbols by Sector",
        description = """
            Retrieves all symbols in a specific business sector.

            Common sectors:
            - Banking
            - Technology
            - Energy
            - Holding
            - Transportation
            - Manufacturing

            Results are cached for 24 hours.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Symbols retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<List<SymbolDto>> getSymbolsBySector(
            @Parameter(description = "Sector name (e.g., Banking)", required = true)
            @PathVariable String sector) {

        log.debug("GET /api/v1/symbols/sector/{}", sector);

        List<SymbolDto> symbols = symbolService.getSymbolsBySector(sector);

        log.debug("Found {} symbols for sector: {}", symbols.size(), sector);
        return ResponseEntity.ok(symbols);
    }

    /**
     * Get real-time price update for a symbol (bypasses cache).
     */
    @PostMapping("/{symbol}/refresh")
    @PreAuthorize("hasAnyAuthority('trading:read', 'portfolio:read')")
    @Operation(
        summary = "Refresh Symbol Data",
        description = """
            Forces a refresh of symbol data from AlgoLab, bypassing cache.

            Use this endpoint when you need guaranteed real-time data.
            This endpoint:
            - Bypasses Redis cache
            - Fetches fresh data from AlgoLab
            - Updates cache with new data
            - Returns real-time prices

            Rate limit: Max 1 request per 5 seconds per symbol.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Symbol data refreshed successfully"
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
            description = "Symbol not found"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "AlgoLab service unavailable"
        )
    })
    public ResponseEntity<SymbolDto> refreshSymbol(
            @Parameter(description = "Symbol code to refresh", required = true)
            @PathVariable String symbol) {

        log.info("POST /api/v1/symbols/{}/refresh - Forcing cache refresh", symbol);

        try {
            SymbolDto refreshedSymbol = symbolService.updateSymbolPrice(symbol.toUpperCase());

            log.info("Symbol {} refreshed successfully - Price: {}", symbol, refreshedSymbol.getLastPrice());
            return ResponseEntity.ok(refreshedSymbol);

        } catch (IllegalArgumentException e) {
            log.warn("Symbol not found for refresh: {}", symbol);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to refresh symbol: {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get list of all available sectors.
     */
    @GetMapping("/metadata/sectors")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get All Sectors",
        description = """
            Retrieves a list of all unique sectors from available symbols.

            Use this endpoint to populate sector filter dropdowns.
            Results are cached for 24 hours.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sectors retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<List<String>> getAllSectors() {
        log.debug("GET /api/v1/symbols/metadata/sectors");

        List<String> sectors = symbolService.getAllSectors();

        log.debug("Found {} distinct sectors", sectors.size());
        return ResponseEntity.ok(sectors);
    }

    /**
     * Get list of all available exchanges.
     */
    @GetMapping("/metadata/exchanges")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get All Exchanges",
        description = """
            Retrieves a list of all unique exchanges from available symbols.

            Use this endpoint to populate exchange filter dropdowns.
            Results are cached for 24 hours.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Exchanges retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<List<String>> getAllExchanges() {
        log.debug("GET /api/v1/symbols/metadata/exchanges");

        List<String> exchanges = symbolService.getAllExchanges();

        log.debug("Found {} distinct exchanges", exchanges.size());
        return ResponseEntity.ok(exchanges);
    }

    /**
     * Get symbol statistics summary.
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get Symbol Statistics",
        description = """
            Retrieves summary statistics about all symbols.

            Returns:
            - Total number of symbols
            - Number of active symbols
            - Symbols by exchange
            - Symbols by sector
            - Top traded symbols (if available)

            Use this endpoint for dashboard statistics.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
        )
    })
    public ResponseEntity<Map<String, Object>> getSymbolStatistics() {
        log.debug("GET /api/v1/symbols/statistics");

        List<SymbolDto> allSymbols = symbolService.getAllSymbols();
        List<String> sectors = symbolService.getAllSectors();
        List<String> exchanges = symbolService.getAllExchanges();

        Map<String, Object> statistics = Map.of(
                "totalSymbols", allSymbols.size(),
                "activeSymbols", allSymbols.stream().filter(s -> "ACTIVE".equals(s.getTradingStatus())).count(),
                "totalSectors", sectors.size(),
                "totalExchanges", exchanges.size(),
                "sectors", sectors,
                "exchanges", exchanges
        );

        log.debug("Symbol statistics: {} total symbols, {} sectors, {} exchanges",
                allSymbols.size(), sectors.size(), exchanges.size());

        return ResponseEntity.ok(statistics);
    }
}

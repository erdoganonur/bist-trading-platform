package com.bisttrading.user.marketdata.controller;

import com.bisttrading.user.marketdata.dto.*;
import com.bisttrading.user.marketdata.service.SimplifiedMarketDataService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Market Data", description = "Piyasa verisi analizi ve raporlama işlemleri")
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class MarketDataController {

    private final SimplifiedMarketDataService marketDataService;

    @Operation(
        summary = "Multi-timeframe OHLCV verisi",
        description = "Belirtilen sembol için farklı zaman dilimlerinde OHLCV verilerini getirir (1m, 5m, 15m, 1h, 1d)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OHLCV verileri başarıyla alındı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Erişim reddedildi"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/ohlcv/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<CompletableFuture<Map<String, List<OHLCVData>>>> getMultiTimeframeOHLCV(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Multi-timeframe OHLCV request for symbol: {}, period: {} to {}", symbol, startTime, endTime);

        CompletableFuture<Map<String, List<OHLCVData>>> ohlcvData =
            marketDataService.getMultiTimeframeOHLCV(symbol, startTime, endTime);

        return ResponseEntity.ok(ohlcvData);
    }

    @Operation(
        summary = "Volume analizi",
        description = "Sembol için detaylı hacim analizi: toplam hacim, VWAP, en yüksek hacimli dönemler ve fiyat etkisi"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Volume analizi başarıyla tamamlandı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VolumeAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/volume/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<VolumeAnalysis> analyzeVolume(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Volume analysis request for symbol: {}, period: {} to {}", symbol, startTime, endTime);

        VolumeAnalysis analysis = marketDataService.analyzeVolume(symbol, startTime, endTime);
        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Teknik analiz indikatörleri",
        description = "Komprehensif teknik analiz: SMA, RSI, Bollinger Bands, candlestick patterns, volume profile"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Teknik analiz başarıyla tamamlandı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TechnicalAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/technical/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<CompletableFuture<TechnicalAnalysis>> calculateTechnicalIndicators(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Zaman dilimi (1 minute, 5 minutes, 15 minutes, 1 hour, 1 day)", required = true)
            @RequestParam String timeframe,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Technical analysis request for symbol: {}, timeframe: {}, period: {} to {}",
                 symbol, timeframe, startTime, endTime);

        CompletableFuture<TechnicalAnalysis> analysis =
            marketDataService.calculateTechnicalIndicators(symbol, timeframe, startTime, endTime);

        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Trend analizi",
        description = "Piyasa trend analizi: yön, güç, hacim trendi ve veri noktası sayısı"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trend analizi başarıyla tamamlandı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TrendAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/trend/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<TrendAnalysis> analyzeTrend(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Zaman dilimi (1 minute, 5 minutes, 15 minutes, 1 hour, 1 day)", required = true)
            @RequestParam String timeframe,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Trend analysis request for symbol: {}, timeframe: {}, period: {} to {}",
                 symbol, timeframe, startTime, endTime);

        TrendAnalysis analysis = marketDataService.analyzeTrend(symbol, timeframe, startTime, endTime);
        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Emir defteri analizi",
        description = "Gelişmiş emir defteri analizi: spread, derinlik, likidite dengesizliği, fiyat seviyesi analizi"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Emir defteri analizi başarıyla tamamlandı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrderBookAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/orderbook/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<OrderBookAnalysis> analyzeOrderBook(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Order book analysis request for symbol: {}, period: {} to {}", symbol, startTime, endTime);

        OrderBookAnalysis analysis = marketDataService.analyzeOrderBook(symbol, startTime, endTime);
        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Mikro yapı analizi",
        description = "Piyasa mikro yapısı analizi: spread istatistikleri, emir defteri kalınlığı, tick bazlı spread"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mikro yapı analizi başarıyla tamamlandı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MicrostructureAnalysis.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/microstructure/{symbol}")
    @PreAuthorize("hasAuthority('market:read')")
    public ResponseEntity<MicrostructureAnalysis> analyzeMicrostructure(
            @Parameter(description = "Hisse sembolü (örn: AKBNK, TUPRS)", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Microstructure analysis request for symbol: {}, period: {} to {}", symbol, startTime, endTime);

        MicrostructureAnalysis analysis = marketDataService.analyzeMicrostructure(symbol, startTime, endTime);
        return ResponseEntity.ok(analysis);
    }

    @Operation(
        summary = "Piyasa genel görünümü",
        description = "Piyasa genelinde istatistikler: aktif semboller, en yüksek hacimli semboller, veritabanı metrikleri"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Piyasa genel görünümü başarıyla alındı",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = MarketOverview.class))),
        @ApiResponse(responseCode = "400", description = "Geçersiz parametre"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Sadece admin kullanıcılar erişebilir"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MarketOverview> getMarketOverview(
            @Parameter(description = "Başlangıç tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "Bitiş tarihi (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {

        log.info("Market overview request for period: {} to {}", startTime, endTime);

        MarketOverview overview = marketDataService.getMarketOverview(startTime, endTime);
        return ResponseEntity.ok(overview);
    }

    @Operation(
        summary = "Market Data servis sağlık kontrolü",
        description = "Market Data servisinin durumunu kontrol eder"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Servis sağlıklı"),
        @ApiResponse(responseCode = "503", description = "Servis kullanılamıyor")
    })
    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.debug("Market Data service health check");

        Map<String, Object> healthStatus = Map.of(
            "status", "UP",
            "service", "Market Data Service (Simplified)",
            "timestamp", java.time.Instant.now(),
            "version", "1.0.0"
        );

        return ResponseEntity.ok(healthStatus);
    }
}
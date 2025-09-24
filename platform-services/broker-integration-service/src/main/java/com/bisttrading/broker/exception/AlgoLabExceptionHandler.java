package com.bisttrading.broker.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * AlgoLab API exception handler - Türkçe hata mesajları ile
 */
@RestControllerAdvice
@Slf4j
public class AlgoLabExceptionHandler {

    /**
     * AlgoLab-specific exception handler
     */
    @ExceptionHandler(AlgoLabException.class)
    public ResponseEntity<Map<String, Object>> handleAlgoLabException(AlgoLabException ex) {
        log.error("AlgoLab API hatası: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("error", "AlgoLab API Hatası");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("status", HttpStatus.BAD_GATEWAY.value());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Circuit breaker açık durumunda exception handler
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCallNotPermitted(CallNotPermittedException ex) {
        log.warn("Circuit breaker açık - AlgoLab API'ye erişim engellendi: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("error", "Servis Geçici Olarak Kullanılamıyor");
        errorResponse.put("message", "AlgoLab servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin.");
        errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.put("retryAfter", "30 saniye");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * WebClient response exception handler
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponseException(WebClientResponseException ex) {
        log.error("HTTP hatası - Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", ex.getStatusCode().value());

        switch (ex.getStatusCode().value()) {
            case 401:
                errorResponse.put("error", "Kimlik Doğrulama Hatası");
                errorResponse.put("message", "Token geçersiz veya süresi dolmuş. Lütfen yeniden giriş yapın.");
                break;
            case 403:
                errorResponse.put("error", "Erişim İzni Yok");
                errorResponse.put("message", "Bu işlem için yetkiniz bulunmuyor. API anahtarınızı kontrol edin.");
                break;
            case 429:
                errorResponse.put("error", "Çok Fazla İstek");
                errorResponse.put("message", "Rate limit aşıldı. Lütfen daha sonra tekrar deneyin.");
                errorResponse.put("retryAfter", "5 saniye");
                break;
            case 500:
                errorResponse.put("error", "Sunucu Hatası");
                errorResponse.put("message", "AlgoLab sunucusunda bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
                break;
            case 502:
                errorResponse.put("error", "Bad Gateway");
                errorResponse.put("message", "AlgoLab servisine ulaşılamıyor.");
                break;
            case 503:
                errorResponse.put("error", "Servis Kullanılamıyor");
                errorResponse.put("message", "AlgoLab servisi geçici olarak kullanılamıyor.");
                break;
            case 504:
                errorResponse.put("error", "Gateway Timeout");
                errorResponse.put("message", "AlgoLab servisinden yanıt alınamadı. İşlem zaman aşımına uğradı.");
                break;
            default:
                errorResponse.put("error", "HTTP Hatası");
                errorResponse.put("message", "Beklenmeyen bir hata oluştu: " + ex.getStatusText());
        }

        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    /**
     * Timeout exception handler
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeoutException(TimeoutException ex) {
        log.error("Timeout hatası: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("error", "Zaman Aşımı");
        errorResponse.put("message", "İşlem zaman aşımına uğradı. AlgoLab servisi yanıt vermedi.");
        errorResponse.put("status", HttpStatus.REQUEST_TIMEOUT.value());

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    /**
     * General exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Beklenmeyen hata: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("error", "İç Hata");
        errorResponse.put("message", "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.");
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        // Development ortamında detaylı hata mesajı göster
        if (log.isDebugEnabled()) {
            errorResponse.put("detail", ex.getMessage());
            errorResponse.put("stackTrace", ex.getStackTrace());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
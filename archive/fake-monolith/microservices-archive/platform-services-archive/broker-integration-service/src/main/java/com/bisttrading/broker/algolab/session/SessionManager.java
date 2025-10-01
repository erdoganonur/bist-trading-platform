package com.bisttrading.broker.algolab.session;

import com.bisttrading.broker.algolab.AlgoLabApiClient;
import com.bisttrading.broker.algolab.model.AuthenticationRequest;
import com.bisttrading.broker.algolab.model.AuthenticationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class SessionManager {

    private final AlgoLabApiClient apiClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isAuthenticated;
    private final AtomicReference<String> currentToken;
    private final AtomicReference<Instant> tokenExpiry;
    private final AtomicReference<CompletableFuture<Void>> heartbeatTask;

    @Value("${algolab.session.heartbeat-interval:30}")
    private int heartbeatIntervalSeconds;

    @Value("${algolab.session.token-refresh-buffer:300}")
    private int tokenRefreshBufferSeconds;

    @Value("${algolab.session.max-retry-attempts:3}")
    private int maxRetryAttempts;

    public SessionManager(AlgoLabApiClient apiClient) {
        this.apiClient = apiClient;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.isAuthenticated = new AtomicBoolean(false);
        this.currentToken = new AtomicReference<>();
        this.tokenExpiry = new AtomicReference<>();
        this.heartbeatTask = new AtomicReference<>();
    }

    public CompletableFuture<Boolean> authenticate(String username, String password) {
        return authenticate(username, password, null);
    }

    public CompletableFuture<Boolean> authenticate(String username, String password, String totpCode) {
        log.info("Authenticating user: {}", username);

        // Note: TOTP code parameter is accepted but not currently used
        // Future enhancement can integrate TOTP support

        return CompletableFuture.supplyAsync(() -> apiClient.authenticate(username, password))
                .thenCompose(this::handleAuthenticationResponse)
                .thenApply(success -> {
                    if (success) {
                        startHeartbeat();
                        log.info("Authentication successful for user: {}", username);
                    } else {
                        log.error("Authentication failed for user: {}", username);
                    }
                    return success;
                })
                .exceptionally(throwable -> {
                    log.error("Authentication error for user: {}", username, throwable);
                    return false;
                });
    }

    private CompletableFuture<Boolean> handleAuthenticationResponse(AuthenticationResponse response) {
        if (response != null && response.getAccessToken() != null) {
            currentToken.set(response.getAccessToken());
            tokenExpiry.set(response.getExpiresAt());
            isAuthenticated.set(true);

            scheduleTokenRefresh();
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    private void scheduleTokenRefresh() {
        Instant expiry = tokenExpiry.get();
        if (expiry != null) {
            long refreshTime = expiry.getEpochSecond() - Instant.now().getEpochSecond() - tokenRefreshBufferSeconds;
            if (refreshTime > 0) {
                scheduler.schedule(this::refreshToken, refreshTime, TimeUnit.SECONDS);
                log.debug("Token refresh scheduled in {} seconds", refreshTime);
            }
        }
    }

    private void refreshToken() {
        if (!isAuthenticated.get()) {
            return;
        }

        log.info("Refreshing authentication token");
        apiClient.refreshToken(currentToken.get())
                .thenAccept(response -> {
                    if (response != null && response.getAccessToken() != null) {
                        currentToken.set(response.getAccessToken());
                        tokenExpiry.set(response.getExpiresAt());
                        scheduleTokenRefresh();
                        log.info("Token refresh successful");
                    } else {
                        log.error("Token refresh failed - marking session as unauthenticated");
                        logout();
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Token refresh error", throwable);
                    logout();
                    return null;
                });
    }

    private void startHeartbeat() {
        stopHeartbeat();

        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            while (isAuthenticated.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    sendHeartbeat();
                    Thread.sleep(heartbeatIntervalSeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Heartbeat error", e);
                    retryHeartbeat();
                }
            }
        });

        heartbeatTask.set(task);
        log.info("Heartbeat started with interval: {} seconds", heartbeatIntervalSeconds);
    }

    private void sendHeartbeat() {
        if (!isAuthenticated.get()) {
            return;
        }

        apiClient.heartbeat()
                .thenAccept(response -> {
                    log.debug("Heartbeat successful");
                })
                .exceptionally(throwable -> {
                    log.warn("Heartbeat failed", throwable);
                    return null;
                });
    }

    private void retryHeartbeat() {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                sendHeartbeat();
                return;
            } catch (Exception e) {
                log.warn("Heartbeat retry attempt {} failed", attempt, e);
                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All heartbeat retry attempts failed - logging out");
        logout();
    }

    private void stopHeartbeat() {
        CompletableFuture<Void> task = heartbeatTask.get();
        if (task != null) {
            task.cancel(true);
            heartbeatTask.set(null);
            log.info("Heartbeat stopped");
        }
    }

    public void logout() {
        if (isAuthenticated.get()) {
            log.info("Logging out");
            stopHeartbeat();

            String token = currentToken.get();
            if (token != null) {
                apiClient.logout(token)
                        .thenRun(() -> log.info("Logout successful"))
                        .exceptionally(throwable -> {
                            log.warn("Logout request failed", throwable);
                            return null;
                        });
            }

            isAuthenticated.set(false);
            currentToken.set(null);
            tokenExpiry.set(null);
        }
    }

    public boolean isAuthenticated() {
        return isAuthenticated.get() && currentToken.get() != null && isTokenValid();
    }

    private boolean isTokenValid() {
        Instant expiry = tokenExpiry.get();
        return expiry != null && Instant.now().isBefore(expiry);
    }

    public String getCurrentToken() {
        return isAuthenticated() ? currentToken.get() : null;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry.get();
    }

    public void shutdown() {
        log.info("Shutting down session manager");
        logout();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
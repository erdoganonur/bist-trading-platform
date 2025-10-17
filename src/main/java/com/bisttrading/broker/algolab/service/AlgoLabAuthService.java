package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.dto.response.LoginControlResponse;
import com.bisttrading.broker.algolab.dto.response.LoginUserResponse;
import com.bisttrading.broker.algolab.exception.AlgoLabAuthenticationException;
import com.bisttrading.broker.algolab.model.AlgoLabSession;
import com.bisttrading.broker.algolab.util.AlgoLabEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * AlgoLab authentication service.
 * Handles login flow: LoginUser -> SMS -> LoginUserControl
 */
@Service
@Slf4j
public class AlgoLabAuthService {

    private final AlgoLabProperties properties;
    private final AlgoLabEncryptionUtil encryptionUtil;
    private final AlgoLabRestClient restClient;
    private final AlgoLabSessionManager sessionManager;

    private volatile String token;

    public AlgoLabAuthService(
        AlgoLabProperties properties,
        AlgoLabEncryptionUtil encryptionUtil,
        AlgoLabRestClient restClient,
        AlgoLabSessionManager sessionManager
    ) {
        this.properties = properties;
        this.encryptionUtil = encryptionUtil;
        this.restClient = restClient;
        this.sessionManager = sessionManager;
    }

    /**
     * Auto-login on startup if enabled.
     */
    @PostConstruct
    public void init() {
        if (properties.getAuth().isAutoLogin()) {
            log.info("Auto-login enabled. Attempting to restore session...");
            boolean restored = restoreSession();

            if (!restored) {
                log.warn("Session restoration failed or expired.");
                log.warn("Manual login required. Please call loginUser() and loginUserControl(smsCode)");
            }
        }
    }

    /**
     * Step 1: Login with username and password from properties (auto-login).
     * Returns token. User must then receive SMS code.
     *
     * @return token
     */
    public String loginUser() {
        return loginUser(
            properties.getAuth().getUsername(),
            properties.getAuth().getPassword()
        );
    }

    /**
     * Step 1: Login with custom username and password.
     * Returns token. User must then receive SMS code.
     *
     * This method allows users to provide their own AlgoLab credentials
     * instead of using the ones configured in application properties.
     *
     * @param username AlgoLab broker username
     * @param password AlgoLab broker password
     * @return token
     */
    public String loginUser(String username, String password) {
        log.info("Starting LoginUser flow with custom credentials for username: {}", username);

        // Encrypt credentials
        String encryptedUsername = encryptionUtil.encrypt(username);
        String encryptedPassword = encryptionUtil.encrypt(password);

        Map<String, String> payload = Map.of(
            "username", encryptedUsername,
            "password", encryptedPassword
        );

        try {
            ResponseEntity<LoginUserResponse> response = restClient.post(
                "/api/LoginUser",
                payload,
                false, // not authenticated yet
                LoginUserResponse.class
            );

            LoginUserResponse body = response.getBody();
            if (body == null || !body.isSuccess()) {
                String errorMsg = body != null ? body.getMessage() : "Empty response";
                throw new AlgoLabAuthenticationException("LoginUser failed: " + errorMsg);
            }

            this.token = body.getContent().getToken();
            log.info("LoginUser successful for username: {}. Token received. SMS code sent to user.", username);
            return token;

        } catch (Exception e) {
            log.error("LoginUser failed for username: {}", username, e);
            throw new AlgoLabAuthenticationException("LoginUser failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: Verify SMS code and get authorization hash.
     *
     * @param smsCode SMS code received by user
     * @return authorization hash
     */
    public String loginUserControl(String smsCode) {
        if (token == null || token.isEmpty()) {
            throw new AlgoLabAuthenticationException("Token is null. Please call loginUser() first.");
        }

        log.info("Starting LoginUserControl flow with SMS code...");

        // Encrypt token and SMS code
        String encryptedToken = encryptionUtil.encrypt(token);
        String encryptedSmsCode = encryptionUtil.encrypt(smsCode);

        Map<String, String> payload = Map.of(
            "token", encryptedToken,
            "password", encryptedSmsCode
        );

        try {
            ResponseEntity<LoginControlResponse> response = restClient.post(
                "/api/LoginUserControl",
                payload,
                false, // not authenticated yet
                LoginControlResponse.class
            );

            LoginControlResponse body = response.getBody();
            if (body == null || !body.isSuccess()) {
                String errorMsg = body != null ? body.getMessage() : "Empty response";
                throw new AlgoLabAuthenticationException("LoginUserControl failed: " + errorMsg);
            }

            String hash = body.getContent().getHash();
            restClient.setHash(hash);

            // Save session
            sessionManager.saveSession(token, hash);

            log.info("LoginUserControl successful. Authentication complete.");
            return hash;

        } catch (Exception e) {
            log.error("LoginUserControl failed", e);
            throw new AlgoLabAuthenticationException("LoginUserControl failed: " + e.getMessage(), e);
        }
    }

    /**
     * Restores session from saved token and hash.
     *
     * @return true if session restored and valid, false otherwise
     */
    public boolean restoreSession() {
        AlgoLabSession session = sessionManager.loadSession();
        if (session == null) {
            log.debug("No saved session found");
            return false;
        }

        this.token = session.getToken();
        restClient.setHash(session.getHash());

        log.info("Session restored from storage. Validating with GetSubAccounts...");

        // CRITICAL: Validate session before using for WebSocket
        // This matches the Python implementation (API class with auto_login=True)
        // WebSocket requires fresh, validated session
        if (isAlive()) {
            log.info("✅ Session validated successfully - safe for WebSocket");
            return true;
        } else {
            log.warn("❌ Session validation failed - session expired or invalid");
            log.warn("WebSocket auto-connect will be skipped. Manual login required.");
            clearAuth();
            return false;
        }
    }

    /**
     * Checks if session is alive by calling GetSubAccounts.
     */
    public boolean isAlive() {
        if (!restClient.isAuthenticated()) {
            return false;
        }

        try {
            ResponseEntity<Map> response = restClient.post(
                "/api/GetSubAccounts",
                Map.of(),
                true,
                Map.class
            );

            Map<?, ?> body = response.getBody();
            return body != null && Boolean.TRUE.equals(body.get("success"));

        } catch (Exception e) {
            log.debug("Session alive check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Session refresh (keep-alive).
     * Called periodically if keep-alive is enabled.
     */
    @Scheduled(fixedDelayString = "${algolab.auth.refresh-interval-ms:300000}")
    public void sessionRefresh() {
        if (!properties.getAuth().isKeepAlive()) {
            return;
        }

        if (!restClient.isAuthenticated()) {
            log.debug("Skipping session refresh - not authenticated");
            return;
        }

        try {
            log.debug("Refreshing session...");
            ResponseEntity<Map> response = restClient.post(
                "/api/SessionRefresh",
                Map.of(),
                true,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Session refresh successful");
            } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.warn("Session expired during refresh. Re-login required.");
                clearAuth();
            }

        } catch (Exception e) {
            log.error("Session refresh failed: {}", e.getMessage());
        }
    }

    /**
     * Clears authentication (token and hash).
     */
    public void clearAuth() {
        this.token = null;
        restClient.clearAuth();
        sessionManager.clearSession();
        log.info("Authentication cleared");
    }

    /**
     * Checks if client is authenticated.
     */
    public boolean isAuthenticated() {
        return restClient.isAuthenticated();
    }
}

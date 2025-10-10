package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.config.AlgoLabProperties;
import com.bisttrading.broker.algolab.entity.AlgoLabSessionEntity;
import com.bisttrading.broker.algolab.model.AlgoLabSession;
import com.bisttrading.broker.algolab.repository.AlgoLabSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages AlgoLab session persistence (token and hash).
 * Supports both file-based and database storage.
 */
@Service
@Slf4j
public class AlgoLabSessionManager {

    private final AlgoLabProperties properties;
    private final ObjectMapper objectMapper;
    private final AlgoLabSessionRepository sessionRepository;

    // Current user ID (can be set from security context)
    private volatile UUID currentUserId;

    public AlgoLabSessionManager(
            AlgoLabProperties properties,
            ObjectMapper objectMapper,
            @Autowired(required = false) AlgoLabSessionRepository sessionRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Saves session to persistent storage.
     */
    @Transactional
    public void saveSession(String token, String hash) {
        AlgoLabSession session = AlgoLabSession.builder()
            .token(token)
            .hash(hash)
            .lastUpdate(LocalDateTime.now())
            .websocketConnected(false)
            .build();

        saveSessionObject(session);
    }

    /**
     * Saves session object to persistent storage.
     */
    @Transactional
    public void saveSessionObject(AlgoLabSession session) {
        if (session != null) {
            session.setLastUpdate(LocalDateTime.now());
        }

        String storage = properties.getSession().getStorage();

        if ("database".equalsIgnoreCase(storage)) {
            saveToDatabase(session);
        } else if ("file".equalsIgnoreCase(storage)) {
            saveToFile(session);
        } else {
            log.error("Unknown storage type: {}. Using file storage as fallback.", storage);
            saveToFile(session);
        }
    }

    /**
     * Saves session to database.
     */
    @Transactional
    private void saveToDatabase(AlgoLabSession session) {
        if (sessionRepository == null) {
            log.error("Database storage configured but repository not available. Using file storage.");
            saveToFile(session);
            return;
        }

        try {
            // Deactivate any existing active sessions for this user
            if (currentUserId != null) {
                sessionRepository.deactivateAllUserSessions(
                    currentUserId,
                    "NEW_SESSION",
                    LocalDateTime.now()
                );
            }

            // Create new session entity
            AlgoLabSessionEntity entity = AlgoLabSessionEntity.builder()
                .userId(currentUserId)
                .token(session.getToken())
                .hash(session.getHash())
                .expiresAt(LocalDateTime.now().plusHours(
                    properties.getSession().getExpirationHours()
                ))
                .websocketConnected(session.isWebsocketConnected())
                .websocketLastConnected(session.getWebsocketLastConnected())
                .active(true)
                .build();

            sessionRepository.save(entity);
            log.debug("Session saved to database for user: {}", currentUserId);

        } catch (Exception e) {
            log.error("Failed to save session to database", e);
            throw new RuntimeException("Failed to save session to database", e);
        }
    }

    /**
     * Updates WebSocket connection status in session.
     */
    @Transactional
    public void updateWebSocketStatus(boolean connected) {
        String storage = properties.getSession().getStorage();

        if ("database".equalsIgnoreCase(storage) && sessionRepository != null) {
            updateWebSocketStatusInDatabase(connected);
        } else {
            updateWebSocketStatusInFile(connected);
        }
    }

    /**
     * Updates WebSocket status in database.
     */
    @Transactional
    private void updateWebSocketStatusInDatabase(boolean connected) {
        try {
            Optional<AlgoLabSessionEntity> sessionOpt = currentUserId != null
                ? sessionRepository.findActiveSessionByUserId(currentUserId)
                : sessionRepository.findMostRecentActiveSession();

            if (sessionOpt.isPresent()) {
                AlgoLabSessionEntity entity = sessionOpt.get();
                entity.updateWebSocketStatus(connected);
                sessionRepository.save(entity);
                log.debug("WebSocket status updated in database: connected={}", connected);
            } else {
                log.warn("No active session found to update WebSocket status");
            }

        } catch (Exception e) {
            log.error("Failed to update WebSocket status in database", e);
        }
    }

    /**
     * Updates WebSocket status in file.
     */
    private void updateWebSocketStatusInFile(boolean connected) {
        AlgoLabSession session = loadSession();
        if (session != null) {
            session.setWebsocketConnected(connected);
            if (connected) {
                session.setWebsocketLastConnected(LocalDateTime.now());
            }
            saveSessionObject(session);
            log.debug("WebSocket status updated in file: connected={}", connected);
        }
    }

    /**
     * Sets the current user ID for session management.
     */
    public void setCurrentUserId(UUID userId) {
        this.currentUserId = userId;
        log.debug("Current user ID set: {}", userId);
    }

    /**
     * Gets the current user ID.
     */
    public UUID getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Loads session from persistent storage.
     */
    @Transactional(readOnly = true)
    public AlgoLabSession loadSession() {
        String storage = properties.getSession().getStorage();

        if ("database".equalsIgnoreCase(storage)) {
            return loadFromDatabase();
        } else if ("file".equalsIgnoreCase(storage)) {
            return loadFromFile();
        } else {
            log.error("Unknown storage type: {}. Using file storage as fallback.", storage);
            return loadFromFile();
        }
    }

    /**
     * Loads session from database.
     */
    @Transactional(readOnly = true)
    private AlgoLabSession loadFromDatabase() {
        if (sessionRepository == null) {
            log.error("Database storage configured but repository not available. Using file storage.");
            return loadFromFile();
        }

        try {
            Optional<AlgoLabSessionEntity> sessionOpt = currentUserId != null
                ? sessionRepository.findActiveSessionByUserId(currentUserId)
                : sessionRepository.findMostRecentActiveSession();

            if (sessionOpt.isEmpty()) {
                log.debug("No active session found in database");
                return null;
            }

            AlgoLabSessionEntity entity = sessionOpt.get();

            // Check if session is expired
            if (entity.isExpired()) {
                log.warn("Session found in database but expired. Deactivating...");
                entity.deactivate("EXPIRED");
                sessionRepository.save(entity);
                return null;
            }

            // Convert entity to session model
            AlgoLabSession session = AlgoLabSession.builder()
                .token(entity.getToken())
                .hash(entity.getHash())
                .lastUpdate(entity.getLastRefreshAt() != null
                    ? entity.getLastRefreshAt()
                    : entity.getCreatedAt())
                .websocketConnected(entity.isWebsocketConnected())
                .websocketLastConnected(entity.getWebsocketLastConnected())
                .build();

            log.debug("Session loaded from database for user: {}", currentUserId);
            return session;

        } catch (Exception e) {
            log.error("Failed to load session from database", e);
            return null;
        }
    }

    /**
     * Clears saved session.
     */
    @Transactional
    public void clearSession() {
        String storage = properties.getSession().getStorage();

        if ("database".equalsIgnoreCase(storage)) {
            clearSessionFromDatabase();
        } else if ("file".equalsIgnoreCase(storage)) {
            clearSessionFromFile();
        }
    }

    /**
     * Clears session from database.
     */
    @Transactional
    private void clearSessionFromDatabase() {
        if (sessionRepository == null) {
            log.warn("Database storage configured but repository not available");
            return;
        }

        try {
            if (currentUserId != null) {
                int deactivated = sessionRepository.deactivateAllUserSessions(
                    currentUserId,
                    "LOGOUT",
                    LocalDateTime.now()
                );
                log.info("Deactivated {} sessions for user: {}", deactivated, currentUserId);
            } else {
                // Deactivate the most recent session
                Optional<AlgoLabSessionEntity> sessionOpt =
                    sessionRepository.findMostRecentActiveSession();
                sessionOpt.ifPresent(session -> {
                    session.deactivate("LOGOUT");
                    sessionRepository.save(session);
                    log.info("Deactivated session: {}", session.getId());
                });
            }

        } catch (Exception e) {
            log.error("Failed to clear session from database", e);
        }
    }

    /**
     * Clears session from file.
     */
    private void clearSessionFromFile() {
        File file = new File(properties.getSession().getFilePath());
        if (file.exists() && file.delete()) {
            log.info("Session file deleted");
        }
    }

    /**
     * Scheduled task to clean up expired sessions.
     * Runs every hour.
     */
    @Scheduled(cron = "${algolab.session.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupExpiredSessions() {
        if (!"database".equalsIgnoreCase(properties.getSession().getStorage())
            || sessionRepository == null) {
            return;
        }

        try {
            // Deactivate expired sessions
            int deactivated = sessionRepository.deactivateExpiredSessions(LocalDateTime.now());
            if (deactivated > 0) {
                log.info("Deactivated {} expired sessions", deactivated);
            }

            // Delete old inactive sessions (older than retention period)
            int retentionDays = properties.getSession().getRetentionDays();
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            int deleted = sessionRepository.deleteOldInactiveSessions(cutoffDate);
            if (deleted > 0) {
                log.info("Deleted {} old inactive sessions (older than {} days)",
                    deleted, retentionDays);
            }

        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
        }
    }

    /**
     * Gets session statistics.
     */
    @Transactional(readOnly = true)
    public SessionStatistics getStatistics() {
        if (!"database".equalsIgnoreCase(properties.getSession().getStorage())
            || sessionRepository == null) {
            return SessionStatistics.builder()
                .storage("file")
                .totalActiveSessions(0)
                .build();
        }

        try {
            long totalActive = sessionRepository.countAllActiveSessions();
            long userActive = currentUserId != null
                ? sessionRepository.countActiveSessionsByUserId(currentUserId)
                : 0;

            return SessionStatistics.builder()
                .storage("database")
                .totalActiveSessions(totalActive)
                .userActiveSessions(userActive)
                .build();

        } catch (Exception e) {
            log.error("Failed to get session statistics", e);
            return SessionStatistics.builder()
                .storage("database")
                .totalActiveSessions(0)
                .error(e.getMessage())
                .build();
        }
    }

    /**
     * Session statistics model.
     */
    @lombok.Data
    @lombok.Builder
    public static class SessionStatistics {
        private String storage;
        private long totalActiveSessions;
        private long userActiveSessions;
        private String error;
    }

    /**
     * Saves session to JSON file.
     */
    private void saveToFile(AlgoLabSession session) {
        try {
            File file = new File(properties.getSession().getFilePath());
            objectMapper.writeValue(file, session);
            log.debug("Session saved to file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save session to file", e);
        }
    }

    /**
     * Loads session from JSON file.
     */
    private AlgoLabSession loadFromFile() {
        try {
            File file = new File(properties.getSession().getFilePath());
            if (!file.exists()) {
                log.debug("Session file does not exist: {}", file.getAbsolutePath());
                return null;
            }

            AlgoLabSession session = objectMapper.readValue(file, AlgoLabSession.class);
            log.debug("Session loaded from file: {}", file.getAbsolutePath());
            return session;

        } catch (IOException e) {
            log.error("Failed to load session from file", e);
            return null;
        }
    }
}

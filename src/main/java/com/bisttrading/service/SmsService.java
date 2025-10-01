package com.bisttrading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS service for sending phone verification codes and notifications.
 * Currently provides mock implementation for development.
 * In production, this would integrate with Turkish SMS providers (Turkcell, Vodafone, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${bist.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${bist.notifications.sms.sender-name:BISTRADE}")
    private String senderName;

    @Value("${bist.notifications.sms.provider:turkcell}")
    private String smsProvider;

    /**
     * Sends SMS verification code to user's phone number.
     *
     * @param phoneNumber User's phone number in international format (+905551234567)
     * @param verificationCode 6-digit verification code
     * @param firstName User's first name for personalization
     */
    public void sendVerificationSms(String phoneNumber, String verificationCode, String firstName) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled - skipping SMS verification for: {}", phoneNumber);
            return;
        }

        // Validate Turkish phone number format
        if (!isValidTurkishPhoneNumber(phoneNumber)) {
            log.error("Invalid Turkish phone number format: {}", phoneNumber);
            throw new IllegalArgumentException("Geçersiz telefon numarası formatı");
        }

        log.info("Sending SMS verification code to: {}", maskPhoneNumber(phoneNumber));

        try {
            String message = buildVerificationSmsTemplate(verificationCode, firstName);

            // TODO: Integrate with actual SMS service provider
            simulateSmsSending(phoneNumber, message);

            log.info("SMS verification code sent successfully to: {}", maskPhoneNumber(phoneNumber));

        } catch (Exception e) {
            log.error("Failed to send SMS verification code to: {}", maskPhoneNumber(phoneNumber), e);
            throw new RuntimeException("SMS doğrulama kodu gönderilemedi", e);
        }
    }

    /**
     * Sends security notification SMS for important account changes.
     *
     * @param phoneNumber User's phone number
     * @param message Security notification message
     */
    public void sendSecurityNotification(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.debug("SMS notifications disabled - skipping security notification for: {}", phoneNumber);
            return;
        }

        if (!isValidTurkishPhoneNumber(phoneNumber)) {
            log.error("Invalid Turkish phone number format: {}", phoneNumber);
            return; // Don't throw exception for notifications
        }

        log.info("Sending security notification SMS to: {}", maskPhoneNumber(phoneNumber));

        try {
            String smsMessage = String.format("%s\n\nBIST Trading Platform", message);
            simulateSmsSending(phoneNumber, smsMessage);

            log.info("Security notification SMS sent successfully to: {}", maskPhoneNumber(phoneNumber));

        } catch (Exception e) {
            log.error("Failed to send security notification SMS to: {}", maskPhoneNumber(phoneNumber), e);
            // Don't throw exception for notifications
        }
    }

    /**
     * Sends password change notification SMS.
     *
     * @param phoneNumber User's phone number
     * @param clientIp IP address from which password was changed
     */
    public void sendPasswordChangeNotification(String phoneNumber, String clientIp) {
        String message = String.format(
            "Hesabınızın şifresi değiştirildi. IP: %s. Bu işlemi siz yapmadıysanız derhal iletişime geçin.",
            clientIp
        );
        sendSecurityNotification(phoneNumber, message);
    }

    /**
     * Sends account login notification SMS for suspicious activity.
     *
     * @param phoneNumber User's phone number
     * @param clientIp IP address
     * @param location Approximate location
     */
    public void sendLoginNotification(String phoneNumber, String clientIp, String location) {
        String message = String.format(
            "Hesabınıza yeni bir giriş yapıldı. IP: %s, Konum: %s. Bu siz değilseniz derhal şifrenizi değiştirin.",
            clientIp,
            location != null ? location : "Bilinmeyen"
        );
        sendSecurityNotification(phoneNumber, message);
    }

    /**
     * Builds SMS verification template.
     */
    private String buildVerificationSmsTemplate(String verificationCode, String firstName) {
        return String.format(
            "%s, BIST Trading Platform doğrulama kodunuz: %s. 5 dakika geçerlidir. Kimseyle paylaşmayın.",
            firstName != null ? firstName : "Sayın müşterimiz",
            verificationCode
        );
    }

    /**
     * Validates Turkish phone number format.
     */
    private boolean isValidTurkishPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Turkish mobile numbers: +90 5XX XXX XX XX
        return phoneNumber.matches("^\\+905\\d{9}$");
    }

    /**
     * Masks phone number for logging (keeps first 3 and last 2 digits).
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***";
        }

        if (phoneNumber.startsWith("+90")) {
            // +905551234567 -> +9055****67
            return phoneNumber.substring(0, 5) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
        }

        // Generic masking
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }

    /**
     * Simulates SMS sending for development/testing.
     */
    private void simulateSmsSending(String phoneNumber, String message) {
        log.info("📱 SMS SIMULATION 📱");
        log.info("Provider: {}", smsProvider);
        log.info("Sender: {}", senderName);
        log.info("To: {}", maskPhoneNumber(phoneNumber));
        log.info("Message: {}", message);
        log.info("Message Length: {} characters", message.length());
        log.info("📱 END SMS SIMULATION 📱");

        // Simulate network delay
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate SMS provider response
        if (Math.random() < 0.05) { // 5% failure rate for testing
            throw new RuntimeException("SMS provider temporarily unavailable");
        }
    }

    /**
     * Normalizes Turkish phone number to international format.
     */
    public String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        // Handle different formats and convert to +90XXXXXXXXXX
        if (cleaned.startsWith("+90") && cleaned.length() == 13) {
            return cleaned;
        } else if (cleaned.startsWith("90") && cleaned.length() == 12) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return "+9" + cleaned;
        } else if (cleaned.length() == 10 && cleaned.startsWith("5")) {
            return "+90" + cleaned;
        }

        log.warn("Could not normalize phone number: {}", phoneNumber);
        return phoneNumber; // Return original if can't normalize
    }

    /**
     * Formats phone number for display.
     */
    public String formatPhoneNumber(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        if (normalized == null || !isValidTurkishPhoneNumber(normalized)) {
            return phoneNumber;
        }

        // Format as +90 5XX XXX XX XX
        return String.format("%s %s %s %s %s",
            normalized.substring(0, 3),   // +90
            normalized.substring(3, 6),   // 5XX
            normalized.substring(6, 9),   // XXX
            normalized.substring(9, 11),  // XX
            normalized.substring(11, 13)  // XX
        );
    }
}
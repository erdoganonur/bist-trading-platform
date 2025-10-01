package com.bisttrading.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Email service for sending user-related notifications.
 * Currently provides mock implementation for development.
 * In production, this would integrate with actual email providers (SMTP, SES, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${bist.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${bist.notifications.email.from-address:noreply@bisttrading.com}")
    private String fromAddress;

    @Value("${bist.notifications.email.from-name:BIST Trading Platform}")
    private String fromName;

    /**
     * Sends email verification code to user.
     *
     * @param emailAddress User's email address
     * @param verificationCode 6-digit verification code
     * @param firstName User's first name for personalization
     */
    public void sendEmailVerificationCode(String emailAddress, String verificationCode, String firstName) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled - skipping email verification for: {}", emailAddress);
            return;
        }

        log.info("Sending email verification code to: {}", emailAddress);

        try {
            // Mock email sending - in production, integrate with actual email service
            String subject = "BIST Trading Platform - E-posta Doğrulama";
            String body = buildEmailVerificationTemplate(verificationCode, firstName);

            // TODO: Integrate with actual email service (SMTP, AWS SES, SendGrid, etc.)
            simulateEmailSending(emailAddress, subject, body);

            log.info("Email verification code sent successfully to: {}", emailAddress);

        } catch (Exception e) {
            log.error("Failed to send email verification code to: {}", emailAddress, e);
            throw new RuntimeException("E-posta doğrulama kodu gönderilemedi", e);
        }
    }

    /**
     * Sends password change notification to user.
     *
     * @param emailAddress User's email address
     * @param clientIp IP address from which password was changed
     * @param userAgent User agent string
     */
    public void sendPasswordChangeNotification(String emailAddress, String clientIp, String userAgent) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled - skipping password change notification for: {}", emailAddress);
            return;
        }

        log.info("Sending password change notification to: {}", emailAddress);

        try {
            String subject = "BIST Trading Platform - Şifre Değişikliği";
            String body = buildPasswordChangeTemplate(clientIp, userAgent);

            simulateEmailSending(emailAddress, subject, body);

            log.info("Password change notification sent successfully to: {}", emailAddress);

        } catch (Exception e) {
            log.error("Failed to send password change notification to: {}", emailAddress, e);
            // Don't throw exception for notifications - they shouldn't fail the main operation
        }
    }

    /**
     * Sends account deactivation notification to user.
     *
     * @param emailAddress User's email address
     * @param firstName User's first name
     */
    public void sendAccountDeactivationNotification(String emailAddress, String firstName) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled - skipping deactivation notification for: {}", emailAddress);
            return;
        }

        log.info("Sending account deactivation notification to: {}", emailAddress);

        try {
            String subject = "BIST Trading Platform - Hesap Deaktivasyonu";
            String body = buildAccountDeactivationTemplate(firstName);

            simulateEmailSending(emailAddress, subject, body);

            log.info("Account deactivation notification sent successfully to: {}", emailAddress);

        } catch (Exception e) {
            log.error("Failed to send deactivation notification to: {}", emailAddress, e);
            // Don't throw exception for notifications
        }
    }

    /**
     * Sends welcome email to newly registered users.
     *
     * @param emailAddress User's email address
     * @param firstName User's first name
     * @param username User's username
     */
    public void sendWelcomeEmail(String emailAddress, String firstName, String username) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled - skipping welcome email for: {}", emailAddress);
            return;
        }

        log.info("Sending welcome email to: {}", emailAddress);

        try {
            String subject = "BIST Trading Platform'a Hoş Geldiniz!";
            String body = buildWelcomeTemplate(firstName, username);

            simulateEmailSending(emailAddress, subject, body);

            log.info("Welcome email sent successfully to: {}", emailAddress);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", emailAddress, e);
            // Don't throw exception for notifications
        }
    }

    /**
     * Builds email verification template.
     */
    private String buildEmailVerificationTemplate(String verificationCode, String firstName) {
        return String.format("""
            Merhaba %s,

            BIST Trading Platform hesabınızı doğrulamak için aşağıdaki kodu kullanın:

            Doğrulama Kodu: %s

            Bu kod 15 dakika geçerlidir.

            Eğer bu işlemi siz yapmadıysanız, lütfen bu e-postayı dikkate almayın.

            Saygılarımızla,
            BIST Trading Platform Ekibi
            """, firstName != null ? firstName : "Değerli Kullanıcı", verificationCode);
    }

    /**
     * Builds password change notification template.
     */
    private String buildPasswordChangeTemplate(String clientIp, String userAgent) {
        return String.format("""
            Merhaba,

            BIST Trading Platform hesabınızın şifresi başarıyla değiştirildi.

            İşlem Detayları:
            - Tarih: %s
            - IP Adresi: %s
            - Tarayıcı: %s

            Eğer bu işlemi siz yapmadıysanız, lütfen derhal bizimle iletişime geçin.

            Güvenlik için şifrenizi düzenli olarak değiştirmenizi öneririz.

            Saygılarımızla,
            BIST Trading Platform Ekibi
            """,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            clientIp,
            extractBrowserInfo(userAgent)
        );
    }

    /**
     * Builds account deactivation template.
     */
    private String buildAccountDeactivationTemplate(String firstName) {
        return String.format("""
            Merhaba %s,

            BIST Trading Platform hesabınız başarıyla deaktive edildi.

            Hesabınızı tekrar aktif hale getirmek isterseniz, müşteri hizmetlerimiz ile iletişime geçebilirsiniz.

            Bizi tercih ettiğiniz için teşekkür ederiz.

            Saygılarımızla,
            BIST Trading Platform Ekibi
            """, firstName != null ? firstName : "Değerli Kullanıcı");
    }

    /**
     * Builds welcome email template.
     */
    private String buildWelcomeTemplate(String firstName, String username) {
        return String.format("""
            Merhaba %s,

            BIST Trading Platform'a hoş geldiniz!

            Hesabınız başarıyla oluşturuldu:
            - Kullanıcı Adı: %s
            - Kayıt Tarihi: %s

            Hesabınızı tam olarak kullanabilmek için:
            1. E-posta adresinizi doğrulayın
            2. Telefon numaranızı doğrulayın
            3. KYC sürecini tamamlayın

            Herhangi bir sorunuz varsa, müşteri hizmetlerimiz 7/24 hizmetinizdedir.

            İyi yatırımlar dileriz!

            Saygılarımızla,
            BIST Trading Platform Ekibi
            """,
            firstName != null ? firstName : "Değerli Kullanıcı",
            username,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        );
    }

    /**
     * Simulates email sending for development/testing.
     */
    private void simulateEmailSending(String to, String subject, String body) {
        log.info("📧 EMAIL SIMULATION 📧");
        log.info("From: {} <{}>", fromName, fromAddress);
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body:\n{}", body);
        log.info("📧 END EMAIL SIMULATION 📧");

        // Simulate network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Extracts browser information from user agent.
     */
    private String extractBrowserInfo(String userAgent) {
        if (userAgent == null) {
            return "Bilinmeyen tarayıcı";
        }

        if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("Edge")) {
            return "Microsoft Edge";
        } else {
            return "Bilinmeyen tarayıcı";
        }
    }
}
package com.bisttrading.core.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Error codes used throughout the BIST Trading Platform.
 * Each error code contains a unique code and Turkish localized message.
 */
@Getter
public enum ErrorCodes {

    // General System Errors (1000-1999)
    INTERNAL_SERVER_ERROR("BIST_1000", "Sistem hatası oluştu. Lütfen daha sonra tekrar deneyiniz."),
    VALIDATION_ERROR("BIST_1001", "Girilen bilgilerde hata bulunmaktadır."),
    UNAUTHORIZED("BIST_1002", "Bu işlem için yetkiniz bulunmamaktadır."),
    FORBIDDEN("BIST_1003", "Bu kaynağa erişim izniniz bulunmamaktadır."),
    NOT_FOUND("BIST_1004", "İstenen kaynak bulunamadı."),
    BAD_REQUEST("BIST_1005", "Geçersiz istek gönderildi."),
    METHOD_NOT_ALLOWED("BIST_1006", "Bu HTTP metodu desteklenmiyor."),
    CONFLICT("BIST_1007", "İşlem çakışması oluştu."),
    TOO_MANY_REQUESTS("BIST_1008", "Çok fazla istek gönderildi. Lütfen bekleyiniz."),
    SERVICE_UNAVAILABLE("BIST_1009", "Servis şu anda kullanılamıyor."),

    // Authentication & Authorization Errors (2000-2999)
    INVALID_CREDENTIALS("BIST_2000", "Kullanıcı adı veya şifre hatalı."),
    TOKEN_EXPIRED("BIST_2001", "Oturum süresi dolmuş. Lütfen tekrar giriş yapınız."),
    TOKEN_INVALID("BIST_2002", "Geçersiz oturum bilgisi."),
    TOKEN_MISSING("BIST_2003", "Oturum bilgisi eksik."),
    ACCOUNT_LOCKED("BIST_2004", "Hesabınız kilitlenmiştir. Müşteri hizmetleri ile iletişime geçiniz."),
    ACCOUNT_DISABLED("BIST_2005", "Hesabınız devre dışı bırakılmıştır."),
    ACCOUNT_EXPIRED("BIST_2006", "Hesap süresi dolmuştur."),
    PASSWORD_EXPIRED("BIST_2007", "Şifrenizin süresi dolmuştur. Lütfen şifrenizi güncelleyiniz."),
    INVALID_REFRESH_TOKEN("BIST_2008", "Geçersiz yenileme token'ı."),

    // User Management Errors (3000-3999)
    USER_NOT_FOUND("BIST_3000", "Kullanıcı bulunamadı."),
    USER_ALREADY_EXISTS("BIST_3001", "Bu kullanıcı zaten mevcut."),
    EMAIL_ALREADY_EXISTS("BIST_3002", "Bu e-posta adresi zaten kullanılmaktadır."),
    INVALID_EMAIL_FORMAT("BIST_3003", "Geçersiz e-posta formatı."),
    INVALID_TC_KIMLIK("BIST_3004", "Geçersiz TC Kimlik numarası."),
    INVALID_PHONE_NUMBER("BIST_3005", "Geçersiz telefon numarası."),
    WEAK_PASSWORD("BIST_3006", "Şifre güvenlik kriterlerini karşılamıyor."),
    PASSWORD_MISMATCH("BIST_3007", "Şifreler eşleşmiyor."),
    INVALID_BIRTH_DATE("BIST_3008", "Geçersiz doğum tarihi."),
    USER_UNDER_AGE("BIST_3009", "Yaş sınırı altında kullanıcı kaydı yapılamaz."),

    // Trading Errors (4000-4999)
    INSUFFICIENT_BALANCE("BIST_4000", "Yetersiz bakiye."),
    INVALID_ORDER_QUANTITY("BIST_4001", "Geçersiz emir miktarı."),
    INVALID_ORDER_PRICE("BIST_4002", "Geçersiz emir fiyatı."),
    ORDER_NOT_FOUND("BIST_4003", "Emir bulunamadı."),
    ORDER_ALREADY_EXECUTED("BIST_4004", "Emir zaten gerçekleştirilmiş."),
    ORDER_CANCELLED("BIST_4005", "Emir iptal edilmiş."),
    MARKET_CLOSED("BIST_4006", "Piyasa kapalı."),
    SYMBOL_NOT_FOUND("BIST_4007", "Hisse senedi bulunamadı."),
    SYMBOL_SUSPENDED("BIST_4008", "Hisse senedi işlemleri durdurulmuş."),
    INVALID_ORDER_TYPE("BIST_4009", "Geçersiz emir tipi."),
    POSITION_NOT_FOUND("BIST_4010", "Pozisyon bulunamadı."),
    INSUFFICIENT_POSITION("BIST_4011", "Yetersiz pozisyon."),

    // Portfolio Errors (5000-5999)
    PORTFOLIO_NOT_FOUND("BIST_5000", "Portföy bulunamadı."),
    PORTFOLIO_ACCESS_DENIED("BIST_5001", "Portföy erişim izni yok."),
    INVALID_PORTFOLIO_NAME("BIST_5002", "Geçersiz portföy adı."),
    PORTFOLIO_LIMIT_EXCEEDED("BIST_5003", "Maksimum portföy sayısı aşıldı."),

    // Payment & Banking Errors (6000-6999)
    INVALID_IBAN("BIST_6000", "Geçersiz IBAN numarası."),
    BANK_NOT_SUPPORTED("BIST_6001", "Bu banka desteklenmiyor."),
    PAYMENT_FAILED("BIST_6002", "Ödeme işlemi başarısız."),
    WITHDRAWAL_LIMIT_EXCEEDED("BIST_6003", "Para çekme limiti aşıldı."),
    DEPOSIT_LIMIT_EXCEEDED("BIST_6004", "Para yatırma limiti aşıldı."),
    INVALID_AMOUNT("BIST_6005", "Geçersiz tutar."),
    MINIMUM_AMOUNT_ERROR("BIST_6006", "Minimum tutar şartı sağlanmıyor."),
    MAXIMUM_AMOUNT_ERROR("BIST_6007", "Maksimum tutar şartı aşılıyor."),

    // Data Validation Errors (7000-7999)
    FIELD_REQUIRED("BIST_7000", "Zorunlu alan boş bırakılamaz."),
    FIELD_TOO_LONG("BIST_7001", "Alan maksimum karakter sayısını aşıyor."),
    FIELD_TOO_SHORT("BIST_7002", "Alan minimum karakter sayısını karşılamıyor."),
    INVALID_DATE_FORMAT("BIST_7003", "Geçersiz tarih formatı."),
    INVALID_NUMBER_FORMAT("BIST_7004", "Geçersiz sayı formatı."),
    FUTURE_DATE_NOT_ALLOWED("BIST_7005", "Gelecek tarih girilemez."),
    PAST_DATE_NOT_ALLOWED("BIST_7006", "Geçmiş tarih girilemez."),

    // External Service Errors (8000-8999)
    EXTERNAL_SERVICE_ERROR("BIST_8000", "Dış servis hatası."),
    BIST_API_ERROR("BIST_8001", "BIST API bağlantı hatası."),
    BANK_API_ERROR("BIST_8002", "Banka API bağlantı hatası."),
    SMS_SERVICE_ERROR("BIST_8003", "SMS servisi hatası."),
    EMAIL_SERVICE_ERROR("BIST_8004", "E-posta servisi hatası."),

    // Business Logic Errors (9000-9999)
    TRADING_HOURS_VIOLATION("BIST_9000", "İşlem saatleri dışında emir verilemez."),
    DAILY_LIMIT_EXCEEDED("BIST_9001", "Günlük işlem limiti aşıldı."),
    MONTHLY_LIMIT_EXCEEDED("BIST_9002", "Aylık işlem limiti aşıldı."),
    RISK_LIMIT_EXCEEDED("BIST_9003", "Risk limiti aşıldı."),
    MARGIN_CALL("BIST_9004", "Teminat çağrısı."),
    POSITION_LIMIT_EXCEEDED("BIST_9005", "Pozisyon limiti aşıldı.");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the formatted error message with additional parameters.
     *
     * @param params Parameters to format the message
     * @return Formatted error message
     */
    public String getFormattedMessage(Object... params) {
        return String.format(this.message, params);
    }

    /**
     * Finds error code by code string.
     *
     * @param code Error code string
     * @return ErrorCodes enum or null if not found
     */
    public static ErrorCodes findByCode(String code) {
        for (ErrorCodes errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}
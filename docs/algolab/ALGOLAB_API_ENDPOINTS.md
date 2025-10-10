# AlgoLab API - Endpoint Reference

**Doküman Tarihi:** 2025-10-09
**Python Kaynak:** `algolab.py` ve `config.py`
**Base URL:** `https://www.algolab.com.tr/api`

---

## 1. Endpoint Genel Bakış

| Kategori | Endpoint Sayısı | Durum |
|----------|----------------|-------|
| Authentication | 2 | ✅ Aktif |
| Account & Portfolio | 5 | ✅ Aktif |
| Market Data | 2 | ✅ Aktif |
| Order Management | 5 | ✅ Aktif |
| VIOP (Futures) | 4 | ⚠️ VIOP hesabı gerekli |
| Account Operations | 2 | ✅ Aktif |
| Order History | 2 | ❌ 404 Error (Python'da not edilmiş) |
| **TOPLAM** | **22** | - |

---

## 2. Authentication Endpoints

### 2.1 POST /api/LoginUser

**Açıklama:** İlk login adımı. Kullanıcı TC kimlik no ve Denizbank şifresi ile giriş yapar.

**Auth Required:** ❌ No (sadece APIKEY header)

**Request Headers:**
```json
{
  "APIKEY": "API-{base64_key}"
}
```

**Request Body:**
```json
{
  "username": "<encrypted_tc_kimlik_no>",
  "password": "<encrypted_denizbank_password>"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Login başarılı",
  "content": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Response (Failure):**
```json
{
  "success": false,
  "message": "Kullanıcı adı veya şifre hatalı"
}
```

**Python Kod:** `algolab.py:85-112`

---

### 2.2 POST /api/LoginUserControl

**Açıklama:** SMS doğrulama adımı. LoginUser'dan dönen token ve SMS kodu ile hash alınır.

**Auth Required:** ❌ No (sadece APIKEY header)

**Request Headers:**
```json
{
  "APIKEY": "API-{base64_key}"
}
```

**Request Body:**
```json
{
  "token": "<encrypted_token_from_loginUser>",
  "password": "<encrypted_sms_code>"
}
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Login kontrolü başarılı",
  "content": {
    "hash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6..."
  }
}
```

**Python Kod:** `algolab.py:114-145`

---

## 3. Session Management

### 3.1 POST /api/SessionRefresh

**Açıklama:** Oturum süresini uzatmak için kullanılır. Keep-alive mekanizması.

**Auth Required:** ✅ Yes

**Request Headers:**
```json
{
  "APIKEY": "API-{base64_key}",
  "Checker": "<sha256_hash>",
  "Authorization": "<hash_from_loginControl>"
}
```

**Request Body:**
```json
{}
```

**Response (Success):**
```json
{
  "success": true
}
```

**Response (Session Expired):**
```
HTTP 401 Unauthorized
```

**Python Kod:** `algolab.py:150-163`

**Kullanım:**
- Python'da her 5 dakikada bir otomatik çağrılıyor (`ping()` thread)
- Java'da `@Scheduled(fixedDelay = 300000)` ile yapılabilir

---

## 4. Account & Portfolio Endpoints

### 4.1 POST /api/GetSubAccounts

**Açıklama:** Alt hesapları (101, 102 vb.) ve limitlerini getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "subAccounts": [
      {
        "accountNo": "101",
        "balance": 50000.00,
        "buyingPower": 75000.00,
        "blockedAmount": 0.00
      }
    ]
  }
}
```

**Python Kod:** `algolab.py:181-188`

---

### 4.2 POST /api/InstantPosition

**Açıklama:** Anlık pozisyonları ve portföy değerini getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```
- `Subaccount` boş gönderilirse aktif hesap bilgileri gelir

**Response:**
```json
{
  "success": true,
  "content": {
    "positions": [
      {
        "symbol": "AKBNK",
        "quantity": 1000,
        "averagePrice": 15.50,
        "currentPrice": 15.75,
        "pnl": 250.00,
        "pnlPercent": 1.61
      }
    ],
    "totalValue": 78500.00,
    "totalPnl": 2750.00,
    "cashBalance": 25000.00
  }
}
```

**Python Kod:** `algolab.py:191-202`

---

### 4.3 POST /api/TodaysTransaction

**Açıklama:** Günün işlemlerini (bekleyen, gerçekleşen, iptal edilen) getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "transactions": [
      {
        "orderId": "ORD-12345",
        "symbol": "THYAO",
        "side": "BUY",
        "price": 125.50,
        "quantity": 500,
        "status": "EXECUTED",
        "timestamp": "2024-09-24T10:30:00Z"
      }
    ]
  }
}
```

**Python Kod:** `algolab.py:205-216`

---

### 4.4 POST /api/CashFlow

**Açıklama:** Nakit akışı bilgilerini getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "cashIn": 100000.00,
    "cashOut": 25000.00,
    "netCashFlow": 75000.00
  }
}
```

**Python Kod:** `algolab.py:282-290`

---

### 4.5 POST /api/AccountExtre

**Açıklama:** Hesap özeti (ekstresi) - tarih aralığı ile filtrelenebilir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "start": "2024-09-01T00:00:00.0000",
  "end": "2024-09-30T23:59:59.9999",
  "Subaccount": ""
}
```
- `start` ve `end` ISO format datetime
- Nullable - null gönderilirse tüm geçmiş gelir

**Response:**
```json
{
  "success": true,
  "content": {
    "transactions": [
      {
        "date": "2024-09-24",
        "description": "AKBNK Alış",
        "debit": 15750.00,
        "credit": 0.00,
        "balance": 84250.00
      }
    ]
  }
}
```

**Python Kod:** `algolab.py:263-280`

---

## 5. Market Data Endpoints

### 5.1 POST /api/GetEquityInfo

**Açıklama:** Sembol bilgilerini getirir (tavan, taban, son fiyat, vb.)

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "symbol": "AKBNK"
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "symbol": "AKBNK",
    "lastPrice": 15.75,
    "dailyHigh": 15.90,
    "dailyLow": 15.50,
    "ceiling": 16.50,
    "floor": 15.00,
    "volume": 125000000,
    "timestamp": "2024-09-24T15:30:00Z"
  }
}
```

**Python Kod:** `algolab.py:166-178`

---

### 5.2 POST /api/GetCandleData

**Açıklama:** Son 250 bar OHLCV verisi getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "symbol": "TSKB",
  "period": "1440"
}
```

**Period değerleri (dakika cinsinden):**
- `1` - 1 dakikalık
- `5` - 5 dakikalık
- `15` - 15 dakikalık
- `30` - 30 dakikalık
- `60` - 1 saatlik
- `120` - 2 saatlik
- `240` - 4 saatlik
- `480` - 8 saatlik
- `1440` - Günlük (1 day = 1440 minutes)

**Response:**
```json
{
  "success": true,
  "content": {
    "candles": [
      {
        "timestamp": "2024-09-24T00:00:00Z",
        "open": 2.01,
        "high": 2.05,
        "low": 1.98,
        "close": 2.03,
        "volume": 1500000
      },
      {
        "timestamp": "2024-09-23T00:00:00Z",
        "open": 1.99,
        "high": 2.02,
        "low": 1.97,
        "close": 2.01,
        "volume": 1200000
      }
    ]
  }
}
```

**NOT:** Son 250 bar limit var. Daha eski veri için başka endpoint olabilir (dokümanda belirtilmemiş).

**Python Kod:** `algolab.py:292-314`

---

## 6. Order Management Endpoints

### 6.1 POST /api/SendOrder

**Açıklama:** Yeni emir gönderir (alış/satış).

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "symbol": "TSKB",
  "direction": "BUY",
  "pricetype": "limit",
  "price": "2.01",
  "lot": "1",
  "sms": true,
  "email": false,
  "subAccount": ""
}
```

**Parametreler:**
- `symbol` (String): Sembol kodu (örn. "AKBNK", "THYAO")
- `direction` (String): "BUY" veya "SELL"
- `pricetype` (String): "limit" veya "piyasa" (market)
- `price` (String): Fiyat (limit emri için gerekli)
- `lot` (String): Lot miktarı (1 lot = 100 hisse)
- `sms` (Boolean): SMS bildirimi gönderilsin mi?
- `email` (Boolean): Email bildirimi gönderilsin mi?
- `subAccount` (String): Alt hesap (boş gönderilirse aktif hesap)

**Response:**
```json
{
  "success": true,
  "message": "Emir başarıyla gönderildi",
  "content": {
    "orderId": "ORD-12345678",
    "brokerOrderId": "ALG-987654321",
    "status": "SUBMITTED"
  }
}
```

**Python Kod:** `algolab.py:319-366`

---

### 6.2 POST /api/ModifyOrder

**Açıklama:** Mevcut emri değiştirir (fiyat ve/veya miktar).

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "id": "001VEV",
  "price": "2.04",
  "lot": "0",
  "viop": false,
  "subAccount": ""
}
```

**Parametreler:**
- `id` (String): Emrin ID'si
- `price` (String): Yeni fiyat
- `lot` (String): Lot miktarı (VIOP emri için gerekli, equity için "0" gönderilebilir)
- `viop` (Boolean): VIOP emri mi? (false = hisse senedi)
- `subAccount` (String): Alt hesap

**Response:**
```json
{
  "success": true,
  "message": "Emir başarıyla değiştirildi",
  "content": {
    "orderId": "001VEV",
    "newPrice": "2.04",
    "status": "MODIFIED"
  }
}
```

**Python Kod:** `algolab.py:369-407`

---

### 6.3 POST /api/DeleteOrder

**Açıklama:** Emri iptal eder (hisse senedi emirleri için).

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "id": "001VEV",
  "subAccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "message": "Emir başarıyla iptal edildi",
  "content": {
    "orderId": "001VEV",
    "status": "CANCELLED"
  }
}
```

**Python Kod:** `algolab.py:410-439`

---

### 6.4 POST /api/DeleteOrderViop

**Açıklama:** VIOP emrini iptal eder (kısmi iptal destekli).

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "id": "001VEV",
  "adet": "1",
  "subAccount": ""
}
```

**Parametreler:**
- `id` (String): Emrin ID'si
- `adet` (String): İptal edilecek miktar
- `subAccount` (String): Alt hesap

**Response:**
```json
{
  "success": true,
  "message": "VIOP emri iptal edildi",
  "content": {
    "orderId": "001VEV",
    "cancelledQuantity": "1",
    "status": "CANCELLED"
  }
}
```

**Python Kod:** `algolab.py:442-474`

---

### 6.5 POST /api/GetEquityOrderHistory

**Açıklama:** Hisse senedi emrinin geçmişini getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "id": "001VEV",
  "subAccount": ""
}
```

**Durum:** ❌ **404 Error** (Python kodunda not edilmiş - `config.py:46`)

**Response:** Henüz aktif değil.

**Python Kod:** `algolab.py:477-506`

---

### 6.6 POST /api/GetViopOrderHistory

**Açıklama:** VIOP emrinin geçmişini getirir.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "id": "001VEV",
  "subAccount": ""
}
```

**Durum:** ❌ **404 Error** (Python kodunda not edilmiş - `config.py:47`)

**Python Kod:** `algolab.py:509-539`

---

## 7. VIOP (Futures) Endpoints

### 7.1 POST /api/ViopCustomerOverall

**Açıklama:** VIOP müşteri genel bakış bilgileri.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "totalMargin": 50000.00,
    "usedMargin": 25000.00,
    "freeMargin": 25000.00,
    "openPositions": 3
  }
}
```

**Python Kod:** `algolab.py:219-227`

---

### 7.2 POST /api/ViopCustomerTransactions

**Açıklama:** VIOP müşteri işlemleri.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "transactions": [
      {
        "orderId": "VIOP-123",
        "symbol": "XU030C1024",
        "side": "BUY",
        "quantity": 1,
        "price": 10500.00,
        "status": "EXECUTED"
      }
    ]
  }
}
```

**Python Kod:** `algolab.py:230-238`

---

### 7.3 POST /api/ViopCollateralInfo

**Açıklama:** VIOP teminat bilgileri.

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "collateral": 50000.00,
    "requiredMargin": 25000.00,
    "excessMargin": 25000.00
  }
}
```

**Python Kod:** `algolab.py:242-250`

---

### 7.4 POST /api/RiskSimulation

**Açıklama:** Risk simülasyonu (VIOP için).

**Auth Required:** ✅ Yes

**Request Body:**
```json
{
  "Subaccount": ""
}
```

**Response:**
```json
{
  "success": true,
  "content": {
    "potentialLoss": -5000.00,
    "potentialProfit": 8000.00,
    "riskLevel": "MEDIUM"
  }
}
```

**Python Kod:** `algolab.py:253-261`

---

## 8. WebSocket API

**WebSocket URL:** `wss://www.algolab.com.tr/api/ws`

**Durum:** Python kodda import edilmiş (`config.py:10`) ama kullanım örneği yok.

**Tahmin edilen kullanım:**
- Real-time price updates
- Order book updates
- Order status updates

**Java İmplementasyonu için gerekli:**
- Spring WebSocket veya Stomp client
- Subscribe/unsubscribe mekanizması
- Reconnection logic
- Message handler

**Örnek subscription format (tahmin):**
```json
{
  "action": "subscribe",
  "channel": "ticker",
  "symbol": "AKBNK"
}
```

---

## 9. Error Response Format

**Standard Error Response:**
```json
{
  "success": false,
  "message": "Hata mesajı",
  "error": "ERROR_CODE",
  "details": "Detaylı hata açıklaması"
}
```

**HTTP Status Codes:**
- `200` - Success
- `400` - Bad Request (eksik/hatalı parametreler)
- `401` - Unauthorized (geçersiz hash/token)
- `404` - Not Found (endpoint aktif değil veya bulunamadı)
- `429` - Too Many Requests (rate limit aşıldı)
- `500` - Internal Server Error

---

## 10. Rate Limiting

**Tespit edilen limit:**
- **Minimum 5 saniye** aralıkla request atılmalı
- Python kodda global lock ile implement edilmiş

**Java'da önerilen çözüm:**
- Guava RateLimiter: `RateLimiter.create(0.2)` (0.2 request/second = 5 saniye interval)
- Resilience4j RateLimiter

**Aşırı kullanımda:**
- HTTP 429 dönebilir
- Hesap geçici olarak bloklanabilir

---

## 11. Endpoint Önceliklendirme (Java İmplementasyonu için)

### 🔴 Öncelik 1: Kritik (Hemen implement edilmeli)
1. `/api/LoginUser`
2. `/api/LoginUserControl`
3. `/api/SessionRefresh`
4. `/api/SendOrder`
5. `/api/DeleteOrder`

### 🟠 Öncelik 2: Yüksek (İlk sprint'te)
6. `/api/GetSubAccounts`
7. `/api/InstantPosition`
8. `/api/TodaysTransaction`
9. `/api/GetEquityInfo`
10. `/api/GetCandleData`

### 🟡 Öncelik 3: Orta (İkinci sprint'te)
11. `/api/ModifyOrder`
12. `/api/CashFlow`
13. `/api/AccountExtre`

### 🟢 Öncelik 4: Düşük (VIOP kullanıcıları için)
14. `/api/ViopCustomerOverall`
15. `/api/ViopCustomerTransactions`
16. `/api/ViopCollateralInfo`
17. `/api/RiskSimulation`
18. `/api/DeleteOrderViop`

### 🔵 Öncelik 5: Opsiyonel (Henüz aktif değil)
19. `/api/GetEquityOrderHistory` (404)
20. `/api/GetViopOrderHistory` (404)
21. WebSocket API (dokümantasyon eksik)

---

## 12. Java Service Interface Önerisi

```java
public interface AlgoLabApiService {

    // Authentication
    String loginUser(String username, String password);
    String loginUserControl(String token, String smsCode);
    boolean sessionRefresh();

    // Account
    List<SubAccount> getSubAccounts();
    InstantPositionResponse getInstantPosition(String subAccount);
    List<Transaction> getTodaysTransactions(String subAccount);
    CashFlowResponse getCashFlow(String subAccount);
    AccountExtreResponse getAccountExtre(String subAccount, LocalDateTime start, LocalDateTime end);

    // Market Data
    EquityInfoResponse getEquityInfo(String symbol);
    List<OHLCVData> getCandleData(String symbol, int periodMinutes);

    // Orders
    SendOrderResponse sendOrder(SendOrderRequest request);
    ModifyOrderResponse modifyOrder(String orderId, ModifyOrderRequest request);
    CancelOrderResponse deleteOrder(String orderId, String subAccount);

    // VIOP
    ViopCustomerOverallResponse getViopCustomerOverall(String subAccount);
    List<ViopTransaction> getViopCustomerTransactions(String subAccount);
    ViopCollateralInfoResponse getViopCollateralInfo(String subAccount);
    RiskSimulationResponse getRiskSimulation(String subAccount);
}
```

---

**Doküman Sonu**
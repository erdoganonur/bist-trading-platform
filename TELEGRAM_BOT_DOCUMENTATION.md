# BIST Trading Platform - Telegram Bot Dokümantasyonu

## 📋 İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Tüm Komutlar](#tüm-komutlar)
3. [Özellik Durumları](#özellik-durumları)
4. [Kullanıcı Akışları](#kullanıcı-akışları)
5. [Teknik Detaylar](#teknik-detaylar)

---

## 🎯 Genel Bakış

BIST Trading Platform Telegram Bot, kullanıcıların Borsa İstanbul işlemlerini Telegram üzerinden yönetmelerine olanak sağlar. Bot, AlgoLab broker entegrasyonu ile gerçek zamanlı emir gönderme, pozisyon takibi ve hesap yönetimi özellikleri sunar.

**Bot Kullanıcı Adı:** `@bist_trading_dev_bot`
**Platform:** Telegram Bot API 7.10.0
**Backend:** Spring Boot 3.3.4 + AlgoLab REST & WebSocket API

---

## 📱 Tüm Komutlar

### 🔹 Temel Komutlar (Her zaman erişilebilir)

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/start` | Bot'u başlatır ve hoş geldin mesajı gösterir | ✅ Çalışıyor |
| `/help` | Komut listesi ve yardım bilgisi | ✅ Çalışıyor |
| `/menu` | Ana menüyü gösterir | ✅ Çalışıyor |

### 🔐 Giriş ve Kimlik Doğrulama

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/login` | Platform'a giriş yapar (username + password) | ✅ Çalışıyor |
| `/logout` | Platform'dan çıkış yapar | ✅ Çalışıyor |

### 📊 Piyasa Verileri

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/quote SEMBOL` | Hisse fiyat bilgisi gösterir | ⚠️ Mock (Geliştirme aşamasında) |
| `/search ARAMA` | Sembol arama yapar | ⚠️ Mock (Geliştirme aşamasında) |
| `/sectors` | Sektör listesini gösterir | ⚠️ Mock (Geliştirme aşamasında) |

**Piyasa Verileri Menüsü (menu:market):**
- 🔍 Sembol Ara → ⚠️ Mock
- 📈 Hisse Fiyatı → ⚠️ Mock
- 📊 Sektörler → ⚠️ Mock
- 🔥 Popüler Hisseler → ⚠️ Mock

### 💼 Broker İşlemleri

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/algolab` | AlgoLab broker hesabı bağlantısı | ✅ Çalışıyor |
| `/account` | Hesap bilgilerini gösterir | ⚠️ Mock (Geliştirme aşamasında) |
| `/positions` | Açık pozisyonları listeler | ✅ Çalışıyor (AlgoLab API) |

**Broker Menüsü (menu:broker):**
- 🔗 AlgoLab Bağlan → ✅ Çalışıyor (3 adımlı akış: username, password, OTP)
- ✅ AlgoLab Durumu → ✅ Çalışıyor (Token ve geçerlilik bilgisi)
- 💰 Hesap Bilgileri → ⚠️ Mock
- 📊 Pozisyonlar → ✅ Çalışıyor (Gerçek AlgoLab verileri)

### 📋 Emir İşlemleri

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/order` | Yeni emir oluşturur | ✅ Çalışıyor |
| `/orders` | Bekleyen emirleri listeler | ✅ Çalışıyor |
| `/history` | Emir geçmişini gösterir | ⚠️ Mock (Geliştirme aşamasında) |

**Emir Menüsü (menu:orders):**
- 📋 Bekleyen Emirler → ✅ Çalışıyor (AlgoLab TodaysTransaction API)
- ➕ Yeni Emir → ✅ Çalışıyor (5 adımlı akış)
- ✏️ Emir Düzenleme → ✅ Çalışıyor (ModifyOrder API)
- ❌ Emir İptali → ✅ Çalışıyor (DeleteOrder API)

### ⭐ Watchlist (İzleme Listesi)

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/watchlist` | İzlenen hisseleri listeler | ⚠️ Mock (Geliştirme aşamasında) |
| `/watch SEMBOL` | İzlemeye ekler | ⚠️ Mock (Geliştirme aşamasında) |
| `/unwatch SEMBOL` | İzlemeden çıkarır | ⚠️ Mock (Geliştirme aşamasında) |

### 👤 Hesap Yönetimi

| Komut | Açıklama | Durum |
|-------|----------|-------|
| `/profile` | Profil bilgilerini gösterir | ⚠️ Mock (Geliştirme aşamasında) |
| `/settings` | Bot ayarlarını düzenler | ⚠️ Mock (Geliştirme aşamasında) |

---

## ✅ Özellik Durumları

### 🟢 Çalışan Özellikler (Production Ready)

#### 1. Kimlik Doğrulama
- ✅ Platform girişi (JWT tabanlı)
- ✅ AlgoLab broker hesabı bağlantısı (3 adımlı: username, password, OTP)
- ✅ Oturum yönetimi (Redis'te saklanıyor)
- ✅ Token geçerlilik kontrolü
- ✅ Otomatik çıkış

#### 2. Emir Yönetimi (AlgoLab API)
- ✅ Bekleyen emirleri listeleme (TodaysTransaction API)
- ✅ Yeni emir gönderme (SendOrder API)
  - Sembol girişi
  - Alış/Satış seçimi (Keyboard buttons)
  - Piyasa/Limit seçimi (Keyboard buttons)
  - Limit fiyat girişi
  - Lot miktarı girişi (1 lot = 100 adet)
- ✅ Emir iptali (DeleteOrder API)
- ✅ Emir düzenleme (ModifyOrder API)
- ✅ Emir durumu takibi (WAITING, PARTIAL, DONE, DELETED)

#### 3. Pozisyon Takibi (AlgoLab API)
- ✅ Açık pozisyonları listeleme (InstantPosition API)
- ✅ Pozisyon detayları:
  - Sembol kodu
  - Miktar (adet)
  - Ortalama maliyet
  - Güncel fiyat
  - Kar/Zarar (TL ve %)
- ✅ Pozisyon gruplama (aynı sembolden birden fazla işlem varsa)
- ✅ Nakit ve özet satırlarını filtreleme

#### 4. Konuşma Akışı Yönetimi
- ✅ Çok adımlı konuşmalar (Multi-step conversations)
- ✅ Durum yönetimi (ConversationState enum)
- ✅ Geçici veri saklama (TempData)
- ✅ Konuşma iptali (/cancel)

#### 5. Kullanıcı Arayüzü
- ✅ Inline keyboard butonları
- ✅ Ana menü navigasyonu
- ✅ Dinamik menüler (giriş durumuna göre)
- ✅ Markdown formatlaması
- ✅ Emoji desteği

### ⚠️ Mock/Geliştirme Aşamasında

#### Piyasa Verileri
- ⚠️ Sembol arama
- ⚠️ Hisse fiyat sorgulama
- ⚠️ Sektör listesi
- ⚠️ Popüler hisseler

#### Watchlist
- ⚠️ İzleme listesi oluşturma
- ⚠️ Fiyat alarmları
- ⚠️ Bildirimler

#### Hesap Yönetimi
- ⚠️ Profil görüntüleme
- ⚠️ Bot ayarları
- ⚠️ Hesap özeti

### ❌ Çalışmayan/Eksik Özellikler
- ❌ Gerçek zamanlı fiyat güncellemeleri (WebSocket)
- ❌ Portföy analizi
- ❌ İşlem geçmişi (history detayları)
- ❌ Çoklu hesap desteği
- ❌ Dil seçimi (şu an sadece Türkçe)

---

## 🔄 Kullanıcı Akışları

### 1. İlk Kullanım Akışı

```
1. Kullanıcı /start komutunu gönderir
   └─> Bot hoş geldin mesajı ve ana menü gösterir

2. Kullanıcı "Giriş Yap" butonuna tıklar
   └─> /login komutu tetiklenir

3. Bot kullanıcı adı ister
   └─> Kullanıcı username girer

4. Bot şifre ister
   └─> Kullanıcı password girer

5. Giriş başarılı
   └─> Bot ana menüyü gösterir (giriş yapmış haliyle)
```

**Conversation States:**
- `WAITING_USERNAME` → `WAITING_PASSWORD` → `NONE`

**Session:**
- `TelegramUserSession` Redis'e kaydedilir
- JWT token saklanır
- 24 saat geçerli

---

### 2. AlgoLab Broker Bağlantısı Akışı

```
1. Kullanıcı "Broker" menüsüne gider
   └─> menu:broker callback

2. "AlgoLab Bağlan" butonuna tıklar
   └─> broker:connect callback

3. Bot AlgoLab kullanıcı adı ister
   └─> Kullanıcı AlgoLab username girer

4. Bot AlgoLab şifresi ister
   └─> Kullanıcı AlgoLab password girer
   └─> Bot AlgoLabAuthService.loginUser() çağırır

5. Bot SMS kodu ister
   └─> Kullanıcı telefonuna gelen OTP kodunu girer
   └─> Bot AlgoLabAuthService.loginUserControl() çağırır

6. Bağlantı başarılı
   └─> AlgoLab token ve hash session'a kaydedilir
   └─> 24 saat geçerli
```

**Conversation States:**
- `WAITING_ALGOLAB_USERNAME` → `WAITING_ALGOLAB_PASSWORD` → `WAITING_ALGOLAB_OTP` → `NONE`

**API Calls:**
- `POST /api/LoginUser` (username, password)
- `POST /api/LoginUserControl` (smsCode)

**Session:**
- `algoLabToken`, `algoLabHash`, `algoLabSessionExpires` kaydedilir

---

### 3. Pozisyon Görüntüleme Akışı

```
1. Kullanıcı "Broker" → "Pozisyonlar" seçer
   └─> broker:positions callback

2. Bot AlgoLab session kontrolü yapar
   └─> Eğer bağlı değilse: Hata mesajı + "AlgoLab Bağlan" yönlendirmesi
   └─> Eğer bağlıysa: Devam et

3. Bot AlgoLabOrderService.getInstantPosition("") çağırır
   └─> API Response: {"success": true, "content": [...]}

4. Bot pozisyonları filtreler:
   - Özet satırı (code="-") çıkarılır
   - Nakit (type="CA" veya code="TRY") çıkarılır

5. Bot aynı semboldeki pozisyonları gruplar
   - totalstock toplamı
   - profit toplamı
   - Ağırlıklı ortalama cost

6. Pozisyonlar formatlanıp gösterilir:
   - Sembol
   - Miktar (adet)
   - Ortalama fiyat (₺)
   - Son fiyat (₺)
   - Kar/Zarar (₺ ve %)
```

**API:**
- `POST /api/InstantPosition` → AlgoLab API

**Response Format:**
```json
{
  "success": true,
  "content": [
    {
      "code": "THYAO",
      "totalstock": 1000,
      "cost": 78.50,
      "unitprice": 82.30,
      "profit": 3800.00,
      "type": "HIS"
    }
  ]
}
```

---

### 4. Yeni Emir Gönderme Akışı

```
1. Kullanıcı "Emirler" → "Yeni Emir" seçer
   └─> orders:create callback

2. Bot sembol ister
   └─> Kullanıcı sembol girer (örn: THYAO)
   └─> Sembol doğrulaması YOK (AlgoLab API'de doğrulanır)

3. Bot alış/satış butonlarını gösterir
   └─> Kullanıcı "ALIS (BUY)" veya "SATIŞ (SELL)" seçer
   └─> orders:side:BUY veya orders:side:SELL callback

4. Bot emir tipi butonlarını gösterir
   └─> Kullanıcı "PIYASA (MARKET)" veya "LIMIT" seçer
   └─> orders:type:MARKET veya orders:type:LIMIT callback

5. Eğer LIMIT seçildiyse:
   └─> Bot limit fiyat ister
   └─> Kullanıcı fiyat girer (örn: 82.50)

6. Bot lot miktarı ister
   └─> Kullanıcı lot girer (örn: 5)
   └─> 💡 1 lot = 100 adet

7. Bot onay mesajı gösterir:
   - Sembol
   - İşlem yönü
   - Emir tipi
   - Fiyat (limit için)
   - Miktar (lot ve adet)
   - "Evet, Gönder" / "Hayır" butonları

8. Kullanıcı "Evet, Gönder" seçerse:
   └─> Bot AlgoLabOrderService.sendOrder() çağırır
   └─> Başarılı: Emir detayları + "Bekleyen Emirler" butonu
   └─> Hata: Hata mesajı gösterilir
```

**Conversation States:**
- `WAITING_ORDER_SYMBOL` → `WAITING_ORDER_SIDE` → `WAITING_ORDER_PRICE_TYPE` →
- (Eğer LIMIT) `WAITING_ORDER_PRICE` → `WAITING_ORDER_QUANTITY` → `NONE`
- (Eğer MARKET) `WAITING_ORDER_QUANTITY` → `NONE`

**TempData Keys:**
- `order_symbol`: Sembol kodu (örn: "THYAO")
- `order_side`: "BUY" veya "SELL"
- `order_type`: "MARKET" veya "LIMIT"
- `order_price`: Limit fiyat (BigDecimal)
- `order_quantity`: Lot miktarı (Integer)

**API Call:**
```java
algoLabOrderService.sendOrder(
    symbol,        // "THYAO"
    direction,     // "BUY" veya "SELL"
    priceType,     // "P" (piyasa) veya "L" (limit)
    price,         // BigDecimal (limit için)
    lot,           // Integer (kullanıcı inputu direkt)
    false,         // sms
    false,         // email
    ""             // subAccount
)
```

**AlgoLab API:**
- `POST /api/SendOrder`

**Payload:**
```json
{
  "symbol": "THYAO",
  "direction": "BUY",
  "pricetype": "limit",
  "price": "82.50",
  "lot": "5",
  "sms": false,
  "email": false,
  "subAccount": ""
}
```

---

### 5. Bekleyen Emirleri Görüntüleme Akışı

```
1. Kullanıcı "Emirler" → "Bekleyen Emirler" seçer
   └─> orders:pending callback

2. Bot AlgoLab session kontrolü yapar

3. Bot AlgoLabOrderService.getPendingOrders("") çağırır
   └─> TodaysTransaction API çağrılır
   └─> Tüm günün emirleri gelir

4. Bot emirleri filtreler:
   - Sadece equityStatusDescription == "WAITING" olanlar
   - Veya description içinde "İletildi", "Bekle", "Kısmi" olanlar

5. Emirler formatlanıp gösterilir:
   - Sembol
   - İşlem yönü (🟢 ALIS / 🔴 SATIŞ)
   - Emir tipi
   - Fiyat
   - Lot (ve adet)
   - Durum
   - İki buton: "✏️ Düzenle" ve "❌ İptal"
```

**API Chain:**
- `getPendingOrders()` → `getTodaysTransactions()` → `POST /api/TodaysTransaction`

**Response Format:**
```json
{
  "success": true,
  "content": [
    {
      "transactionId": "20251027FOTPBS",
      "ticker": "THYAO",
      "buysell": "Alış",
      "ordersize": 500,
      "waitingprice": 82.50,
      "equityStatusDescription": "WAITING",
      "description": "İletildi"
    }
  ]
}
```

**Filtering Logic:**
```java
.filter(order -> {
    String equityStatus = order.get("equityStatusDescription");
    String description = order.get("description");

    boolean isWaiting = "WAITING".equals(equityStatus);
    boolean isPendingByDescription = description != null && (
        description.contains("İletildi") ||
        description.contains("Bekle") ||
        description.contains("Kısmi")
    );

    return isWaiting || isPendingByDescription;
})
```

---

### 6. Emir İptali Akışı

```
1. Kullanıcı bekleyen emirler listesinden "❌ İptal #X" butonuna tıklar
   └─> orders:cancel:ORDER_ID callback

2. Bot onay mesajı gösterir:
   - Emir detayları
   - "Evet, İptal Et" / "Hayır" butonları

3. Kullanıcı "Evet, İptal Et" seçerse:
   └─> orders:cancel_confirm:ORDER_ID callback
   └─> Bot AlgoLabOrderService.deleteOrder(orderId, "") çağırır

4. İptal başarılı:
   └─> Başarı mesajı + "Bekleyen Emirler" butonu

5. İptal başarısız:
   └─> Hata mesajı gösterilir
```

**API Call:**
```java
algoLabOrderService.deleteOrder(
    orderId,       // "20251027FOTPBS"
    ""             // subAccount
)
```

**AlgoLab API:**
- `POST /api/DeleteOrder`

**Payload:**
```json
{
  "id": "20251027FOTPBS",
  "subAccount": ""
}
```

**⚠️ CRITICAL:** Parameter key must be `"subAccount"` (lowercase 's', uppercase 'A'), not `"Subaccount"`.

---

### 7. Emir Düzenleme Akışı

```
1. Kullanıcı bekleyen emirler listesinden "✏️ Düzenle #X" butonuna tıklar
   └─> orders:modify:ORDER_ID callback

2. Bot mevcut emir bilgilerini ve iki seçenek gösterir:
   - "Fiyat Değiştir"
   - "Miktar Değiştir"
   - "İptal"

3a. Fiyat değiştirme:
   └─> Bot yeni fiyat ister
   └─> Kullanıcı yeni fiyat girer
   └─> Bot onay ister
   └─> AlgoLabOrderService.modifyOrder(orderId, newPrice, null, false, "") çağırır

3b. Miktar değiştirme:
   └─> Bot yeni lot miktarı ister
   └─> Kullanıcı yeni lot girer
   └─> Bot onay ister
   └─> AlgoLabOrderService.modifyOrder(orderId, null, newLot, false, "") çağırır

4. Güncelleme başarılı:
   └─> Başarı mesajı + "Bekleyen Emirler" butonu
```

**Conversation States:**
- `WAITING_MODIFY_PRICE` → Yeni fiyat için
- `WAITING_MODIFY_QUANTITY` → Yeni miktar için

**API Call:**
```java
algoLabOrderService.modifyOrder(
    orderId,       // "20251027FOTPBS"
    price,         // BigDecimal (yeni fiyat veya null)
    lot,           // Integer (yeni lot veya null)
    false,         // viop
    ""             // subAccount
)
```

**AlgoLab API:**
- `POST /api/ModifyOrder`

**Payload:**
```json
{
  "id": "20251027FOTPBS",
  "price": "83.00",
  "lot": "10",
  "viop": false,
  "subAccount": ""
}
```

---

### 8. Çıkış Akışı

```
1. Kullanıcı "Ana Menü" → "Çıkış Yap" seçer
   └─> menu:logout callback

2. Bot oturumu sonlandırır:
   └─> sessionService.logout(userId)
   └─> Redis'teki TelegramUserSession silinir

3. Bot çıkış mesajı gösterir:
   └─> "Başarıyla çıkış yaptınız"
   └─> Ana menü (giriş yapmamış haliyle)
   └─> "Giriş Yap" butonu
```

---

## 🔧 Teknik Detaylar

### Mimari

```
┌─────────────────┐
│  Telegram User  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Telegram API   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  BistTelegramBot            │
│  - Message Handling         │
│  - Callback Query Handling  │
│  - Command Routing          │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Command Handlers           │
│  - StartCommandHandler      │
│  - LoginCommandHandler      │
│  - MenuCommandHandler       │
│  - BrokerCommandHandler     │
│  - OrderCommandHandler      │
│  - MarketDataCommandHandler │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Services                   │
│  - TelegramSessionService   │
│  - AuthenticationService    │
│  - AlgoLabAuthService       │
│  - AlgoLabOrderService      │
└────────┬────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│  Data Layer                 │
│  - Redis (Sessions)         │
│  - PostgreSQL (Users)       │
│  - AlgoLab API (Orders)     │
└─────────────────────────────┘
```

### Session Management

**TelegramUserSession (Redis):**
```java
{
  "telegramUserId": 123456789,
  "platformUserId": 1,
  "username": "user123",
  "jwtToken": "eyJ...",
  "refreshToken": "eyJ...",
  "algoLabToken": "abc123",
  "algoLabHash": "xyz789",
  "algoLabSessionExpires": "2025-10-28T10:00:00",
  "conversationState": "WAITING_ORDER_SYMBOL",
  "tempData": {
    "order_symbol": "THYAO",
    "order_side": "BUY"
  }
}
```

**TTL:** 24 saat (AlgoLab session ile senkron)

### Conversation State Machine

```
NONE
  │
  ├─> WAITING_USERNAME
  │     └─> WAITING_PASSWORD
  │           └─> NONE (Login Success)
  │
  ├─> WAITING_ALGOLAB_USERNAME
  │     └─> WAITING_ALGOLAB_PASSWORD
  │           └─> WAITING_ALGOLAB_OTP
  │                 └─> NONE (AlgoLab Connected)
  │
  └─> WAITING_ORDER_SYMBOL
        └─> WAITING_ORDER_SIDE
              └─> WAITING_ORDER_PRICE_TYPE
                    ├─> WAITING_ORDER_PRICE (LIMIT)
                    │     └─> WAITING_ORDER_QUANTITY
                    │           └─> NONE (Order Sent)
                    │
                    └─> WAITING_ORDER_QUANTITY (MARKET)
                          └─> NONE (Order Sent)
```

### AlgoLab API Entegrasyonu

#### Authentication Flow

1. **LoginUser**
   - Endpoint: `POST /api/LoginUser`
   - Payload: `{username, password, apiKey}`
   - Response: `{success, content: {token}}`

2. **LoginUserControl (OTP)**
   - Endpoint: `POST /api/LoginUserControl`
   - Payload: `{smsCode, apiKey}`
   - Response: `{success, content: {hash}}`

#### Order Operations

1. **SendOrder**
   - Endpoint: `POST /api/SendOrder`
   - Payload: `{symbol, direction, pricetype, price, lot, sms, email, subAccount}`
   - Auth: `algolab-token` ve `algolab-hash` header
   - LinkedHashMap kullanılmalı (key order önemli)

2. **ModifyOrder**
   - Endpoint: `POST /api/ModifyOrder`
   - Payload: `{id, price, lot, viop, subAccount}`
   - Auth: `algolab-token` ve `algolab-hash` header

3. **DeleteOrder**
   - Endpoint: `POST /api/DeleteOrder`
   - Payload: `{id, subAccount}`
   - Auth: `algolab-token` ve `algolab-hash` header
   - ⚠️ CRITICAL: Key `"subAccount"` (lowercase 's', uppercase 'A')

4. **TodaysTransaction**
   - Endpoint: `POST /api/TodaysTransaction`
   - Payload: `{Subaccount}` (capital 'S')
   - Response: `{success, content: [orders...]}`
   - Filtering: `equityStatusDescription == "WAITING"`

5. **InstantPosition**
   - Endpoint: `POST /api/InstantPosition`
   - Payload: `{Subaccount}` (capital 'S')
   - Response: `{success, content: [positions...]}`
   - Filtering: `code != "-"` ve `type != "CA"` ve `code != "TRY"`

### Field Name Mapping (AlgoLab API)

**Order Response:**
- `ticker` → Symbol
- `buysell` → "Alış" / "Satış"
- `ordersize` → Miktar (adet)
- `waitingprice` → Fiyat
- `transactionId` veya `atpref` → Order ID
- `equityStatusDescription` → Durum

**Position Response:**
- `code` → Symbol
- `totalstock` → Miktar (adet)
- `cost` → Ortalama maliyet
- `unitprice` → Güncel fiyat
- `profit` → Kar/Zarar (TL)
- `type` → "HIS" (hisse), "CA" (nakit)

### Callback Data Format

**Pattern:** `command:action:parameter`

**Examples:**
- `menu:main` → Ana menüyü göster
- `menu:broker` → Broker menüsünü göster
- `orders:pending` → Bekleyen emirleri göster
- `orders:create` → Yeni emir akışını başlat
- `orders:side:BUY` → Alış seçildi
- `orders:type:LIMIT` → Limit emir seçildi
- `orders:cancel:20251027FOTPBS` → Bu emri iptal et
- `orders:modify:20251027FOTPBS` → Bu emri düzenle
- `broker:connect` → AlgoLab bağlantısı başlat
- `broker:positions` → Pozisyonları göster

**Routing:**
```java
String[] parts = callbackData.split(":");
String command = parts[0];      // "orders"
String action = parts[1];       // "cancel"
String parameter = parts[2];    // "20251027FOTPBS"
```

### Inline Keyboard Factory

**KeyboardFactory.java** tüm butonları oluşturur:
- `createMainMenuKeyboard(isLoggedIn)`
- `createMarketDataKeyboard()`
- `createBrokerKeyboard(algoLabConnected)`
- `createOrdersMenuKeyboard()`
- `createOrderSideKeyboard()`
- `createOrderTypeKeyboard()`
- `createOrderListKeyboard(orders)`
- `createConfirmationKeyboard(action)`
- `createBackButton(backTo)`

---

## 🐛 Bilinen Sorunlar ve Çözümleri

### 1. ✅ ÇÖZÜLDÜ: Order Cancellation 401 Error
**Sorun:** Emir iptal edilirken 401 Unauthorized hatası alınıyordu.
**Neden:** `deleteOrder()` ve `modifyOrder()` metodlarında `"Subaccount"` kullanılıyordu, oysa doğrusu `"subAccount"` olmalıydı.
**Çözüm:** Parameter key `"subAccount"` olarak değiştirildi (AlgoLabOrderService.java:155, 119).

### 2. ✅ ÇÖZÜLDÜ: Keyboard Buttons Not Working
**Sorun:** Yeni emir girişinde Alış/Satış butonları ve iptal butonları çalışmıyordu.
**Neden:** Handler command `"orders"` (plural) ama callback prefix `"order:"` (singular) kullanılıyordu.
**Çözüm:** Tüm callback prefix'ler `"orders:"` olarak değiştirildi.

### 3. ✅ ÇÖZÜLDÜ: Lot Calculation Error
**Sorun:** 5 adet girildiğinde 1 lot (100 adet) gönderiliyordu.
**Neden:** Kod `quantity / 100` yapıyordu ve 5/100=0, 0 ise default 1 oluyordu.
**Çözüm:** Input "lot" olarak değiştirildi, kullanıcı direkt lot giriyor (1 lot = 100 adet açıklamasıyla).

### 4. ✅ ÇÖZÜLDÜ: Empty Order Data
**Sorun:** Bekleyen emirler geliyordu ama tüm alanlar "N/A" gösteriyordu.
**Neden:** AlgoLab API field name'leri beklenenle farklıydı (ticker, buysell, ordersize vs).
**Çözüm:** Field mapping güncel AlgoLab response'una göre düzenlendi.

### 5. ✅ ÇÖZÜLDÜ: Symbol Validation Blocking Orders
**Sorun:** THYAO gibi geçerli semboller için "sembol bulunamadı" hatası alınıyordu.
**Neden:** Lokal veritabanında THYAO yoktu.
**Çözüm:** Lokal validasyon kaldırıldı, AlgoLab API'nin kendi validasyonu kullanılıyor.

---

## 📊 İstatistikler

**Toplam Handler:** 6
**Toplam Command:** 25+
**Çalışan Özellik:** ~40%
**Mock Özellik:** ~60%

**API Entegrasyonları:**
- ✅ Platform Authentication (JWT)
- ✅ AlgoLab Authentication (Token + Hash + OTP)
- ✅ AlgoLab Orders (SendOrder, ModifyOrder, DeleteOrder, TodaysTransaction)
- ✅ AlgoLab Positions (InstantPosition)

**Session Management:**
- ✅ Redis (TelegramUserSession)
- ✅ PostgreSQL (TelegramUser entity)
- ✅ JWT Token storage
- ✅ AlgoLab Token + Hash storage

---

## 🚀 Gelecek Özellikler

### Yüksek Öncelik
- [ ] Gerçek zamanlı fiyat güncellemeleri (WebSocket)
- [ ] İşlem geçmişi (TodaysTransaction full details)
- [ ] Watchlist ve fiyat alarmları
- [ ] Portföy analizi ve grafikler

### Orta Öncelik
- [ ] Piyasa verileri (sembol arama, sektörler, popüler hisseler)
- [ ] Hesap özeti ve bilgileri
- [ ] Bot ayarları (bildirim tercihleri, dil)
- [ ] Profil yönetimi

### Düşük Öncelik
- [ ] Çoklu hesap desteği
- [ ] İleri düzey emir tipleri (stop-loss, trailing stop)
- [ ] Teknik analiz göstergeleri
- [ ] İngilizce dil desteği

---

## 📝 Notlar

1. **Güvenlik:** Şifreler ve API key'ler loglanmaz, sadece hash'ler saklanır.
2. **Session:** Redis TTL 24 saat, AlgoLab session ile senkron.
3. **Error Handling:** Tüm AlgoLab API hataları kullanıcıya açıklayıcı mesajlarla iletilir.
4. **Logging:** Detaylı log seviyesi: DEBUG (geliştirme), INFO (production).
5. **Rate Limiting:** Şu an yok, gelecekte eklenecek.

---

**Son Güncelleme:** 2025-10-27
**Version:** 1.0.0
**Bot PID:** 36801
**Log File:** `/tmp/telegram-deleteorder-fixed.log`

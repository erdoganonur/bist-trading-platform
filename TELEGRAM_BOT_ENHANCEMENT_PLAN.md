# BIST Trading Platform - Telegram Bot İyileştirme Planı

**Tarih:** 2025-10-27
**Versiyon:** 1.0
**Durum:** Planlama Tamamlandı ✅

---

## 📋 İçindekiler

1. [Kritik Düzeltmeler (HEMEN)](#kritik-düzeltmeler)
2. [Öncelikli Özellikler (1-2 Hafta)](#öncelikli-özellikler)
3. [Orta Vadeli Özellikler (2-4 Hafta)](#orta-vadeli-özellikler)
4. [Uzun Vadeli Özellikler (1-2 Ay)](#uzun-vadeli-özellikler)
5. [Teknik Detaylar](#teknik-detaylar)

---

## 🔴 Kritik Düzeltmeler (HEMEN)

### 1. LOT/ADET Terminoloji Hatası ❌ CRITICAL BUG

**Problem:**
- Kod şu an 1 lot = 100 adet varsayımıyla çalışıyor
- **YANLIŞ!** Borsa İstanbul Pay Piyasası'nda: **1 LOT = 1 ADET**
- Kullanıcı 5 adet almak isterse 5 giriyor, ama kod 500 adet gönderiyor!

**Etkilenen Dosyalar:**
- `OrderCommandHandler.java` - submitOrder(), formatAlgoLabOrder()
- `BrokerCommandHandler.java` - formatAlgoLabPosition()
- `AlgoLabOrderService.java` - sendOrder() javadoc

**Düzeltilecek Yerler:**

#### 1.1. OrderCommandHandler.java

**Miktar Girişi Mesajı (Line ~271-278):**
```java
// ÖNCE: ❌
sendMessage(chatId,
    "*📝 Yeni Emir Oluştur*\n\n" +
    "Sembol: *" + symbol + "*\n" +
    "İşlem: " + sideEmoji + " *" + sideStr + "*\n" +
    "Tip: *" + orderType + "*\n\n" +
    "📦 *Miktar* girin (lot):\n" +
    "💡 _1 lot = 100 adet_",
    null);

// SONRA: ✅
sendMessage(chatId,
    "*📝 Yeni Emir Oluştur*\n\n" +
    "Sembol: *" + symbol + "*\n" +
    "İşlem: " + sideEmoji + " *" + sideStr + "*\n" +
    "Tip: *" + orderType + "*\n\n" +
    "📦 *Kaç adet* almak/satmak istiyorsunuz?",
    null);
```

**submitOrder() Metodu (Line ~398-452):**
```java
// ÖNCE: ❌
int lot = Integer.parseInt(quantityStr);
int adet = lot * 100; // YANLIŞ!

String confirmation = String.format(
    "Miktar: %d lot (%d adet)\n", lot, adet);

// SONRA: ✅
int quantity = Integer.parseInt(quantityStr);

String confirmation = String.format(
    "Miktar: %d adet\n", quantity);

// AlgoLab API çağrısı - değişiklik YOK (zaten doğru)
algoLabOrderService.sendOrder(
    symbol,
    direction,
    priceType,
    price,
    quantity,  // Direkt gönder
    false,
    false,
    ""
);
```

**formatAlgoLabOrder() Metodu (Line ~937-1000):**
```java
// ÖNCE: ❌
int orderSize;
if (ordersizeObj instanceof Integer) {
    orderSize = (Integer) ordersizeObj;
} else if (ordersizeObj instanceof Double) {
    orderSize = ((Double) ordersizeObj).intValue();
} else {
    orderSize = Integer.parseInt(String.valueOf(ordersizeObj));
}

int lot = orderSize / 100;  // YANLIŞ!
int adet = orderSize;

sb.append(String.format("Lot: %d (%d adet)\n", lot, adet));

// SONRA: ✅
int quantity;
if (ordersizeObj instanceof Integer) {
    quantity = (Integer) ordersizeObj;
} else if (ordersizeObj instanceof Double) {
    quantity = ((Double) ordersizeObj).intValue();
} else {
    quantity = Integer.parseInt(String.valueOf(ordersizeObj));
}

sb.append(String.format("Miktar: %d adet\n", quantity));
```

#### 1.2. BrokerCommandHandler.java

**formatAlgoLabPosition() Metodu (Line ~320-351):**
```java
// ÖNCE: ❌
double totalstock = parseDouble(position.get("totalstock"));
// ...
sb.append(String.format("Miktar: %d adet\n", (int) totalstock));

// Hesaplamada:
int adet = (int) totalstock;
int lot = adet / 100;  // YANLIŞ!

// SONRA: ✅
double totalstock = parseDouble(position.get("totalstock"));
int quantity = (int) totalstock;

sb.append(String.format("Miktar: %d adet\n", quantity));
```

#### 1.3. AlgoLabOrderService.java

**JavaDoc Düzeltmesi (Line ~27-38):**
```java
// ÖNCE: ❌
/**
 * @param lot Lot miktarı (1 lot = 100 hisse)
 */

// SONRA: ✅
/**
 * @param quantity Miktar (adet/hisse sayısı)
 */
```

**Parametre İsmi Değişikliği:**
```java
// ÖNCE:
public Map<String, Object> sendOrder(
    String symbol,
    String direction,
    String priceType,
    BigDecimal price,
    Integer lot,  // ❌
    Boolean sms,
    Boolean email,
    String subAccount
)

// SONRA:
public Map<String, Object> sendOrder(
    String symbol,
    String direction,
    String priceType,
    BigDecimal price,
    Integer quantity,  // ✅
    Boolean sms,
    Boolean email,
    String subAccount
)
```

**Payload Oluşturma:**
```java
// AlgoLab API lot parametresi = adet sayısı
payload.put("lot", quantity.toString());
```

**Tahmini Süre:** 30-45 dakika
**Öncelik:** 🔴 CRITICAL - Önce bu yapılmalı!

---

## ✅ Öncelikli Özellikler (1-2 Hafta)

### 2. Pozisyonlardan Direkt İşlem Butonları

**Mevcut Durum:**
```
*THYAO*
Miktar: 1000 adet
Ort. Fiyat: ₺78.50
Son Fiyat: ₺82.30
Kar/Zarar: 🟢 +₺3,800.00
```

**Yeni Görünüm:**
```
*THYAO*
Miktar: 1000 adet
Ort. Fiyat: ₺78.50
Son Fiyat: ₺82.30 (+4.84%)
Kar/Zarar: 🟢 +₺3,800.00 (+4.84%)

[🔴 Hızlı Sat]  [🟢 Ortalama Al]  [📊 Detay]
```

**Akışlar:**

#### 2.1. Hızlı Satış
```
1. [🔴 Hızlı Sat] tıkla
2. Satış seçenekleri:
   • [Tümü (1000 adet)]
   • [Yarısı (500 adet)]
   • [1/4 (250 adet)]
   • [Özel Miktar...]
3. Emir tipi seç:
   • [💰 PIYASA] (hızlı)
   • [📊 LIMIT]
4. Onay ekranı (tutar + komisyon)
5. Emir gönder
```

**Callback Data:**
- `broker:quicksell:SYMBOL` → Satış menüsü
- `broker:quicksell:SYMBOL:ALL` → Tümünü sat (piyasa)
- `broker:quicksell:SYMBOL:HALF` → Yarısını sat
- `broker:quicksell:SYMBOL:CUSTOM` → Özel miktar iste

#### 2.2. Ortalama Alım
```
1. [🟢 Ortalama Al] tıkla
2. Miktar gir (adet)
3. Yeni ortalama hesapla ve göster:
   - Mevcut: 1000 adet @ ₺78.50
   - Eklenecek: 500 adet @ ₺82.30
   - Yeni Ortalama: 1500 adet @ ₺79.77
4. Onay
5. Emir gönder
```

**Callback Data:**
- `broker:avgdown:SYMBOL` → Ortalama alım akışı

#### 2.3. Detaylı Görünüm
```
*THYAO - Detaylı Bilgi*

📊 Pozisyon:
Miktar: 1000 adet
Ort. Maliyet: ₺78,500
Güncel Değer: ₺82,300
P&L: 🟢 +₺3,800 (+4.84%)

📈 Piyasa:
Son: ₺82.30
Alış: ₺82.25  |  Satış: ₺82.35
Gün: ₺80.50 - ₺83.20
Değişim: +₺2.10 (+2.62%)

📅 İşlemler:
• 27.10.2025 - 500 adet @ ₺78.00
• 26.10.2025 - 500 adet @ ₺79.00

[🔴 Sat]  [🟢 Al]  [🔙 Geri]
```

**Implementation:**

**BrokerCommandHandler.java:**
```java
// formatAlgoLabPosition() metodunda buton ekle
InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

keyboardBuilder.keyboardRow(new InlineKeyboardRow(
    InlineKeyboardButton.builder()
        .text("🔴 Hızlı Sat")
        .callbackData("broker:quicksell:" + symbol)
        .build(),
    InlineKeyboardButton.builder()
        .text("🟢 Ortalama Al")
        .callbackData("broker:avgdown:" + symbol)
        .build(),
    InlineKeyboardButton.builder()
        .text("📊 Detay")
        .callbackData("broker:posdetail:" + symbol)
        .build()
));

// Callback handler
private void handleBrokerAction(...) {
    switch (action) {
        case "quicksell" -> handleQuickSell(chatId, userId, parts[2]); // symbol
        case "avgdown" -> handleAvgDown(chatId, userId, parts[2]);
        case "posdetail" -> handlePositionDetail(chatId, userId, parts[2]);
        // ...
    }
}
```

**Tahmini Süre:** 4-6 saat
**Öncelik:** 🔴 Yüksek

---

### 3. Anlık Fiyat ve Tutar Gösterimi

**Mevcut Durum:**
```
📦 Kaç adet almak istiyorsunuz?

Kullanıcı: 500
```

**Yeni Görünüm:**
```
*📝 Yeni Emir - THYAO*

📊 Güncel Piyasa:
   Alış: ₺82.25  |  Satış: ₺82.35
   Son: ₺82.30 (+2.5% 🟢)
   Hacim: 12.5M

📦 Miktar: 500 adet

💰 Tutar Hesabı:
   ┌────────────────────────────
   │ 500 adet × ₺82.50 = ₺41,250.00
   │ Komisyon (~0.2%): +₺82.50
   │ BSMV (0.1%): +₺8.25
   │ ────────────────────────────
   │ TOPLAM: ₺41,340.75
   └────────────────────────────

⚠️ Limitiniz: ₺50,000
   Kalan: ₺8,659.25

[✅ Onayla] [❌ İptal] [✏️ Değiştir]
```

**Özellikler:**
- Gerçek zamanlı fiyat (WebSocket veya REST)
- Spread gösterimi (alış-satış farkı)
- Komisyon hesaplama
- BSMV hesaplama
- Bakiye kontrolü (varsa)
- Pozisyon varsa kar/zarar tahmini

**Implementation:**

**1. Market Data Service:**
```java
@Service
public class MarketDataService {

    @Autowired
    private AlgoLabWebSocketClient webSocketClient;

    public QuoteData getCurrentQuote(String symbol) {
        // WebSocket'ten son fiyat
        // veya REST API'den çek
        return QuoteData.builder()
            .symbol(symbol)
            .lastPrice(82.30)
            .bidPrice(82.25)
            .askPrice(82.35)
            .change(2.10)
            .changePercent(2.62)
            .volume(12500000)
            .build();
    }
}
```

**2. Order Calculation Service:**
```java
@Service
public class OrderCalculationService {

    private static final double COMMISSION_RATE = 0.002; // 0.2%
    private static final double BSMV_RATE = 0.001;       // 0.1%

    public OrderEstimate calculateOrderCost(
        String symbol,
        int quantity,
        BigDecimal price,
        OrderSide side
    ) {
        BigDecimal totalValue = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal commission = totalValue.multiply(BigDecimal.valueOf(COMMISSION_RATE));
        BigDecimal bsmv = commission.multiply(BigDecimal.valueOf(BSMV_RATE));
        BigDecimal total = totalValue.add(commission).add(bsmv);

        return OrderEstimate.builder()
            .quantity(quantity)
            .price(price)
            .totalValue(totalValue)
            .commission(commission)
            .bsmv(bsmv)
            .grandTotal(total)
            .build();
    }

    public String formatOrderEstimate(OrderEstimate estimate, QuoteData quote) {
        StringBuilder sb = new StringBuilder();

        sb.append("*📝 Emir Özeti*\n\n");

        // Piyasa bilgisi
        sb.append("📊 Güncel Piyasa:\n");
        sb.append(String.format("   Alış: ₺%.2f  |  Satış: ₺%.2f\n",
            quote.getBidPrice(), quote.getAskPrice()));
        sb.append(String.format("   Son: ₺%.2f (%+.2f%% %s)\n",
            quote.getLastPrice(),
            quote.getChangePercent(),
            quote.getChangePercent() >= 0 ? "🟢" : "🔴"));
        sb.append(String.format("   Hacim: %.1fM\n\n", quote.getVolume() / 1_000_000.0));

        // Miktar
        sb.append(String.format("📦 Miktar: %d adet\n\n", estimate.getQuantity()));

        // Tutar hesabı
        sb.append("💰 Tutar Hesabı:\n");
        sb.append("   ┌────────────────────────────\n");
        sb.append(String.format("   │ %d adet × ₺%.2f = ₺%,.2f\n",
            estimate.getQuantity(),
            estimate.getPrice(),
            estimate.getTotalValue()));
        sb.append(String.format("   │ Komisyon (~0.2%%): +₺%,.2f\n", estimate.getCommission()));
        sb.append(String.format("   │ BSMV (0.1%%): +₺%,.2f\n", estimate.getBsmv()));
        sb.append("   │ ────────────────────────────\n");
        sb.append(String.format("   │ TOPLAM: ₺%,.2f\n", estimate.getGrandTotal()));
        sb.append("   └────────────────────────────\n");

        return sb.toString();
    }
}
```

**3. OrderCommandHandler Integration:**
```java
@Autowired
private MarketDataService marketDataService;

@Autowired
private OrderCalculationService orderCalculationService;

// submitOrder() metodunda
QuoteData quote = marketDataService.getCurrentQuote(symbol);
OrderEstimate estimate = orderCalculationService.calculateOrderCost(
    symbol, quantity, price, side);

String estimateMessage = orderCalculationService.formatOrderEstimate(estimate, quote);

// Onay ekranında göster
sendMessage(chatId, estimateMessage, confirmKeyboard);
```

**Tahmini Süre:** 5-7 saat
**Öncelik:** 🔴 Yüksek

---

### 4. Emir Gerçekleşme Bildirimleri

**Problem:**
- Kullanıcı emrini gönderiyor, ne zaman gerçekleştiğini bilmiyor
- Sürekli "Bekleyen Emirler" menüsüne girip kontrol etmek zorunda

**Çözüm: Otomatik Bildirimler**

**Bildirim Tipleri:**

#### 4.1. Emir Gerçekleşti ✅
```
🟢 *Emir Gerçekleşti!*

THYAO - ALIS
500 adet @ ₺82.50
Toplam: ₺41,340.75

Emir ID: 20251027FOTPBS
Durum: TAMAMLANDI ✅
Tarih: 27.10.2025 10:30:45

[📊 Pozisyonları Gör]  [➕ Yeni Emir]
```

#### 4.2. Kısmi Gerçekleşme ⚠️
```
⚠️ *Emir Kısmen Gerçekleşti*

THYAO - ALIS
Gerçekleşen: 200 / 500 adet
Kalan: 300 adet

200 adet @ ₺82.50
Tutar: ₺16,536.30

[📋 Bekleyen Emirler]  [❌ Kalan İptal]
```

#### 4.3. Emir İptal Edildi ❌
```
❌ *Emir İptal Edildi*

THYAO - ALIS
500 adet @ ₺82.50

Sebep: Sistem tarafından iptal
Tarih: 27.10.2025 11:45

[📋 Bekleyen Emirler]  [🔄 Yeniden Gönder]
```

#### 4.4. Emir Bekliyor ⏰
```
⏰ *Emir Hala Beklemede*

THYAO - LIMIT ALIS
500 adet @ ₺82.50
Güncel Fiyat: ₺82.80

Geçen Süre: 30 dakika
Durum: BEKLEMEDE

💡 Fiyat yükseldi, limit fiyatı
   güncellemek ister misiniz?

[✏️ Fiyat Güncelle]  [❌ İptal Et]
```

**Implementation:**

**1. Background Job:**
```java
@Component
public class OrderStatusMonitorJob {

    @Autowired
    private TelegramSessionService sessionService;

    @Autowired
    private AlgoLabOrderService algoLabOrderService;

    @Autowired
    private TelegramNotificationService notificationService;

    @Scheduled(fixedDelay = 60000) // Her 1 dakika
    public void monitorOrderStatus() {
        log.debug("Checking order status for all active users...");

        // Tüm aktif kullanıcıları al
        List<TelegramUserSession> activeSessions = sessionService.getAllActiveSessions();

        for (TelegramUserSession session : activeSessions) {
            if (!session.isAlgoLabSessionValid()) {
                continue; // AlgoLab bağlantısı yok
            }

            try {
                checkUserOrders(session);
            } catch (Exception e) {
                log.error("Error checking orders for user {}", session.getTelegramUserId(), e);
            }
        }
    }

    private void checkUserOrders(TelegramUserSession session) {
        Long userId = session.getTelegramUserId();

        // Bekleyen emirleri al
        List<Map<String, Object>> currentOrders = algoLabOrderService.getPendingOrders("");

        // Redis'te saklanan son durum
        Set<String> lastKnownOrderIds = sessionService.getTrackedOrderIds(userId);
        Set<String> currentOrderIds = currentOrders.stream()
            .map(o -> (String) o.get("transactionId"))
            .collect(Collectors.toSet());

        // Gerçekleşen emirleri bul (artık listede yok)
        Set<String> completedOrderIds = new HashSet<>(lastKnownOrderIds);
        completedOrderIds.removeAll(currentOrderIds);

        // Bildirim gönder
        for (String orderId : completedOrderIds) {
            OrderDetails order = getOrderDetails(orderId, session);
            notificationService.sendOrderCompletedNotification(userId, order);
        }

        // Güncel listeyi sakla
        sessionService.setTrackedOrderIds(userId, currentOrderIds);
    }
}
```

**2. Notification Service:**
```java
@Service
public class TelegramNotificationService {

    @Autowired
    private TelegramClient telegramClient;

    public void sendOrderCompletedNotification(Long userId, OrderDetails order) {
        Long chatId = getChatIdForUser(userId);

        StringBuilder message = new StringBuilder();
        message.append("🟢 *Emir Gerçekleşti!*\n\n");
        message.append(String.format("%s - %s\n", order.getSymbol(), order.getSide()));
        message.append(String.format("%d adet @ ₺%.2f\n", order.getQuantity(), order.getPrice()));
        message.append(String.format("Toplam: ₺%,.2f\n\n", order.getTotalAmount()));
        message.append(String.format("Emir ID: %s\n", order.getOrderId()));
        message.append("Durum: TAMAMLANDI ✅\n");
        message.append(String.format("Tarih: %s\n", formatDateTime(order.getExecutionTime())));

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(
                createButton("📊 Pozisyonları Gör", "broker:positions"),
                createButton("➕ Yeni Emir", "orders:create")
            ))
            .build();

        sendMessage(chatId, message.toString(), keyboard);
    }
}
```

**3. Redis Order Tracking:**
```java
// TelegramSessionService.java
public void setTrackedOrderIds(Long userId, Set<String> orderIds) {
    String key = "telegram:user:" + userId + ":tracked_orders";
    redisTemplate.opsForSet().getOperations().delete(key);
    if (!orderIds.isEmpty()) {
        redisTemplate.opsForSet().add(key, orderIds.toArray(new String[0]));
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }
}

public Set<String> getTrackedOrderIds(Long userId) {
    String key = "telegram:user:" + userId + ":tracked_orders";
    return redisTemplate.opsForSet().members(key);
}
```

**Tahmini Süre:** 6-8 saat
**Öncelik:** 🟡 Orta-Yüksek

---

## 🟡 Orta Vadeli Özellikler (2-4 Hafta)

### 5. Fiyat Alarmları ve Watchlist

**Kullanım:**
```
/watch THYAO 90.00  → THYAO ₺90'a ulaşınca bildir
/watch AKBNK 50.00  → AKBNK ₺50'ye düşünce bildir
/watchlist          → İzlenen hisseleri göster
/unwatch THYAO      → THYAO'yu izlemeden çıkar
```

**Bildirim:**
```
⚡ *Fiyat Alarmı!*

THYAO hedef fiyata ulaştı!
Ayarladığınız: ₺90.00
Güncel Fiyat: ₺90.20 🟢 (+15.3%)

Pozisyonunuz:
1000 adet @ ₺78.50
P&L: 🟢 +₺11,700 (+14.9%)

[🔴 Sat]  [🟢 Daha Al]  [📊 Analiz]
```

**Database Schema:**
```sql
CREATE TABLE price_alerts (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    target_price DECIMAL(10,2) NOT NULL,
    direction VARCHAR(5) NOT NULL, -- 'ABOVE' or 'BELOW'
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    triggered_at TIMESTAMP,
    FOREIGN KEY (telegram_user_id) REFERENCES telegram_users(id)
);

CREATE INDEX idx_active_alerts ON price_alerts(is_active) WHERE is_active = true;
```

**Background Job:**
```java
@Scheduled(fixedDelay = 30000) // Her 30 saniye
public void checkPriceAlerts() {
    List<PriceAlert> activeAlerts = alertRepository.findAllActive();

    for (PriceAlert alert : activeAlerts) {
        BigDecimal currentPrice = getCurrentPrice(alert.getSymbol());

        boolean triggered = (alert.getDirection() == Direction.ABOVE &&
                            currentPrice.compareTo(alert.getTargetPrice()) >= 0) ||
                           (alert.getDirection() == Direction.BELOW &&
                            currentPrice.compareTo(alert.getTargetPrice()) <= 0);

        if (triggered) {
            sendPriceAlertNotification(alert, currentPrice);
            alert.setIsActive(false);
            alert.setTriggeredAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
    }
}
```

**Tahmini Süre:** 8-10 saat
**Öncelik:** 🟡 Orta

---

### 6. Hızlı İşlem Şablonları

**Kullanım:**
```
*⚡ Hızlı Emirler*

[THYAO - 500 adet Al]   [AKBNK - 1000 adet Al]
[GARAN - Tümünü Sat]    [SAHOL - 300 adet Sat]

[➕ Yeni Şablon Ekle]

──────────────
Son Kullanılan:
• THYAO - 500 adet ALIS @ ₺82.50 (2 dk önce)
• AKBNK - 300 adet SATIŞ @ ₺55.30 (1 saat önce)
```

**Şablon Oluşturma:**
```
1. Bir emir gönder
2. "Bu emri şablon olarak kaydet?" sor
3. Kaydet:
   - Sembol
   - Yön (Alış/Satış)
   - Miktar
   - Tip (Piyasa/Limit)
   - Fiyat (opsiyonel)
4. Şablon adı gir (örn: "THYAO Günlük")
```

**Database:**
```sql
CREATE TABLE order_templates (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    template_name VARCHAR(50) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    side VARCHAR(4) NOT NULL,
    order_type VARCHAR(6) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2),
    use_count INT DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Tahmini Süre:** 6-8 saat
**Öncelik:** 🟡 Orta

---

### 7. Portföy Analizi ve Özet

**Günlük Özet (Sabah 09:00):**
```
🌅 *Günaydın! Portföy Özeti*

27 Ekim 2025 - Pazartesi

📊 Genel Durum:
Toplam Değer: ₺125,450.00
Nakit: ₺23,200.00 (18.5%)
Hisseler: ₺102,250.00 (81.5%)

📈 Performans:
Dün: 🟢 +₺2,350 (+1.91%)
Haftalık: 🟢 +₺8,200 (+6.99%)
Aylık: 🟢 +₺15,300 (+13.90%)

🏆 En İyi Performans:
🥇 THYAO: +12.5% (₺3,800 kar)
🥈 AKBNK: +8.3% (₺2,100 kar)
🥉 GARAN: +5.1% (₺1,200 kar)

📉 Kayıplar:
SAHOL: -2.3% (-₺450)

💡 Öneriler:
• THYAO yüksek, kar realizasyonu düşünün
• SAHOL destek seviyesinde, al-sat fırsatı
• Nakit oranı düşük, risk yönetimi

[📊 Detaylı Analiz]  [📈 Grafik]
```

**Detaylı Analiz:**
```
*📊 Portföy Analizi*

🎯 Hedef Dağılım vs Mevcut:
• Teknoloji: 30% / 35% ⚠️
• Finans: 40% / 30% 💡
• Sanayi: 20% / 25% ⚠️
• Diğer: 10% / 10% ✅

📊 Risk Analizi:
Risk Seviyesi: Orta 🟡

✅ Güçlü Yönler:
• İyi çeşitlendirme (6 sektör)
• Kar eden pozisyon: %83

⚠️ Dikkat Edilmesi Gerekenler:
• Tek hisse ağırlığı yüksek (THYAO: %32)
• Nakit oranı düşük (%18.5)
• Finansal sektör eksik

💡 Öneriler:
1. THYAO'dan kısmi kar al (risk azalt)
2. Finans sektörüne ağırlık ver
3. Nakit oranını %25'e çıkar
```

**Implementation:**
```java
@Service
public class PortfolioAnalysisService {

    public PortfolioSummary calculateSummary(Long userId) {
        List<Position> positions = getPositions(userId);
        BigDecimal cash = getCashBalance(userId);

        BigDecimal totalValue = cash;
        BigDecimal totalPnL = BigDecimal.ZERO;

        for (Position pos : positions) {
            totalValue = totalValue.add(pos.getCurrentValue());
            totalPnL = totalPnL.add(pos.getProfitLoss());
        }

        return PortfolioSummary.builder()
            .totalValue(totalValue)
            .cashBalance(cash)
            .stocksValue(totalValue.subtract(cash))
            .totalPnL(totalPnL)
            .positions(positions)
            .build();
    }
}

@Scheduled(cron = "0 0 9 * * MON-FRI") // Her iş günü 09:00
public void sendDailySummary() {
    List<TelegramUserSession> users = sessionService.getAllActiveUsers();

    for (TelegramUserSession user : users) {
        PortfolioSummary summary = portfolioAnalysisService.calculateSummary(
            user.getPlatformUserId());

        String message = formatDailySummary(summary);
        notificationService.send(user.getTelegramUserId(), message);
    }
}
```

**Tahmini Süre:** 12-15 saat
**Öncelik:** 🟡 Orta

---

## 🟢 Uzun Vadeli Özellikler (1-2 Ay)

### 8. Stop-Loss / Take-Profit Emirleri

**Kullanım:**
```
*🛡️ Koruma Emri Kur*

THYAO pozisyonu için:
Miktar: 1000 adet
Alış: ₺78.50
Güncel: ₺82.30 (+4.84%)

Stop-Loss (Zarar Kes):
₺75.00 (-4.46%) önerilir

Take-Profit (Kar Al):
₺90.00 (+14.65%) önerilir

[⚙️ Özelleştir]  [✅ Kur]  [❌ İptal]
```

**Tetiklenme:**
```
🛑 *Stop-Loss Tetiklendi!*

THYAO pozisyonunuz
₺75.00 seviyesine ulaştı

Otomatik SATIŞ emri gönderildi:
1000 adet @ Piyasa

Koruma hedefi: ₺75,000
Gerçekleşen: ₺74,850
Kayıp: -₺3,650 (-4.65%)

[📊 Pozisyonlar]  [📈 Detay]
```

**Tahmini Süre:** 15-20 saat
**Öncelik:** 🟢 Düşük

---

### 9. Akıllı Emir Önerileri (AI-Powered)

**Öneri Motoru:**
```
💡 *Akıllı Öneriler*

THYAO için:

✅ Kar Realizasyonu
RSI: 72 (Aşırı Alım)
Öneri: 300-400 adet sat
Hedef Kar: ₺1,200 - ₺1,600

⚠️ Destek Seviyesi Yakın
₺78.00 kritik destek
Stop-loss öneriyoruz: ₺77.50

📊 Teknik Göstergeler:
• MACD: Satış sinyali
• Bollinger: Üst bantta
• Hacim: Normal

🎯 Hedef Fiyat:
Destek: ₺78.00
Direnç: ₺85.00
```

**Tahmini Süre:** 20-30 saat
**Öncelik:** 🟢 Düşük

---

## 📋 Teknik Detaylar

### Kullanılacak Teknolojiler

**Backend:**
- Spring Boot 3.3.4
- Spring @Scheduled (Background Jobs)
- Redis (Session + Order Tracking)
- PostgreSQL (Price Alerts, Templates)

**APIs:**
- AlgoLab REST API (Order operations)
- AlgoLab WebSocket API (Real-time prices)
- Telegram Bot API 7.10.0

**Libraries:**
- Jackson (JSON parsing)
- Lombok (Boilerplate reduction)
- Spring Data JPA
- Spring Data Redis

### Veritabanı Tabloları

```sql
-- Fiyat Alarmları
CREATE TABLE price_alerts (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    target_price DECIMAL(10,2) NOT NULL,
    direction VARCHAR(5) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    triggered_at TIMESTAMP
);

-- Emir Şablonları
CREATE TABLE order_templates (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    template_name VARCHAR(50) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    side VARCHAR(4) NOT NULL,
    order_type VARCHAR(6) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2),
    use_count INT DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Watchlist
CREATE TABLE watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    added_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(telegram_user_id, symbol)
);

-- Stop-Loss / Take-Profit
CREATE TABLE protection_orders (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    stop_loss_price DECIMAL(10,2),
    take_profit_price DECIMAL(10,2),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    triggered_at TIMESTAMP
);
```

### Redis Keys

```
telegram:user:{userId}:session              → TelegramUserSession
telegram:user:{userId}:tracked_orders       → Set<OrderId>
telegram:user:{userId}:conversation_state   → ConversationState
telegram:user:{userId}:temp_data:{key}      → String (temp data)
telegram:quote:{symbol}                     → QuoteData (cached)
```

### Background Jobs Schedule

```java
@Scheduled(fixedDelay = 60000)    // Her 1 dakika
public void checkOrderStatus()

@Scheduled(fixedDelay = 30000)    // Her 30 saniye
public void checkPriceAlerts()

@Scheduled(fixedDelay = 10000)    // Her 10 saniye
public void checkStopLossOrders()

@Scheduled(cron = "0 0 9 * * MON-FRI")  // Her iş günü 09:00
public void sendDailySummary()
```

---

## 📊 İmplementasyon Sırası

### Faz 1: Kritik Düzeltmeler (1 gün)
1. ✅ LOT/ADET terminoloji hatası düzelt
   - OrderCommandHandler.java
   - BrokerCommandHandler.java
   - AlgoLabOrderService.java
   - Tüm mesajlar ve javadoc'lar

### Faz 2: Temel UX İyileştirmeleri (1 hafta)
2. ✅ Pozisyonlardan direkt işlem butonları
   - Hızlı sat
   - Ortalama al
   - Detaylı görünüm
3. ✅ Anlık fiyat ve tutar gösterimi
   - Market data service
   - Order calculation service
   - Güncel fiyat entegrasyonu

### Faz 3: Bildirimler (1 hafta)
4. ✅ Emir gerçekleşme bildirimleri
   - Background job
   - Order tracking (Redis)
   - Notification service

### Faz 4: Ek Özellikler (2 hafta)
5. ✅ Fiyat alarmları ve watchlist
6. ✅ Hızlı işlem şablonları
7. ✅ Portföy analizi

### Faz 5: İleri Özellikler (1 ay)
8. ✅ Stop-loss / Take-profit
9. ✅ Akıllı öneriler

---

## ✅ Kontrol Listesi

### Faz 1 - Önce Yapılacaklar:
- [ ] LOT/ADET hatası düzelt (30-45 dk)
- [ ] Testleri çalıştır
- [ ] Dokümantasyonu güncelle

### Faz 2 - Hemen Sonra:
- [ ] Pozisyon butonları (4-6 saat)
- [ ] Anlık fiyat gösterimi (5-7 saat)
- [ ] Test ve kullanıcı feedback

### Faz 3 - Sonraki Adım:
- [ ] Bildirim sistemi (6-8 saat)
- [ ] Background job setup
- [ ] Production deployment

---

**Son Güncelleme:** 2025-10-27
**Durum:** ✅ Plan hazır, geliştirmeye başlanabilir
**İlk Hedef:** LOT/ADET düzeltmesi (30-45 dakika)

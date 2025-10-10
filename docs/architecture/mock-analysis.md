# BIST Trading Platform - Mock Implementation Analiz Raporu

**Tarih:** 2025-10-09
**Analiz Edilen Dosyalar:**
- SimplifiedMarketDataService.java
- BrokerIntegrationService.java (formerly SimplifiedBrokerService.java)
- Market Data DTOs (7 adet)
- Broker DTOs (3 adet)
- BrokerController.java

---

## 1. Mock Metotlar Listesi

### 1.1 SimplifiedMarketDataService Mock Metotları

| # | Metod Adı | Return Type | Ne Döndürüyor | Gerçek İmplementasyon İçin Gereksinimler |
|---|-----------|-------------|---------------|------------------------------------------|
| 1 | `getMultiTimeframeOHLCV()` | `CompletableFuture<Map<String, List<OHLCVData>>>` | 4 farklı timeframe için (1m, 5m, 1h, 1d) sahte OHLCV verisi üretiyor | AlgoLab WebSocket'ten gerçek zamanlı mum verisi çekmeli. Her timeframe için ayrı subscription gerekebilir. |
| 2 | `analyzeVolume()` | `VolumeAnalysis` | Statik volüm analizi (VWAP, volatilite, peak volume) | Gerçek OHLCV verisinden VWAP ve volüm trendleri hesaplanmalı. TimescaleDB'den aggregation yapılabilir. |
| 3 | `calculateTechnicalIndicators()` | `CompletableFuture<TechnicalAnalysis>` | Sabit teknik indikatörler (SMA, EMA, RSI, MACD, Bollinger Bands) | Ta4j veya benzer bir kütüphane ile OHLCV verisinden gerçek hesaplama yapılmalı. |
| 4 | `analyzeTrend()` | `TrendAnalysis` | Statik trend analizi (UP trend, %2.28 değişim) | Linear regression ile trend direction ve strength hesaplanmalı. |
| 5 | `analyzeOrderBook()` | `OrderBookAnalysis` | Sabit order book metrikleri (spread, market depth, liquidity) | AlgoLab WebSocket'ten L2 order book verisi çekilmeli ve gerçek zamanlı analiz yapılmalı. |
| 6 | `analyzeMicrostructure()` | `MicrostructureAnalysis` | Statik mikroyapı analizi (spread metrics, order imbalance) | Order book ve trade tick verilerinden gerçek hesaplama yapılmalı. |
| 7 | `getMarketOverview()` | `MarketOverview` | Sabit market genel görünümü (125 aktif sembol, top performers) | TimescaleDB'den tüm semboller için aggregation yapılmalı. Gerçek günlük performans hesaplanmalı. |
| 8 | `generateMockOHLCVData()` | `List<OHLCVData>` | Rastgele OHLCV verisi üretiyor (private helper method) | Bu metod kaldırılacak, gerçek veri kullanılacak. |

**Market Data Servisi Toplam:** 7 ana mock metod, 1 yardımcı mock metod

---

### 1.2 BrokerIntegrationService Mock Metotları

| # | Metod Adı | Return Type | Ne Döndürüyor | Gerçek İmplementasyon İçin Gereksinimler |
|---|-----------|-------------|---------------|------------------------------------------|
| 1 | `sendOrder()` | `AlgoLabResponse<Object>` | UUID ve timestamp ile sahte emir ID'si, status: SUBMITTED | AlgoLab REST API'ye POST request atılmalı (`/sendorder` endpoint). Gerçek broker order ID dönmeli. |
| 2 | `modifyOrder()` | `AlgoLabResponse<Object>` | Değiştirme zamanı ve status: MODIFIED | AlgoLab REST API'ye PUT/POST request (`/modifyorder` endpoint). Gerçek order modification yapılmalı. |
| 3 | `deleteOrder()` | `AlgoLabResponse<Object>` | İptal zamanı ve status: CANCELLED | AlgoLab REST API'ye DELETE/POST request (`/deleteorder` endpoint). Gerçek order cancellation yapılmalı. |
| 4 | `getPositions()` | `List<Map<String, Object>>` | 2 adet sahte pozisyon (AKBNK, THYAO) - sabit veriler | AlgoLab REST API'den gerçek pozisyonlar çekilmeli (`/getpositions` veya benzeri). |
| 5 | `getTodaysTransactions()` | `AlgoLabResponse<Object>` | 1 adet sahte transaction - sabit commission ve amount | AlgoLab REST API'den günün gerçek transaction'ları çekilmeli. Date range parametresi eklenebilir. |
| 6 | `getInstantPosition()` | `AlgoLabResponse<Object>` | Sabit total PnL (%3.63) ve 2 pozisyon | AlgoLab'den gerçek zamanlı pozisyon verisi çekilmeli. Cache edilebilir (örn. 1 saniye TTL). |
| 7 | `isAuthenticated()` | `boolean` | Her zaman `true` döner | AlgoLab authentication durumu kontrol edilmeli. Token expiry check yapılmalı. |
| 8 | `isConnected()` | `boolean` | Her zaman `true` döner | AlgoLab WebSocket ve REST API bağlantı durumu kontrol edilmeli. |

**Broker Servisi Toplam:** 8 mock metod

---

### 1.3 BrokerController'da Mock Endpoint

| # | Endpoint | HTTP Method | Controller Metod | Mock Data Kaynağı |
|---|----------|-------------|------------------|-------------------|
| 1 | `/api/v1/broker/symbols` | GET | `getAvailableSymbols()` | Controller içinde hardcoded 5 adet BIST hisse | Gerçek sembol listesi AlgoLab API'den veya statik bir config'den çekilmeli. |

**Not:** Diğer tüm controller endpoint'leri BrokerIntegrationService'e delegate ediyor, dolayısıyla yukarıdaki broker mock'larını kullanıyor.

---

## 2. DTO Uyumluluk Analizi

### 2.1 Market Data DTOs

| DTO Sınıfı | Alan Sayısı | AlgoLab API Uyumluluğu | Notlar |
|------------|-------------|------------------------|--------|
| `OHLCVData` | 6 alan | ✅ **UYUMLU** | Temel OHLCV standardı. AlgoLab'ın döndüğü format map edilebilir. |
| `VolumeAnalysis` | 11 alan | ⚠️ **KISMİ UYUMLU** | VWAP gibi temel metrikler AlgoLab'dan gelebilir ama custom analiz alanları (`highVolumePeriodsWithPriceImpact`) backend'de hesaplanmalı. |
| `TechnicalAnalysis` | 23 alan | ❌ **UYUMSUZ** | AlgoLab teknik indikatör hesaplama API'si sunmuyor. Tamamı backend'de hesaplanmalı (Ta4j kullanılabilir). |
| `TrendAnalysis` | 12 alan | ❌ **UYUMSUZ** | Tamamen custom analiz. Backend'de regression ve istatistiksel hesaplama gerekli. |
| `OrderBookAnalysis` | 15 alan | ⚠️ **KISMİ UYUMLU** | AlgoLab L2 order book verisi sağlıyorsa temel metrikler (spread, depth) hesaplanabilir. Custom analiz backend'de yapılmalı. |
| `MicrostructureAnalysis` | 15 alan | ❌ **UYUMSUZ** | İleri düzey mikroyapı analizi. Backend'de karmaşık hesaplama gerekli. |
| `MarketOverview` | 12 alan | ⚠️ **KISMİ UYUMLU** | Temel market stats AlgoLab veya TimescaleDB'den gelebilir. Aggregation backend'de yapılmalı. |

**Özet:**
- **Tamamen Uyumlu:** 1/7 (OHLCVData)
- **Kısmi Uyumlu:** 3/7 (VolumeAnalysis, OrderBookAnalysis, MarketOverview)
- **Uyumsuz (Backend Hesaplama Gerekli):** 3/7 (TechnicalAnalysis, TrendAnalysis, MicrostructureAnalysis)

### 2.2 Broker DTOs

| DTO Sınıfı | Alan Sayısı | AlgoLab API Uyumluluğu | Notlar |
|------------|-------------|------------------------|--------|
| `SendOrderRequest` | 8 alan | ✅ **UYUMLU** | AlgoLab `/sendorder` endpoint'ine doğrudan map edilebilir. Field isimleri farklı olabilir (örn. `direction` → `side`). |
| `ModifyOrderRequest` | 4 alan | ✅ **UYUMLU** | AlgoLab `/modifyorder` endpoint'ine map edilebilir. `orderId` path parameter olarak eklenmeli. |
| `AlgoLabResponse<T>` | 6 alan | ✅ **UYUMLU** | Generic wrapper. AlgoLab'ın response formatı map edilebilir. Error handling için uygun. |

**Özet:**
- **Tamamen Uyumlu:** 3/3
- Broker DTO'ları AlgoLab API contract'ına uygun şekilde tasarlanmış.

---

## 3. Öncelikli İmplementasyon Listesi

Aşağıdaki liste, işlevsellik ve bağımlılıklara göre **öncelik sırasına** göre düzenlenmiştir.

### 🔴 **ÖNCELİK 1: KRİTİK - Broker Temel İşlemler**

| Sıra | Metod | Sebep | Bağımlılıklar |
|------|-------|-------|---------------|
| 1 | `BrokerService.isAuthenticated()` | Tüm broker işlemleri authentication'a bağımlı | AlgoLab login/token yönetimi |
| 2 | `BrokerService.isConnected()` | Connection health check gerekli | WebSocket ve REST API bağlantı kontrolü |
| 3 | `BrokerService.sendOrder()` | En kritik işlem - emir verme | AlgoLab `/sendorder` REST endpoint |
| 4 | `BrokerService.deleteOrder()` | Risk yönetimi - acil iptal gerekebilir | AlgoLab `/deleteorder` REST endpoint |
| 5 | `BrokerService.modifyOrder()` | Emir değiştirme | AlgoLab `/modifyorder` REST endpoint |

**Tahmini Süre:** 1-2 hafta
**Gerekli Kaynaklar:**
- AlgoLab API dokümantasyonu
- WebSocket client implementasyonu (login için)
- REST client (HTTP requests için)
- Token/session yönetimi

---

### 🟠 **ÖNCELİK 2: YÜKSEK - Portföy ve İşlemler**

| Sıra | Metod | Sebep | Bağımlılıklar |
|------|-------|-------|---------------|
| 6 | `BrokerService.getPositions()` | Kullanıcıya mevcut pozisyonları göstermek gerekli | AlgoLab positions API |
| 7 | `BrokerService.getInstantPosition()` | Gerçek zamanlı portföy değeri | Positions + current market prices |
| 8 | `BrokerService.getTodaysTransactions()` | İşlem geçmişi | AlgoLab transactions API |
| 9 | `BrokerController.getAvailableSymbols()` | Kullanıcıya işlem yapılabilir semboller gösterilmeli | AlgoLab symbols API veya statik config |

**Tahmini Süre:** 1 hafta
**Gerekli Kaynaklar:**
- AlgoLab portfolio API endpoint'leri
- Caching mekanizması (Redis - positions için)

---

### 🟡 **ÖNCELİK 3: ORTA - Market Data Temel Fonksiyonlar**

| Sıra | Metod | Sebep | Bağımlılıklar |
|------|-------|-------|---------------|
| 10 | `MarketDataService.getMultiTimeframeOHLCV()` | Grafik verisi göstermek gerekli | AlgoLab WebSocket OHLCV subscription |
| 11 | `MarketDataService.getMarketOverview()` | Dashboard için market genel görünümü | TimescaleDB aggregation + AlgoLab data |
| 12 | `MarketDataService.analyzeOrderBook()` | Likidite analizi için önemli | AlgoLab WebSocket L2 order book |

**Tahmini Süre:** 2 hafta
**Gerekli Kaynaklar:**
- AlgoLab WebSocket client implementasyonu
- TimescaleDB time-series aggregation queries
- Real-time data streaming altyapısı

---

### 🟢 **ÖNCELİK 4: DÜŞÜK - İleri Düzey Analiz Fonksiyonları**

| Sıra | Metod | Sebep | Bağımlılıklar |
|------|-------|-------|---------------|
| 13 | `MarketDataService.calculateTechnicalIndicators()` | İleri düzey trading stratejileri için | OHLCV verisi + Ta4j kütüphanesi |
| 14 | `MarketDataService.analyzeVolume()` | Volüm tabanlı stratejiler için | OHLCV verisi + backend hesaplama |
| 15 | `MarketDataService.analyzeTrend()` | Trend following stratejileri için | OHLCV verisi + regression hesaplama |
| 16 | `MarketDataService.analyzeMicrostructure()` | Mikroyapı analizi niche bir kullanım | Order book + tick data |

**Tahmini Süre:** 3-4 hafta
**Gerekli Kaynaklar:**
- Ta4j veya benzer teknik analiz kütüphanesi
- Statistical computation library (Apache Commons Math)
- Performance optimization (caching, async processing)

---

## 4. Eksik Olan Şeyler

### 4.1 Henüz Mock Bile Olmayan API Endpoint'leri

| # | Eksik Fonksiyon | Açıklama | Gerekli Mi? |
|---|-----------------|----------|-------------|
| 1 | **Order Status Query** | Belirli bir emrin durumunu sorgulama | ✅ **Evet** - Order tracking için kritik |
| 2 | **Active Orders List** | Bekleyen/aktif emirlerin listesi | ✅ **Evet** - Kullanıcı pending emirleri görmeli |
| 3 | **Order History (Completed/Cancelled)** | Tamamlanmış ve iptal edilmiş emir geçmişi | ⚠️ **İstenir** - İstatistiksel analiz için |
| 4 | **Account Balance** | Kullanıcının hesap bakiyesi (cash, buying power) | ✅ **Evet** - Emir vermeden önce bakiye kontrolü |
| 5 | **Real-time Price Quotes** | Belirli bir sembol için anlık fiyat | ✅ **Evet** - Market data'nın en temel parçası |
| 6 | **Symbol Search/Filter** | Sembol arama ve filtreleme | ⚠️ **İstenir** - UX iyileştirmesi için |
| 7 | **Historical Trade Ticks** | Geçmiş trade tick verisi (price, volume, timestamp) | ⚠️ **İstenir** - Mikroyapı analizi için |
| 8 | **News/Announcements Feed** | Hisse senetleri ile ilgili haberler | 🔵 **Opsiyonel** - Sentiment analysis için |
| 9 | **Watchlist Management** | Kullanıcının takip listesi oluşturması | 🔵 **Opsiyonel** - UX feature |
| 10 | **Risk Metrics** | Portföy risk metrikleri (VaR, Sharpe ratio) | 🔵 **Opsiyonel** - İleri düzey feature |

### 4.2 AlgoLab API Kapsam Dışı Kalan Alanlar

AlgoLab muhtemelen aşağıdaki fonksiyonları **sağlamıyor**. Bu durumda **alternatif çözümler** gerekli:

| Fonksiyon | AlgoLab Sağlar mı? | Alternatif Çözüm |
|-----------|-------------------|------------------|
| Teknik İndikatör Hesaplama | ❌ Hayır | Backend'de Ta4j kullanarak hesaplanmalı |
| Trend Analizi | ❌ Hayır | Backend'de istatistiksel hesaplama yapılmalı |
| Mikroyapı Analizi | ❌ Hayır | Backend'de order book ve tick data'dan hesaplanmalı |
| Market Overview Aggregation | ❌ Hayır | TimescaleDB'de continuous aggregates kullanılmalı |
| Haber/Sentiment Analizi | ❌ Hayır | Üçüncü parti API (örn. Bloomberg, Reuters) veya web scraping |

### 4.3 Mimari ve Altyapı Eksikleri

| # | Eksik Bileşen | Açıklama | Gereklilik |
|---|---------------|----------|------------|
| 1 | **AlgoLab WebSocket Client** | Gerçek zamanlı veri akışı için | ✅ Kritik |
| 2 | **AlgoLab REST Client** | Emir verme ve portföy sorguları için | ✅ Kritik |
| 3 | **Token/Session Yönetimi** | AlgoLab authentication ve token refresh | ✅ Kritik |
| 4 | **Rate Limiting** | AlgoLab API rate limit'lerini aşmamak için | ✅ Kritik |
| 5 | **Error Handling & Retry Logic** | API failure durumlarında retry ve fallback | ✅ Kritik |
| 6 | **Data Caching Layer** | Redis - sık kullanılan verileri cache'lemek için | ⚠️ Önemli |
| 7 | **Time-Series Data Ingestion** | AlgoLab'dan gelen OHLCV verisini TimescaleDB'ye yazmak | ⚠️ Önemli |
| 8 | **Async Processing** | Ağır hesaplamaları (teknik analiz) async yapmak | ⚠️ Önemli |
| 9 | **Monitoring & Alerting** | AlgoLab bağlantı durumu ve API hatalarını izlemek | ⚠️ Önemli |
| 10 | **Backtesting Module** | Kullanıcıların stratejilerini test etmesi için | 🔵 Opsiyonel |

---

## 5. AlgoLab API Entegrasyonu Gereksinimleri

### 5.1 Gerekli AlgoLab API Endpoint'leri

Aşağıdaki endpoint'lerin AlgoLab API dokümantasyonunda araştırılması ve kullanılması gerekiyor:

#### **Authentication & Connection**
- `POST /login` veya WebSocket auth - Kullanıcı girişi
- `POST /logout` - Oturum kapatma
- WebSocket handshake ve heartbeat

#### **Order Management**
- `POST /sendorder` - Emir verme ✅
- `POST /modifyorder` - Emir değiştirme ✅
- `POST /deleteorder` - Emir iptal ✅
- `GET /getorder?orderId={id}` - Emir durumu sorgulama ❓
- `GET /getorders` - Aktif emirler listesi ❓

#### **Portfolio & Positions**
- `GET /getpositions` - Mevcut pozisyonlar ✅
- `GET /gettransactions` - İşlem geçmişi ✅
- `GET /getbalance` - Hesap bakiyesi ❓

#### **Market Data (WebSocket)**
- `subscribe:ohlcv` - OHLCV verisi ❓
- `subscribe:orderbook` - L2 order book ❓
- `subscribe:trades` - Trade tick verisi ❓
- `subscribe:quote` - Anlık fiyat ❓

#### **Symbols & Metadata**
- `GET /getsymbols` - İşlem yapılabilir semboller ❓

**Not:** ✅ işaretli endpoint'ler mevcut mock'larda kullanılmış. ❓ işaretliler AlgoLab dokümantasyonundan teyit edilmeli.

### 5.2 AlgoLab API Bilinmeyenleri

Aşağıdaki konuların AlgoLab ile netleştirilmesi gerekiyor:

1. **Rate Limits:** API çağrıları için rate limit var mı? Varsa ne kadar?
2. **WebSocket Subscription Limits:** Aynı anda kaç sembol/channel subscribe edilebilir?
3. **Historical Data:** AlgoLab geçmiş OHLCV verisini sağlıyor mu? Yoksa sadece real-time mı?
4. **Order Book Depth:** L2 order book kaç seviye derinliğinde?
5. **Data Latency:** AlgoLab WebSocket verisi gerçek zamana ne kadar yakın? (örn. <100ms?)
6. **Authentication Mechanism:** JWT token mı, session cookie mi, yoksa başka bir yöntem mi?
7. **Error Codes:** AlgoLab API standart HTTP error code'ları mı kullanıyor, yoksa custom error response'ları mı var?

---

## 6. Önerilen İmplementasyon Yol Haritası

### **Faz 1: Temel Broker Entegrasyonu (Hafta 1-2)**
- AlgoLab authentication ve connection management
- `sendOrder()`, `deleteOrder()`, `modifyOrder()` gerçek implementasyonu
- Basic error handling ve logging

**Deliverable:** Kullanıcılar gerçek emir verebilir ve iptal edebilir.

### **Faz 2: Portföy Yönetimi (Hafta 3)**
- `getPositions()`, `getInstantPosition()`, `getTodaysTransactions()` gerçek implementasyonu
- Account balance endpoint eklenmesi
- Redis caching entegrasyonu

**Deliverable:** Kullanıcılar portföylerini ve işlem geçmişlerini görebilir.

### **Faz 3: Market Data - Temel (Hafta 4-5)**
- AlgoLab WebSocket entegrasyonu
- `getMultiTimeframeOHLCV()` gerçek implementasyonu
- Real-time price quotes endpoint eklenmesi
- TimescaleDB'ye OHLCV verisi yazma

**Deliverable:** Kullanıcılar gerçek zamanlı grafik görebilir.

### **Faz 4: Market Data - İleri (Hafta 6-8)**
- Ta4j entegrasyonu
- `calculateTechnicalIndicators()` gerçek implementasyonu
- `analyzeVolume()` ve `analyzeTrend()` implementasyonu
- Order book analysis (`analyzeOrderBook()`)

**Deliverable:** Kullanıcılar teknik indikatörler ve analiz araçları kullanabilir.

### **Faz 5: Optimizasyon & Extras (Hafta 9-10)**
- Performance tuning (async processing, caching)
- Monitoring ve alerting
- `analyzeMicrostructure()` implementasyonu
- Backtesting modülü (opsiyonel)

**Deliverable:** Platform production-ready, yüksek performanslı ve izlenebilir.

---

## 7. Sonuç ve Öneri

### Mevcut Durum:
- **15 mock metod** var (7 market data + 8 broker)
- **Kritik eksikler:** Order status query, active orders list, account balance, real-time price quotes
- **AlgoLab bağımlılıkları:** Authentication, order management, positions, ve WebSocket market data
- **Backend hesaplama gerektiren:** Teknik indikatörler, trend analizi, mikroyapı analizi

### Öncelik Sırası:
1. **Önce broker fonksiyonlarını implemente et** (order management, positions) - Kullanıcılar işlem yapabilsin
2. **Sonra temel market data** (OHLCV, price quotes) - Kullanıcılar piyasayı görebilsin
3. **En son ileri düzey analiz** (teknik indikatörler, trend analizi) - Value-add feature'lar

### Tahmini Toplam Süre:
- **Minimum Viable Product (MVP):** 3-4 hafta (Faz 1-2)
- **Tam Özellikli Platform:** 8-10 hafta (Tüm fazlar)

### Başlamadan Önce Yapılması Gerekenler:
1. ✅ AlgoLab API dokümantasyonunu detaylı incele
2. ✅ AlgoLab test hesabı ve API credentials temin et
3. ✅ AlgoLab rate limit ve restriction'ları öğren
4. ✅ TimescaleDB time-series schema tasarımını tamamla
5. ✅ Redis caching stratejisini belirle

---

**Rapor Sonu**
# Dinamik WebSocket Subscription Testi

## Yapılan Değişiklikler

**Sorun:** CLI'da THYAO gibi sembolleri görüntülerken tablo boş geliyordu çünkü backend sadece USDTRY'ye subscribe oluyordu.

**Çözüm:**
1. Backend'e dinamik subscription endpoint'i eklendi: `POST /api/v1/broker/websocket/subscribe`
2. CLI otomatik olarak istenen sembole subscribe oluyor

## Test Adımları

### 1. Backend'i Restart Et

IntelliJ IDE'den backend'i yeniden başlat. Ya da terminalden:

```bash
# Önce varsa eski process'i durdur
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Backend'i başlat
SPRING_DATASOURCE_USERNAME=bist_user \
SPRING_DATASOURCE_PASSWORD=bist_password \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading \
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration \
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long \
SERVER_PORT=8080 \
./gradlew bootRun
```

### 2. Backend Loglarını Kontrol Et

IntelliJ Console'da şu logları ara:

```
✅ WebSocket connected
✅ IMMEDIATE subscription sent: [USDTRY]
✅ Received TICK data: Symbol=USDTRY
```

Bu loglar backend'in WebSocket'e bağlandığını gösterir.

### 3. CLI'yı Test Et

```bash
cd cli-client
./start.sh
```

### 4. AlgoLab'a Login

Menu: `2` (Broker) → `3` (AlgoLab Durumu)

Eğer "Bağlı Değil" yazıyorsa:

Ana menüden `4` (AlgoLab Login) seçip login yap.

### 5. Real-Time Tick Data Testi

Menu: `2` (Broker) → `5` (Real-Time Tick Data)

**Test Case 1: THYAO Sembolü**
- Sembol gir: `THYAO`
- Beklenen çıktı:
  ```
  THYAO için WebSocket subscription yapılıyor...
  ✓ THYAO için subscription başarılı
  THYAO için real-time tick data gösteriliyor...
  ```
- Sonuç: THYAO verileri tablolanarak görünmeli

**Test Case 2: AKBNK Sembolü**
- Ctrl+C ile çık
- Tekrar menu: `2` → `5`
- Sembol gir: `AKBNK`
- Beklenen çıktı: AKBNK verileri tablolanarak görünmeli

**Test Case 3: USDTRY (Eski Çalışan)**
- Sembol gir: `USDTRY`
- Sonuç: USDTRY verileri görünmeli (eskiden olduğu gibi)

## Beklenen Backend Logları

Her CLI subscription isteğinde backend'de şunlar görünmeli:

```
INFO  --- WebSocket subscription request from user: testuser for symbol: THYAO, channel: tick
INFO  --- ✅ Successfully subscribed to symbol: THYAO, channel: tick
INFO  --- ✅ Received TICK data: Symbol=THYAO, Price=XX.XX
```

## Sorun Giderme

### CLI'da "Subscription başarısız" Hatası

**Sebep:** Backend WebSocket bağlı değil

**Çözüm:**
1. IntelliJ Console'da WebSocket bağlantısını kontrol et
2. `algolab-session-dev.json` dosyasının güncel olduğundan emin ol
3. Gerekirse CLI'dan yeniden login yap

### Tabloda Veri Görünmüyor

**Sebep:** Sembol için henüz veri gelmemiş olabilir

**Çözüm:**
1. Backend loglarında "Received TICK data" mesajını ara
2. İşlem saatleri dışında bazı semboller veri göndermeyebilir
3. USDTRY'yi dene (24/7 aktif)

### Backend Compile Hatası

```bash
# Clean build
./gradlew clean compileJava

# Hata yoksa başarılı olmalı
BUILD SUCCESSFUL
```

## Özellik Detayları

### BrokerController.java

Yeni endpoint: `POST /api/v1/broker/websocket/subscribe`

**Parametreler:**
- `symbol` (required): Sembol kodu (örn: THYAO, AKBNK)
- `channel` (optional, default: "tick"): Kanal tipi (tick, depth, order)

**Response:**
```json
{
  "success": true,
  "symbol": "THYAO",
  "channel": "tick",
  "message": "Subscription successful",
  "timestamp": "2025-10-17T16:30:00Z"
}
```

### broker.py (CLI)

`view_realtime_ticks()` metoduna eklenen kod:

```python
# Subscribe to this symbol via backend WebSocket
try:
    response = self.api.post("/api/v1/broker/websocket/subscribe",
                            params={"symbol": symbol, "channel": "tick"})
    if response.get("success"):
        console.print(f"[green]✓ {symbol} için subscription başarılı[/green]")
except Exception as e:
    console.print(f"[yellow]⚠ Subscription hatası (devam ediliyor): {str(e)}[/yellow]")
```

## Commit

Değişiklikler commit edildi:

```
commit b1b4513
feat: Implement dynamic WebSocket symbol subscription
```

---

**Test Tarihi:** 2025-10-17
**Durum:** Test edilmeye hazır
**Next Step:** Backend'i restart et ve yukarıdaki test case'leri çalıştır

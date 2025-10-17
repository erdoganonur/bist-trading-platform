# 🚀 BIST Trading Platform - Başlatma Rehberi

Bu rehber, Docker servislerini command line'dan, Spring Boot ve Frontend uygulamalarını IDE'den nasıl başlatacağınızı adım adım gösterir.

---

## 📋 Gereksinimler

### Yazılımlar
- ✅ Java 21 (OpenJDK)
- ✅ Node.js 18+ (npm ile birlikte)
- ✅ Docker Desktop (çalışır durumda)
- ✅ IntelliJ IDEA (Spring Boot için - önerilir)
- ✅ VS Code veya IntelliJ (Frontend için)
- ✅ PostgreSQL Client (opsiyonel - veritabanı kontrolü için)

### Kontroller
```bash
# Java versiyonunu kontrol et
java -version
# Çıktı: openjdk version "21.0.x"

# Node versiyonunu kontrol et
node -v
# Çıktı: v18.x.x veya üstü

# Docker'ın çalıştığını kontrol et
docker --version
docker ps
```

---

## 🔧 ADIM 1: Docker Servislerini Başlatma

### 1.1 Docker Desktop'ı Başlat
Docker Desktop uygulamasının çalıştığından emin olun.

### 1.2 Terminal Aç ve Proje Dizinine Git
```bash
cd /Users/onurerdogan/dev/bist-trading-platform
```

### 1.3 Gerekli Docker Servislerini Başlat

**PostgreSQL ve Redis'i Başlat:**
```bash
docker-compose up -d postgres redis
```

**Çıktı:**
```
✔ Container bist-trading-platform-postgres-1  Started
✔ Container bist-trading-platform-redis-1     Started
```

### 1.4 Servislerin Çalıştığını Doğrula

```bash
# Çalışan konteynerleri listele
docker ps

# PostgreSQL'in hazır olduğunu kontrol et
docker logs bist-trading-platform-postgres-1 --tail 20

# Redis'in hazır olduğunu kontrol et
docker logs bist-trading-platform-redis-1 --tail 20
```

**Beklenen Çıktı:**
```
CONTAINER ID   IMAGE          PORTS                    STATUS
xxxxx          postgres:16    0.0.0.0:5432->5432/tcp   Up
xxxxx          redis:7.4      0.0.0.0:6379->6379/tcp   Up
```

### 1.5 PostgreSQL'e Bağlanma (Opsiyonel)

```bash
# psql ile bağlan
docker exec -it bist-trading-platform-postgres-1 psql -U bist_user -d bist_trading

# Veya dışarıdan bağlan
psql -h localhost -U bist_user -d bist_trading
# Şifre: bist_password
```

**Veritabanı Kontrolleri:**
```sql
-- Bağlı mısın kontrol et
\conninfo

-- Tabloları listele
\dt

-- Çıkış
\q
```

---

## 🍃 ADIM 2: Spring Boot Uygulamasını IntelliJ'den Başlatma

### 2.1 IntelliJ IDEA'yı Aç

1. IntelliJ IDEA'yı başlat
2. **File → Open** menüsünden projeyi aç:
   ```
   /Users/onurerdogan/dev/bist-trading-platform
   ```

### 2.2 Gradle Senkronizasyonu

IntelliJ açıldığında:
1. Sağ altta **Load Gradle Project** popup'ı çıkarsa **Load** tıkla
2. Veya: **View → Tool Windows → Gradle** → **Reload All Gradle Projects** (🔄 icon)

**Bekleme süresi:** 1-2 dakika (bağımlılıklar indirilecek)

### 2.3 Run Configuration Oluştur

#### Yöntem A: Otomatik (Önerilir)

1. Ana sınıfı aç:
   ```
   src/main/java/com/bisttrading/BistTradingPlatformApplication.java
   ```

2. Sınıfın yanındaki **yeşil ▶️ ok**a tıkla → **Run 'BistTradingPlatformApplication'**

#### Yöntem B: Manuel Run Configuration

1. **Run → Edit Configurations...**
2. **+ → Application**
3. Aşağıdaki ayarları yap:

**Run Configuration Ayarları:**
```
Name: BIST Trading Platform

Build and run:
  - Java: 21
  - Main class: com.bisttrading.BistTradingPlatformApplication
  - Module: bist-trading-platform.main

Environment variables:
SPRING_DATASOURCE_USERNAME=bist_user
SPRING_DATASOURCE_PASSWORD=bist_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long
SERVER_PORT=8080

Working directory: $MODULE_WORKING_DIR$
```

4. **Apply → OK**

### 2.4 Uygulamayı Başlat

1. **Run → Run 'BistTradingPlatformApplication'** (Shift + F10)
2. Veya toolbar'daki **▶️ Run** butonuna tıkla

### 2.5 Başarılı Başlatma Logları

**IntelliJ Console'da göreceğiniz loglar:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

2025-10-10 12:30:45.123  INFO ... : Starting BistTradingPlatformApplication
2025-10-10 12:30:45.456  INFO ... : HikariPool-1 - Start completed.
2025-10-10 12:30:46.789  INFO ... : Started BistTradingPlatformApplication in 2.5 seconds

🚀 BIST Trading Platform STARTED
📊 Swagger UI: http://localhost:8080/swagger-ui.html
🔌 WebSocket: ws://localhost:8080/api/v1/broker/websocket
💚 Actuator Health: http://localhost:8080/actuator/health
```

### 2.6 Backend'in Çalıştığını Doğrula

**Terminal'de Test Et:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Beklenen çıktı:
# {"status":"UP"}

# Swagger UI'ı aç
open http://localhost:8080/swagger-ui.html
```

---

## ⚛️ ADIM 3: Frontend'i IDE'den Başlatma

### Seçenek A: VS Code ile Başlatma (Önerilir)

#### 3.1 VS Code'u Aç

```bash
cd /Users/onurerdogan/dev/bist-trading-platform/frontend
code .
```

#### 3.2 Bağımlılıkları Yükle (İlk Seferinde)

**Terminal:** View → Terminal (Ctrl + `)

```bash
npm install
```

#### 3.3 Development Server'ı Başlat

```bash
npm run dev
```

**Beklenen Çıktı:**
```
  VITE v7.1.9  ready in 450 ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
  ➜  press h + enter to show help
```

### Seçenek B: IntelliJ IDEA ile Başlatma

#### 3.1 Frontend Klasörünü Aç

1. IntelliJ'de **File → Open**
2. Frontend klasörünü seç:
   ```
   /Users/onurerdogan/dev/bist-trading-platform/frontend
   ```
3. **New Window** seç (yeni pencerede açılsın)

#### 3.2 package.json'dan Başlat

1. `package.json` dosyasını aç
2. `"scripts"` bölümünde `"dev"` satırını bul
3. Satırın yanındaki **▶️** ikona tıkla → **Run 'dev'**

#### 3.3 Veya NPM Script Panel'den

1. **View → Tool Windows → npm**
2. **dev** script'ine çift tıkla

### 3.4 Frontend'in Çalıştığını Doğrula

```bash
# Browser'da aç
open http://localhost:3000

# Veya curl ile test et
curl http://localhost:3000
```

---

## ✅ ADIM 4: Tam Sistem Doğrulama

### 4.1 Tüm Servislerin Durumu

| Servis | Port | URL | Durum Kontrol |
|--------|------|-----|---------------|
| **PostgreSQL** | 5432 | - | `docker ps` |
| **Redis** | 6379 | - | `docker ps` |
| **Spring Boot** | 8080 | http://localhost:8080 | `curl http://localhost:8080/actuator/health` |
| **Frontend** | 3000 | http://localhost:3000 | Browser'da aç |

### 4.2 Swagger UI'da API Test

1. Swagger UI'ı aç: http://localhost:8080/swagger-ui.html
2. **auth-controller** → **POST /api/v1/auth/register** endpoint'ini aç
3. **Try it out** tıkla
4. Test kullanıcısı oluştur:

```json
{
  "email": "test@bisttrading.com",
  "username": "testuser",
  "password": "Test123!",
  "fullName": "Test User",
  "phoneNumber": "+905551234567"
}
```

5. **Execute** tıkla
6. Response 200 OK dönmeli ve JWT token almalısınız

### 4.3 Frontend'de Login Test

1. Browser'da aç: http://localhost:3000
2. Login sayfası açılmalı
3. Yukarıda oluşturduğunuz kullanıcı ile giriş yapın:
   - **Username:** testuser
   - **Password:** Test123!
4. **Sign In** butonuna tıklayın
5. Dashboard sayfasına yönlendirilmelisiniz

### 4.4 WebSocket Bağlantı Testi

1. Dashboard'da **F12** → Console sekmesini açın
2. Aşağıdaki logları görmelisiniz:

```
[WebSocket] Connecting to: ws://localhost:8080/api/v1/broker/websocket
[WebSocket] Connected
[WebSocket] Authenticated
[useWebSocket] Connection established
```

3. **Network** sekmesi → **WS** filtresi
4. WebSocket bağlantısını görebilirsiniz
5. **Messages** tab'inde mesaj akışını izleyebilirsiniz

---

## 🎯 ADIM 5: Development Workflow

### Normal Kullanım (Her Gün)

```bash
# 1. Docker servislerini başlat (sadece bir kez)
cd /Users/onurerdogan/dev/bist-trading-platform
docker-compose up -d postgres redis

# 2. IntelliJ'den Spring Boot'u başlat
# → Run butonuna bas

# 3. VS Code/IntelliJ'den Frontend'i başlat
cd frontend
npm run dev

# Artık geliştirmeye hazırsınız! 🎉
```

### Kod Değişikliklerinde

**Spring Boot:**
- IntelliJ'de **Update Running Application** (Ctrl+F10) veya
- **Restart** (Spring Boot DevTools auto-restart yapar)

**Frontend:**
- Vite otomatik Hot Module Replacement (HMR) yapar
- Kaydettiğinizde browser otomatik güncellenir

---

## 🛑 Servisleri Durdurma

### Docker Servislerini Durdur

```bash
# Tüm servisleri durdur
docker-compose down

# Sadece belirli servisleri durdur
docker-compose stop postgres redis

# Veya tek tek durdur
docker stop bist-trading-platform-postgres-1
docker stop bist-trading-platform-redis-1
```

### Spring Boot'u Durdur

IntelliJ'de:
- **🟥 Stop** butonuna tıkla (Ctrl+F2)

### Frontend'i Durdur

Terminal'de:
- **Ctrl + C** tuşlarına bas

---

## 🔧 Sorun Giderme

### Problem 1: Docker Servisleri Başlamıyor

**Hata:** `Cannot start service postgres: port is already allocated`

**Çözüm:**
```bash
# Port kullanan process'i bul
lsof -i :5432
lsof -i :6379

# Process'i durdur
kill -9 <PID>

# Veya tüm Docker containerları temizle
docker-compose down -v
docker system prune -a
```

### Problem 2: Spring Boot Database Bağlantı Hatası

**Hata:** `Connection to localhost:5432 refused`

**Kontrol Et:**
```bash
# PostgreSQL çalışıyor mu?
docker ps | grep postgres

# Log'lara bak
docker logs bist-trading-platform-postgres-1

# PostgreSQL'e manuel bağlan
docker exec -it bist-trading-platform-postgres-1 psql -U bist_user -d bist_trading
```

**Çözüm:**
- PostgreSQL'in başladığından emin ol: `docker-compose up -d postgres`
- Environment variable'ları kontrol et (şifre, kullanıcı adı)

### Problem 3: Frontend WebSocket Bağlanamıyor

**Hata:** Console'da `WebSocket connection failed`

**Kontrol Et:**
```bash
# Backend çalışıyor mu?
curl http://localhost:8080/actuator/health

# WebSocket endpoint erişilebilir mi?
curl -i -N -H "Connection: Upgrade" \
     -H "Upgrade: websocket" \
     http://localhost:8080/api/v1/broker/websocket
```

**Çözüm:**
- Spring Boot'un çalıştığından emin ol
- JWT token'ın geçerli olduğundan emin ol (yeniden login yap)
- CORS ayarlarını kontrol et

### Problem 4: Gradle Build Hatası

**Hata:** `Could not resolve dependencies`

**Çözüm:**
```bash
# Gradle cache'i temizle
./gradlew clean --refresh-dependencies

# IntelliJ'de:
# File → Invalidate Caches → Invalidate and Restart
```

### Problem 5: Frontend Build Hatası

**Hata:** `Cannot find module` veya `Type error`

**Çözüm:**
```bash
cd frontend

# node_modules'u temizle
rm -rf node_modules package-lock.json

# Yeniden yükle
npm install

# Vite cache'i temizle
rm -rf node_modules/.vite
```

### Problem 6: Port Zaten Kullanımda

**Backend (8080):**
```bash
# Port 8080'i kullanan process'i bul
lsof -i :8080

# Durdur
kill -9 <PID>
```

**Frontend (3000):**
```bash
# Port 3000'i kullanan process'i bul
lsof -i :3000

# Durdur
kill -9 <PID>
```

---

## 📊 Sistem Durumu Dashboard

### Tüm Servisleri Kontrol Et

```bash
#!/bin/bash
# check-services.sh

echo "🔍 BIST Trading Platform - Servis Durumu"
echo "========================================"

# Docker
echo "📦 Docker Servisleri:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep bist

# Spring Boot
echo ""
echo "🍃 Spring Boot (Port 8080):"
curl -s http://localhost:8080/actuator/health | jq '.' 2>/dev/null || echo "❌ Çalışmıyor"

# Frontend
echo ""
echo "⚛️  Frontend (Port 3000):"
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 && echo "✅ Çalışıyor" || echo "❌ Çalışmıyor"

echo ""
echo "✅ Kontrol tamamlandı!"
```

**Kullanım:**
```bash
chmod +x check-services.sh
./check-services.sh
```

---

## 🎓 Faydalı IntelliJ Shortcuts

| Kısayol | Açıklama |
|---------|----------|
| **Shift + F10** | Run çalıştır |
| **Ctrl + F2** | Stop durdur |
| **Ctrl + F10** | Update running application |
| **Alt + Shift + F10** | Run menüsünü aç |
| **Ctrl + Shift + F10** | Cursor'daki sınıfı/testi çalıştır |
| **Cmd + ,** | Settings |

---

## 📚 Ek Kaynaklar

### Swagger UI Endpoints
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs

### Actuator Endpoints
- **Health:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics
- **Info:** http://localhost:8080/actuator/info

### Frontend URLs
- **Login:** http://localhost:3000/login
- **Dashboard:** http://localhost:3000/dashboard

### Database Tools
- **pgAdmin:** http://localhost:5050 (eğer docker-compose'da varsa)
- **Redis Commander:** http://localhost:8081 (eğer docker-compose'da varsa)

---

## ✅ Başarılı Başlatma Checklist

- [ ] Docker Desktop çalışıyor
- [ ] PostgreSQL container başladı (port 5432)
- [ ] Redis container başladı (port 6379)
- [ ] Spring Boot IntelliJ'den başladı (port 8080)
- [ ] Swagger UI erişilebilir (http://localhost:8080/swagger-ui.html)
- [ ] Frontend başladı (port 3000)
- [ ] Login yapabiliyorum
- [ ] WebSocket bağlantısı başarılı
- [ ] Console'da hata yok

---

## 🎉 Tebrikler!

Tüm servisler çalışıyorsa, artık geliştirmeye hazırsınız!

**Keyifli kodlamalar!** 🚀

---

**Not:** Bu rehberi bookmark'layın veya README.md'ye link verin, her zaman kolayca erişebilmeniz için!

# BIST Trading Platform - Test Ortamı Kılavuzu

## 🚀 Sistem Durumu

### Çalışan Servisler
- ✅ **PostgreSQL** (TimescaleDB) - Port: 5432
- ✅ **Redis** - Port: 6379
- ✅ **Apache Kafka** - Port: 29092
- ✅ **Spring Boot Monolith** - Port: 8080

---

## 📖 Test Araçları

### 1️⃣ Swagger UI (Önerilen - En Kolay Yöntem) ⭐

Swagger UI, API'yi görsel olarak test etmenin en kolay yoludur.

**Erişim:**
```
http://localhost:8080/swagger-ui/index.html
```

**NOT:** Tarayıcınızda yukarıdaki linke tıklayın veya kopyalayıp yapıştırın.

**Özellikler:**
- 🎯 Tüm endpoint'leri görsel arayüzde görebilirsiniz
- 🔐 JWT token authentication desteği
- 📝 Request/Response örnekleri
- ✅ Direkt tarayıcıdan test edebilirsiniz

**Nasıl Kullanılır:**

1. Swagger UI'ı açın: http://localhost:8080/swagger-ui/index.html

2. **Authentication (JWT Token Alma):**
   - "Authentication Controller" bölümünü açın
   - `POST /api/auth/register` ile kullanıcı oluşturun
   - Veya `POST /api/auth/login` ile giriş yapın
   - Response'dan `token` değerini kopyalayın

3. **Token'ı Ayarlama:**
   - Sayfanın üst kısmındaki **"Authorize"** butonuna tıklayın
   - "Value" alanına: `Bearer <token>` yazın (örnek: `Bearer eyJhbGc...`)
   - "Authorize" butonuna basın

4. **API Test Etme:**
   - İstediğiniz endpoint'i seçin
   - "Try it out" butonuna basın
   - Gerekli parametreleri doldurun
   - "Execute" butonuna basın

---

### 2️⃣ Postman Collection

Detaylı test senaryoları için hazır Postman collection oluşturuldu.

**Dosya:** `BIST-Trading-Platform.postman_collection.json`

**Import Etme:**
1. Postman'i açın
2. "Import" butonuna tıklayın
3. `BIST-Trading-Platform.postman_collection.json` dosyasını seçin
4. Collection otomatik olarak yüklenecek

**Özellikler:**
- 🔄 Otomatik JWT token yönetimi
- 📦 7 farklı test kategorisi (40+ endpoint)
- 🎯 Environment variables ile kolay yönetim
- ✅ Test script'leri ile otomatik validasyon

**Collection İçeriği:**
1. **Authentication** - Register, Login, Logout, Profile
2. **User Management** - CRUD operations
3. **Symbols** - Market data operations
4. **Broker Accounts** - Broker hesap yönetimi
5. **Orders** - Emir oluşturma, iptal, sorgulama
6. **Positions** - Pozisyon takibi
7. **Health & Monitoring** - Sistem sağlık kontrolleri

**Kullanım:**
1. Koleksiyonu import edin
2. İlk olarak "Register New User" veya "Login" isteğini çalıştırın
3. JWT token otomatik olarak environment'a kaydedilir
4. Diğer istekleri sırayla test edebilirsiniz

---

### 3️⃣ cURL (Terminal'den Test)

Terminal kullanarak hızlı test yapmak için:

#### Authentication
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!@#",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!@#"
  }'

# Token'ı bir değişkene kaydedin
TOKEN="<yukarıdaki response'dan gelen token>"
```

#### Authenticated Requests
```bash
# Get Current User
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# Create Symbol
curl -X POST http://localhost:8080/api/symbols \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "GARAN",
    "name": "Garanti Bankası",
    "exchange": "BIST",
    "sector": "Bankalar",
    "active": true
  }'

# Create Order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "symbolCode": "GARAN",
    "side": "BUY",
    "type": "LIMIT",
    "quantity": 100,
    "price": 50.25,
    "timeInForce": "DAY"
  }'
```

---

## 🔍 Endpoint'ler

### Ana Kategoriler

#### 🔐 Authentication (`/api/auth`)
- `POST /api/auth/register` - Yeni kullanıcı kaydı
- `POST /api/auth/login` - Kullanıcı girişi
- `POST /api/auth/logout` - Çıkış
- `GET /api/auth/me` - Mevcut kullanıcı bilgisi

#### 👤 User Management (`/api/users`)
- `GET /api/users` - Tüm kullanıcılar
- `GET /api/users/{username}` - Kullanıcı detayı
- `PUT /api/users/{username}` - Kullanıcı güncelleme

#### 📊 Symbols (`/api/symbols`)
- `GET /api/symbols` - Tüm semboller
- `POST /api/symbols` - Yeni sembol oluşturma
- `GET /api/symbols/{code}` - Sembol detayı
- `PUT /api/symbols/{code}` - Sembol güncelleme

#### 🏦 Broker Accounts (`/api/broker-accounts`)
- `GET /api/broker-accounts` - Tüm broker hesapları
- `POST /api/broker-accounts` - Yeni broker hesabı
- `GET /api/broker-accounts/{id}` - Hesap detayı
- `PUT /api/broker-accounts/{id}` - Hesap güncelleme

#### 📝 Orders (`/api/orders`)
- `GET /api/orders` - Tüm emirler
- `POST /api/orders` - Yeni emir oluşturma
- `GET /api/orders/{id}` - Emir detayı
- `POST /api/orders/{id}/cancel` - Emir iptali
- `GET /api/orders/symbol/{code}` - Sembole göre emirler

#### 💼 Positions (`/api/positions`)
- `GET /api/positions` - Tüm pozisyonlar
- `GET /api/positions/symbol/{code}` - Sembole göre pozisyon
- `GET /api/positions/summary` - Pozisyon özeti

---

## 🎯 Test Senaryoları

### Senaryo 1: Temel Kullanıcı Akışı

1. **Kullanıcı Kaydı**
   ```
   POST /api/auth/register
   ```

2. **Giriş Yapma**
   ```
   POST /api/auth/login
   ```

3. **Profil Görüntüleme**
   ```
   GET /api/auth/me
   ```

### Senaryo 2: Trading İşlemleri

1. **Sembol Oluşturma**
   ```
   POST /api/symbols
   Body: { "code": "GARAN", "name": "Garanti BBVA", ... }
   ```

2. **Broker Hesabı Ekleme**
   ```
   POST /api/broker-accounts
   Body: { "brokerName": "AlgoLab", "accountNumber": "123456", ... }
   ```

3. **Emir Verme**
   ```
   POST /api/orders
   Body: { "symbolCode": "GARAN", "side": "BUY", "quantity": 100, ... }
   ```

4. **Emirleri Görüntüleme**
   ```
   GET /api/orders
   ```

5. **Pozisyonları Kontrol Etme**
   ```
   GET /api/positions
   ```

### Senaryo 3: Sistem Sağlık Kontrolü

1. **Health Check**
   ```
   GET /actuator/health
   ```

2. **Application Info**
   ```
   GET /actuator/info
   ```

---

## 🛠️ Sistem Yönetimi

### Servisleri Kontrol Etme

```bash
# Docker servislerini görüntüleme
docker ps

# Uygulama loglarını görüntüleme
# (Spring Boot arka planda çalışıyor)

# Health check
curl http://localhost:8080/actuator/health
```

### Servisleri Durdurma

```bash
# Spring Boot uygulamasını durdurmak için
# Ctrl+C veya terminal'i kapatın

# Docker servislerini durdurmak için
docker-compose down
```

### Servisleri Yeniden Başlatma

```bash
# Docker servisleri
docker-compose up -d

# Spring Boot uygulaması
SPRING_DATASOURCE_USERNAME=bist_user \
SPRING_DATASOURCE_PASSWORD=bist_password \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading \
SPRING_FLYWAY_ENABLED=false \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
SPRING_DATA_REDIS_PASSWORD=redis_password \
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long \
SERVER_PORT=8080 \
./gradlew bootRun
```

---

## 📚 Örnek Test Data

### Kullanıcı Kayıt Verisi
```json
{
  "username": "trader01",
  "email": "trader01@example.com",
  "password": "Secure123!@#",
  "firstName": "Ahmet",
  "lastName": "Yılmaz"
}
```

### Sembol Verisi
```json
{
  "code": "GARAN",
  "name": "Garanti BBVA",
  "exchange": "BIST",
  "sector": "Bankalar",
  "active": true
}
```

### Emir Verisi
```json
{
  "symbolCode": "GARAN",
  "side": "BUY",
  "type": "LIMIT",
  "quantity": 100,
  "price": 50.25,
  "timeInForce": "DAY"
}
```

---

## ⚠️ Önemli Notlar

1. **JWT Token Süresi:** Token'lar belirli bir süre sonra expire olur. Yeni token almak için tekrar login yapın.

2. **Database:** Her yeniden başlatmada `hibernate.ddl-auto=update` kullanıldığından veriler korunur.

3. **Redis:** Session ve cache bilgileri Redis'te saklanır.

4. **CORS:** Tüm origin'lere izin verilmiştir (development ortamı için).

---

## 🎉 Başarılı Test Örnekleri

### Başarılı Register Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User"
  }
}
```

### Başarılı Order Response
```json
{
  "id": 1,
  "symbolCode": "GARAN",
  "side": "BUY",
  "type": "LIMIT",
  "status": "PENDING",
  "quantity": 100,
  "price": 50.25,
  "createdAt": "2025-10-07T09:30:00"
}
```

---

## 🔗 Faydalı Linkler

- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **API Docs (JSON):** http://localhost:8080/v3/api-docs
- **Health Check:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics

---

## 💡 İpuçları

1. **Swagger UI'ı kullanın** - En kolay ve hızlı test yöntemidir
2. **Postman Collection'ı kullanın** - Detaylı test senaryoları için
3. **Önce authentication yapın** - Diğer endpoint'ler JWT token gerektirir
4. **Token'ı kaydedin** - Her istekte yeniden login yapmayın
5. **Health endpoint'i kontrol edin** - Sistem durumunu takip edin

---

## 🆘 Sorun Giderme

### Uygulama Başlamıyor
```bash
# Port kontrolü
lsof -i :8080

# Docker servisleri kontrolü
docker ps
```

### Database Bağlantı Hatası
```bash
# PostgreSQL kontrolü
docker logs bist-postgres
```

### Redis Bağlantı Hatası
```bash
# Redis kontrolü
docker logs bist-redis
```

---

**Happy Testing! 🚀**
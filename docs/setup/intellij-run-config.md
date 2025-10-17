# IntelliJ IDEA - Spring Boot Run Configuration

Bu dokümanda IntelliJ IDEA'da Spring Boot uygulamasını nasıl yapılandırıp çalıştıracağınızı bulabilirsiniz.

## 🎯 Yöntem 1: Otomatik Configuration (Önerilir)

### Adımlar:
1. IntelliJ'de projeyi açın
2. `src/main/java/com/bisttrading/BistTradingPlatformApplication.java` dosyasını açın
3. Sınıf isminin yanındaki **yeşil ▶️ play** ikonuna tıklayın
4. **"Run 'BistTradingPlatformApplication.main()'"** seçeneğini seçin

IntelliJ otomatik olarak bir run configuration oluşturacaktır.

## 🔧 Yöntem 2: Manuel Configuration

Eğer environment variable'ları özelleştirmek istiyorsanız:

### Adımlar:

1. **Run → Edit Configurations...**
2. Sol üstte **+ (Add New Configuration)** butonuna tıklayın
3. **Application** seçin

### Configuration Detayları:

```
┌─────────────────────────────────────────────────────────┐
│ Name: BIST Trading Platform                              │
├─────────────────────────────────────────────────────────┤
│                                                           │
│ Build and run:                                            │
│   ☑ Java: 21                                             │
│   ☑ Main class: com.bisttrading.BistTradingPlatform...  │
│       [Browse... kullanarak bulabilirsiniz]              │
│                                                           │
│   ☑ Module: bist-trading-platform.main                   │
│       [Dropdown'dan seçin]                               │
│                                                           │
├─────────────────────────────────────────────────────────┤
│ Environment variables:                                    │
│   [Modify... butonuna tıklayın]                         │
│                                                           │
│   Aşağıdaki değerleri ekleyin:                           │
│                                                           │
│   SPRING_DATASOURCE_USERNAME=bist_user                   │
│   SPRING_DATASOURCE_PASSWORD=bist_password               │
│   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:... │
│   SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework....  │
│   BIST_SECURITY_JWT_SECRET=bist-trading-platform-...    │
│   SERVER_PORT=8080                                        │
│                                                           │
├─────────────────────────────────────────────────────────┤
│ Working directory:                                        │
│   $MODULE_WORKING_DIR$                                    │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### Environment Variables (Detaylı):

```bash
# Veritabanı Bağlantısı
SPRING_DATASOURCE_USERNAME=bist_user
SPRING_DATASOURCE_PASSWORD=bist_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading

# Redis'i devre dışı bırak (şimdilik)
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

# JWT Secret
BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long

# Server Port
SERVER_PORT=8080
```

## 📋 Environment Variables - Kopyala/Yapıştır

IntelliJ'deki **Environment variables** alanına doğrudan yapıştırın:

```
SPRING_DATASOURCE_USERNAME=bist_user;SPRING_DATASOURCE_PASSWORD=bist_password;SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bist_trading;SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;BIST_SECURITY_JWT_SECRET=bist-trading-platform-super-secret-key-for-jwt-tokens-256-bit-long;SERVER_PORT=8080
```

> **Not:** Mac/Linux'ta `;` ile, Windows'ta `;` ile ayırın.

## 🚀 Uygulamayı Çalıştırma

### Kısayollar:
- **Shift + F10** - Son çalıştırılan configuration'ı run et
- **Ctrl + Shift + F10** - Aktif dosyayı run et
- **Ctrl + F2** - Çalışan uygulamayı durdur

### Toolbar:
1. Üst toolbar'da run configuration dropdown'ı açın
2. **BIST Trading Platform** seçin
3. **▶️ Run** butonuna tıklayın

## ✅ Başarılı Başlatma Kontrolü

### Console'da Görecekleriniz:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.3.4)

2025-10-10 12:30:45.123  INFO 12345 --- [main] c.b.BistTradingPlatformApplication       : Starting BistTradingPlatformApplication
2025-10-10 12:30:45.456  INFO 12345 --- [main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Starting...
2025-10-10 12:30:45.789  INFO 12345 --- [main] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Start completed.
2025-10-10 12:30:46.123  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2025-10-10 12:30:46.456  INFO 12345 --- [main] c.b.BistTradingPlatformApplication       : Started BistTradingPlatformApplication in 2.5 seconds

🚀 BIST Trading Platform STARTED
📊 Swagger UI: http://localhost:8080/swagger-ui.html
```

### Test Endpoint'leri:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

## 🐛 Sorun Giderme

### Problem 1: Main Class Bulunamıyor

**Hata:** `Error: Could not find or load main class`

**Çözüm:**
1. Gradle'ı yeniden sync edin: **Gradle → Reload All Gradle Projects**
2. **File → Invalidate Caches → Invalidate and Restart**
3. **Build → Rebuild Project**

### Problem 2: Database Connection Error

**Hata:** `Connection to localhost:5432 refused`

**Çözüm:**
```bash
# PostgreSQL'in çalıştığını kontrol et
docker ps | grep postgres

# Başlat
docker-compose up -d postgres

# Bağlantıyı test et
docker exec -it bist-trading-platform-postgres-1 psql -U bist_user -d bist_trading
```

### Problem 3: Port Already in Use

**Hata:** `Port 8080 is already in use`

**Çözüm:**
```bash
# Port 8080'i kullanan process'i bul
lsof -i :8080

# Process'i durdur
kill -9 <PID>
```

### Problem 4: Redis Error

**Hata:** `Cannot get Jedis connection`

**Çözüm:**
Environment variables'a ekleyin:
```
SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

## 🎨 IntelliJ Plugins (Önerilir)

Geliştirme deneyimini iyileştirecek plugin'ler:

1. **Lombok** - Lombok annotations için
2. **Spring Boot Assistant** - Spring Boot özellikler için
3. **Database Tools and SQL** - Database yönetimi için (built-in)
4. **Rainbow Brackets** - Kod okunabilirliği için
5. **String Manipulation** - String işlemleri için

**Kurulum:** File → Settings → Plugins → Marketplace'den arayın

## 🔥 Hot Reload (DevTools)

Spring Boot DevTools aktif, bu yüzden:

1. Kod değiştirin
2. **Build → Build Project** (Ctrl+F9)
3. Uygulama otomatik restart olacak

Veya:
- **Run → Update Running Application** (Ctrl+F10)

## 📊 Useful IntelliJ Views

### Tool Windows:
- **Alt+1** - Project view
- **Alt+4** - Run console
- **Alt+5** - Debug console
- **Alt+6** - TODO list
- **Alt+7** - Structure
- **Alt+9** - Git

### Debugging:
1. Breakpoint ekle (satır numarasına tıkla)
2. **Debug** butonuyla başlat (Shift+F9)
3. Variables, Watches, Console tab'lerini kullan

## 🎓 Faydalı Shortcuts

| Kısayol | Açıklama |
|---------|----------|
| **Shift+F10** | Run |
| **Shift+F9** | Debug |
| **Ctrl+F2** | Stop |
| **Ctrl+F10** | Update running application |
| **Ctrl+F9** | Build project |
| **Cmd+Shift+A** | Find action (her şeyi ara!) |
| **Cmd+E** | Recent files |
| **Cmd+Shift+F** | Find in files |
| **Double Shift** | Search everywhere |

## ✅ Checklist

Başlatmadan önce kontrol edin:

- [ ] Java 21 kurulu ve IntelliJ'de seçili
- [ ] Gradle sync tamamlandı
- [ ] PostgreSQL container çalışıyor
- [ ] Environment variables doğru girildi
- [ ] Run configuration oluşturuldu
- [ ] Main class doğru seçildi
- [ ] Module doğru seçildi (.main)

## 🎉 Başarılı!

Artık IntelliJ'den kolayca Spring Boot uygulamanızı başlatabilirsiniz!

**Keyifli kodlamalar!** 🚀

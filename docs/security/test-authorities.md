# Test Authorities Fix - "onur" Kullanıcısı

## ✅ Yapılan Değişiklikler

### 1. CustomUserDetailsService.java Güncellendi

**Eski Durum** ❌:
- `determineUserRoles()` ve `determineUserPermissions()` metodları hardcoded mantık kullanıyordu
- Email domain'e veya professional investor flag'ine bakarak roller atanıyordu
- Database'deki `authorities` kolonu **hiç okunmuyordu**!

**Yeni Durum** ✅:
- `mapToCustomUserDetails()` artık database'deki `authorities` kolonunu okuyor
- `parseAuthoritiesFromDatabase()` metodu authorities string'ini parse ediyor
- Format: `"ROLE_ADMIN,ROLE_TRADER,market:read,portfolio:read,..."`
- Rolleri ve permission'ları doğru şekilde ayırıyor:
  - `ROLE_` ile başlayanlar → roles
  - Diğerleri (özellikle `permission:action` formatı) → permissions

### 2. Authority Parsing Mantığı

```java
// Database'den okunan string:
"ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,trading:place,trading:modify,trading:cancel,portfolio:read,orders:read"

// Parse ediliyor:
roles = ["ADMIN", "TRADER", "USER"]  // ROLE_ prefix kaldırılıyor
permissions = ["market:read", "trading:read", "trading:place", "trading:modify", "trading:cancel", "portfolio:read", "orders:read"]

// Spring Security'ye veriliyor:
authorities = ["ROLE_ADMIN", "ROLE_TRADER", "ROLE_USER", "market:read", "trading:read", ...]
```

## 🧪 Test Adımları

### 1. Backend'i Yeniden Başlatın

```bash
cd /Users/onurerdogan/dev/bist-trading-platform

# Mevcut backend'i durdurun
pkill -f bootRun

# Yeniden başlatın
./gradlew bootRun
```

**VEYA** hızlı başlatma scriptleri:

```bash
./stop-app.sh
./start-monolith.sh
```

### 2. CLI Token'larını Temizleyin

```bash
cd cli-client
source venv/bin/activate
python -m bist_cli.main --clear-tokens
```

### 3. CLI'yı Başlatın ve Login Yapın

```bash
./start.sh
```

```
Kullanıcı Adı: onur
Şifre: (şifreniz)
```

### 4. Broker İşlemlerini Test Edin

```
Ana Menü → 2. Broker İşlemleri
         → 2. Açık Pozisyonlar
```

**Beklenen Sonuç**: ✅ Artık 403 hatası almamalısınız!

### 5. Backend Loglarını Kontrol Edin

Terminal'de backend loglarında şunu göreceksiniz:

```
DEBUG ... CustomUserDetailsService - Parsed authorities - roles: [ADMIN, TRADER, USER], permissions: [market:read, trading:read, trading:place, trading:modify, trading:cancel, portfolio:read, orders:read]
DEBUG ... JwtAuthenticationFilter - JWT authentication başarılı - userId: 30d094a1-96f6-4327-ab74-c185da8a007a
DEBUG ... BrokerController - Instant positions request from user: onur
```

**403 hatası OLMAMALI** ✅

## 🔍 Sorun Giderme

### Hala 403 Alıyorsanız:

1. **Backend yeniden başladı mı kontrol edin**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Database'de authorities var mı kontrol edin**:
   ```bash
   docker exec bist-postgres psql -U bist_user -d bist_trading -c "SELECT username, authorities FROM user_entities WHERE username = 'onur';"
   ```

   Beklenen:
   ```
   username | authorities
   ---------+---------------------------------------------------------------
   onur     | ROLE_ADMIN,ROLE_TRADER,ROLE_USER,market:read,trading:read,...
   ```

3. **CLI token'ları tamamen temizlenmiş mi**:
   ```bash
   # macOS için:
   security delete-generic-password -s bist-trading-access-token 2>/dev/null || echo "Token zaten yok"
   security delete-generic-password -s bist-trading-refresh-token 2>/dev/null || echo "Token zaten yok"
   ```

4. **Yeni login yapıldı mı kontrol edin**:
   - CLI'dan çıkın ve yeniden girin
   - Logout yapıp login yapın

## 🎯 Teknik Detaylar

### CustomUserDetails.getAuthorities() Nasıl Çalışıyor?

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    // Roles: ["ADMIN", "TRADER", "USER"]
    // Spring Security için "ROLE_" prefix ekleniyor: ["ROLE_ADMIN", "ROLE_TRADER", "ROLE_USER"]
    Set<GrantedAuthority> authorities = roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toSet());

    // Permissions: ["market:read", "portfolio:read", ...]
    // Aynen ekleniyor (prefix yok)
    if (permissions != null) {
        permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
    }

    // Final authorities:
    // ["ROLE_ADMIN", "ROLE_TRADER", "ROLE_USER", "market:read", "trading:read", "portfolio:read", ...]
    return authorities;
}
```

### Spring Security @PreAuthorize Kontrolü

```java
@GetMapping("/positions")
@PreAuthorize("hasAuthority('portfolio:read')")  // ✅ Artık geçecek!
public ResponseEntity<AlgoLabResponse<Object>> getInstantPositions(...)
```

Spring Security:
1. JWT token'dan user ID'yi alıyor
2. `CustomUserDetailsService.loadUserByUserId()` çağırıyor
3. Bu metod database'den user'ı yüklüyor ve `authorities` kolonunu parse ediyor
4. Parse edilen authorities Spring Security'ye veriliyor
5. `hasAuthority('portfolio:read')` kontrolü yapılıyor
6. ✅ "portfolio:read" bulunuyor → 200 OK dönüyor

## 📊 Sonuç

**Root Cause**: CustomUserDetailsService database'deki authorities kolonunu okumuyordu.

**Fix**: `mapToCustomUserDetails()` metoduna `parseAuthoritiesFromDatabase()` eklendi.

**Impact**: Artık tüm kullanıcılar için database'deki authorities doğru şekilde yükleniyor.

---

**Test Tarihi**: 2025-10-16
**Fix Committed**: CustomUserDetailsService.java
**Status**: ✅ Ready for Testing

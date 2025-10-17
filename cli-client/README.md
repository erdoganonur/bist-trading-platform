# BIST Trading Platform - CLI Client

🚀 **Interaktif Python CLI Client** - BIST Trading Platform için komut satırı arayüzü

## ✨ Özellikler

- 🔐 **Güvenli Kimlik Doğrulama**: JWT token tabanlı oturum yönetimi
- 📱 **AlgoLab Entegrasyonu**: OTP ile iki faktörlü broker kimlik doğrulama
- 📊 **Gerçek Zamanlı Piyasa Verileri**: Hisse fiyatları ve teknik göstergeler
- 💼 **Broker İşlemleri**: Hesap bilgileri ve portföy görüntüleme
- 🎨 **Modern UI**: Rich console ile renkli ve interaktif arayüz
- 💾 **Güvenli Token Saklama**: Keyring ile işletim sistemi güvenlik mekanizması

## 📦 Kurulum

### Gereksinimler

- Python 3.10 veya üstü
- BIST Trading Platform API (localhost:8080)

### Adımlar

```bash
# 1. CLI client dizinine gidin
cd cli-client

# 2. Virtual environment oluşturun
python3 -m venv venv

# 3. Virtual environment'ı aktif edin
# macOS/Linux:
source venv/bin/activate
# Windows:
# venv\Scripts\activate

# 4. Bağımlılıkları yükleyin
pip install -r requirements.txt

# 5. Konfigürasyon dosyasını oluşturun
cp .env.example .env

# 6. .env dosyasını düzenleyin (isteğe bağlı)
# vim .env
```

## 🚀 Kullanım

### Başlatma

```bash
# CLI client'ı başlat
python -m bist_cli.main

# veya
python bist_cli/main.py
```

### İlk Giriş Akışı

1. **Kullanıcı Girişi**
   - Kullanıcı adı ve şifrenizi girin
   - Sistem JWT token alır ve güvenli şekilde saklar

2. **AlgoLab Kimlik Doğrulama** (İsteğe bağlı - Broker işlemleri için gerekli)

   a. **Login (Adım 1)**
   - Ana menüden "4. AlgoLab Bağlantısı" seçin
   - AlgoLab broker kullanıcı adı ve şifrenizi girin
   - Backend AlgoLab'a bağlanır ve SMS kodu gönderir

   b. **OTP Doğrulama (Adım 2)**
   - Telefonunuza gelen SMS kodunu girin (4-8 hane)
   - Backend OTP'yi doğrular
   - AlgoLab session oluşturulur (24 saat geçerli)

   c. **Başarılı Bağlantı**
   - Artık broker işlemlerini kullanabilirsiniz
   - Hesap bilgileri, pozisyonlar, vb. erişilebilir

3. **Ana Menü**
   - İnteraktif menü ile fonksiyonlara erişin

### Ana Menü Seçenekleri

```
BIST Trading Platform - Ana Menü
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1.  📊 Piyasa Verileri
2.  💼 Broker İşlemleri
3.  👤 Profil Bilgileri
4.  🔐 AlgoLab Bağlantısı
5.  ⚙️  Ayarlar
6.  🚪 Çıkış
```

## 🛠️ Geliştirme

### Proje Yapısı

```
cli-client/
├── bist_cli/
│   ├── __init__.py         # Package initialization
│   ├── main.py             # Ana giriş noktası
│   ├── config.py           # Konfigürasyon yönetimi
│   ├── api_client.py       # HTTP API client
│   ├── auth.py             # Kimlik doğrulama akışları
│   ├── menu.py             # Ana menü ve UI
│   ├── market_data.py      # Piyasa verisi fonksiyonları
│   ├── broker.py           # Broker işlemleri
│   └── utils.py            # Yardımcı fonksiyonlar
├── requirements.txt        # Python bağımlılıkları
├── .env.example            # Örnek konfigürasyon
├── .env                    # Kullanıcı konfigürasyonu (git'de yok)
└── README.md               # Bu dosya
```

### Test

```bash
# Bağlantı testi
python -m bist_cli.main --test-connection

# Verbose mode
python -m bist_cli.main --verbose
```

## 📋 API Endpoint'leri

CLI client şu API endpoint'lerini kullanır:

### Kullanıcı Yönetimi
- `POST /api/v1/auth/login` - Kullanıcı girişi (JWT token)
- `POST /api/v1/auth/refresh` - Token yenileme
- `GET /api/v1/users/profile` - Profil bilgileri

### AlgoLab Broker Authentication
- `POST /api/v1/broker/auth/login` - AlgoLab login (SMS tetikler)
  - Request: `{ "username": "...", "password": "..." }`
  - Response: `{ "success": true, "smsSent": true, "message": "..." }`
- `POST /api/v1/broker/auth/verify-otp` - OTP doğrulama
  - Request: `{ "otpCode": "123456" }`
  - Response: `{ "success": true, "authenticated": true, "sessionExpiresAt": "..." }`
- `GET /api/v1/broker/auth/status` - Bağlantı durumu kontrolü

### Piyasa Verileri
- `GET /api/v1/symbols` - Hisse sembolleri (paginated)
- `GET /api/v1/symbols/search?q=AKBNK` - Sembol ara
- `GET /api/v1/symbols/{symbol}` - Sembol detayı

### Broker İşlemleri (AlgoLab auth gerekli)
- `GET /api/v1/broker/positions` - Açık pozisyonlar
- `GET /api/v1/broker/portfolio` - Portfolio bilgileri
- `GET /api/v1/broker/status` - Broker bağlantı durumu

## 🔒 Güvenlik

- JWT token'lar sistem keyring'inde saklanır
- Şifreler asla dosyaya yazılmaz
- Otomatik session timeout (30 dakika)
- Token auto-refresh desteği

## 🐛 Sorun Giderme

### "ModuleNotFoundError" hatası

```bash
# Virtual environment'ın aktif olduğundan emin olun
source venv/bin/activate
pip install -r requirements.txt
```

### "Connection refused" hatası

```bash
# BIST Trading Platform API'sinin çalıştığından emin olun
curl http://localhost:8080/actuator/health
```

### Token sorunları

```bash
# Saved token'ları temizle
python -m bist_cli.main --clear-tokens
```

### AlgoLab authentication sorunları

**"Token bulunamadı" hatası:**
- Önce `/login` endpoint'i çağrılmadan `/verify-otp` çağrılıyor
- AlgoLab menüsünden çıkıp tekrar girin

**"Geçersiz OTP" hatası:**
- SMS kodunun doğru girildiğinden emin olun
- OTP genelde 5 dakika içinde geçersiz olur
- Gerekirse işlemi baştan başlatın

**Debug mode için:**
```bash
# Detaylı hata logları ile çalıştır
DEBUG=1 python -m bist_cli.main

# veya
VERBOSE=1 python -m bist_cli.main
```

## 📞 Destek

Sorularınız için:
- Email: support@bisttrading.com
- GitHub Issues: [BIST Trading Platform](https://github.com/bisttrading/platform)

## 📄 Lisans

Proprietary - BIST Trading Platform
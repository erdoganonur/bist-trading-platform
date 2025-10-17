#!/bin/bash
# Test 403 Fix for "onur" user

echo "🧪 Testing 403 Fix..."
echo ""

# 1. Clear tokens
echo "1️⃣ Clearing old tokens..."
source venv/bin/activate
python -m bist_cli.main --clear-tokens
echo ""

# 2. Test login and authorities
echo "2️⃣ Starting CLI to test login..."
echo ""
echo "📝 TEST ADIMLAR:"
echo "   1. Kullanıcı adı: onur"
echo "   2. Şifre: (şifrenizi girin)"
echo "   3. Ana Menü → 2. Broker İşlemleri"
echo "   4. 2. Açık Pozisyonlar"
echo ""
echo "✅ BEKLENEN: 403 hatası OLMAMALI"
echo ""

# Start CLI
python -m bist_cli.main

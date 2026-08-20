# ChatTabs

Android'de sık kullanılan ChatGPT konuşmalarını sekme/kısayol gibi açmak için küçük, reklamsız ve çevrimdışı yardımcı uygulama.

## Özellikler
- Yalnızca gerçek özel konuşma adreslerini kabul eder: `https://chatgpt.com/c/...`
- `/share/` bağlantılarını reddeder; bunlar canlı konuşmanın devam bağlantısı değil, paylaşım görüntüsüdür.
- Linki önce resmi ChatGPT Android uygulamasında açmayı dener, olmazsa tarayıcıya düşer.
- Ana ekran widget'ında ilk 6 konuşmayı tek satırda gösterir.
- ChatTabs ikonuna uzun basıldığında ilk 4 konuşmayı dinamik kısayol olarak sunar.
- Ekle, düzenle, sil ve yukarı/aşağı sıralama vardır.
- Tarayıcıdan metin/link paylaşımıyla yeni konuşma eklenebilir.
- INTERNET izni istemez. Kayıtlar cihazdaki SharedPreferences içinde tutulur.

## Kullanım
1. ChatGPT'yi tarayıcıda aç.
2. Devam etmek istediğin konuşmaya gir.
3. Adres çubuğundaki `https://chatgpt.com/c/...` adresini kopyala.
4. ChatTabs > Konuşma ekle > Panodan yapıştır.
5. Ana ekrana uzun bas > Widget'lar > ChatTabs ile tek satır widget'ı ekle.

## Derleme
GitHub Actions iş akışı `Build Android APK` adıyla debug APK üretir ve `ChatTabs-debug-apk` artifact'ı olarak yükler.

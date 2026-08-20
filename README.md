# ChatTabs 2.0

ChatTabs, resmî ChatGPT Android uygulamasındaki favori konuşmaları sekme gibi açmak için küçük bir native switcher'dır.

- WebView yoktur.
- Tarayıcı fallback yoktur.
- `com.openai.chatgpt` dışında Accessibility işlemi yapmaz.
- Konuşma başlığı tek başına yeterlidir; `/c/` linki isteğe bağlıdır.
- Link varsa önce resmî ChatGPT uygulamasına native deep-link denenir.
- Deep-link cihazda/uygulamada konuşmaya gitmezse ChatTabs Native Switch, ChatGPT arayüzünde kayıtlı başlığı bulup açmayı dener.
- İlk 6 favori ana ekran widget'ında, ilk 4 favori uygulama ikonunun uzun-bas kısayollarında görünür.
- ChatTabs mesaj içeriğini kaydetmez.

## İlk kurulum
1. ChatTabs'i aç.
2. Native Switch erişimini aç düğmesine bas.
3. Android Erişilebilirlik ekranında ChatTabs Native Switch'i etkinleştir.
4. ChatGPT'deki sohbet başlıklarını ChatTabs'e favori olarak ekle.

package com.alpay.chattabs;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

public class OpenConversationActivity extends Activity {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_URL = "url";
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = safe(getIntent().getStringExtra(EXTRA_TITLE));
        String url = safe(getIntent().getStringExtra(EXTRA_URL));
        openNative(title, url);
        finish();
    }

    private void openNative(String title, String url) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(CHATGPT_PACKAGE);
        if (launch == null) {
            Toast.makeText(this, "Resmî ChatGPT Android uygulaması bulunamadı.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!title.isEmpty()) SwitchRequestStore.request(this, title, url);

        if (!url.isEmpty() && UrlTools.isPrivateConversationUrl(url)) {
            Intent deepLink = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            deepLink.setPackage(CHATGPT_PACKAGE);
            deepLink.addCategory(Intent.CATEGORY_BROWSABLE);
            deepLink.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                startActivity(deepLink);
                if (!title.isEmpty() && !isNativeSwitchEnabled(this)) {
                    Toast.makeText(this, "ChatGPT açıldı. Doğrudan sohbet açılmazsa ChatTabs içinden Native Switch'i bir kez etkinleştir.", Toast.LENGTH_LONG).show();
                }
                return;
            } catch (ActivityNotFoundException ignored) {
                // Browser fallback is intentionally forbidden. Continue with the official app only.
            }
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(launch);
        if (!title.isEmpty() && !isNativeSwitchEnabled(this)) {
            Toast.makeText(this, "Native sohbet seçimi için ChatTabs'ta Native Switch erişimini bir kez etkinleştir.", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isNativeSwitchEnabled(Context context) {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String expected = new ComponentName(context, ChatSwitchAccessibilityService.class).flattenToString();
        for (String part : enabled.split(":")) {
            if (TextUtils.equals(part, expected)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

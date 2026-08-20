package com.alpay.chattabs;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class OpenConversationActivity extends Activity {
    public static final String EXTRA_URL = "url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) {
            finish();
            return;
        }
        open(url);
        finish();
    }

    private void open(String url) {
        Uri uri = Uri.parse(url);
        Intent chatGpt = new Intent(Intent.ACTION_VIEW, uri);
        chatGpt.setPackage("com.openai.chatgpt");
        chatGpt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(chatGpt);
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        Intent fallback = new Intent(Intent.ACTION_VIEW, uri);
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(fallback);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bu bağlantıyı açabilecek bir uygulama bulunamadı.", Toast.LENGTH_LONG).show();
        }
    }
}

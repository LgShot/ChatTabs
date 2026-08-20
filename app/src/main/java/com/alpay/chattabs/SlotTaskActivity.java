package com.alpay.chattabs;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Root activity kept underneath one independent ChatGPT task slot. */
public class SlotTaskActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String label = getIntent().getStringExtra(OpenConversationActivity.EXTRA_TITLE);
        if (label == null || label.trim().isEmpty()) label = "ChatTabs";
        label = label.trim();
        setTitle(label);
        try {
            setTaskDescription(new ActivityManager.TaskDescription(label, null, getColor(R.color.bg)));
        } catch (Exception ignored) {}

        if (savedInstanceState == null) {
            Intent chatGpt = OpenConversationActivity.chatGptLaunchIntent(this);
            if (chatGpt == null) {
                Toast.makeText(this, "Resmî ChatGPT Android uygulaması bulunamadı.", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                startActivity(chatGpt);
            } catch (Exception e) {
                Toast.makeText(this, "ChatGPT bu bağımsız task içinde başlatılamadı.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

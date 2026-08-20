package com.alpay.chattabs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public final class ShortcutPublisher {
    private ShortcutPublisher() {}

    public static void publish(Context context, List<Conversation> items) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        if (manager == null) return;
        List<ShortcutInfo> shortcuts = new ArrayList<>();
        int limit = Math.min(4, items.size());
        for (int i = 0; i < limit; i++) {
            Conversation item = items.get(i);
            Intent intent = new Intent(context, OpenConversationActivity.class)
                    .setAction("OPEN_SLOT_" + item.id)
                    .setData(Uri.parse(OpenConversationActivity.slotUri(item.id)))
                    .putExtra(OpenConversationActivity.EXTRA_ID, item.id)
                    .putExtra(OpenConversationActivity.EXTRA_TITLE, item.title);
            String shortLabel = item.title.length() > 20 ? item.title.substring(0, 20) : item.title;
            shortcuts.add(new ShortcutInfo.Builder(context, "slot_" + item.id)
                    .setShortLabel(shortLabel)
                    .setLongLabel(item.title)
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_round))
                    .setIntent(intent)
                    .build());
        }
        try { manager.setDynamicShortcuts(shortcuts); } catch (Exception ignored) {}
    }
}

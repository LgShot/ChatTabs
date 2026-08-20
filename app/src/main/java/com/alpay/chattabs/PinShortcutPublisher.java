package com.alpay.chattabs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;

public final class PinShortcutPublisher {
    private PinShortcutPublisher() {}

    public static void request(Context context, Conversation item) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, "Ana ekran kısayolu Android 8+ gerektiriyor.", Toast.LENGTH_LONG).show();
            return;
        }
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) {
            Toast.makeText(context, "Bu launcher sabit kısayol eklemeyi desteklemiyor.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent open = new Intent(context, OpenConversationActivity.class)
                .setAction("PINNED_SLOT_" + item.id)
                .setData(android.net.Uri.parse(OpenConversationActivity.slotUri(item.id)))
                .putExtra(OpenConversationActivity.EXTRA_ID, item.id)
                .putExtra(OpenConversationActivity.EXTRA_TITLE, item.title);

        String shortLabel = item.title.length() > 20 ? item.title.substring(0, 20) : item.title;
        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "pinned_slot_" + item.id)
                .setShortLabel(shortLabel)
                .setLongLabel(item.title)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_round))
                .setIntent(open)
                .build();

        PendingIntent callback = PendingIntent.getBroadcast(
                context,
                (int) (item.id & 0x7fffffff),
                new Intent("com.alpay.chattabs.PIN_RESULT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.requestPinShortcut(shortcut, callback.getIntentSender());
    }
}

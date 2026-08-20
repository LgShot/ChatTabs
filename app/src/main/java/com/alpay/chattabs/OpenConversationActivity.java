package com.alpay.chattabs;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;

/**
 * Lightweight router for one saved ChatTabs slot.
 *
 * Each slot owns a distinct Android document task whose base intent data is
 * chattabs://slot/<id>. The official ChatGPT launcher activity is then pushed
 * on top of that task without FLAG_ACTIVITY_NEW_TASK. If Android/ChatGPT honors
 * normal task semantics, each slot keeps an independent ChatGPT activity stack.
 */
public class OpenConversationActivity extends Activity {
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_URL = "url"; // v2 pinned-shortcut compatibility
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getLongExtra(EXTRA_ID, -1L);
        String title = safe(getIntent().getStringExtra(EXTRA_TITLE));
        if (id < 0L) {
            // v2 shortcuts carried title/url but no slot id. Resolve them against
            // the local list so previously placed home-screen icons keep working.
            String oldUrl = safe(getIntent().getStringExtra(EXTRA_URL));
            for (Conversation item : ConversationStore.load(this)) {
                if ((!title.isEmpty() && title.equals(item.title))
                        || (!oldUrl.isEmpty() && oldUrl.equals(item.url))) {
                    id = item.id;
                    if (title.isEmpty()) title = item.title;
                    break;
                }
            }
        }
        if (id < 0L) {
            Toast.makeText(this, "Bu eski kısayol artık eşleşmiyor. ChatTabs'tan yeniden ekle.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        openOrCreateSlot(id, title);
        finish();
    }

    private void openOrCreateSlot(long id, String title) {
        String slotUri = slotUri(id);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            List<ActivityManager.AppTask> tasks = manager.getAppTasks();
            for (ActivityManager.AppTask task : tasks) {
                try {
                    ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                    Intent base = info.baseIntent;
                    Uri data = base == null ? null : base.getData();
                    if (data != null && slotUri.equals(data.toString())) {
                        task.moveToFront();
                        ComponentName top = info.topActivity;
                        if (top == null || !CHATGPT_PACKAGE.equals(top.getPackageName())) {
                            Intent chatGpt = chatGptLaunchIntent();
                            if (chatGpt != null) task.startActivity(this, chatGpt);
                        }
                        return;
                    }
                } catch (Exception ignored) {
                    // A stale task should not prevent opening the slot again.
                }
            }
        }

        Intent create = new Intent(this, SlotTaskActivity.class)
                .setAction("CREATE_SLOT_" + id)
                .setData(Uri.parse(slotUri))
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITLE, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS);
        startActivity(create);
    }

    static Intent chatGptLaunchIntent(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(CHATGPT_PACKAGE);
        if (launch == null) return null;
        // Critical: keep ChatGPT inside the slot task instead of jumping to its
        // existing global task. If ChatGPT declares a singleTask/singleInstance
        // launcher, Android may override this; v3 exists specifically to test it.
        launch.setFlags(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS);
        return launch;
    }

    private Intent chatGptLaunchIntent() {
        Intent launch = chatGptLaunchIntent(this);
        if (launch == null) {
            Toast.makeText(this, "Resmî ChatGPT Android uygulaması bulunamadı.", Toast.LENGTH_LONG).show();
        }
        return launch;
    }

    public static String slotUri(long id) {
        return "chattabs://slot/" + id;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
